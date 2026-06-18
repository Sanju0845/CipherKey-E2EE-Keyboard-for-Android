package com.singularitysoftware.encryptboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.singularitysoftware.encryptboard.cipher.CipherEngine
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CipherEngineRoundTripTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("cipher_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun encryptDecrypt_hi_withCustomPassphrase() {
        CipherEngine.setStoredPassphrase(context, "22345")
        val encrypted = CipherEngine.encrypt(context, "hi", useSymbols = true)
        val decrypted = CipherEngine.decrypt(context, encrypted)
        assertEquals("hi", decrypted)
    }

    @Test
    fun encryptDecrypt_hi_withDefaultPassphrase() {
        CipherEngine.setStoredPassphrase(context, "")
        val encrypted = CipherEngine.encrypt(context, "hi", useSymbols = true)
        val decrypted = CipherEngine.decrypt(context, encrypted)
        assertEquals("hi", decrypted)
    }
}
