package com.example.keyboard

import android.content.ClipboardManager
import android.content.Context
import com.example.cipher.CipherDetector

/**
 * In-memory clipboard history store.
 * Listens to system clipboard changes and keeps up to MAX_ENTRIES unique entries.
 */
object ClipboardStore {

    private const val MAX_ENTRIES = 20

    // Ordered newest-first
    private val _entries = mutableListOf<ClipboardEntry>()
    private var nextId = 0
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var manager: ClipboardManager? = null

    /** Call once from the keyboard service onCreate / onStartInputView */
    fun attach(context: Context, onChange: () -> Unit) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager = cm
        // Read whatever is already on the clipboard
        ingestCurrentClip(context)
        // Listen for future changes
        val l = ClipboardManager.OnPrimaryClipChangedListener {
            ingestCurrentClip(context)
            onChange()
        }
        cm.addPrimaryClipChangedListener(l)
        listener = l
    }

    /** Call from the keyboard service onDestroy */
    fun detach() {
        listener?.let { manager?.removePrimaryClipChangedListener(it) }
        listener = null
        manager = null
    }

    fun getEntries(): List<ClipboardEntry> = _entries.toList()

    fun removeEntry(id: Int) {
        _entries.removeAll { it.id == id }
    }

    // ── private ──────────────────────────────────────────────────────────────

    private fun ingestCurrentClip(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        for (i in 0 until clip.itemCount) {
            val text = clip.getItemAt(i)
                .coerceToText(context)
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() } ?: continue
            addEntry(text)
        }
    }

    private fun addEntry(text: String) {
        // Don't add duplicates — move to top instead
        val existing = _entries.indexOfFirst { it.text == text }
        if (existing != -1) {
            val entry = _entries.removeAt(existing)
            _entries.add(0, entry)
            return
        }
        _entries.add(
            0, ClipboardEntry(
                id = nextId++,
                text = text,
                isCipher = CipherDetector.looksLikeCipher(text)
            )
        )
        if (_entries.size > MAX_ENTRIES) {
            _entries.removeAt(_entries.size - 1)
        }
    }
}
