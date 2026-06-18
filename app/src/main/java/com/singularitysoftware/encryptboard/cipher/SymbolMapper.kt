package com.singularitysoftware.encryptboard.cipher

object SymbolMapper {
    // 16 unique, highly specialized futuristic symbols
    val SYMBOLS = listOf(
        "⟟", "☍", "∆", "☄", "༒", "⌘", "⇢", "⊕", "⌿", "⍓", "⚙", "✦", "☯", "🔱", "❖", "⧓"
    )

    // 16 aesthetic emojis (no variation selectors — WhatsApp may strip FE0F)
    val EMOJIS = listOf(
        "🌙", "✨", "☄", "🫧", "⚡", "🪐", "🔮", "🧿", "🧬", "🖤", "🌌", "🚀", "👾", "📡", "🛡", "🔑"
    )

    val SYMBOL_STRINGS = SYMBOLS
    val EMOJI_STRINGS = EMOJIS

    fun normalizeVisualChar(text: String): String {
        return text
            .replace("\uFE0F", "")
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\u2060", "")
    }

    /**
     * Inspects the cleaned text to detect if it's formatted in symbols vs emoji mode.
     */
    fun detectMode(cleanedText: String): Boolean {
        val normalized = normalizeVisualChar(cleanedText)
        val symbolCount = SYMBOL_STRINGS.count { normalized.contains(normalizeVisualChar(it)) }
        val emojiCount = EMOJI_STRINGS.count { normalized.contains(normalizeVisualChar(it)) }
        return symbolCount >= emojiCount
    }

    fun mapToVisual(hexString: String, useSymbols: Boolean, rotation: Int): String {
        val pool = if (useSymbols) SYMBOL_STRINGS else EMOJI_STRINGS
        val rotatedPool = buildRotatedPool(pool, rotation)

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
     * Strict decode: fails if any character cannot be mapped (avoids silent corruption).
     */
    fun mapFromVisualStrict(visualString: String, useSymbols: Boolean, rotation: Int): String? {
        val normalized = normalizeVisualChar(visualString)
        val rotatedPool = buildRotatedPool(
            if (useSymbols) SYMBOL_STRINGS else EMOJI_STRINGS,
            rotation
        )
        val matchOrder = (0 until 16)
            .map { digit -> digit to normalizeVisualChar(rotatedPool[digit]) }
            .sortedByDescending { it.second.length }

        val result = StringBuilder()
        var i = 0
        while (i < normalized.length) {
            if (normalized[i].isWhitespace()) {
                i++
                continue
            }
            var matched = false
            for ((digit, symbol) in matchOrder) {
                if (symbol.isNotEmpty() && normalized.startsWith(symbol, i)) {
                    result.append(Integer.toHexString(digit))
                    i += symbol.length
                    matched = true
                    break
                }
            }
            if (!matched) return null
        }
        return result.toString()
    }

    /**
     * Lenient decode: skips unknown characters (legacy / heavily damaged payloads).
     */
    fun mapFromVisual(visualString: String, useSymbols: Boolean, rotation: Int): String {
        val normalized = normalizeVisualChar(visualString)
        val rotatedPool = buildRotatedPool(
            if (useSymbols) SYMBOL_STRINGS else EMOJI_STRINGS,
            rotation
        )
        val matchOrder = (0 until 16)
            .map { digit -> digit to normalizeVisualChar(rotatedPool[digit]) }
            .sortedByDescending { it.second.length }

        val result = StringBuilder()
        var i = 0
        while (i < normalized.length) {
            var matched = false
            for ((digit, symbol) in matchOrder) {
                if (symbol.isNotEmpty() && normalized.startsWith(symbol, i)) {
                    result.append(Integer.toHexString(digit))
                    i += symbol.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                i++
            }
        }
        return result.toString()
    }

    fun rotationPrefixSymbol(rotation: Int, useSymbols: Boolean): String {
        val rotationHex = Integer.toHexString(rotation % 16)
        return normalizeVisualChar(mapToVisual(rotationHex, useSymbols, 0))
    }

    private fun buildRotatedPool(pool: List<String>, rotation: Int): List<String> {
        val r = rotation % 16
        return List(16) { i -> pool[(i + r) % 16] }
    }
}
