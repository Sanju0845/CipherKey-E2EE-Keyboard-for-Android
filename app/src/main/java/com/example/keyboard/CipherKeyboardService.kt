package com.example.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.cipher.CipherDetector
import com.example.cipher.CipherEngine
import com.example.cipher.CipherPrefs
import com.example.cipher.CipherScanner
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.MyApplicationTheme

class CipherKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // Manage standard Lifecycle, ViewModelStore, and SavedState bindings on service
    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store by lazy { ViewModelStore() }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // Layout configuration states
    private var activePage by mutableStateOf(KeyboardPage.LETTERS_LOWER)
    private var isCipherModeOn by mutableStateOf(false)
    private var useSymbols by mutableStateOf(true)
    private var composingDraft by mutableStateOf("")
    private val mainHandler = Handler(Looper.getMainLooper())

    // Word prediction state
    private var wordSuggestions by mutableStateOf<List<String>>(emptyList())

    // Which panel is showing: keyboard or clipboard
    private var showClipboard by mutableStateOf(false)

    // Clipboard entries shown in the panel
    private var clipboardEntries by mutableStateOf<List<ClipboardEntry>>(emptyList())

    override fun onCreate() {
        setTheme(com.example.R.style.Theme_MyApplication)
        super.onCreate()
        useSymbols = CipherPrefs.getUseSymbols(applicationContext)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // Attach clipboard store — listens for new copies automatically
        ClipboardStore.attach(applicationContext) {
            mainHandler.post { clipboardEntries = ClipboardStore.getEntries() }
        }
    }

    override fun onCreateInputView(): View {
        // Ensure the Window DecorView propagates the view-tree lifecycle/dependencies so child view lookups never fail
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this@CipherKeyboardService)
            decorView.setViewTreeViewModelStoreOwner(this@CipherKeyboardService)
            decorView.setViewTreeSavedStateRegistryOwner(this@CipherKeyboardService)
        }

        val composeView = ComposeView(this).apply {
            // Set disposal strategy tied to detaching from window since IMS is not destroyed with views
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            
            // Inject View tree bindings so that Jetpack Compose can boot smoothly inside IMS
            setViewTreeLifecycleOwner(this@CipherKeyboardService)
            setViewTreeViewModelStoreOwner(this@CipherKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@CipherKeyboardService)
            
            setContent {
                MyApplicationTheme {
                    KeyboardAppContent()
                }
            }
        }
        return composeView
    }

    @Composable
    fun KeyboardAppContent() {
        LaunchedEffect(Unit) {
            refreshClipboardEntries()
        }

        // Capture keyboard height once so clipboard uses the same value
        var keyboardHeightPx by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveBg)
        ) {
            // ── Always-visible strip ──────────────────────────────────────────
            KeyboardStrip(
                suggestions = if (showClipboard) emptyList() else wordSuggestions,
                onSuggestionClick = { handleSuggestionTap(it) },
                isCipherModeOn = isCipherModeOn,
                composingDraft = composingDraft,
                showClipboard = showClipboard,
                onToggleCipherMode = { isCipherModeOn = !isCipherModeOn },
                onOpenClipboard = {
                    if (!showClipboard) refreshClipboardEntries()
                    showClipboard = !showClipboard
                }
            )

            // ── Fixed-height container — prevents any layout jump ─────────────
            // The keyboard is always composed (invisible when clipboard shows) so
            // its height is always measured. Clipboard sits in the same Box at the
            // same height. Crossfade swaps them with no resize.
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Keyboard — always laid out so height is always known
                KeyboardView(
                    activePage = activePage,
                    isCipherModeOn = isCipherModeOn,
                    useSymbols = useSymbols,
                    onPageChange = { activePage = it },
                    onKeyPress = { handleKeyPress(it) },
                    onGlideWord = { handleGlideWord(it) },
                    onBackspace = { handleBackspace() },
                    onDeleteWord = { handleDeleteWord() },
                    onSpace = { handleSpace() },
                    onEnter = { handleEnter() },
                    onToggleCipher = { isCipherModeOn = !isCipherModeOn },
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            if (coords.size.height > 0) keyboardHeightPx = coords.size.height
                        }
                        .alpha(if (showClipboard) 0f else 1f)
                )

                // Clipboard — overlaid at the same height, crossfades in/out
                androidx.compose.animation.AnimatedVisibility(
                    visible = showClipboard,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(180))
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val clipH = if (keyboardHeightPx > 0)
                        with(density) { keyboardHeightPx.toDp() }
                    else 240.dp

                    ClipboardPanel(
                        entries = clipboardEntries,
                        onBackToKeyboard = { showClipboard = false },
                        onPasteRaw = { text ->
                            pasteIntoField(text)
                            showClipboard = false
                        },
                        onEncryptAndPaste = { text ->
                            val encrypted = CipherEngine.encrypt(applicationContext, text, useSymbols)
                            pasteIntoField(encrypted)
                            showClipboard = false
                        },
                        onDecryptAndPaste = { text ->
                            pasteIntoField(text)
                            showClipboard = false
                        },
                        onDecryptPreview = { text ->
                            val result = CipherEngine.decryptWithIntegrity(applicationContext, text)
                            DecryptPreviewResult(
                                plaintext = result.plaintext ?: "",
                                integrityOk = result.integrityOk
                            )
                        },
                        onDelete = { id ->
                            ClipboardStore.removeEntry(id)
                            clipboardEntries = ClipboardStore.getEntries()
                        },
                        modifier = Modifier.height(clipH)
                    )
                }
            }
        }
    }

    private fun handleKeyPress(key: String) {
        try {
            if (isCipherModeOn) {
                composingDraft += key
            } else {
                currentInputConnection?.commitText(key, 1)
                updateWordSuggestions()
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling key press: ${e.message}")
        }
    }

    private fun handleGlideWord(word: String) {
        if (word.isEmpty()) return
        try {
            if (isCipherModeOn) {
                composingDraft += word
            } else {
                currentInputConnection?.commitText(word, 1)
                updateWordSuggestions()
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling glide word: ${e.message}")
        }
    }

    private fun handleBackspace() {
        try {
            if (isCipherModeOn && composingDraft.isNotEmpty()) {
                composingDraft = composingDraft.dropLast(1)
                return
            }
            val ic = currentInputConnection ?: return
            // If there's a selection, delete it entirely
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
            updateWordSuggestions()
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling backspace: ${e.message}")
        }
    }

    private fun handleDeleteWord() {
        try {
            if (isCipherModeOn && composingDraft.isNotEmpty()) {
                // Delete last word in draft
                val trimmed = composingDraft.trimEnd()
                val lastSpace = trimmed.lastIndexOf(' ')
                composingDraft = if (lastSpace >= 0) trimmed.substring(0, lastSpace + 1) else ""
                return
            }
            val ic = currentInputConnection ?: return
            val before = ic.getTextBeforeCursor(200, 0)?.toString() ?: return
            if (before.isEmpty()) return
            // Find how many chars to delete: trim trailing spaces then delete back to prev space
            val trimmed = before.trimEnd()
            val charsToDelete = if (trimmed.isEmpty()) {
                before.length
            } else {
                val lastSpace = trimmed.lastIndexOf(' ')
                if (lastSpace < 0) trimmed.length + (before.length - trimmed.length)
                else (trimmed.length - lastSpace - 1) + (before.length - trimmed.length)
            }
            if (charsToDelete > 0) ic.deleteSurroundingText(charsToDelete, 0)
            updateWordSuggestions()
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error deleting word: ${e.message}")
        }
    }

    private fun handleSpace() {
        try {
            if (isCipherModeOn) {
                if (composingDraft.isNotEmpty()) {
                    val encrypted = CipherEngine.encrypt(applicationContext, composingDraft, useSymbols)
                    currentInputConnection?.commitText(encrypted, 1)
                    composingDraft = ""
                }
                currentInputConnection?.commitText(" ", 1)
            } else {
                currentInputConnection?.commitText(" ", 1)
                updateWordSuggestions()
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling space: ${e.message}")
        }
    }

    private fun handleEnter() {
        try {
            if (isCipherModeOn) {
                if (composingDraft.isNotEmpty()) {
                    val encrypted = CipherEngine.encrypt(applicationContext, composingDraft, useSymbols)
                    currentInputConnection?.commitText(encrypted, 1)
                    composingDraft = ""
                }
                currentInputConnection?.commitText("\n", 1)
            } else {
                currentInputConnection?.commitText("\n", 1)
                wordSuggestions = emptyList()
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling enter: ${e.message}")
        }
    }

    private fun handleSuggestionTap(word: String) {
        try {
            val ic = currentInputConnection ?: return
            // Delete the current partial word being typed
            val before = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
            val partialWord = before.takeLastWhile { it.isLetter() || it == '\'' }
            if (partialWord.isNotEmpty()) {
                ic.deleteSurroundingText(partialWord.length, 0)
            }
            // Commit the suggestion + space
            ic.commitText("$word ", 1)
            updateWordSuggestions()
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error committing suggestion: ${e.message}")
        }
    }

    private fun updateWordSuggestions() {
        if (isCipherModeOn) {
            wordSuggestions = emptyList()
            return
        }
        try {
            val ic = currentInputConnection ?: run {
                wordSuggestions = emptyList()
                return
            }
            val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
            // Current partial word (letters being typed right now)
            val currentWord = before.takeLastWhile { it.isLetter() || it == '\'' }
            // Last completed word (for bigram context)
            val beforeCurrent = before.dropLast(currentWord.length).trimEnd()
            val lastWord = beforeCurrent.takeLastWhile { it.isLetter() || it == '\'' }
            wordSuggestions = WordPredictor.predict(currentWord, lastWord)
        } catch (e: Exception) {
            wordSuggestions = emptyList()
        }
    }

    private fun pasteIntoField(text: String) {
        try {
            currentInputConnection?.commitText(text, 1)
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Paste failed: ${e.message}")
        }
    }

    /** Decrypts every cipher block found in a text, replacing each with its plaintext. */
    private fun decryptFullText(text: String): String {
        var result = text
        var iterations = 0
        while (iterations++ < 50) {
            val block = com.example.cipher.CipherDetector.extractExactVisualBlock(result) ?: break
            val plain = CipherEngine.decrypt(applicationContext, block) ?: break
            result = result.replace(block, plain)
        }
        return result
    }

    private fun refreshClipboardEntries() {
        clipboardEntries = ClipboardStore.getEntries()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        activePage = KeyboardPage.LETTERS_LOWER
        composingDraft = ""
        wordSuggestions = emptyList()
        clipboardEntries = emptyList()
        showClipboard = false
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        refreshClipboardEntries()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Correctly tear down / pause the view tree lifecycle when input view finishes
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        composingDraft = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        ClipboardStore.detach()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
