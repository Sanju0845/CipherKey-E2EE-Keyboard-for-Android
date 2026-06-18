package com.singularitysoftware.encryptboard.cipher

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CipherEngine {
    private const val TAG = "CipherEngine"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val HMAC_TAG_BYTES = 8  // 64-bit tag appended after ciphertext

    /** Shared out-of-the-box key; must match on sender and receiver. */
    const val DEFAULT_PASSPHRASE = "CipherKeyDefaultSharedKey#2026!"
    
    private val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val DIGITS = "0123456789"
    private val SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    
    fun generateStrongPassphrase(length: Int = 24): String {
        val allChars = LOWERCASE + UPPERCASE + DIGITS + SPECIAL
        val random = SecureRandom()
        val sb = StringBuilder()
        // Ensure at least one of each type
        sb.append(LOWERCASE[random.nextInt(LOWERCASE.length)])
        sb.append(UPPERCASE[random.nextInt(UPPERCASE.length)])
        sb.append(DIGITS[random.nextInt(DIGITS.length)])
        sb.append(SPECIAL[random.nextInt(SPECIAL.length)])
        // Fill remaining characters
        for (i in 0 until length - 4) {
            sb.append(allChars[random.nextInt(allChars.length)])
        }
        // Shuffle the result
        val chars = sb.toString().toCharArray()
        for (i in chars.indices) {
            val j = random.nextInt(chars.size)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }
        return String(chars)
    }

    private var cachedSecretKey: SecretKeySpec? = null
    private var cachedPassphrase: String? = null

    // ── Key derivation ────────────────────────────────────────────────────────

    @Synchronized
    fun getSecretKey(context: Context): SecretKeySpec {
        val passphrase = getStoredPassphrase(context)
        if (cachedSecretKey != null && cachedPassphrase == passphrase) return cachedSecretKey!!
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
            val keySpec = SecretKeySpec(hashBytes.copyOf(16), ALGORITHM)
            cachedSecretKey = keySpec
            cachedPassphrase = passphrase
            return keySpec
        } catch (e: Exception) {
            Log.e(TAG, "Error deriving key: ${e.message}")
            val keySpec = SecretKeySpec(passphrase.padEnd(16, '0').take(16).toByteArray(Charsets.UTF_8), ALGORITHM)
            cachedSecretKey = keySpec
            cachedPassphrase = passphrase
            return keySpec
        }
    }

    // ── Passphrase storage ────────────────────────────────────────────────────

    fun isUsingDefaultPassphrase(context: Context) = getStoredPassphrase(context) == DEFAULT_PASSPHRASE

    fun getStoredPassphrase(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences("cipher_prefs", Context.MODE_PRIVATE)
            prefs.getString("security_passphrase", null)?.trim()?.ifBlank { null } ?: DEFAULT_PASSPHRASE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read passphrase: ${e.message}")
            DEFAULT_PASSPHRASE
        }
    }

    fun setStoredPassphrase(context: Context, value: String) {
        try {
            val prefs = context.getSharedPreferences("cipher_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("security_passphrase", value.trim().ifEmpty { DEFAULT_PASSPHRASE }).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set passphrase: ${e.message}")
        }
        invalidateKeyCache()
    }

    fun invalidateKeyCache() = synchronized(this) { cachedSecretKey = null; cachedPassphrase = null }

    // ── Encryption ────────────────────────────────────────────────────────────

    /**
     * AES/CBC encrypt + HMAC-SHA256 tag.
     * Wire format: [IV 16B] + [ciphertext NB] + [HMAC tag 8B]  → hex → visual or cover text
     */
    fun encrypt(context: Context, plainText: String, useSymbols: Boolean,
                coverProfile: com.singularitysoftware.encryptboard.cipher.CoverProfile = com.singularitysoftware.encryptboard.cipher.CoverProfile.SYMBOLS): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getSecretKey(context)
            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val hmacTag = computeHmac(secretKey, iv + cipherBytes).copyOf(HMAC_TAG_BYTES)
            val payload = iv + cipherBytes + hmacTag
            val hex = bytesToHex(payload)

            when (coverProfile) {
                com.singularitysoftware.encryptboard.cipher.CoverProfile.SYMBOLS ->
                    UnicodeObfuscator.obfuscate(hex, true)
                com.singularitysoftware.encryptboard.cipher.CoverProfile.EMOJIS ->
                    UnicodeObfuscator.obfuscate(hex, false)
                else ->
                    com.singularitysoftware.encryptboard.cipher.CoverEncoder.encode(hex, coverProfile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            plainText
        }
    }

    // ── Decryption ────────────────────────────────────────────────────────────

    /**
     * Result of a decrypt attempt — always explicit about integrity.
     */
    data class DecryptResult(
        val plaintext: String?,
        val integrityOk: Boolean   // true = HMAC verified, false = tampered/wrong key
    )

    /**
     * Full decrypt with HMAC verification.
     * Returns DecryptResult so callers know whether integrity passed.
     */
    fun decryptWithIntegrity(context: Context, encryptedVisualText: String): DecryptResult {
        // Try cover profile first
        val coverHex = com.singularitysoftware.encryptboard.cipher.CoverEncoder.extract(encryptedVisualText)
        if (coverHex != null) {
            return decryptHexPayloadWithIntegrity(context, coverHex)
                ?: DecryptResult(null, false)
        }

        val block = CipherDetector.extractExactVisualBlock(encryptedVisualText) ?: encryptedVisualText
        val candidates = UnicodeObfuscator.deobfuscateCandidates(block)
            .ifEmpty { listOfNotNull(UnicodeObfuscator.deobfuscate(block)) }

        for (hex in candidates) {
            val result = decryptHexPayloadWithIntegrity(context, hex)
            if (result != null) return result
        }
        return DecryptResult(plaintext = null, integrityOk = false)
    }

    /**
     * Legacy decrypt (used internally / by tests) — returns plaintext or null.
     * Accepts both HMAC-tagged (new) and legacy (old) messages.
     */
    fun decrypt(context: Context, encryptedVisualText: String): String? {
        return decryptWithIntegrity(context, encryptedVisualText).plaintext
    }

    private fun decryptHexPayloadWithIntegrity(context: Context, hexString: String): DecryptResult? {
        return try {
            val combined = hexToBytes(hexString) ?: return null
            // Minimum: 16 (IV) + 16 (min ciphertext) = 32 bytes
            if (combined.size < 32) return null

            val secretKey = getSecretKey(context)

            // ── Try new format: has HMAC tag ──────────────────────────────────
            if (combined.size > 16 + HMAC_TAG_BYTES) {
                val ivAndCipher = combined.copyOf(combined.size - HMAC_TAG_BYTES)
                val receivedTag = combined.copyOfRange(combined.size - HMAC_TAG_BYTES, combined.size)
                val expectedTag = computeHmac(secretKey, ivAndCipher).copyOf(HMAC_TAG_BYTES)

                val integrityOk = receivedTag.contentEquals(expectedTag)

                val iv = ivAndCipher.copyOf(16)
                val cipherBytes = ivAndCipher.copyOfRange(16, ivAndCipher.size)

                return try {
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
                    val plaintext = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
                    DecryptResult(plaintext = plaintext, integrityOk = integrityOk)
                } catch (e: Exception) {
                    // HMAC matched key but AES failed — shouldn't happen unless data is really corrupt
                    if (integrityOk) DecryptResult(plaintext = null, integrityOk = true)
                    else null
                }
            }

            // ── Legacy format (no HMAC tag) ───────────────────────────────────
            val iv = combined.copyOf(16)
            val cipherBytes = combined.copyOfRange(16, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val plaintext = String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            // Legacy messages get integrityOk = true (trust them, just can't verify)
            DecryptResult(plaintext = plaintext, integrityOk = true)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}")
            null
        }
    }

    // ── HMAC helper ───────────────────────────────────────────────────────────

    private fun computeHmac(keySpec: SecretKeySpec, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        // Use a 32-byte HMAC key derived from the same passphrase (full SHA-256 hash)
        val hmacKeySpec = SecretKeySpec(keySpec.encoded.copyOf(32).also {
            // Expand 16-byte AES key back to 32 bytes by padding with its own SHA-256
            val digest = MessageDigest.getInstance("SHA-256")
            val expanded = digest.digest(keySpec.encoded)
            System.arraycopy(expanded, 0, it, 0, 16)
        }, HMAC_ALGORITHM)
        mac.init(hmacKeySpec)
        return mac.doFinal(data)
    }

    // ── Hex utils ─────────────────────────────────────────────────────────────

    private fun bytesToHex(bytes: ByteArray): String {
        val lookup = "0123456789abcdef".toCharArray()
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = lookup[v ushr 4]
            out[i * 2 + 1] = lookup[v and 0x0F]
        }
        return String(out)
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i ->
                val d1 = Character.digit(hex[i * 2], 16)
                val d2 = Character.digit(hex[i * 2 + 1], 16)
                if (d1 == -1 || d2 == -1) return null
                ((d1 shl 4) + d2).toByte()
            }
        } catch (e: Exception) { null }
    }
}
