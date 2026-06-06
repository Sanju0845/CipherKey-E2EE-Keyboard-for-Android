package com.singularitysoftware.encryptboard.cipher

import android.content.Context

/**
 * Tries multiple ways to locate and decrypt CipherKey payloads from messy chat-app text.
 */
object CipherScanner {
    data class ScanResult(
        val plaintext: String,
        val sourceBlock: String
    )

    enum class FailureReason {
        NONE,
        NOT_CIPHER,
        DAMAGED,
        WRONG_KEY
    }

    fun tryDecrypt(context: Context, rawText: String?): ScanResult? {
        if (rawText.isNullOrBlank()) return null

        val candidates = buildCandidateTexts(rawText)
        for (candidate in candidates) {
            val block = CipherDetector.extractExactVisualBlock(candidate) ?: candidate.trim()
            if (!CipherDetector.looksLikeCipher(block) && !CipherDetector.looksLikeCipher(candidate)) {
                continue
            }
            val decrypted = CipherEngine.decrypt(context, block) ?: continue
            if (decrypted.isNotEmpty()) {
                return ScanResult(decrypted, block)
            }
        }
        return null
    }

    fun diagnose(context: Context, rawText: String?): FailureReason {
        if (rawText.isNullOrBlank()) return FailureReason.NOT_CIPHER

        val candidates = buildCandidateTexts(rawText)
        var sawCipherShape = false
        var sawRecoverableHex = false

        for (candidate in candidates) {
            if (!CipherDetector.looksLikeCipher(candidate)) continue
            sawCipherShape = true
            val block = CipherDetector.extractExactVisualBlock(candidate) ?: candidate.trim()
            val hexList = UnicodeObfuscator.deobfuscateCandidates(block)
            if (hexList.isEmpty()) continue
            sawRecoverableHex = true
            if (CipherEngine.decrypt(context, block) != null) {
                return FailureReason.NONE
            }
        }

        return when {
            !sawCipherShape -> FailureReason.NOT_CIPHER
            !sawRecoverableHex -> FailureReason.DAMAGED
            else -> FailureReason.WRONG_KEY
        }
    }

    private fun buildCandidateTexts(raw: String): List<String> {
        val trimmed = raw.trim()
        val cleaned = normalizeForScan(raw)
        val block = CipherDetector.extractExactVisualBlock(raw)
            ?: CipherDetector.extractExactVisualBlock(cleaned)

        return listOfNotNull(
            raw,
            trimmed,
            cleaned,
            block,
            block?.let { normalizeForScan(it) }
        ).distinct().filter { it.isNotEmpty() }
    }

    private fun normalizeForScan(text: String): String {
        return InvisibleCharInjector.remove(
            text
                .replace("\u200e", "")
                .replace("\u200f", "")
                .replace("\u202a", "")
                .replace("\u202c", "")
        ).trim()
    }
}
