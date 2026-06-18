package com.singularitysoftware.encryptboard

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
import com.singularitysoftware.encryptboard.cipher.CipherEngine
import com.singularitysoftware.encryptboard.keyboard.KeyboardPage
import com.singularitysoftware.encryptboard.keyboard.KeyboardStrip
import com.singularitysoftware.encryptboard.keyboard.KeyboardView
import com.singularitysoftware.encryptboard.ui.theme.EncryptBoardTheme

class DevPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EncryptBoardTheme {
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

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KeyboardStrip(
            suggestions = emptyList(),
            onSuggestionClick = {},
            isCipherModeOn = isCipherModeOn,
            composingDraft = composingDraft,
            showClipboard = false,
            onToggleCipherMode = { isCipherModeOn = !isCipherModeOn },
            onOpenClipboard = {}
        )

        OutlinedTextField(
            value = composedText,
            onValueChange = { composedText = it },
            label = { Text("Target text field") },
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )

        KeyboardView(
            activePage = KeyboardPage.LETTERS_LOWER,
            isCipherModeOn = isCipherModeOn,
            useSymbols = useSymbols,
            onPageChange = {},
            onKeyPress = { k ->
                if (isCipherModeOn) composingDraft += k
                else composedText = TextFieldValue(composedText.text + k)
            },
            onGlideWord = { word ->
                if (isCipherModeOn) composingDraft += word
                else composedText = TextFieldValue(composedText.text + word)
            },
            onBackspace = {
                if (isCipherModeOn && composingDraft.isNotEmpty()) composingDraft = composingDraft.dropLast(1)
                else if (composedText.text.isNotEmpty()) composedText = TextFieldValue(composedText.text.dropLast(1))
            },
            onDeleteWord = {
                if (composedText.text.isNotEmpty()) {
                    val trimmed = composedText.text.trimEnd()
                    val lastSpace = trimmed.lastIndexOf(' ')
                    composedText = TextFieldValue(if (lastSpace >= 0) trimmed.substring(0, lastSpace + 1) else "")
                }
            },
            onSpace = {
                if (isCipherModeOn && composingDraft.isNotEmpty()) {
                    composedText = TextFieldValue(composedText.text + CipherEngine.encrypt(ctx, composingDraft, useSymbols) + " ")
                    composingDraft = ""
                } else {
                    composedText = TextFieldValue(composedText.text + " ")
                }
            },
            onEnter = {
                if (isCipherModeOn && composingDraft.isNotEmpty()) {
                    composedText = TextFieldValue(composedText.text + CipherEngine.encrypt(ctx, composingDraft, useSymbols))
                    composingDraft = ""
                } else {
                    composedText = TextFieldValue(composedText.text + "\n")
                }
            },
            onToggleCipher = { isCipherModeOn = !isCipherModeOn }
        )
    }
}
