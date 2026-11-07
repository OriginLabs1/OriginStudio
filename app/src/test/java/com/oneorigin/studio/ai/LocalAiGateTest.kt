package com.oneorigin.studio.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiGateTest {

    @Test
    fun acceptsApi29OnArm64() {
        assertTrue(LocalAiGate.supports(29, listOf("arm64-v8a")))
        assertTrue(LocalAiGate.supports(35, listOf("armeabi-v7a", "arm64-v8a")))
    }

    @Test
    fun rejectsOlderAndroidEvenOnArm64() {
        assertFalse(LocalAiGate.supports(28, listOf("arm64-v8a")))
        assertFalse(LocalAiGate.supports(26, listOf("arm64-v8a")))
    }

    @Test
    fun rejects32BitAndEmulatorAbis() {
        assertFalse(LocalAiGate.supports(35, listOf("armeabi-v7a")))
        assertFalse(LocalAiGate.supports(35, listOf("x86_64")))
        assertFalse(LocalAiGate.supports(35, emptyList()))
    }

    @Test
    fun unsupportedReasonNamesTheRealConstraint() {
        assertTrue(LocalAiGate.unsupportedReason(28).contains("Android 10"))
        assertTrue(LocalAiGate.unsupportedReason(35).contains("ARM"))
    }
}
