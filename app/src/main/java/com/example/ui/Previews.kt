package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.keyboard.KeyboardView
import com.example.keyboard.KeyboardPage
import com.example.keyboard.KeyboardStrip

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun Preview_OnboardingScreen() {
    MyApplicationTheme {
        OnboardingScreen()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 280)
@Composable
fun Preview_Keyboard_Lowercase() {
    MyApplicationTheme {
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
    MyApplicationTheme {
        KeyboardStrip(
            suggestions = listOf("hello", "world", "how"),
            onSuggestionClick = {},
            isCipherModeOn = false,
            composingDraft = "",
            showClipboard = false,
            activePanel = ActivePanel.KEYBOARD,
            onToggleCipherMode = {},
            onOpenClipboard = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 40)
@Composable
fun Preview_KeyboardStrip_CipherMode() {
    MyApplicationTheme {
        KeyboardStrip(
            suggestions = emptyList(),
            onSuggestionClick = {},
            isCipherModeOn = true,
            composingDraft = "hello there",
            showClipboard = false,
            activePanel = ActivePanel.KEYBOARD,
            onToggleCipherMode = {},
            onOpenClipboard = {}
        )
    }
}
