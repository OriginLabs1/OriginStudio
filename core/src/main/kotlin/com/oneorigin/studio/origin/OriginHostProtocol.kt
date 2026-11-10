package com.oneorigin.studio.origin

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ProtocolException(message: String) : Exception(message)

enum class HostMessageType(val wire: String) {
    HELLO("hello"),
    CAPABILITIES("capabilities"),
    OPEN_WORKSPACE("open_workspace"),
    START_SESSION("start_session"),
    RUN_TURN("run_turn"),
    STREAM_EVENT("stream_event"),
    CHECKPOINT("checkpoint"),
    RESTORE("restore"),
    CANCEL("cancel"),
    REVOKE("revoke"),
    HEALTH("health"),
    ERROR("error");

    companion object {
        fun fromWire(wire: String): HostMessageType =
            entries.firstOrNull { it.wire == wire }
                ?: throw ProtocolException("unknown host message type: $wire")
    }
}

enum class HostLocation(val wire: String, val remote: Boolean) {
    DEVICE("device", false),
    HUB("hub", true),
    MANAGED("managed", true),
    THIRD_PARTY("third-party", true);

    companion object {
        fun fromWire(wire: String): HostLocation =
            entries.firstOrNull { it.wire == wire }
                ?: throw ProtocolException("unknown host location: $wire")
    }
}

enum class IsolationClass(
    val wire: String,
    val label: String,
    val untrustedCodeAllowed: Boolean,
) {
    APP_SANDBOX("app-sandbox", "App sandbox", false),
    WASM("wasm", "WASM capability sandbox", true),
    COMPATIBILITY("compatibility", "Compatibility layer (not a sandbox)", false),
    CONTAINER("container", "Container", true),
    MICROVM("microvm", "MicroVM", true),
    REMOTE_DISCLOSED("remote-disclosed", "Remote host (disclosed by operator)", false);

    companion object {
        fun fromWire(wire: String): IsolationClass =
            entries.firstOrNull { it.wire == wire }
                ?: throw ProtocolException("unknown isolation class: $wire")
    }
}

/**
 * What a host must disclose before it is used. A host that cannot state these facts is not
 * allowed to claim the corresponding guarantee.
 */
data class HostCapabilities(
    val hostId: String,
    val operator: String,
    val region: String,
    val location: HostLocation,
    val isolation: IsolationClass,
    val retention: String,
    val availability: String,
    val price: String,
    val protocol: String = OriginHostProtocol.VERSION,
    val capabilities: List<String> = emptyList(),
    val dataFlowClass: DataFlowClass = DataFlowClass.NONE,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("hostId", hostId)
        .put("operator", operator)
        .put("region", region)
        .put("location", location.wire)
        .put("isolation", isolation.wire)
        .put("retention", retention)
        .put("availability", availability)
        .put("price", price)
        .put("protocol", protocol)
        .put("capabilities", JSONArray(capabilities))
        .put("dataFlowClass", dataFlowClass.level)

    companion object {
        fun fromJson(json: JSONObject): HostCapabilities {
            val dataFlow = json.optInt("dataFlowClass", 0)
            val capsJson = json.optJSONArray("capabilities") ?: JSONArray()
            return HostCapabilities(
                hostId = json.getString("hostId"),
                operator = json.getString("operator"),
                region = json.getString("region"),
                location = HostLocation.fromWire(json.getString("location")),
                isolation = IsolationClass.fromWire(json.getString("isolation")),
                retention = json.getString("retention"),
                availability = json.getString("availability"),
                price = json.getString("price"),
                protocol = json.optString("protocol", OriginHostProtocol.VERSION),
                capabilities = List(capsJson.length()) { capsJson.getString(it) },
                dataFlowClass = DataFlowClass.fromLevel(dataFlow),
            )
        }
    }
}

/**
 * The versioned envelope every host and client uses. Unknown major versions fail closed;
 * unknown message types fail closed; every mutating call carries an idempotency key.
 */
data class HostEnvelope(
    val type: HostMessageType,
    val id: String = UUID.randomUUID().toString(),
    val seq: Long = 1,
    val idempotencyKey: String? = null,
    val payload: JSONObject = JSONObject(),
    val protocol: String = OriginHostProtocol.VERSION,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("protocol", protocol)
        .put("type", type.wire)
        .put("id", id)
        .put("seq", seq)
        .put("idempotencyKey", idempotencyKey ?: JSONObject.NULL)
        .put("payload", JSONObject(payload.toString()))

    fun encode(): String = toJson().toString()

    companion object {
        fun fromJson(json: JSONObject): HostEnvelope {
            val protocol = json.optString("protocol")
            OriginHostProtocol.requireCompatible(protocol)
            val type = HostMessageType.fromWire(json.optString("type"))
            val seq = json.optLong("seq", 1)
            require(seq > 0) { "seq must be positive" }
            return HostEnvelope(
                type = type,
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                seq = seq,
                idempotencyKey = json.optString("idempotencyKey").takeIf { it.isNotBlank() && it != "null" },
                payload = json.optJSONObject("payload") ?: JSONObject(),
                protocol = protocol,
            )
        }

        fun decode(text: String): HostEnvelope = fromJson(JSONObject(text))
    }
}

