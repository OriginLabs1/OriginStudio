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
import java.io.File
import java.time.Instant
import java.util.zip.ZipFile

class OriginWorkspaceTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `ensure creates a portable layout and verifies`() {
        val dir = temp.newFolder("project")
        val workspace = OriginWorkspace.ensure(dir, "Demo", owner = "did:origin:test")

        assertTrue(workspace.manifestFile.isFile)
        assertTrue(workspace.eventsFile.isFile)
        assertTrue(workspace.checkpointsDir.isDirectory)
        assertEquals(WorkspaceManifest.SCHEMA, workspace.manifest().schema)
        assertEquals("Demo", workspace.manifest().name)

        val verification = workspace.verify()
        assertTrue(verification.error, verification.ok)
        assertEquals(1, verification.eventCount)
        assertNull(workspace.lease())
    }

    @Test
    fun `events are hash chained and detect tampering`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("events"), "Events")
        workspace.appendEvent("file.saved", mapOf("path" to "index.html"))
        workspace.appendEvent("file.saved", mapOf("path" to "app.js"))

        val events = workspace.events()
        assertEquals(3, events.size)
        assertEquals(events[1].hash, events[2].prev)
        assertTrue(workspace.verify().ok)

        val tampered = workspace.eventsFile.readText()
            .replace("index.html", "evil.html")
        workspace.eventsFile.writeText(tampered)

        val verification = workspace.verify()
        assertFalse(verification.ok)
        assertNotNull(verification.error)
    }

    @Test
    fun `lease prevents a second writer and transfers explicitly`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("lease"), "Lease")
        val first = workspace.acquireLease("phone", ttlMillis = 60_000)

        assertEquals("phone", first.hostId)
        assertEquals(1L, first.epoch)

        try {
            workspace.acquireLease("laptop", ttlMillis = 60_000)
            fail("a second writer must not acquire an active lease")
        } catch (expected: LeaseConflictException) {
            assertTrue(expected.message!!.contains("phone"))
        }

        val handedOff = workspace.transferLease("phone", "laptop", ttlMillis = 60_000)
        assertEquals("laptop", handedOff.hostId)
        assertEquals(2L, handedOff.epoch)
        assertFalse(workspace.releaseLease("phone"))
        assertTrue(workspace.releaseLease("laptop"))
        assertNull(workspace.lease())
    }

    @Test
    fun `export is readable without origin and excludes the lease`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("export"), "Export")
        File(workspace.dir, "index.html").writeText("<h1>hello</h1>")
        workspace.appendEvent("file.saved", mapOf("path" to "index.html"))
        workspace.acquireLease("phone")

        val zip = File(temp.root, "export.zip")
        workspace.exportTo(zip)

        val entries = ZipFile(zip).use { archive ->
            archive.entries().toList().map { it.name }
        }
        assertTrue(entries.contains("index.html"))
        assertTrue(entries.contains(".origin/manifest.json"))
        assertTrue(entries.contains(".origin/events.jsonl"))
        assertFalse(entries.contains(".origin/lease.json"))
    }

    @Test
    fun `a manifest from another schema is refused`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("schema"), "Schema")
        val json = JSONObject(workspace.manifestFile.readText()).put("schema", "origin.workspace/v999")
        workspace.manifestFile.writeText(json.toString())

        assertFalse(workspace.verify().ok)
    }

    @Test
    fun `an expired lease can be acquired by another host`() {
        val workspace = OriginWorkspace.ensure(temp.newFolder("expiry"), "Expiry")
        val past = Instant.now().minusSeconds(600)
        workspace.acquireLease("phone", ttlMillis = 1_000, now = past)

        val second = workspace.acquireLease("laptop", ttlMillis = 60_000)
        assertEquals("laptop", second.hostId)
        assertEquals(2L, second.epoch)
    }
}
