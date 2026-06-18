package com.singularitysoftware.encryptboard.quick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.singularitysoftware.encryptboard.cipher.CipherDetector
import com.singularitysoftware.encryptboard.cipher.CipherEngine

/** Shown as "🔒 Encrypt" in the text selection toolbar. */
class EncryptTextAction : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()?.trim().orEmpty()
        if (text.isEmpty()) { finish(); return }

        if (CipherDetector.looksLikeCipher(text)) {
            Toast.makeText(this, "Already encrypted", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        val useSymbols = getSharedPreferences("cipher_prefs", MODE_PRIVATE)
            .getBoolean("use_symbols", true)
        val encrypted = CipherEngine.encrypt(this, text, useSymbols)
        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, encrypted))
        Toast.makeText(this, "🔒 Encrypted", Toast.LENGTH_SHORT).show()
        finish()
    }
}
