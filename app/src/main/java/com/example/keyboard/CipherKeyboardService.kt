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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

    // Which panel is showing: keyboard, clipboard or AI
    private var activePanel by mutableStateOf(ActivePanel.KEYBOARD)
    private var showPanelSelector by mutableStateOf(false)

    // Cover profile state
    private var coverProfile by mutableStateOf(com.example.cipher.CoverProfile.SYMBOLS)
    private var showProfilePicker by mutableStateOf(false)

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
        // Ensure the Window DecorView propagates the view-tree lifecycle/dependencies
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this@CipherKeyboardService)
            decorView.setViewTreeViewModelStoreOwner(this@CipherKeyboardService)
            decorView.setViewTreeSavedStateRegistryOwner(this@CipherKeyboardService)
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
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
        LaunchedEffect(Unit) { refreshClipboardEntries() }

        var keyboardHeightPx by remember { mutableStateOf(0) }
        val isClipboard = activePanel == ActivePanel.CLIPBOARD
        val isAi = activePanel == ActivePanel.AI

        Column(modifier = Modifier.fillMaxWidth().background(ImmersiveBg)) {

            // ── Strip ──────────────────────────────────────────────────────────
            KeyboardStrip(
                suggestions = if (activePanel != ActivePanel.KEYBOARD) emptyList() else wordSuggestions,
                onSuggestionClick = { handleSuggestionTap(it) },
                isCipherModeOn = isCipherModeOn,
                composingDraft = composingDraft,
                showClipboard = isClipboard,
                activePanel = activePanel,
                onToggleCipherMode = { isCipherModeOn = !isCipherModeOn },
                onOpenClipboard = {
                    showPanelSelector = !showPanelSelector
                }
            )

            // ── Profile picker ────────────────────────────────────────────────
            if (showProfilePicker && activePanel == ActivePanel.KEYBOARD) {
                ProfilePickerSheet(
                    currentProfile = coverProfile,
                    onSelectProfile = { profile ->
                        coverProfile = profile
                        showProfilePicker = false
                    }
                )
            }

            // ── Main content area ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                // Keyboard (always laid out, invisible when another panel shows)
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
                    onLongPressCipher = { showProfilePicker = !showProfilePicker },
                    coverProfileEmoji = coverProfile.emoji,
                    onSwitchKeyboard = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                            as android.view.inputmethod.InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            if (coords.size.height > 0) keyboardHeightPx = coords.size.height
                        }
                        .alpha(if (activePanel == ActivePanel.KEYBOARD) 1f else 0f)
                        .then(
                            if (activePanel != ActivePanel.KEYBOARD)
                                Modifier.pointerInput(Unit) {} else Modifier
                        )
                )

                // Clipboard panel
                androidx.compose.animation.AnimatedVisibility(
                    visible = isClipboard,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(180))
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val clipH = if (keyboardHeightPx > 0) with(density) { keyboardHeightPx.toDp() } else 240.dp
                    ClipboardPanel(
                        entries = clipboardEntries,
                        onBackToKeyboard = { activePanel = ActivePanel.KEYBOARD },
                        onPasteRaw = { text -> pasteIntoField(text); activePanel = ActivePanel.KEYBOARD },
                        onEncryptAndPaste = { text ->
                            pasteIntoField(CipherEngine.encrypt(applicationContext, text, useSymbols))
                            activePanel = ActivePanel.KEYBOARD
                        },
                        onDecryptAndPaste = { text -> pasteIntoField(text); activePanel = ActivePanel.KEYBOARD },
                        onDecryptPreview = { text ->
                            val r = CipherEngine.decryptWithIntegrity(applicationContext, text)
                            DecryptPreviewResult(r.plaintext ?: "", r.integrityOk)
                        },
                        onDelete = { id ->
                            ClipboardStore.removeEntry(id)
                            clipboardEntries = ClipboardStore.getEntries()
                        },
                        modifier = Modifier.height(clipH)
                    )
                }

                // AI panel — WebView kept alive, conversation persists
                androidx.compose.animation.AnimatedVisibility(
                    visible = isAi,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(180))
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val panelH = if (keyboardHeightPx > 0) with(density) { keyboardHeightPx.toDp() } else 240.dp
                    AiPanel(modifier = Modifier.height(panelH))
                }

                // Panel selector — overlays at keyboard height, no extra height added
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPanelSelector,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(120))
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val selectorH = if (keyboardHeightPx > 0) with(density) { keyboardHeightPx.toDp() } else 240.dp
                    Box(modifier = Modifier.height(selectorH)) {
                        PanelSelector(
                            currentPanel = activePanel,
                            onSelectPanel = { panel ->
                                if (panel == ActivePanel.CLIPBOARD) refreshClipboardEntries()
                                activePanel = panel
                                showPanelSelector = false
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
                val trimmed = composingDraft.trimEnd()
                val lastSpace = trimmed.lastIndexOf(' ')
                composingDraft = if (lastSpace >= 0) trimmed.substring(0, lastSpace + 1) else ""
                return
            }
            val ic = currentInputConnection ?: return
            val before = ic.getTextBeforeCursor(200, 0)?.toString() ?: return
            if (before.isEmpty()) return
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
                    val encrypted = CipherEngine.encrypt(applicationContext, composingDraft, useSymbols, coverProfile)
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
                    val encrypted = CipherEngine.encrypt(applicationContext, composingDraft, useSymbols, coverProfile)
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
            val before = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
            val partialWord = before.takeLastWhile { it.isLetter() || it == '\'' }
            if (partialWord.isNotEmpty()) {
                ic.deleteSurroundingText(partialWord.length, 0)
            }
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
            val currentWord = before.takeLastWhile { it.isLetter() || it == '\'' }
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
            val block = CipherDetector.extractExactVisualBlock(result) ?: break
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
        activePanel = ActivePanel.KEYBOARD
        showPanelSelector = false
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        refreshClipboardEntries()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
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
