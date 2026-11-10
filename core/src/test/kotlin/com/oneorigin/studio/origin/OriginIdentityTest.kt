package com.oneorigin.studio.origin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OriginIdentityTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `an identity signs and verifies`() {
        val identity = OriginIdentity.create()
        val message = "the constitution wins".toByteArray()

        val signature = identity.sign(message)
        assertTrue(identity.verify(message, signature))
        assertFalse(identity.verify("tampered".toByteArray(), signature))
        assertTrue(identity.id.startsWith("did:origin:"))
    }

    @Test
    fun `an identity round-trips through its private keystore json`() {
        val identity = OriginIdentity.create()
        val restored = OriginIdentity.fromJson(identity.privateJson())

        assertEquals(identity.id, restored.id)
        assertEquals(identity.publicKeyBase64, restored.publicKeyBase64)
        assertTrue(restored.verify("hello".toByteArray(), identity.sign("hello".toByteArray())))
    }

    @Test
    fun `loadOrCreate persists the same identity`() {
        val file = File(temp.root, "identity.json")
        val first = OriginIdentity.loadOrCreate(file)
        val second = OriginIdentity.loadOrCreate(file)

        assertTrue(file.isFile)
        assertEquals(first.id, second.id)
    }
}
