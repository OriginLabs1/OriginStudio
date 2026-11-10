package com.oneorigin.studio.origin

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The portable workspace manifest. It is deliberately boring: a small JSON file beside the
 * user's plain files, readable without Origin Studio.
 */
data class WorkspaceManifest(
    val id: String,
    val name: String,
    val createdAt: String,
    val owners: List<String>,
    val defaultDataFlowClass: Int = 0,
    val policy: String = "local-authoritative",
    val schema: String = SCHEMA,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("schema", schema)
        .put("id", id)
        .put("name", name)
        .put("createdAt", createdAt)
        .put("owners", JSONArray(owners))
        .put("defaultDataFlowClass", defaultDataFlowClass)
        .put("policy", policy)

    fun canonicalBytes(): ByteArray = toJson().toString().toByteArray(Charsets.UTF_8)

    companion object {
        const val SCHEMA = "origin.workspace/v1"

        fun fromJson(json: JSONObject): WorkspaceManifest {
            val schema = json.optString("schema")
            require(schema == SCHEMA) { "unsupported workspace schema: $schema" }
            val ownersJson = json.optJSONArray("owners") ?: JSONArray()
            val dataFlow = json.optInt("defaultDataFlowClass", 0)
            require(dataFlow in 0..4) { "defaultDataFlowClass must be 0..4" }
            return WorkspaceManifest(
                id = json.getString("id"),
                name = json.getString("name"),
                createdAt = json.getString("createdAt"),
                owners = List(ownersJson.length()) { ownersJson.getString(it) },
                defaultDataFlowClass = dataFlow,
                policy = json.optString("policy", "local-authoritative"),
                schema = schema,
            )
        }
    }
}

/** One append-only, hash-chained workspace event. */
data class WorkspaceEvent(
    val seq: Long,
    val at: String,
    val type: String,
    val payload: Map<String, String>,
    val prev: String,
    val hash: String,
) {
    /** The exact bytes that are hashed. Field order is fixed and payload keys are sorted. */
    fun canonical(): String = buildString {
        append(seq).append('\n')
        append(at).append('\n')
        append(type).append('\n')
        append(prev).append('\n')
        append(Hashing.canonicalMap(payload))
    }

    fun toJson(): JSONObject {
        val payloadJson = JSONObject()
        payload.toSortedMap().forEach { (key, value) -> payloadJson.put(key, value) }
        return JSONObject()
            .put("seq", seq)
            .put("at", at)
            .put("type", type)
            .put("payload", payloadJson)
            .put("prev", prev)
            .put("hash", hash)
    }

    companion object {
        fun fromJson(json: JSONObject): WorkspaceEvent {
            val payloadJson = json.optJSONObject("payload") ?: JSONObject()
            val payload = buildMap {
                payloadJson.keys().forEach { key -> put(key, payloadJson.optString(key)) }
            }
            return WorkspaceEvent(
                seq = json.getLong("seq"),
                at = json.getString("at"),
                type = json.getString("type"),
                payload = payload,
                prev = json.getString("prev"),
                hash = json.getString("hash"),
            )
        }
    }
}

/** A single-writer lease. Handoff is a lease transfer, not live process migration. */
data class WorkspaceLease(
    val workspaceId: String,
    val hostId: String,
    val epoch: Long,
    val acquiredAt: String,
    val expiresAt: String,
) {
    fun isActive(now: Instant = Instant.now()): Boolean = Instant.parse(expiresAt).isAfter(now)

    fun toJson(): JSONObject = JSONObject()
        .put("workspaceId", workspaceId)
        .put("hostId", hostId)
        .put("epoch", epoch)
        .put("acquiredAt", acquiredAt)
        .put("expiresAt", expiresAt)

    companion object {
        fun fromJson(json: JSONObject): WorkspaceLease = WorkspaceLease(
            workspaceId = json.getString("workspaceId"),
            hostId = json.getString("hostId"),
            epoch = json.getLong("epoch"),
            acquiredAt = json.getString("acquiredAt"),
            expiresAt = json.getString("expiresAt"),
        )
    }
}

data class WorkspaceVerification(
    val ok: Boolean,
    val manifestHash: String,
    val eventCount: Int,
    val lease: WorkspaceLease? = null,
    val error: String? = null,
)

class LeaseConflictException(message: String) : IllegalStateException(message)

/**
 * A workspace on disk:
 *
 * ```
 * project/
 *   ... plain files ...
 *   .origin/
 *     manifest.json
 *     events.jsonl
 *     checkpoints/
 *     lease.json        (transient, excluded from export)
 * ```
 *
 * The class is intentionally free of Android imports so it can be tested on the JVM and used
 * by a desktop or remote host.
 */
