package com.example.cipher

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CipherEngine {
    private const val TAG = "CipherEngine"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    
    /** Shared out-of-the-box key; must match on sender and receiver. */
    const val DEFAULT_PASSPHRASE = "CipherKeyDefaultSharedKey#2026!"
    
    private var cachedSecretKey: SecretKeySpec? = null
    private var cachedPassphrase: String? = null

    /**
     * Resolves the AES 128-bit key. Caches the resulting Spec so that key derivation
     * does not block the typing loop, ensuring sub-millisecond operational performance.
     */
    @Synchronized
    fun getSecretKey(context: Context): SecretKeySpec {
        val passphrase = getStoredPassphrase(context)
        if (cachedSecretKey != null && cachedPassphrase == passphrase) {
            return cachedSecretKey!!
        }
        
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
            val keyBytes = hashBytes.copyOf(16) // Truncate to 128 bit (16 bytes) standard AES
            val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
            cachedSecretKey = keySpec
            cachedPassphrase = passphrase
            return keySpec
        } catch (e: Exception) {
            Log.e(TAG, "Error deriving key: ${e.message}")
            val keyBytes = passphrase.padEnd(16, '0').take(16).toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
            cachedSecretKey = keySpec
            cachedPassphrase = passphrase
            return keySpec
        }
    }

    /**
     * Store and Retrieve custom passphrase using local secure SharedPreferences.
     */
    fun isUsingDefaultPassphrase(context: Context): Boolean {
        return getStoredPassphrase(context) == DEFAULT_PASSPHRASE
    }

    fun getStoredPassphrase(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences("cipher_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("security_passphrase", null)
            when {
                raw.isNullOrBlank() -> DEFAULT_PASSPHRASE
                else -> raw.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read getStoredPassphrase: ${e.message}")
            DEFAULT_PASSPHRASE
        }
    }

    fun setStoredPassphrase(context: Context, value: String) {
        val normalized = value.trim().ifEmpty { DEFAULT_PASSPHRASE }
        try {
            val prefs = context.getSharedPreferences("cipher_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("security_passphrase", normalized).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setStoredPassphrase: ${e.message}")
        }
        invalidateKeyCache()
    }

    fun invalidateKeyCache() {
        synchronized(this) {
            cachedSecretKey = null
            cachedPassphrase = null
        }
    }

    /**
     * Performs standard AES/CBC encryption and wraps results inside a custom visual-unicode pack.
     */
    fun encrypt(context: Context, plainText: String, useSymbols: Boolean): String {
        if (plainText.isEmpty()) return ""
        try {
            val secretKey = getSecretKey(context)
            
            // 1. Setup random 16-byte initialization vector
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            
            // 2. Perform AES encryption
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // 3. Assemble: combined = [IV (16)] + [Cipher (Var)]
            val combinedBytes = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combinedBytes, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combinedBytes, iv.size, encryptedBytes.size)
            
            // 4. Encode bytes as hex standard representation
            val hexString = bytesToHex(combinedBytes)
            
            // 5. Wrap inside visual mappings and zero-width spacers
            return UnicodeObfuscator.obfuscate(hexString, useSymbols)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            return plainText
        }
    }

    /**
     * Resolves visual-unicode packets and performs standard AES/CBC decryption.
     */
    fun decrypt(context: Context, encryptedVisualText: String): String? {
        val block = CipherDetector.extractExactVisualBlock(encryptedVisualText) ?: encryptedVisualText
        val hexCandidates = UnicodeObfuscator.deobfuscateCandidates(block)
        if (hexCandidates.isEmpty()) {
            UnicodeObfuscator.deobfuscate(block)?.let { return decryptHexPayload(context, it) }
            return null
        }
        for (hex in hexCandidates) {
            decryptHexPayload(context, hex)?.let { return it }
        }
        return null
    }

    private fun decryptHexPayload(context: Context, hexString: String): String? {
        try {
            val combinedBytes = hexToBytes(hexString) ?: return null
            if (combinedBytes.size <= 16) return null

            val iv = ByteArray(16)
            System.arraycopy(combinedBytes, 0, iv, 0, iv.size)

            val encryptedBytes = ByteArray(combinedBytes.size - iv.size)
            System.arraycopy(combinedBytes, iv.size, encryptedBytes, 0, encryptedBytes.size)

            val secretKey = getSecretKey(context)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}")
            return null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val lookup = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = lookup[v ushr 4]
            hexChars[i * 2 + 1] = lookup[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            val index = i * 2
            val d1 = Character.digit(hex[index], 16)
            val d2 = Character.digit(hex[index + 1], 16)
            if (d1 == -1 || d2 == -1) return null
            bytes[i] = ((d1 shl 4) + d2).toByte()
        }
        return bytes
    }
}
