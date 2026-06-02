package com.example.quick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.cipher.CipherDetector
import com.example.cipher.CipherEngine

/** Shown as "🔓 Decrypt" in the text selection toolbar. */
class DecryptTextAction : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()?.trim().orEmpty()
        if (text.isEmpty()) { finish(); return }

        if (!CipherDetector.looksLikeCipher(text)) {
            Toast.makeText(this, "No cipher text in selection", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        val result = CipherEngine.decryptWithIntegrity(this, text)
        val plain = result.plaintext
        if (plain.isNullOrEmpty()) {
            Toast.makeText(this, "⚠ Decryption failed — wrong passphrase?", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        val msg = if (result.integrityOk) "🔓 Decrypted" else "⚠ Decrypted (integrity warning)"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, plain))
        finish()
    }
}
