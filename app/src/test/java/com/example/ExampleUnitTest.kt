package com.example

import com.example.cipher.CipherDetector
import com.example.cipher.InvisibleCharInjector
import com.example.cipher.SymbolMapper
import com.example.cipher.UnicodeObfuscator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testInvisibleCharInjector() {
        val original = "HELLO_WORLD"
        val injected = InvisibleCharInjector.inject(original)
        
        // Assert that characters are added (invisible)
        assertTrue(injected.length >= original.length)
        
        // Assert that stripping them off returns the exact original text
        val cleaned = InvisibleCharInjector.remove(injected)
        assertEquals(original, cleaned)
    }

    @Test
    fun testSymbolMapperSymbolsMode() {
        val originalHex = "a5e9f"
        
        // 1. Map with rotation shift = 0
        val visual0 = SymbolMapper.mapToVisual(originalHex, useSymbols = true, rotation = 0)
        assertTrue(visual0.isNotEmpty())
        
        val restoredHex0 = SymbolMapper.mapFromVisual(visual0, useSymbols = true, rotation = 0)
        assertEquals(originalHex, restoredHex0)

        // 2. Map with rotating shift = 7 (Dynamic Shuffled Pool)
        val visual7 = SymbolMapper.mapToVisual(originalHex, useSymbols = true, rotation = 7)
        assertNotEquals(visual0, visual7) // Dynamic symbols should be different
        
        val restoredHex7 = SymbolMapper.mapFromVisual(visual7, useSymbols = true, rotation = 7)
        assertEquals(originalHex, restoredHex7)
    }

    @Test
    fun testSymbolMapperEmojisMode() {
        val originalHex = "3b2c8f0"
        
        val visual = SymbolMapper.mapToVisual(originalHex, useSymbols = false, rotation = 3)
        assertTrue(visual.isNotEmpty())
        
        // Ensure emojis are generated
        assertTrue(visual.contains("🫧") || visual.contains("🪐") || visual.contains("⚡") || visual.contains("✨") || visual.contains("🔮") || visual.contains("🔑"))
        
        val restoredHex = SymbolMapper.mapFromVisual(visual, useSymbols = false, rotation = 3)
        assertEquals(originalHex, restoredHex)
    }

    @Test
    fun testUnicodeObfuscator() {
        val originalHex = "deadd00decaf"
        
        // Test symbols mode
        val obfuscatedSym = UnicodeObfuscator.obfuscate(originalHex, useSymbols = true)
        val cleanedSym = InvisibleCharInjector.remove(obfuscatedSym)
        assertTrue(cleanedSym.contains(UnicodeObfuscator.SYMBOL_PREFIX))
        assertTrue(cleanedSym.contains(UnicodeObfuscator.SYMBOL_SUFFIX))
        
        val restoredSym = UnicodeObfuscator.deobfuscate(obfuscatedSym)
        assertEquals(originalHex, restoredSym)

        // Test emoji mode
        val obfuscatedEmo = UnicodeObfuscator.obfuscate(originalHex, useSymbols = false)
        val cleanedEmo = InvisibleCharInjector.remove(obfuscatedEmo)
        assertTrue(cleanedEmo.contains(UnicodeObfuscator.EMOJI_PREFIX))
        assertTrue(cleanedEmo.contains(UnicodeObfuscator.EMOJI_SUFFIX))
        
        val restoredEmo = UnicodeObfuscator.deobfuscate(obfuscatedEmo)
        assertEquals(originalHex, restoredEmo)
    }

    @Test
    fun testCipherDetector() {
        val plainText = "This is a normal message without secure payloads"
        assertFalse(CipherDetector.isCipherText(plainText))
        assertNull(CipherDetector.extractExactVisualBlock(plainText))

        // Create a fake valid visual block
        val targetHex = "cafe"
        val symBlock = UnicodeObfuscator.obfuscate(targetHex, useSymbols = true)
        
        // Wrap it in general text
        val messageWithCipher = "Hello friend, here is the secret payload: ${symBlock} and some noise!"
        
        // Detects cipher
        assertTrue(CipherDetector.isCipherText(messageWithCipher))
        
        // Extracts the visual block perfectly
        val extractedBlock = CipherDetector.extractExactVisualBlock(messageWithCipher)
        assertNotNull(extractedBlock)
        
        // Assert that decryption matches targetHex
        val decrypted = UnicodeObfuscator.deobfuscate(extractedBlock ?: "")
        assertEquals(targetHex, decrypted)
        
        // Validates cipher successfully
        assertTrue(CipherDetector.validateCipher(messageWithCipher))
    }
}