/**
 * Protocol negotiation. Major versions must match exactly; the lower minor version is used.
 * The rule is deliberately strict: a host and a client that do not understand each other must
 * stop, not guess.
 */
object OriginHostProtocol {
    const val VERSION = "origin.host/v1"
    const val MAJOR = 1
    const val MINOR = 0

    private val PATTERN = Regex("^origin\\.host/v(\\d+)(?:\\.(\\d+))?$")

    data class Parsed(val major: Int, val minor: Int)

    fun parse(version: String): Parsed {
        val match = PATTERN.matchEntire(version)
            ?: throw ProtocolException("unrecognised host protocol version: $version")
        return Parsed(
            major = match.groupValues[1].toInt(),
            minor = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0,
        )
    }

    fun requireCompatible(version: String) {
        val parsed = parse(version)
        if (parsed.major != MAJOR) {
            throw ProtocolException(
                "incompatible host protocol: host offered $version, client supports major $MAJOR"
            )
        }
    }

    /** Returns the version both sides can use, or throws. */
    fun negotiate(client: String, host: String): String {
        val c = parse(client)
        val h = parse(host)
        if (c.major != h.major) {
            throw ProtocolException(
                "no compatible host protocol: client $client, host $host"
            )
        }
        val minor = minOf(c.minor, h.minor)
        return if (minor == 0) "origin.host/v${c.major}" else "origin.host/v${c.major}.$minor"
    }
}

/** Picks a host for a required capability. Local hosts win ties; remote needs permission. */
object HostSelection {
    fun choose(
        requiredCapability: String,
        hosts: List<HostCapabilities>,
        allowRemote: Boolean = false,
    ): HostCapabilities? = hosts
        .asSequence()
        .filter { requiredCapability in it.capabilities }
        .filter { allowRemote || !it.location.remote }
        .sortedWith(
            compareBy<HostCapabilities>(
                { if (it.location == HostLocation.DEVICE) 0 else 1 },
                { it.dataFlowClass.level },
            )
        )
        .firstOrNull()
}

/**
 * The in-process local host. It exists so the same protocol can be exercised without a
 * network, and so the app can treat local and remote execution through one interface.
 */
class LoopbackOriginHost(
    private val workspace: OriginWorkspace,
    hostId: String = "local",
) {
    private val advertised = HostCapabilities(
        hostId = hostId,
        operator = "user",
        region = "device",
        location = HostLocation.DEVICE,
        isolation = IsolationClass.APP_SANDBOX,
        retention = "local only; no retention off-device",
        availability = "while the app is running",
        price = "free",
        capabilities = listOf("workspace.open", "workspace.list", "workspace.checkpoint"),
        dataFlowClass = DataFlowClass.NONE,
    )

    fun capabilities(): HostCapabilities = advertised

    fun handle(envelope: HostEnvelope): HostEnvelope = when (envelope.type) {
        HostMessageType.HELLO, HostMessageType.CAPABILITIES -> reply(
            envelope,
            HostMessageType.CAPABILITIES,
            advertised.toJson(),
        )

        HostMessageType.OPEN_WORKSPACE -> {
            val manifest = workspace.manifest()
            val verification = workspace.verify()
            reply(
                envelope,
                if (verification.ok) HostMessageType.STREAM_EVENT else HostMessageType.ERROR,
                JSONObject()
                    .put("workspaceId", manifest.id)
                    .put("name", manifest.name)
                    .put("manifestHash", verification.manifestHash)
                    .put("eventCount", verification.eventCount)
                    .put("verified", verification.ok),
            )
        }

        HostMessageType.HEALTH -> reply(
            envelope,
            HostMessageType.HEALTH,
            JSONObject()
                .put("hostId", advertised.hostId)
                .put("ok", true)
                .put("protocol", OriginHostProtocol.VERSION),
        )

        else -> reply(
            envelope,
            HostMessageType.ERROR,
            JSONObject()
                .put("code", "unsupported_message")
                .put("message", "the local reference host does not implement ${envelope.type.wire}"),
        )
    }

    private fun reply(request: HostEnvelope, type: HostMessageType, payload: JSONObject) =
        HostEnvelope(
            type = type,
            seq = request.seq + 1,
            idempotencyKey = request.idempotencyKey,
            payload = payload,
        )
}

/**
 * A small HTTP client for the Host Protocol. The wire format is one JSON envelope per POST;
 * the reference host in `hosts/hub` implements the other side.
 */
class HttpOriginHostClient(
    private val baseUrl: String,
    private val token: String? = null,
) {
    fun send(envelope: HostEnvelope): HostEnvelope {
        val address = baseUrl.trimEnd('/') + "/origin/host/v1"
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        return try {
            connection.outputStream.bufferedWriter().use { it.write(envelope.encode()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let { BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() } }
                ?: ""
            if (code !in 200..299) {
                throw ProtocolException("host answered $code: ${body.take(300)}")
            }
            HostEnvelope.decode(body)
        } finally {
            connection.disconnect()
        }
    }

    fun hello(): HostCapabilities {
        val response = send(HostEnvelope(type = HostMessageType.HELLO))
        if (response.type == HostMessageType.ERROR) {
            throw ProtocolException("host refused hello: ${response.payload}")
        }
        return HostCapabilities.fromJson(response.payload)
    }
}
