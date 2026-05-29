package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.keyboard.KeyboardView
import com.example.keyboard.KeyboardPage
import com.example.keyboard.SuggestionBar

/**
 * Compose previews so you can inspect UI inside Android Studio without building the APK.
 * Open the Compose Preview panel in Android Studio (View > Tool Windows > Preview) and
 * these previews will render live as you edit the composables.
 */

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
      onBackspace = {},
      onSpace = {},
      onEnter = {},
      onToggleCipher = {}
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 420)
@Composable
fun Preview_Keyboard_CipherMode() {
  MyApplicationTheme {
    KeyboardView(
      activePage = KeyboardPage.LETTERS_LOWER,
      isCipherModeOn = true,
      useSymbols = true,
      onPageChange = {},
      onKeyPress = {},
      onBackspace = {},
      onSpace = {},
      onEnter = {},
      onToggleCipher = {}
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 120)
@Composable
fun Preview_SuggestionBar_Decrypted() {
  MyApplicationTheme {
    SuggestionBar(
      decryptedText = "Hello decrypted",
      composingDraft = "",
      isCipherModeOn = false,
      useSymbols = true,
      onToggleCipherMode = {},
      onToggleUseSymbols = {},
      onCopyDecrypted = {},
      onReplaceDecrypted = {},
      onReplyEncrypted = {},
      onEncryptActiveField = {},
      onDecryptScan = {},
      onOpenConfig = {},
      hasActiveTextToEncrypt = false
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 120)
@Composable
fun Preview_SuggestionBar_CipherMode() {
  MyApplicationTheme {
    SuggestionBar(
      decryptedText = null,
      composingDraft = "hello",
      isCipherModeOn = true,
      useSymbols = true,
      onToggleCipherMode = {},
      onToggleUseSymbols = {},
      onCopyDecrypted = {},
      onReplaceDecrypted = {},
      onReplyEncrypted = {},
      onEncryptActiveField = {},
      onDecryptScan = {},
      onOpenConfig = {},
      hasActiveTextToEncrypt = false
    )
  }
}
