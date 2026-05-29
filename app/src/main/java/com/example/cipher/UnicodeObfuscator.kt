package com.example.cipher

import kotlin.random.Random

object UnicodeObfuscator {
    // Futuristic symbols markers
    const val SYMBOL_PREFIX = "꧁"
    const val SYMBOL_SUFFIX = "꧂"

    // Aesthetic emoji markers
    const val EMOJI_PREFIX = "🌌✨"
    const val EMOJI_SUFFIX = "✨🌌"

    /**
     * Converts a hexadecimal string into an obfuscated visual string.
     * Incorporates dynamic rotation shifting and random zero-width insertion.
     */
    fun obfuscate(hexString: String, useSymbols: Boolean): String {
        // Pick a random rotation shift between 0 and 15
        val rotation = Random.nextInt(16)
        
        // Encode rotation digit as a hex character
        val rotationHex = Integer.toHexString(rotation)
        
        // Map the rotation digit using a FIXED rotation of 0 so it's readable by the decoder.
        val prefixCharVisual = SymbolMapper.mapToVisual(rotationHex, useSymbols, 0)
        
        // Map the payload using the randomized rotation
        val bodyVisual = SymbolMapper.mapToVisual(hexString, useSymbols, rotation)
        
        val rawVisual = if (useSymbols) {
            SYMBOL_PREFIX + prefixCharVisual + bodyVisual + SYMBOL_SUFFIX
        } else {
            EMOJI_PREFIX + prefixCharVisual + bodyVisual + EMOJI_SUFFIX
        }

        // Sprinkle invisible entropy
        return InvisibleCharInjector.inject(rawVisual)
    }

    /**
     * Reverses visual obfuscation to restore the original hex string.
     * Extracts mode automatically based on symbol concentrations.
     */
    fun deobfuscate(obfuscatedText: String): String? {
        // 1. Remove invisible characters first
        val cleaned = InvisibleCharInjector.remove(obfuscatedText).trim()

        // 2. Identify the wrapping mode and strip standard wrappers
        val useSymbols = SymbolMapper.detectMode(cleaned)
        val stripped = if (useSymbols) {
            if (cleaned.startsWith(SYMBOL_PREFIX) && cleaned.endsWith(SYMBOL_SUFFIX)) {
                cleaned.substring(SYMBOL_PREFIX.length, cleaned.length - SYMBOL_SUFFIX.length)
            } else {
                return null
            }
        } else {
            if (cleaned.startsWith(EMOJI_PREFIX) && cleaned.endsWith(EMOJI_SUFFIX)) {
                cleaned.substring(EMOJI_PREFIX.length, cleaned.length - EMOJI_SUFFIX.length)
            } else {
                return null
            }
        }

        if (stripped.isEmpty()) return null

        // 3. Extract the rotation digit symbol (the very first symbol)
        val pool = if (useSymbols) SymbolMapper.SYMBOL_STRINGS else SymbolMapper.EMOJI_STRINGS
        var rotationHexSymbol = ""
        var rotationDigit = -1
        for (digit in 0..15) {
            val symbol = pool[digit]
            if (stripped.startsWith(symbol)) {
                rotationHexSymbol = symbol
                rotationDigit = digit
                break
            }
        }

        if (rotationDigit == -1 || rotationHexSymbol.isEmpty()) return null

        val rotation = rotationDigit
        val bodyVisual = stripped.substring(rotationHexSymbol.length)

        // 4. Trace the body back to hex using the decoded shift key
        return SymbolMapper.mapFromVisual(bodyVisual, useSymbols, rotation)
    }
}