class OriginWorkspace private constructor(val dir: File) {

    val originDir: File = File(dir, DIR)
    val manifestFile: File get() = File(originDir, "manifest.json")
    val eventsFile: File get() = File(originDir, "events.jsonl")
    val leaseFile: File get() = File(originDir, "lease.json")
    val checkpointsDir: File get() = File(originDir, "checkpoints")

    fun ensureLayout() {
        dir.mkdirs()
        originDir.mkdirs()
        checkpointsDir.mkdirs()
    }

    fun manifest(): WorkspaceManifest =
        WorkspaceManifest.fromJson(JSONObject(manifestFile.readText()))

    @Synchronized
    fun appendEvent(
        type: String,
        payload: Map<String, String> = emptyMap(),
        now: Instant = Instant.now(),
    ): WorkspaceEvent {
        require(type.isNotBlank()) { "event type must not be blank" }
        val events = readEvents()
        val prev = events.lastOrNull()?.hash ?: GENESIS
        val seq = (events.lastOrNull()?.seq ?: 0L) + 1
        val draft = WorkspaceEvent(
            seq = seq,
            at = now.toString(),
            type = type,
            payload = payload,
            prev = prev,
            hash = "",
        )
        val hash = Hashing.sha256Hex(prev + draft.canonical())
        val event = draft.copy(hash = hash)
        eventsFile.appendText(event.toJson().toString() + "\n")
        return event
    }

    fun events(): List<WorkspaceEvent> = readEvents()

    private fun readEvents(): List<WorkspaceEvent> {
        if (!eventsFile.isFile) return emptyList()
        return eventsFile.readLines()
            .filter { it.isNotBlank() }
            .map { WorkspaceEvent.fromJson(JSONObject(it)) }
    }

    /**
     * Verifies the manifest and every event. A truncated, reordered or edited log fails.
     * The caller decides what to do with a failed verification; this method never repairs.
     */
    fun verify(now: Instant = Instant.now()): WorkspaceVerification {
        return runCatching {
            require(manifestFile.isFile) { "manifest.json is missing" }
            val manifest = manifest()
            require(manifest.id.isNotBlank()) { "workspace id is blank" }
            val events = readEvents()
            var prev = GENESIS
            events.forEachIndexed { index, event ->
                require(event.seq == index + 1L) { "event ${index + 1} has seq ${event.seq}" }
                require(event.prev == prev) { "event ${event.seq} has a broken prev link" }
                val expected = Hashing.sha256Hex(event.prev + event.canonical())
                require(expected == event.hash) { "event ${event.seq} has an invalid hash" }
                prev = event.hash
            }
            val lease = lease()
            if (lease != null && lease.workspaceId != manifest.id) {
                throw IllegalStateException("lease belongs to another workspace")
            }
            WorkspaceVerification(
                ok = true,
                manifestHash = Hashing.sha256Hex(manifestFile.readBytes()),
                eventCount = events.size,
                lease = lease?.takeIf { it.isActive(now) },
            )
        }.getOrElse { error ->
            WorkspaceVerification(
                ok = false,
                manifestHash = runCatching { Hashing.sha256Hex(manifestFile.readBytes()) }.getOrDefault(""),
                eventCount = runCatching { readEvents().size }.getOrDefault(0),
                error = error.message ?: error::class.java.simpleName,
            )
        }
    }

    fun lease(): WorkspaceLease? {
        if (!leaseFile.isFile) return null
        return runCatching { WorkspaceLease.fromJson(JSONObject(leaseFile.readText())) }.getOrNull()
    }

    /**
     * Acquires the single-writer lease. A different host must wait for expiry or use
     * [transferLease]; this is what prevents two devices silently overwriting each other.
     */
    @Synchronized
    fun acquireLease(
        hostId: String,
        ttlMillis: Long = DEFAULT_LEASE_TTL_MILLIS,
        now: Instant = Instant.now(),
    ): WorkspaceLease {
        require(hostId.isNotBlank()) { "hostId must not be blank" }
        require(ttlMillis > 0) { "ttlMillis must be positive" }
        val current = lease()
        if (current != null && current.isActive(now) && current.hostId != hostId) {
            throw LeaseConflictException(
                "workspace is leased to ${current.hostId} until ${current.expiresAt}"
            )
        }
        val next = WorkspaceLease(
            workspaceId = manifest().id,
            hostId = hostId,
            epoch = (current?.epoch ?: 0L) + 1,
            acquiredAt = now.toString(),
            expiresAt = now.plusMillis(ttlMillis).toString(),
        )
        writeAtomically(leaseFile, next.toJson().toString().toByteArray(Charsets.UTF_8))
        appendEvent("lease.acquired", mapOf("host" to hostId, "epoch" to next.epoch.toString()), now)
        return next
    }

