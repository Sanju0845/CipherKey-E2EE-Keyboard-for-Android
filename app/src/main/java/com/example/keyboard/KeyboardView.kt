package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

enum class KeyboardPage {
    LETTERS_LOWER,
    LETTERS_UPPER,
    NUMBERS_SYMBOLS,
    ALT_SYMBOLS
}

@Composable
fun KeyboardView(
    activePage: KeyboardPage,
    isCipherModeOn: Boolean,
    useSymbols: Boolean,
    onPageChange: (KeyboardPage) -> Unit,
    onKeyPress: (String) -> Unit,
    onGlideWord: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onToggleCipher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glideEnabled = activePage == KeyboardPage.LETTERS_LOWER || activePage == KeyboardPage.LETTERS_UPPER

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ImmersiveSurface)
            .padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
            .testTag("keyboard_keys_layout")
    ) {
        when (activePage) {
            KeyboardPage.LETTERS_LOWER -> {
                GlideLetterRow(
                    keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                    glideEnabled = glideEnabled,
                    onKeyPress = onKeyPress,
                    onGlideWord = onGlideWord
                )
                GlideLetterRow(
                    keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                    glideEnabled = glideEnabled,
                    rowPaddingStart = 12.dp,
                    rowPaddingEnd = 12.dp,
                    onKeyPress = onKeyPress,
                    onGlideWord = onGlideWord
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpecialKey(
                        label = "⇧",
                        modifier = Modifier.weight(1.5f),
                        onClick = { onPageChange(KeyboardPage.LETTERS_UPPER) },
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                    Box(modifier = Modifier.weight(7f)) {
                        GlideLetterRow(
                            keys = listOf("z", "x", "c", "v", "b", "n", "m"),
                            glideEnabled = glideEnabled,
                            rowPaddingStart = 0.dp,
                            rowPaddingEnd = 0.dp,
                            onKeyPress = onKeyPress,
                            onGlideWord = onGlideWord
                        )
                    }
                    SpecialKey(
                        label = "⌫",
                        modifier = Modifier.weight(1.5f),
                        onClick = onBackspace,
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                }
            }
            KeyboardPage.LETTERS_UPPER -> {
                GlideLetterRow(
                    keys = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                    glideEnabled = glideEnabled,
                    onKeyPress = onKeyPress,
                    onGlideWord = onGlideWord
                )
                GlideLetterRow(
                    keys = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                    glideEnabled = glideEnabled,
                    rowPaddingStart = 12.dp,
                    rowPaddingEnd = 12.dp,
                    onKeyPress = onKeyPress,
                    onGlideWord = onGlideWord
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpecialKey(
                        label = "⇪",
                        modifier = Modifier.weight(1.5f),
                        onClick = { onPageChange(KeyboardPage.LETTERS_LOWER) },
                        bgColor = ImmersiveCyan,
                        textColor = Color.Black
                    )
                    Box(modifier = Modifier.weight(7f)) {
                        GlideLetterRow(
                            keys = listOf("Z", "X", "C", "V", "B", "N", "M"),
                            glideEnabled = glideEnabled,
                            rowPaddingStart = 0.dp,
                            rowPaddingEnd = 0.dp,
                            onKeyPress = onKeyPress,
                            onGlideWord = onGlideWord
                        )
                    }
                    SpecialKey(
                        label = "⌫",
                        modifier = Modifier.weight(1.5f),
                        onClick = onBackspace,
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                }
            }
            KeyboardPage.NUMBERS_SYMBOLS -> {
                StaticKeyRow(
                    keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                    onKeyPress = onKeyPress
                )
                StaticKeyRow(
                    keys = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
                    onKeyPress = onKeyPress
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpecialKey(
                        label = "=\\<",
                        modifier = Modifier.weight(1.5f),
                        onClick = { onPageChange(KeyboardPage.ALT_SYMBOLS) },
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                    listOf("*", "\"", "'", ":", ";", "!", "?").forEach { key ->
                        StandardKey(
                            label = key,
                            modifier = Modifier.weight(1f),
                            onClick = { onKeyPress(key) }
                        )
                    }
                    SpecialKey(
                        label = "⌫",
                        modifier = Modifier.weight(1.5f),
                        onClick = onBackspace,
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                }
            }
            KeyboardPage.ALT_SYMBOLS -> {
                StaticKeyRow(
                    keys = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "{", "}"),
                    onKeyPress = onKeyPress
                )
                StaticKeyRow(
                    keys = listOf("[", "]", "\\", "^", "_", "=", "<", ">", "¥"),
                    onKeyPress = onKeyPress
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpecialKey(
                        label = "?123",
                        modifier = Modifier.weight(1.5f),
                        onClick = { onPageChange(KeyboardPage.NUMBERS_SYMBOLS) },
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                    listOf("¢", "£", "¤", "°", "¡", "¿", "§").forEach { key ->
                        StandardKey(
                            label = key,
                            modifier = Modifier.weight(1f),
                            onClick = { onKeyPress(key) }
                        )
                    }
                    SpecialKey(
                        label = "⌫",
                        modifier = Modifier.weight(1.5f),
                        onClick = onBackspace,
                        bgColor = Slate700.copy(alpha = 0.55f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val symbolToggleLabel =
                if (activePage == KeyboardPage.LETTERS_LOWER || activePage == KeyboardPage.LETTERS_UPPER) "?123" else "ABC"
            SpecialKey(
                label = symbolToggleLabel,
                modifier = Modifier.weight(1.5f),
                onClick = {
                    if (activePage == KeyboardPage.LETTERS_LOWER || activePage == KeyboardPage.LETTERS_UPPER) {
                        onPageChange(KeyboardPage.NUMBERS_SYMBOLS)
                    } else {
                        onPageChange(KeyboardPage.LETTERS_LOWER)
                    }
                },
                bgColor = Slate700.copy(alpha = 0.55f)
            )
            SpecialKey(
                label = if (isCipherModeOn) "🔒 ON" else "🔓 OFF",
                modifier = Modifier.weight(1.5f),
                onClick = onToggleCipher,
                bgColor = if (isCipherModeOn) {
                    if (useSymbols) ImmersiveCyan else ImmersiveIndigoLight
                } else {
                    Slate700.copy(alpha = 0.55f)
                },
                textColor = if (isCipherModeOn) Color.Black else Slate100
            )
            StandardKey(
                label = "space",
                modifier = Modifier.weight(3.5f),
                onClick = onSpace,
                bgColor = if (isCipherModeOn) Slate700 else Slate800
            )
            SpecialKey(
                label = "↩",
                modifier = Modifier.weight(1.5f),
                onClick = onEnter,
                bgColor = ImmersiveIndigo,
                textColor = Color.White
            )
        }
    }
}

@Composable
private fun GlideLetterRow(
    keys: List<String>,
    glideEnabled: Boolean,
    modifier: Modifier = Modifier,
    rowPaddingStart: Dp = 4.dp,
    rowPaddingEnd: Dp = 4.dp,
    onKeyPress: (String) -> Unit,
    onGlideWord: (String) -> Unit
) {
    val tracker = remember(keys) { GlideKeyTracker() }
    var glideHighlightKey by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = rowPaddingStart, end = rowPaddingEnd, top = 3.dp, bottom = 3.dp)
            .glideRowGestures(
                tracker = tracker,
                enabled = glideEnabled,
                onTapKey = onKeyPress,
                onGlideWord = onGlideWord,
                onHighlightKey = { glideHighlightKey = it }
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            StandardKey(
                label = key,
                modifier = Modifier
                    .weight(1f)
                    .trackGlideKey(key, tracker),
                onClick = if (glideEnabled) null else { { onKeyPress(key) } },
                glideHighlight = glideHighlightKey == key
            )
        }
    }
}

@Composable
private fun StaticKeyRow(
    keys: List<String>,
    rowPaddingStart: Dp = 4.dp,
    rowPaddingEnd: Dp = 4.dp,
    onKeyPress: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = rowPaddingStart, end = rowPaddingEnd, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            StandardKey(
                label = key,
                modifier = Modifier.weight(1f),
                onClick = { onKeyPress(key) }
            )
        }
    }
}
