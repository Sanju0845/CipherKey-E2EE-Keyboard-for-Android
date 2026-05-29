package com.example.cipher

object CipherDetector {
    /**
     * Instantly checks if any text contains or represents a CipherKey package layout.
     */
    fun isCipherText(text: String): Boolean {
        val cleaned = InvisibleCharInjector.remove(text).trim()
        
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
        // Find standard symbol boundaries in the original text
        val symStart = text.indexOf(UnicodeObfuscator.SYMBOL_PREFIX)
        if (symStart != -1) {
            val symEnd = text.indexOf(UnicodeObfuscator.SYMBOL_SUFFIX, symStart + UnicodeObfuscator.SYMBOL_PREFIX.length)
            if (symEnd != -1) {
                return text.substring(symStart, symEnd + UnicodeObfuscator.SYMBOL_SUFFIX.length)
            }
        }

        // Find standard emoji boundaries in the original text
        val emoStart = text.indexOf(UnicodeObfuscator.EMOJI_PREFIX)
        if (emoStart != -1) {
            val emoEnd = text.indexOf(UnicodeObfuscator.EMOJI_SUFFIX, emoStart + UnicodeObfuscator.EMOJI_PREFIX.length)
            if (emoEnd != -1) {
                return text.substring(emoStart, emoEnd + UnicodeObfuscator.EMOJI_SUFFIX.length)
            }
        }

        return null
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