    @Synchronized
    fun releaseLease(hostId: String, now: Instant = Instant.now()): Boolean {
        val current = lease() ?: return false
        if (current.hostId != hostId) return false
        leaseFile.delete()
        appendEvent("lease.released", mapOf("host" to hostId, "epoch" to current.epoch.toString()), now)
        return true
    }

    /**
     * Explicit handoff: the current holder gives the lease to another host. The caller is
     * responsible for checkpointing and for telling the user the active writer changed.
     */
    @Synchronized
    fun transferLease(
        fromHostId: String,
        toHostId: String,
        ttlMillis: Long = DEFAULT_LEASE_TTL_MILLIS,
        now: Instant = Instant.now(),
    ): WorkspaceLease {
        require(fromHostId != toHostId) { "handoff needs two different hosts" }
        val current = lease() ?: throw LeaseConflictException("no active lease to transfer")
        if (current.hostId != fromHostId) {
            throw LeaseConflictException("lease is held by ${current.hostId}, not $fromHostId")
        }
        if (!current.isActive(now)) throw LeaseConflictException("lease has expired")
        val next = WorkspaceLease(
            workspaceId = current.workspaceId,
            hostId = toHostId,
            epoch = current.epoch + 1,
            acquiredAt = now.toString(),
            expiresAt = now.plusMillis(ttlMillis).toString(),
        )
        writeAtomically(leaseFile, next.toJson().toString().toByteArray(Charsets.UTF_8))
        appendEvent(
            "lease.transferred",
            mapOf(
                "from" to fromHostId,
                "to" to toHostId,
                "epoch" to next.epoch.toString(),
            ),
            now,
        )
        return next
    }

    /**
     * Export is a zip of plain files plus `.origin/` metadata, excluding the transient lease.
     * The result opens with any unzip tool; no Origin Studio required.
     */
    @Synchronized
    fun exportTo(destination: File) {
        destination.parentFile?.mkdirs()
        val root = dir.toPath().toAbsolutePath().normalize()
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            dir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.relativeTo(dir).invariantSeparatorsPath }
                .forEach { file ->
                    val relative = file.relativeTo(dir).invariantSeparatorsPath
                    if (relative == "$DIR/lease.json") return@forEach
                    // Never follow a symlink outside the workspace during export.
                    if (!file.toPath().toAbsolutePath().normalize().startsWith(root)) return@forEach
                    zip.putNextEntry(ZipEntry(relative))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    private fun writeAtomically(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(file)) {
            file.writeBytes(bytes)
            temp.delete()
        }
    }

    companion object {
        const val DIR = ".origin"
        const val GENESIS = "0000000000000000000000000000000000000000000000000000000000000000"
        const val DEFAULT_LEASE_TTL_MILLIS: Long = 5 * 60 * 1000

        /**
         * Creates the `.origin/` layout if it is missing. Existing files are never touched.
         * Call this when a project is created, imported, cloned or first opened.
         */
        fun ensure(
            dir: File,
            name: String = dir.name,
            owner: String = "local",
            now: Instant = Instant.now(),
        ): OriginWorkspace {
            val workspace = OriginWorkspace(dir)
            workspace.ensureLayout()
            if (!workspace.manifestFile.isFile) {
                val id = "ws_" + Hashing.sha256Hex("${dir.absolutePath}|${now}").take(16)
                val manifest = WorkspaceManifest(
                    id = id,
                    name = name.ifBlank { dir.name },
                    createdAt = now.toString(),
                    owners = listOf(owner),
                )
                workspace.writeAtomically(
                    workspace.manifestFile,
                    manifest.toJson().toString().toByteArray(Charsets.UTF_8),
                )
                workspace.appendEvent(
                    "workspace.created",
                    mapOf("name" to manifest.name, "owner" to owner),
                    now,
                )
            }
            return workspace
        }

        fun open(dir: File): OriginWorkspace {
            val workspace = OriginWorkspace(dir)
            require(workspace.manifestFile.isFile) {
                "${dir.path} is not an Origin workspace (manifest.json is missing)"
            }
            return workspace
        }
    }
}
