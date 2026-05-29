package com.example.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
    
    private var detectedEncryptedText by mutableStateOf<String?>(null)
    private var decryptedTextPreview by mutableStateOf<String?>(null)
    private var hasActiveTextToEncrypt by mutableStateOf(false)

    override fun onCreate() {
        setTheme(com.example.R.style.Theme_MyApplication)
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
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
        // Automatically check clipboard status and text boxes whenever compositions shift
        LaunchedEffect(composingDraft, isCipherModeOn) {
            updateActiveTextFieldStatus()
            // Avoid toolbar swapping to decrypt preview while drafting encrypted text
            if (isCipherModeOn && composingDraft.isNotEmpty()) {
                decryptedTextPreview = null
                detectedEncryptedText = null
            } else {
                scanForCiphers()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveBg)
        ) {
            SuggestionBar(
                decryptedText = decryptedTextPreview,
                composingDraft = composingDraft,
                isCipherModeOn = isCipherModeOn,
                useSymbols = useSymbols,
                onToggleCipherMode = { isCipherModeOn = !isCipherModeOn },
                onToggleUseSymbols = { useSymbols = !useSymbols },
                onCopyDecrypted = {
                    decryptedTextPreview?.let { text ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Decrypted Cleartext", text))
                    }
                },
                onReplaceDecrypted = {
                    decryptedTextPreview?.let { text ->
                        replaceWithDecrypted(text)
                    }
                },
                onReplyEncrypted = {
                    isCipherModeOn = true
                    decryptedTextPreview = null
                    detectedEncryptedText = null
                },
                onEncryptActiveField = {
                    encryptActiveField()
                },
                onDecryptScan = { manualDecryptScan() },
                onOpenConfig = {
                    try {
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("CipherKeyboard", "Failed to launch config activity: ${e.message}")
                    }
                },
                hasActiveTextToEncrypt = hasActiveTextToEncrypt && composingDraft.isEmpty()
            )

            KeyboardView(
                activePage = activePage,
                isCipherModeOn = isCipherModeOn,
                useSymbols = useSymbols,
                onPageChange = { activePage = it },
                onKeyPress = { handleKeyPress(it) },
                onBackspace = { handleBackspace() },
                onSpace = { handleSpace() },
                onEnter = { handleEnter() },
                onToggleCipher = { isCipherModeOn = !isCipherModeOn }
            )
        }
    }

    private fun handleKeyPress(key: String) {
        try {
            if (isCipherModeOn) {
                composingDraft += key
            } else {
                currentInputConnection?.commitText(key, 1)
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling key press: ${e.message}")
        }
        updateActiveTextFieldStatus()
    }

    private fun handleBackspace() {
        try {
            if (isCipherModeOn && composingDraft.isNotEmpty()) {
                composingDraft = composingDraft.dropLast(1)
            } else {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling backspace: ${e.message}")
        }
        updateActiveTextFieldStatus()
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
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling space: ${e.message}")
        }
        updateActiveTextFieldStatus()
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
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error handling enter: ${e.message}")
        }
        updateActiveTextFieldStatus()
    }

    private fun updateActiveTextFieldStatus() {
        try {
            val ic = currentInputConnection
            if (ic != null) {
                val before = ic.getTextBeforeCursor(100, 0) ?: ""
                val after = ic.getTextAfterCursor(100, 0) ?: ""
                hasActiveTextToEncrypt = before.isNotEmpty() || after.isNotEmpty()
            } else {
                hasActiveTextToEncrypt = false
            }
        } catch (e: Exception) {
            hasActiveTextToEncrypt = false
        }
    }

    private fun scanForCiphers() {
        // 1. Scan active text box context
        try {
            val ic = currentInputConnection
            if (ic != null) {
                val before = ic.getTextBeforeCursor(2000, 0) ?: ""
                val after = ic.getTextAfterCursor(2000, 0) ?: ""
                val fullContextText = before.toString() + after.toString()
                
                if (CipherDetector.isCipherText(fullContextText)) {
                    val block = CipherDetector.extractExactVisualBlock(fullContextText)
                    if (block != null) {
                        val decrypted = CipherEngine.decrypt(applicationContext, block)
                        if (decrypted != null) {
                            detectedEncryptedText = block
                            decryptedTextPreview = decrypted
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error scanning context for ciphers: ${e.message}")
        }

        // 2. Scan clipboard content as fallback
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                    if (CipherDetector.isCipherText(clipText)) {
                        val block = CipherDetector.extractExactVisualBlock(clipText)
                        if (block != null) {
                            val decrypted = CipherEngine.decrypt(applicationContext, block)
                            if (decrypted != null) {
                                detectedEncryptedText = block
                                decryptedTextPreview = decrypted
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Capture any potential sandbox clipboard retrieval safety exceptions
        }

        detectedEncryptedText = null
        decryptedTextPreview = null
    }

    private fun encryptActiveField() {
        try {
            val ic = currentInputConnection ?: return
            val before = ic.getTextBeforeCursor(1500, 0) ?: ""
            val after = ic.getTextAfterCursor(1500, 0) ?: ""
            val fullText = before.toString() + after.toString()
            if (fullText.isNotEmpty()) {
                val encrypted = CipherEngine.encrypt(applicationContext, fullText, useSymbols)
                ic.performContextMenuAction(android.R.id.selectAll)
                ic.commitText(encrypted, 1)
            }
        } catch (e: Exception) {
            Log.e("CipherKeyboard", "Error encrypting active field: ${e.message}")
        }
    }

    private fun replaceWithDecrypted(decryptedText: String) {
        try {
            val ic = currentInputConnection ?: return
            ic.performContextMenuAction(android.R.id.selectAll)
            ic.commitText(decryptedText, 1)
        } catch (e: Exception) {
            // Context fail-safe
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        activePage = KeyboardPage.LETTERS_LOWER
        composingDraft = ""
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        // Scan field + clipboard as soon as the keyboard opens (e.g. reading a chat message)
        if (!(isCipherModeOn && composingDraft.isNotEmpty())) {
            scanForCiphers()
        }
    }

    private fun manualDecryptScan() {
        scanForCiphers()
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
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
