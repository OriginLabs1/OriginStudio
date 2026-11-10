package com.oneorigin.studio.origin

import java.security.MessageDigest

/** SHA-256 helpers shared by the workspace, egress ledger and capability trust code. */
object Hashing {
    fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    fun sha256Hex(bytes: ByteArray): String =
        sha256(bytes).joinToString("") { "%02x".format(it) }

    fun sha256Hex(text: String): String = sha256Hex(text.toByteArray(Charsets.UTF_8))

    /** A stable hash over a map, independent of insertion order. */
    fun canonicalMap(map: Map<String, String>): String =
        map.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }
}
