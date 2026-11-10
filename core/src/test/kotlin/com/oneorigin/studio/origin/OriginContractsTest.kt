package com.oneorigin.studio.origin

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cross-language conformance: the same fixtures the Node reference host uses are
 * parsed by the Kotlin core. If either side drifts, this test or the host test
 * fails.
 */
class OriginContractsTest {

    private fun fixture(name: String): JSONObject {
        val candidates = listOf(
            File("../contracts/fixtures/$name"),
            File("contracts/fixtures/$name"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("fixture $name not found from ${File(".").absolutePath}")
        return JSONObject(file.readText())
    }

    private fun fixtureText(name: String): String {
        val candidates = listOf(
            File("../contracts/fixtures/$name"),
            File("contracts/fixtures/$name"),
        )
        return candidates.first { it.isFile }.readText()
    }

    @Test
    fun `the workspace fixture parses`() {
        val manifest = WorkspaceManifest.fromJson(fixture("workspace.valid.json"))
        assertEquals(WorkspaceManifest.SCHEMA, manifest.schema)
        assertEquals("ws_0123456789abcdef", manifest.id)
        assertEquals("Reference workspace", manifest.name)
        assertEquals(1, manifest.owners.size)
    }

    @Test
    fun `the workspace event fixture has a valid chain hash`() {
        val event = WorkspaceEvent.fromJson(fixture("workspace-event.valid.json"))
        assertEquals(2L, event.seq)
        assertEquals(
            Hashing.sha256Hex(event.prev + event.canonical()),
            event.hash,
        )
    }

    @Test
    fun `the egress fixture has a valid chain hash`() {
        val entry = EgressEntry.fromJson(fixture("egress-entry.valid.json"))
        assertEquals(1L, entry.seq)
        assertTrue(entry.allowed)
        assertEquals(
            Hashing.sha256Hex(entry.prev + entry.canonical()),
            entry.hash,
        )
    }

    @Test
    fun `the host envelope fixture decodes and negotiates`() {
        val envelope = HostEnvelope.decode(fixtureText("host-envelope.valid.json"))
        assertEquals(HostMessageType.HELLO, envelope.type)
        assertEquals(OriginHostProtocol.VERSION, envelope.protocol)
        assertEquals(
            "origin.host/v1",
            OriginHostProtocol.negotiate(envelope.protocol, "origin.host/v1.9"),
        )
    }

    @Test
    fun `the host disclosure fixture carries every required field`() {
        val host = HostCapabilities.fromJson(fixture("host-capabilities.valid.json"))
        assertEquals(HostLocation.HUB, host.location)
        assertEquals(IsolationClass.CONTAINER, host.isolation)
        assertEquals(DataFlowClass.PLAINTEXT_COMPUTE, host.dataFlowClass)
        assertTrue(host.capabilities.contains("workspace.open"))
    }

    @Test
    fun `the capability fixture parses with its permissions`() {
        val manifest = CapabilityManifest.fromJson(fixture("capability.valid.json"))
        assertEquals("demo.summarise", manifest.id)
        assertEquals(DataFlowClass.METADATA, manifest.dataFlowClass)
        assertEquals(IsolationClass.WASM, manifest.isolation)
        assertTrue(manifest.permissions.contains("workspace.read"))
    }
}
