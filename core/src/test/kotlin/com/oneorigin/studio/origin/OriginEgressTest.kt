package com.oneorigin.studio.origin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class OriginEgressTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `no grant means no egress`() {
        val policy = EgressPolicy(emptyList())
        val decision = policy.decide(
            EgressRequest("ai.ask", "api.example.com", DataFlowClass.METADATA, "ask")
        )
        assertFalse(decision.allowed)
        assertTrue(decision.reason.contains("no egress grant"))
    }

    @Test
    fun `a grant caps the data-flow class`() {
        val policy = EgressPolicy(
            listOf(
                EgressGrant(
                    capability = "ai.ask",
                    host = "api.example.com",
                    maxClass = DataFlowClass.METADATA,
                )
            )
        )
        val metadata = policy.decide(
            EgressRequest("ai.ask", "api.example.com", DataFlowClass.METADATA, "ask")
        )
        val plaintext = policy.decide(
            EgressRequest("ai.ask", "api.example.com", DataFlowClass.PLAINTEXT_COMPUTE, "ask")
        )
        assertTrue(metadata.allowed)
        assertFalse(plaintext.allowed)
        assertTrue(plaintext.reason.contains("exceeds the grant"))
    }

    @Test
    fun `a grant can expire`() {
        val policy = EgressPolicy(
            listOf(
                EgressGrant(
                    capability = "*",
                    host = "*",
                    maxClass = DataFlowClass.THIRD_PARTY_MODEL,
                    expiresAt = Instant.now().minusSeconds(1).toString(),
                )
            )
        )
        val decision = policy.decide(
            EgressRequest("ai.ask", "model.example", DataFlowClass.THIRD_PARTY_MODEL, "ask")
        )
        assertFalse(decision.allowed)
        assertTrue(decision.reason.contains("expired"))
    }

    @Test
    fun `the ledger records grants and denials and detects tampering`() {
        val file = File(temp.root, "egress.jsonl")
        val ledger = EgressLedger(file)
        val policy = EgressPolicy(
            listOf(EgressGrant("ai.ask", "model.example", DataFlowClass.PLAINTEXT_COMPUTE))
        )

        val allowed = policy.decide(
            EgressRequest("ai.ask", "model.example", DataFlowClass.PLAINTEXT_COMPUTE, "ask", bytes = 120)
        )
        val denied = policy.decide(
            EgressRequest("shell", "model.example", DataFlowClass.PLAINTEXT_COMPUTE, "exfiltrate")
        )
        ledger.record(
            EgressRequest("ai.ask", "model.example", DataFlowClass.PLAINTEXT_COMPUTE, "ask", 120),
            allowed,
        )
        ledger.record(
            EgressRequest("shell", "model.example", DataFlowClass.PLAINTEXT_COMPUTE, "exfiltrate"),
            denied,
        )

        assertTrue(ledger.verify())
        assertTrue(ledger.entries().first().allowed)
        assertFalse(ledger.entries().last().allowed)

        file.writeText(file.readText().replace("exfiltrate", "innocent"))
        assertFalse(ledger.verify())
    }
}
