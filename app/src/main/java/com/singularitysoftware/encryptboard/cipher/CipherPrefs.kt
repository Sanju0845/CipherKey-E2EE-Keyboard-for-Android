package com.singularitysoftware.encryptboard.cipher

import android.content.Context

/** Shared prefs for symbol mode and quick-translator UI. */
object CipherPrefs {
    private const val NAME = "cipher_prefs"
    private const val KEY_USE_SYMBOLS = "use_symbols"
    private const val KEY_BUBBLE_ENABLED = "quick_bubble_enabled"
    private const val KEY_THEME_MODE = "theme_mode" // "dark", "light", or "system"

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, "dark") ?: "dark"
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }

    fun getUseSymbols(context: Context): Boolean {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_SYMBOLS, true)
    }

    fun setUseSymbols(context: Context, useSymbols: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_SYMBOLS, useSymbols)
            .apply()
    }

    fun isBubbleEnabled(context: Context): Boolean {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BUBBLE_ENABLED, false)
    }

    fun setBubbleEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BUBBLE_ENABLED, enabled)
            .apply()
    }
}
