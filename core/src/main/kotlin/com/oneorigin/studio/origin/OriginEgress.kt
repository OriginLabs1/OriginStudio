package com.oneorigin.studio.origin

import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * The data-flow classes a capability can declare. The numbers are part of the wire contract:
 * they appear in capability manifests, host capabilities and the egress ledger.
 */
enum class DataFlowClass(val level: Int, val label: String) {
    NONE(0, "no egress"),
    METADATA(1, "metadata only"),
    ENCRYPTED_CONTENT(2, "encrypted workspace content"),
    PLAINTEXT_COMPUTE(3, "plaintext to a compute host"),
    THIRD_PARTY_MODEL(4, "content to a third-party model");

    companion object {
        fun fromLevel(level: Int): DataFlowClass =
            entries.firstOrNull { it.level == level }
                ?: throw IllegalArgumentException("unknown data-flow class: $level")
    }
}

data class EgressGrant(
    val capability: String,
    val host: String,
    val maxClass: DataFlowClass,
    val expiresAt: String? = null,
)

data class EgressRequest(
    val capability: String,
    val host: String,
    val dataFlowClass: DataFlowClass,
    val purpose: String,
    val bytes: Long = 0,
)

data class EgressDecision(
    val allowed: Boolean,
    val reason: String,
    val grant: EgressGrant? = null,
)

data class EgressEntry(
    val seq: Long,
    val at: String,
    val capability: String,
    val host: String,
    val dataFlowClass: DataFlowClass,
    val purpose: String,
    val bytes: Long,
    val allowed: Boolean,
    val reason: String,
    val prev: String,
    val hash: String,
) {
    fun canonical(): String = buildString {
        append(seq).append('\n')
        append(at).append('\n')
        append(capability).append('\n')
        append(host).append('\n')
        append(dataFlowClass.level).append('\n')
        append(purpose).append('\n')
        append(bytes).append('\n')
        append(allowed).append('\n')
        append(reason).append('\n')
        append(prev)
    }

    fun toJson(): JSONObject = JSONObject()
        .put("seq", seq)
        .put("at", at)
        .put("capability", capability)
        .put("host", host)
        .put("dataFlowClass", dataFlowClass.level)
        .put("purpose", purpose)
        .put("bytes", bytes)
        .put("allowed", allowed)
        .put("reason", reason)
        .put("prev", prev)
        .put("hash", hash)

    companion object {
        fun fromJson(json: JSONObject): EgressEntry = EgressEntry(
            seq = json.getLong("seq"),
            at = json.getString("at"),
            capability = json.getString("capability"),
            host = json.getString("host"),
            dataFlowClass = DataFlowClass.fromLevel(json.getInt("dataFlowClass")),
            purpose = json.getString("purpose"),
            bytes = json.optLong("bytes", 0),
            allowed = json.getBoolean("allowed"),
            reason = json.getString("reason"),
            prev = json.getString("prev"),
            hash = json.getString("hash"),
        )
    }
}

/**
 * Deny-by-default egress policy. A grant is scoped to a capability and a host, has a maximum
 * data-flow class and may expire. There is no ambient network right.
 */
class EgressPolicy(private val grants: List<EgressGrant>) {

    fun decide(request: EgressRequest, now: Instant = Instant.now()): EgressDecision {
        if (request.dataFlowClass == DataFlowClass.NONE) {
            return EgressDecision(true, "no egress requested")
        }
        if (request.capability.isBlank() || request.host.isBlank() || request.purpose.isBlank()) {
            return EgressDecision(false, "capability, host and purpose are required")
        }
        val grant = grants.firstOrNull { it.matches(request) }
            ?: return EgressDecision(false, "no egress grant for ${request.capability} -> ${request.host}")
        if (grant.expiresAt != null && Instant.parse(grant.expiresAt).isBefore(now)) {
            return EgressDecision(false, "egress grant expired at ${grant.expiresAt}", grant)
        }
        if (request.dataFlowClass.level > grant.maxClass.level) {
            return EgressDecision(
                false,
                "${request.dataFlowClass.label} exceeds the grant (${grant.maxClass.label})",
                grant,
            )
        }
        return EgressDecision(true, "granted (${grant.maxClass.label})", grant)
    }

    private fun EgressGrant.matches(request: EgressRequest): Boolean =
        (capability == "*" || capability == request.capability) &&
            (host == "*" || host == request.host)
}

/**
 * A hash-chained, user-visible egress log. The ledger records denials as well as grants:
 * a refusal is evidence too.
 */
class EgressLedger(private val file: File) {

    @Synchronized
    fun record(
        request: EgressRequest,
        decision: EgressDecision,
        now: Instant = Instant.now(),
    ): EgressEntry {
        val entries = entries()
        val prev = entries.lastOrNull()?.hash ?: OriginWorkspace.GENESIS
        val seq = (entries.lastOrNull()?.seq ?: 0L) + 1
        val draft = EgressEntry(
            seq = seq,
            at = now.toString(),
            capability = request.capability,
            host = request.host,
            dataFlowClass = request.dataFlowClass,
            purpose = request.purpose,
            bytes = request.bytes,
            allowed = decision.allowed,
            reason = decision.reason,
            prev = prev,
            hash = "",
        )
        val entry = draft.copy(hash = Hashing.sha256Hex(prev + draft.canonical()))
        file.parentFile?.mkdirs()
        file.appendText(entry.toJson().toString() + "\n")
        return entry
    }

    fun entries(): List<EgressEntry> {
        if (!file.isFile) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .map { EgressEntry.fromJson(JSONObject(it)) }
    }

    fun verify(): Boolean {
        return runCatching {
            var prev = OriginWorkspace.GENESIS
            entries().forEachIndexed { index, entry ->
                if (entry.seq != index + 1L) return false
                if (entry.prev != prev) return false
                if (Hashing.sha256Hex(entry.prev + entry.canonical()) != entry.hash) return false
                prev = entry.hash
            }
            true
        }.getOrDefault(false)
    }
}
