package com.example

import com.example.cipher.UnicodeObfuscator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CipherRoundTripTest {

    @Test
    fun obfuscate_deobfuscate_roundTrip_symbols_manyRotations() {
        val payloads = listOf(
            "deadd00decaf",
            "a".repeat(64),
            "0123456789abcdef".repeat(4)
        )
        for (hex in payloads) {
            repeat(32) {
                val visual = UnicodeObfuscator.obfuscate(hex, useSymbols = true)
                val restored = UnicodeObfuscator.deobfuscate(visual)
                assertEquals("symbols payload=$hex iter=$it", hex, restored)
            }
        }
    }

    @Test
    fun obfuscate_deobfuscate_roundTrip_emojis() {
        val hex = "a".repeat(64)
        repeat(16) {
            val visual = UnicodeObfuscator.obfuscate(hex, useSymbols = false)
            val restored = UnicodeObfuscator.deobfuscate(visual)
            assertNotNull(restored)
            assertEquals(hex, restored)
        }
    }
}
