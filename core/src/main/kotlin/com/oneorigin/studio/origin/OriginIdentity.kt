package com.oneorigin.studio.origin

import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Base64

/**
 * A locally held asymmetric identity.
 *
 * EC P-256 is used deliberately: it is available on every supported Android API level and on
 * the JVM, so the same identity code runs in the app, in tests and in the reference host.
 * Ed25519 may be adopted later through an ADR once the minimum platform guarantees it.
 *
 * The private key never leaves the device unless the user exports it explicitly. On Android the
 * file lives in app-private storage; the platform keystore wrapper is tracked in DEVIATIONS.md.
 */
class OriginIdentity private constructor(
    private val keyPair: KeyPair,
    val createdAt: String = Instant.now().toString(),
) {

    val algorithm: String = "EC-P256"
    val publicKeyBase64: String =
        Base64.getEncoder().encodeToString(keyPair.public.encoded)
    val id: String = "did:origin:" + Hashing.sha256Hex(keyPair.public.encoded).take(32)

    fun sign(bytes: ByteArray): ByteArray {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keyPair.private)
        signer.update(bytes)
        return signer.sign()
    }

    fun verify(bytes: ByteArray, signature: ByteArray): Boolean = runCatching {
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(keyPair.public)
        verifier.update(bytes)
        verifier.verify(signature)
    }.getOrDefault(false)

    /** The public half, safe to publish. */
    fun publicJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("algorithm", algorithm)
        .put("publicKey", publicKeyBase64)
        .put("createdAt", createdAt)

    /**
     * The private half. Only for an explicit user export or an app-private keystore file.
     * Never put this in a workspace, an event log or a remote request.
     */
    fun privateJson(): JSONObject = publicJson()
        .put("privateKey", Base64.getEncoder().encodeToString(keyPair.private.encoded))

    companion object {
        private const val CURVE = "secp256r1"

        fun create(now: Instant = Instant.now()): OriginIdentity {
            val generator = KeyPairGenerator.getInstance("EC")
            generator.initialize(ECGenParameterSpec(CURVE))
            return OriginIdentity(generator.generateKeyPair(), now.toString())
        }

        fun fromJson(json: JSONObject): OriginIdentity {
            val algorithm = json.optString("algorithm", "EC-P256")
            require(algorithm == "EC-P256") { "unsupported identity algorithm: $algorithm" }
            val factory = KeyFactory.getInstance("EC")
            val public: PublicKey = factory.generatePublic(
                X509EncodedKeySpec(Base64.getDecoder().decode(json.getString("publicKey")))
            )
            val private: PrivateKey = factory.generatePrivate(
                PKCS8EncodedKeySpec(Base64.getDecoder().decode(json.getString("privateKey")))
            )
            val identity = OriginIdentity(
                KeyPair(public, private),
                json.optString("createdAt").ifBlank { Instant.now().toString() },
            )
            require(identity.publicKeyBase64 == json.getString("publicKey")) {
                "identity key pair does not match"
            }
            return identity
        }

        /**
         * Loads the identity from [file] or creates and persists one. The file is written
         * atomically and restricted to the owner where the platform supports it.
         */
        fun loadOrCreate(file: File, now: Instant = Instant.now()): OriginIdentity {
            if (file.isFile) {
                return fromJson(JSONObject(file.readText()))
            }
            file.parentFile?.mkdirs()
            val identity = create(now)
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(identity.privateJson().toString())
            restrictToOwner(temp)
            if (!temp.renameTo(file)) {
                file.writeText(identity.privateJson().toString())
                temp.delete()
                restrictToOwner(file)
            }
            return identity
        }

        private fun restrictToOwner(file: File) {
            runCatching {
                file.setReadable(false, false)
                file.setWritable(false, false)
                file.setReadable(true, true)
                file.setWritable(true, true)
            }
        }
    }
}
