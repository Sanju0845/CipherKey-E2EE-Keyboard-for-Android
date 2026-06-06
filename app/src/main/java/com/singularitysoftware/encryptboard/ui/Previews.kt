package com.singularitysoftware.encryptboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.singularitysoftware.encryptboard.OnboardingScreen
import com.singularitysoftware.encryptboard.ui.theme.EncryptBoardTheme
import com.singularitysoftware.encryptboard.keyboard.KeyboardView
import com.singularitysoftware.encryptboard.keyboard.KeyboardPage
import com.singularitysoftware.encryptboard.keyboard.KeyboardStrip

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun Preview_OnboardingScreen() {
    EncryptBoardTheme {
        OnboardingScreen()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 280)
@Composable
fun Preview_Keyboard_Lowercase() {
    EncryptBoardTheme {
        KeyboardView(
            activePage = KeyboardPage.LETTERS_LOWER,
            isCipherModeOn = false,
            useSymbols = true,
            onPageChange = {},
            onKeyPress = {},
            onGlideWord = {},
            onBackspace = {},
            onDeleteWord = {},
            onSpace = {},
            onEnter = {},
            onToggleCipher = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 40)
@Composable
fun Preview_KeyboardStrip_Normal() {
    EncryptBoardTheme {
        KeyboardStrip(
            suggestions = listOf("hello", "world", "how"),
            onSuggestionClick = {},
            isCipherModeOn = false,
            composingDraft = "",
            showClipboard = false,
            onToggleCipherMode = {},
            onOpenClipboard = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 40)
@Composable
fun Preview_KeyboardStrip_CipherMode() {
    EncryptBoardTheme {
        KeyboardStrip(
            suggestions = emptyList(),
            onSuggestionClick = {},
            isCipherModeOn = true,
            composingDraft = "hello there",
            showClipboard = false,
            onToggleCipherMode = {},
            onOpenClipboard = {}
        )
    }
}
