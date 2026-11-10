package com.oneorigin.studio.origin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Base64

class OriginCapabilityTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun signedManifest(
        identity: OriginIdentity,
        artifact: File,
        id: String = "demo.capability",
    ): CapabilityManifest {
        val unsigned = CapabilityManifest(
            id = id,
            version = "1.0.0",
            publisher = identity.id,
            publisherPublicKey = identity.publicKeyBase64,
            permissions = listOf("workspace.read", "network.metadata"),
            dataFlowClass = DataFlowClass.METADATA,
            isolation = IsolationClass.WASM,
            artifactSha256 = Hashing.sha256Hex(artifact.readBytes()),
        )
        val signature = identity.sign(unsigned.canonicalBytes())
        return unsigned.copy(signature = Base64.getEncoder().encodeToString(signature))
    }

    @Test
    fun `a signed capability with the right hash is trusted`() {
        val identity = OriginIdentity.create()
        val artifact = File(temp.root, "cap.wasm").apply { writeText("not really wasm") }
        val manifest = signedManifest(identity, artifact)

        val decision = CapabilityTrust.verify(manifest, artifact)
        assertTrue(decision.reason, decision.trusted)
    }

    @Test
    fun `a modified artifact is refused`() {
        val identity = OriginIdentity.create()
        val artifact = File(temp.root, "cap.wasm").apply { writeText("original") }
        val manifest = signedManifest(identity, artifact)
        artifact.writeText("tampered")

        val decision = CapabilityTrust.verify(manifest, artifact)
        assertFalse(decision.trusted)
        assertTrue(decision.reason.contains("hash"))
    }

    @Test
    fun `a revoked capability is refused before the artifact is run`() {
        val identity = OriginIdentity.create()
        val artifact = File(temp.root, "cap.wasm").apply { writeText("code") }
        val manifest = signedManifest(identity, artifact)

        val decision = CapabilityTrust.verify(manifest, artifact, revoked = setOf(manifest.id))
        assertFalse(decision.trusted)
        assertTrue(decision.reason.contains("revoked"))
    }

    @Test
    fun `an edited manifest breaks the signature`() {
        val identity = OriginIdentity.create()
        val artifact = File(temp.root, "cap.wasm").apply { writeText("code") }
        val manifest = signedManifest(identity, artifact)
        val edited = manifest.copy(permissions = manifest.permissions + "filesystem.write")

        val decision = CapabilityTrust.verify(edited, artifact)
        assertFalse(decision.trusted)
        assertTrue(decision.reason.contains("signature"))
    }

    @Test
    fun `an unsigned manifest is refused`() {
        val artifact = File(temp.root, "cap.wasm").apply { writeText("code") }
        val manifest = CapabilityManifest(
            id = "unsigned",
            version = "1.0.0",
            publisher = "nobody",
            publisherPublicKey = "",
            permissions = emptyList(),
            dataFlowClass = DataFlowClass.NONE,
            isolation = IsolationClass.WASM,
            artifactSha256 = Hashing.sha256Hex(artifact.readBytes()),
        )
        val decision = CapabilityTrust.verify(manifest, artifact)
        assertFalse(decision.trusted)
        assertTrue(decision.reason.contains("not signed"))
    }
}
