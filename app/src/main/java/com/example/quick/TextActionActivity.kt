package com.example.quick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.cipher.CipherDetector
import com.example.cipher.CipherEngine

/**
 * Invisible activity registered for android.intent.action.PROCESS_TEXT.
 * Android calls this when the user selects text and taps "Encrypt" or "Decrypt"
 * from the text selection toolbar (the Copy / Paste / Cut bar).
 *
 * Works in ANY app that uses the standard Android text selection:
 * WhatsApp, Telegram, Notes, Gmail, Browser, Instagram, etc.
 *
 * Two actions are registered:
 *   • com.example.cipherkey.ENCRYPT  — AES-encrypt selected text, return encrypted
 *   • com.example.cipherkey.DECRYPT  — AES-decrypt selected text, return plaintext
 */
class TextActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action ?: run { finish(); return }
        val selectedText = intent
            .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            .orEmpty()
            .trim()

        if (selectedText.isEmpty()) {
            toast("No text selected")
            finish()
            return
        }

        val readonly = !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        when (action) {
            ACTION_ENCRYPT -> handleEncrypt(selectedText, readonly)
            ACTION_DECRYPT -> handleDecrypt(selectedText, readonly)
            else -> finish()
        }
    }

    private fun handleEncrypt(text: String, readonly: Boolean) {
        if (CipherDetector.looksLikeCipher(text)) {
            toast("Already encrypted")
            finish()
            return
        }
        val useSymbols = getSharedPreferences("cipher_prefs", MODE_PRIVATE)
            .getBoolean("use_symbols", true)
        val encrypted = CipherEngine.encrypt(this, text, useSymbols)
        returnResult(encrypted)
        toast("🔒 Encrypted")
    }

    private fun handleDecrypt(text: String, readonly: Boolean) {
        if (!CipherDetector.looksLikeCipher(text)) {
            toast("No cipher text detected in selection")
            finish()
            return
        }
        val result = CipherEngine.decryptWithIntegrity(this, text)
        val plaintext = result.plaintext
        if (plaintext.isNullOrEmpty()) {
            toast("⚠ Decryption failed — wrong passphrase?")
            finish()
            return
        }
        if (!result.integrityOk) {
            toast("⚠ Integrity check failed — message may be tampered")
        } else {
            toast("🔓 Decrypted")
        }
        returnResult(plaintext)
    }

    private fun returnResult(text: String) {
        val resultIntent = Intent().apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val ACTION_ENCRYPT = "com.example.cipherkey.ENCRYPT"
        const val ACTION_DECRYPT = "com.example.cipherkey.DECRYPT"
    }
}
