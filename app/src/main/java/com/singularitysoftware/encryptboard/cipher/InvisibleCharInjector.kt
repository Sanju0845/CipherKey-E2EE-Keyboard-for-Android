package com.singularitysoftware.encryptboard.cipher

import kotlin.random.Random

object InvisibleCharInjector {
    private val INVISIBLE_CHARS = listOf(
        '\u200B', // Zero Width Space
        '\u200D', // Zero Width Joiner
        '\u200C', // Zero Width Non-Joiner
        '\u2060'  // Word Joiner
    )

    /**
     * Randomly sprinkles invisible unicode characters into a text block to disrupt
     * standard pattern analysis.
     */
    fun inject(text: String): String {
        val result = StringBuilder()
        for (char in text) {
            result.append(char)
            // 30% chance to insert a random invisible character after an visible char
            if (Random.nextFloat() < 0.3f) {
                result.append(INVISIBLE_CHARS.random())
            }
        }
        return result.toString()
    }

    /**
     * Strips all invisible unicode characters from a text block prior to decryption.
     */
    fun remove(text: String): String {
        return text.filter { it !in INVISIBLE_CHARS }
    }
}
