package com.oneorigin.studio.origin

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OriginHostProtocolTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `compatible versions negotiate to the lower minor`() {
        assertEquals("origin.host/v1", OriginHostProtocol.negotiate("origin.host/v1", "origin.host/v1.4"))
        assertEquals("origin.host/v1.2", OriginHostProtocol.negotiate("origin.host/v1.2", "origin.host/v1.4"))
        assertEquals("origin.host/v1.2", OriginHostProtocol.negotiate("origin.host/v1.4", "origin.host/v1.2"))
    }

    @Test
    fun `a major mismatch fails closed`() {
        try {
            OriginHostProtocol.negotiate("origin.host/v1", "origin.host/v2")
            fail("major versions must not negotiate")
        } catch (expected: ProtocolException) {
            assertTrue(expected.message!!.contains("no compatible"))
        }
        try {
            HostEnvelope.fromJson(JSONObject().put("protocol", "origin.host/v9").put("type", "hello"))
            fail("an unknown protocol must be refused")
        } catch (expected: ProtocolException) {
            assertTrue(expected.message!!.contains("incompatible"))
        }
    }

    @Test
    fun `an envelope round-trips with its idempotency key`() {
        val original = HostEnvelope(
            type = HostMessageType.RUN_TURN,
            seq = 7,
            idempotencyKey = "turn-1",
            payload = JSONObject().put("prompt", "list files"),
        )
        val decoded = HostEnvelope.decode(original.encode())

        assertEquals(original.type, decoded.type)
        assertEquals(original.seq, decoded.seq)
        assertEquals(original.idempotencyKey, decoded.idempotencyKey)
        assertEquals("list files", decoded.payload.getString("prompt"))
        assertEquals(OriginHostProtocol.VERSION, decoded.protocol)
    }

    @Test
    fun `an unknown message type is refused`() {
        try {
            HostEnvelope.fromJson(
                JSONObject()
                    .put("protocol", OriginHostProtocol.VERSION)
                    .put("type", "launch_missiles")
            )
            fail("unknown message types must be refused")
        } catch (expected: ProtocolException) {
            assertTrue(expected.message!!.contains("unknown host message type"))
        }
    }

    @Test
    fun `capabilities carry the disclosure fields`() {
        val capabilities = HostCapabilities(
            hostId = "hub-1",
            operator = "the user",
            region = "home",
            location = HostLocation.HUB,
            isolation = IsolationClass.CONTAINER,
            retention = "deleted after 24h",
            availability = "99.5%",
            price = "free",
            capabilities = listOf("workspace.open", "run.turn"),
            dataFlowClass = DataFlowClass.PLAINTEXT_COMPUTE,
        )
        val decoded = HostCapabilities.fromJson(JSONObject(capabilities.toJson().toString()))

        assertEquals(capabilities, decoded)
        assertEquals(IsolationClass.CONTAINER, decoded.isolation)
        assertEquals(DataFlowClass.PLAINTEXT_COMPUTE, decoded.dataFlowClass)
    }

    @Test
    fun `selection prefers a local host unless remote is allowed`() {
        val local = HostCapabilities(
            hostId = "phone",
            operator = "user",
            region = "device",
            location = HostLocation.DEVICE,
            isolation = IsolationClass.APP_SANDBOX,
            retention = "local",
            availability = "app running",
            price = "free",
            capabilities = listOf("run.turn"),
        )
        val remote = HostCapabilities(
            hostId = "hub",
            operator = "user",
            region = "server",
            location = HostLocation.HUB,
            isolation = IsolationClass.CONTAINER,
            retention = "24h",
            availability = "99.5%",
            price = "free",
            capabilities = listOf("run.turn"),
            dataFlowClass = DataFlowClass.PLAINTEXT_COMPUTE,
        )

        assertEquals("phone", HostSelection.choose("run.turn", listOf(remote, local))?.hostId)
        assertNull(HostSelection.choose("run.turn", listOf(remote)))
        assertEquals("hub", HostSelection.choose("run.turn", listOf(remote), allowRemote = true)?.hostId)
    }

    @Test
    fun `the loopback host answers hello and opens a workspace`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("loopback"), "Loopback")
        val host = LoopbackOriginHost(workspace, hostId = "phone")

        val hello = host.handle(HostEnvelope(type = HostMessageType.HELLO))
        assertEquals(HostMessageType.CAPABILITIES, hello.type)
        assertNotNull(HostCapabilities.fromJson(hello.payload).hostId)

        val opened = host.handle(
            HostEnvelope(type = HostMessageType.OPEN_WORKSPACE, payload = JSONObject())
        )
        assertEquals(HostMessageType.STREAM_EVENT, opened.type)
        assertTrue(opened.payload.getBoolean("verified"))
        assertEquals(workspace.manifest().id, opened.payload.getString("workspaceId"))
    }

    @Test
    fun `an unsupported local message returns a structured error`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("unsupported"), "Unsupported")
        val host = LoopbackOriginHost(workspace)
        val response = host.handle(HostEnvelope(type = HostMessageType.RUN_TURN))
        assertEquals(HostMessageType.ERROR, response.type)
        assertEquals("unsupported_message", response.payload.getString("code"))
    }
}
