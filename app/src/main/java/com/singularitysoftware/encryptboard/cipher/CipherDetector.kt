package com.singularitysoftware.encryptboard.cipher

object CipherDetector {
    /**
     * Instantly checks if any text contains or represents a CipherKey package layout.
     */
    fun isCipherText(text: String): Boolean = looksLikeCipher(text)

    fun looksLikeCipher(text: String): Boolean {
        // Check cover profile FIRST on raw text (before any normalization strips invisible chars)
        val hasCover = com.singularitysoftware.encryptboard.cipher.CoverEncoder.isCoverText(text)
        if (hasCover) return true
        val cleaned = normalizeForDetection(text)
        val hasSymbols = cleaned.contains(UnicodeObfuscator.SYMBOL_PREFIX) &&
            cleaned.contains(UnicodeObfuscator.SYMBOL_SUFFIX)
        val hasEmojis = cleaned.contains(UnicodeObfuscator.EMOJI_PREFIX) &&
            cleaned.contains(UnicodeObfuscator.EMOJI_SUFFIX)
        return hasSymbols || hasEmojis
    }

    /**
     * Extracts the specific styled CipherKey visual block from a surround message (if any).
     */
    fun extractExactVisualBlock(text: String): String? {
        val sources = listOf(text, normalizeForDetection(text)).distinct()
        for (source in sources) {
            val symStart = source.indexOf(UnicodeObfuscator.SYMBOL_PREFIX)
            if (symStart != -1) {
                val symEnd = source.indexOf(
                    UnicodeObfuscator.SYMBOL_SUFFIX,
                    symStart + UnicodeObfuscator.SYMBOL_PREFIX.length
                )
                if (symEnd != -1) {
                    return source.substring(symStart, symEnd + UnicodeObfuscator.SYMBOL_SUFFIX.length)
                }
            }

            val emoStart = source.indexOf(UnicodeObfuscator.EMOJI_PREFIX)
            if (emoStart != -1) {
                val emoEnd = source.indexOf(
                    UnicodeObfuscator.EMOJI_SUFFIX,
                    emoStart + UnicodeObfuscator.EMOJI_PREFIX.length
                )
                if (emoEnd != -1) {
                    return source.substring(emoStart, emoEnd + UnicodeObfuscator.EMOJI_SUFFIX.length)
                }
            }
        }
        return null
    }

    private fun normalizeForDetection(text: String): String {
        return InvisibleCharInjector.remove(
            text
                .replace("\u200e", "")
                .replace("\u200f", "")
                .replace("\u202a", "")
                .replace("\u202c", "")
        ).trim()
    }

    /**
     * Validates if a message is indeed a complete, decryptable CipherKey load.
     */
    fun validateCipher(text: String): Boolean {
        val block = extractExactVisualBlock(text) ?: return false
        val hex = UnicodeObfuscator.deobfuscate(block)
        return hex != null && hex.isNotEmpty()
    }
}
