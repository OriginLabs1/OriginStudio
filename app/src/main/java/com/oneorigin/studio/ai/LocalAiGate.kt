package com.oneorigin.studio.ai

import android.os.Build

/**
 * The one place that decides whether local AI can run on this device.
 *
 * QVAC's Android runtime is arm64-v8a and API 29+. The app itself supports API 26 and
 * x86_64 so the workspace keeps working there; those devices must get an honest
 * "unavailable" state instead of a button that fails later. Keeping the predicate pure
 * makes the policy testable on the JVM, without an emulator or a device.
 */
object LocalAiGate {

    const val MIN_API = 29
    const val REQUIRED_ABI = "arm64-v8a"

    /** True only when both the OS floor and the native ABI are satisfied. */
    fun supports(apiLevel: Int, abis: List<String>): Boolean =
        apiLevel >= MIN_API && abis.any { it == REQUIRED_ABI }

    fun isSupported(): Boolean =
        supports(Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS?.toList().orEmpty())

    /** A short, user-facing reason when [isSupported] is false. */
    fun unsupportedReason(apiLevel: Int = Build.VERSION.SDK_INT): String = when {
        apiLevel < MIN_API -> "Local AI needs Android ${apiLabel(MIN_API)} or newer."
        else -> "Local AI needs a 64-bit ARM device."
    }

    private fun apiLabel(api: Int): String = when (api) {
        29 -> "10"
        30 -> "11"
        31 -> "12"
        32 -> "12L"
        33 -> "13"
        34 -> "14"
        35 -> "15"
        36 -> "16"
        else -> "API $api"
    }
}
