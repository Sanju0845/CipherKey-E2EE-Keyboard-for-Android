package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.cipher.CipherDetector
import com.example.cipher.CipherEngine
import com.example.keyboard.KeyboardPage
import com.example.keyboard.KeyboardView
import com.example.keyboard.SuggestionBar
import com.example.ui.theme.MyApplicationTheme

/**
 * Development preview Activity that hosts the keyboard UI and a sample text area so
 * you can run and interact with the keyboard inside an ordinary Activity (emulator/device)
 * without registering it as an IME. Useful for rapid iterations and checking behavior.
 */
class DevPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DevPreviewContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevPreviewContent() {
    val ctx = LocalContext.current
    var composedText by remember { mutableStateOf(TextFieldValue("")) }
    var composingDraft by remember { mutableStateOf("") }
    var isCipherModeOn by remember { mutableStateOf(false) }
    var useSymbols by remember { mutableStateOf(true) }
    var decryptedPreview by remember { mutableStateOf<String?>(null) }

    fun scanForDecrypt() {
        val text = composedText.text
        val block = CipherDetector.extractExactVisualBlock(text)
        decryptedPreview = if (block != null) CipherEngine.decrypt(ctx, block) else null
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionBar(
            decryptedText = decryptedPreview,
            composingDraft = composingDraft,
            isCipherModeOn = isCipherModeOn,
            useSymbols = useSymbols,
            onToggleCipherMode = { isCipherModeOn = !isCipherModeOn },
            onToggleUseSymbols = { useSymbols = !useSymbols },
            onCopyDecrypted = {
                decryptedPreview?.let { composedText = TextFieldValue(it) }
            },
            onReplaceDecrypted = {
                decryptedPreview?.let { composedText = TextFieldValue(it) }
            },
            onReplyEncrypted = {
                isCipherModeOn = true
                decryptedPreview = null
            },
            onEncryptActiveField = {
                if (composedText.text.isNotEmpty()) {
                    composedText = TextFieldValue(
                        CipherEngine.encrypt(ctx, composedText.text, useSymbols)
                    )
                }
            },
            onDecryptScan = { scanForDecrypt() },
            onOpenConfig = {},
            hasActiveTextToEncrypt = composedText.text.isNotEmpty() && composingDraft.isEmpty()
        )

        OutlinedTextField(
            value = composedText,
            onValueChange = { composedText = it },
            label = { Text("Target text field (simulate app field)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { isCipherModeOn = !isCipherModeOn }) {
                Text(if (isCipherModeOn) "Cipher: ON" else "Cipher: OFF")
            }
            Button(onClick = { useSymbols = !useSymbols }) {
                Text(if (useSymbols) "Symbols" else "Emojis")
            }
            Button(onClick = { decryptedPreview = null }) { Text("Clear") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        KeyboardView(
            activePage = KeyboardPage.LETTERS_LOWER,
            isCipherModeOn = isCipherModeOn,
            useSymbols = useSymbols,
            onPageChange = {},
            onKeyPress = { k ->
                if (isCipherModeOn) {
                    composingDraft += k
                } else {
                    composedText = TextFieldValue(composedText.text + k)
                }
            },
            onBackspace = {
                if (isCipherModeOn && composingDraft.isNotEmpty()) {
                    composingDraft = composingDraft.dropLast(1)
                } else if (composedText.text.isNotEmpty()) {
                    composedText = TextFieldValue(composedText.text.dropLast(1))
                }
            },
            onSpace = {
                if (isCipherModeOn) {
                    if (composingDraft.isNotEmpty()) {
                        val enc = CipherEngine.encrypt(ctx, composingDraft, useSymbols)
                        composedText = TextFieldValue(composedText.text + enc + " ")
                        composingDraft = ""
                    }
                } else {
                    composedText = TextFieldValue(composedText.text + " ")
                }
            },
            onEnter = {
                if (isCipherModeOn) {
                    if (composingDraft.isNotEmpty()) {
                        val enc = CipherEngine.encrypt(ctx, composingDraft, useSymbols)
                        composedText = TextFieldValue(composedText.text + enc)
                        composingDraft = ""
                    } else {
                        composedText = TextFieldValue(composedText.text + "\n")
                    }
                } else {
                    composedText = TextFieldValue(composedText.text + "\n")
                }
            },
            onToggleCipher = { isCipherModeOn = !isCipherModeOn }
        )
    }
}
