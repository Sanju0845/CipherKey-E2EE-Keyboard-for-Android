package com.singularitysoftware.encryptboard.cipher

import kotlin.random.Random

object UnicodeObfuscator {
    const val SYMBOL_PREFIX = "꧁"
    const val SYMBOL_SUFFIX = "꧂"
    const val EMOJI_PREFIX = "🌌✨"
    const val EMOJI_SUFFIX = "✨🌌"

    fun obfuscate(hexString: String, useSymbols: Boolean): String {
        val rotation = Random.nextInt(16)
        val prefixCharVisual = SymbolMapper.rotationPrefixSymbol(rotation, useSymbols)
        val bodyVisual = SymbolMapper.mapToVisual(hexString, useSymbols, rotation)

        return if (useSymbols) {
            SYMBOL_PREFIX + prefixCharVisual + bodyVisual + SYMBOL_SUFFIX
        } else {
            EMOJI_PREFIX + prefixCharVisual + bodyVisual + EMOJI_SUFFIX
        }
    }

    fun deobfuscate(obfuscatedText: String): String? {
        return deobfuscateCandidates(obfuscatedText).firstOrNull()
    }

    fun deobfuscateCandidates(obfuscatedText: String): List<String> {
        val cleaned = SymbolMapper.normalizeVisualChar(
            InvisibleCharInjector.remove(obfuscatedText).trim()
        )
        if (cleaned.isEmpty()) return emptyList()

        val results = LinkedHashSet<String>()
        for (useSymbols in listOf(true, false)) {
            decodeFromCleaned(cleaned, useSymbols)?.let { results.add(it) }
        }
        return results.toList()
    }

    private fun decodeFromCleaned(cleaned: String, useSymbols: Boolean): String? {
        val stripped = stripWrappers(cleaned, useSymbols) ?: return null
        if (stripped.isEmpty()) return null

        for (rotation in 0 until 16) {
            val prefix = SymbolMapper.rotationPrefixSymbol(rotation, useSymbols)
            if (!stripped.startsWith(prefix)) continue
            val body = stripped.substring(prefix.length)
            val strict = SymbolMapper.mapFromVisualStrict(body, useSymbols, rotation)
            if (strict != null && isValidHex(strict)) return strict
        }
        return null
    }

    private fun stripWrappers(cleaned: String, useSymbols: Boolean): String? {
        return if (useSymbols) {
            if (!cleaned.contains(SYMBOL_PREFIX) || !cleaned.contains(SYMBOL_SUFFIX)) return null
            val start = cleaned.indexOf(SYMBOL_PREFIX)
            val end = cleaned.indexOf(SYMBOL_SUFFIX, start + SYMBOL_PREFIX.length)
            if (end == -1) return null
            cleaned.substring(start + SYMBOL_PREFIX.length, end)
        } else {
            if (!cleaned.contains(EMOJI_PREFIX) || !cleaned.contains(EMOJI_SUFFIX)) return null
            val start = cleaned.indexOf(EMOJI_PREFIX)
            val end = cleaned.indexOf(EMOJI_SUFFIX, start + EMOJI_PREFIX.length)
            if (end == -1) return null
            cleaned.substring(start + EMOJI_PREFIX.length, end)
        }
    }

    private fun isValidHex(hex: String): Boolean {
        return hex.isNotEmpty() && hex.length % 2 == 0 && hex.all { Character.digit(it, 16) >= 0 }
    }
}
