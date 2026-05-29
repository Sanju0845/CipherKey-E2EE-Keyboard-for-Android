package com.example.cipher

object SymbolMapper {
    // 16 unique, highly specialized futuristic symbols
    val SYMBOLS = listOf(
        "⟟", "☍", "∆", "☄", "༒", "⌘", "⇢", "⊕", "⌿", "⍓", "⚙", "✦", "☯", "🔱", "❖", "⧓"
    )

    // 16 aesthetic emojis
    val EMOJIS = listOf(
        "🌙", "✨", "☄️", "🫧", "⚡", "🪐", "🔮", "🧿", "🧬", "🖤", "🌌", "🚀", "👾", "📡", "🛡️", "🔑"
    )

    val SYMBOL_STRINGS = SYMBOLS
    val EMOJI_STRINGS = EMOJIS

    /**
     * Inspects the cleaned text to detect if it's formatted in symbols vs emoji mode.
     */
    fun detectMode(cleanedText: String): Boolean {
        val symbolCount = SYMBOL_STRINGS.count { cleanedText.contains(it) }
        val emojiCount = EMOJI_STRINGS.count { cleanedText.contains(it) }
        return symbolCount >= emojiCount
    }

    /**
     * Map a hex string (0-9, a-f) to visual symbols or emojis with a rotation shift index.
     * @param hexString The hexadecimal representation of the cipher data (e.g. "a5f3").
     * @param useSymbols Mode toggle (true for futuristic symbols, false for emojis).
     * @param rotation Shift value between 0 and 15 to rotate the mapping table.
     */
    fun mapToVisual(hexString: String, useSymbols: Boolean, rotation: Int): String {
        val pool = if (useSymbols) SYMBOL_STRINGS else EMOJI_STRINGS
        val rotatedPool = ArrayList<String>(16)
        val r = rotation % 16
        // Rotate pool
        for (i in 0 until 16) {
            rotatedPool.add(pool[(i + r) % 16])
        }

        val result = StringBuilder()
        for (char in hexString) {
            val digit = Character.digit(char, 16)
            if (digit in 0..15) {
                result.append(rotatedPool[digit])
            }
        }
        return result.toString()
    }

    /**
     * Map visual symbols or emojis back to a hex string using the specified rotation.
     */
    fun mapFromVisual(visualString: String, useSymbols: Boolean, rotation: Int): String {
        val pool = if (useSymbols) SYMBOL_STRINGS else EMOJI_STRINGS
        val rotatedPool = ArrayList<String>(16)
        val r = rotation % 16
        for (i in 0 until 16) {
            rotatedPool.add(pool[(i + r) % 16])
        }

        val result = StringBuilder()
        var i = 0
        while (i < visualString.length) {
            var matched = false
            for (digit in 0..15) {
                val symbol = rotatedPool[digit]
                if (visualString.startsWith(symbol, i)) {
                    result.append(Integer.toHexString(digit))
                    i += symbol.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                i++ // Skip characters that do not match the key symbols (noise defense)
            }
        }
        return result.toString()
    }
}
