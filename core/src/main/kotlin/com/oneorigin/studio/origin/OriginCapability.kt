package com.oneorigin.studio.origin

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * A capability manifest. The signature covers every field except `signature` itself, in a
 * fixed order, so a manifest cannot be edited without invalidating it.
 */
data class CapabilityManifest(
    val id: String,
    val version: String,
    val publisher: String,
    val publisherPublicKey: String,
    val permissions: List<String>,
    val dataFlowClass: DataFlowClass,
    val isolation: IsolationClass,
    val artifactSha256: String,
    val signature: String = "",
    val schema: String = SCHEMA,
) {
    fun canonicalBytes(): ByteArray = buildString {
        append(schema).append('\n')
        append(id).append('\n')
        append(version).append('\n')
        append(publisher).append('\n')
        append(publisherPublicKey).append('\n')
        permissions.sorted().forEach { append(it).append('\n') }
        append(dataFlowClass.level).append('\n')
        append(isolation.wire).append('\n')
        append(artifactSha256.lowercase()).append('\n')
    }.toByteArray(Charsets.UTF_8)

    fun toJson(): JSONObject = JSONObject()
        .put("schema", schema)
        .put("id", id)
        .put("version", version)
        .put("publisher", publisher)
        .put("publisherPublicKey", publisherPublicKey)
        .put("permissions", JSONArray(permissions))
        .put("dataFlowClass", dataFlowClass.level)
        .put("isolation", isolation.wire)
        .put("artifactSha256", artifactSha256)
        .put("signature", signature)

    companion object {
        const val SCHEMA = "origin.capability/v1"

        fun fromJson(json: JSONObject): CapabilityManifest {
            val schema = json.getString("schema")
            require(schema == SCHEMA) { "unsupported capability schema: $schema" }
            val permissionsJson = json.optJSONArray("permissions") ?: JSONArray()
            return CapabilityManifest(
                id = json.getString("id"),
                version = json.getString("version"),
                publisher = json.getString("publisher"),
                publisherPublicKey = json.getString("publisherPublicKey"),
                permissions = List(permissionsJson.length()) { permissionsJson.getString(it) },
                dataFlowClass = DataFlowClass.fromLevel(json.getInt("dataFlowClass")),
                isolation = IsolationClass.fromWire(json.getString("isolation")),
                artifactSha256 = json.getString("artifactSha256"),
                signature = json.getString("signature"),
                schema = schema,
            )
        }
    }
}

data class CapabilityTrustDecision(val trusted: Boolean, val reason: String)

/**
 * Capability trust:
 * - the schema is known;
 * - the artifact hash matches the manifest;
 * - the publisher signature verifies against the embedded public key;
 * - the capability is not revoked.
 *
 * A valid signature proves integrity and publisher identity. It does not prove the artifact is
 * safe; the isolation class and permission list are separate, user-visible facts.
 */
object CapabilityTrust {

    fun verify(
        manifest: CapabilityManifest,
        artifact: File,
        revoked: Set<String> = emptySet(),
    ): CapabilityTrustDecision {
        if (manifest.schema != CapabilityManifest.SCHEMA) {
            return CapabilityTrustDecision(false, "unsupported capability schema")
        }
        if (manifest.id.isBlank() || manifest.version.isBlank() || manifest.publisher.isBlank()) {
            return CapabilityTrustDecision(false, "manifest is missing id, version or publisher")
        }
        if (manifest.id in revoked) {
            return CapabilityTrustDecision(false, "capability ${manifest.id} is revoked")
        }
        if (!artifact.isFile) {
            return CapabilityTrustDecision(false, "artifact is missing")
        }
        val actual = Hashing.sha256Hex(artifact.readBytes())
        if (!actual.equals(manifest.artifactSha256, ignoreCase = true)) {
            return CapabilityTrustDecision(false, "artifact hash does not match the manifest")
        }
        if (manifest.signature.isBlank()) {
            return CapabilityTrustDecision(false, "manifest is not signed")
        }
        return runCatching {
            val publicKey: PublicKey = KeyFactory.getInstance("EC").generatePublic(
                X509EncodedKeySpec(Base64.getDecoder().decode(manifest.publisherPublicKey))
            )
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(manifest.canonicalBytes())
            if (!verifier.verify(Base64.getDecoder().decode(manifest.signature))) {
                return CapabilityTrustDecision(false, "publisher signature does not verify")
            }
            CapabilityTrustDecision(true, "hash and publisher signature verified")
        }.getOrElse { error ->
            CapabilityTrustDecision(false, "could not verify publisher: ${error.message}")
        }
    }
}
