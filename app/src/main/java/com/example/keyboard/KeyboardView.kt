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
    LETTERS_CAPS_LOCK,
    NUMBERS_SYMBOLS,
    ALT_SYMBOLS
}

// Shift state machine
enum class ShiftState { OFF, ONE_SHOT, CAPS_LOCK }

@Composable
fun KeyboardView(
    activePage: KeyboardPage,
    isCipherModeOn: Boolean,
    useSymbols: Boolean,
    onPageChange: (KeyboardPage) -> Unit,
    onKeyPress: (String) -> Unit,
    onGlideWord: (String) -> Unit,
    onBackspace: () -> Unit,
    onDeleteWord: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onToggleCipher: () -> Unit,
    onLongPressCipher: () -> Unit = {},
    coverProfileEmoji: String = "✦",
    onSwitchKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Shift state: OFF / ONE_SHOT / CAPS_LOCK
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var lastShiftTapMs by remember { mutableStateOf(0L) }

    val isUpper = shiftState != ShiftState.OFF ||
            activePage == KeyboardPage.LETTERS_UPPER ||
            activePage == KeyboardPage.LETTERS_CAPS_LOCK
    val isCapsLock = shiftState == ShiftState.CAPS_LOCK ||
            activePage == KeyboardPage.LETTERS_CAPS_LOCK

    fun onShiftTap() {
        val now = System.currentTimeMillis()
        shiftState = when {
            shiftState == ShiftState.CAPS_LOCK -> ShiftState.OFF
            shiftState == ShiftState.ONE_SHOT && (now - lastShiftTapMs) < 400L -> ShiftState.CAPS_LOCK
            else -> ShiftState.ONE_SHOT
        }
        lastShiftTapMs = now
    }

    // After a key is pressed in ONE_SHOT mode, drop back to OFF
    fun onLetterKey(key: String) {
        val actual = if (isUpper) key.uppercase() else key.lowercase()
        onKeyPress(actual)
        if (shiftState == ShiftState.ONE_SHOT) shiftState = ShiftState.OFF
    }

    val glideEnabled = activePage == KeyboardPage.LETTERS_LOWER ||
            activePage == KeyboardPage.LETTERS_UPPER ||
            activePage == KeyboardPage.LETTERS_CAPS_LOCK

    val topRowLetters = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val topRowNumbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ImmersiveSurface)
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
            .testTag("keyboard_keys_layout")
    ) {
        when {
            // ── Letter pages ──────────────────────────────────────────────────
            activePage == KeyboardPage.LETTERS_LOWER ||
            activePage == KeyboardPage.LETTERS_UPPER ||
            activePage == KeyboardPage.LETTERS_CAPS_LOCK -> {

                // Row 1: q-p with long-press numbers
                NumberLongPressRow(
                    letters = topRowLetters,
                    numbers = topRowNumbers,
                    isUpper = isUpper,
                    glideEnabled = glideEnabled,
                    onKeyPress = { onLetterKey(it) },
                    onGlideWord = { word ->
                        val actual = if (isUpper) word.uppercase() else word
                        onGlideWord(actual)
                        if (shiftState == ShiftState.ONE_SHOT) shiftState = ShiftState.OFF
                    }
                )

                // Row 2: a-l
                GlideLetterRow(
                    keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                    isUpper = isUpper,
                    glideEnabled = glideEnabled,
                    rowPaddingStart = 16.dp,
                    rowPaddingEnd = 16.dp,
                    onKeyPress = { onLetterKey(it) },
                    onGlideWord = { word ->
                        val actual = if (isUpper) word.uppercase() else word
                        onGlideWord(actual)
                        if (shiftState == ShiftState.ONE_SHOT) shiftState = ShiftState.OFF
                    }
                )

                // Row 3: shift + z-m + backspace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShiftKey(
                        shiftState = shiftState,
                        modifier = Modifier.weight(1.5f),
                        onTap = { onShiftTap() }
                    )
                    Box(modifier = Modifier.weight(7f)) {
                        GlideLetterRow(
                            keys = listOf("z", "x", "c", "v", "b", "n", "m"),
                            isUpper = isUpper,
                            glideEnabled = glideEnabled,
                            rowPaddingStart = 0.dp,
                            rowPaddingEnd = 0.dp,
                            onKeyPress = { onLetterKey(it) },
                            onGlideWord = { word ->
                                val actual = if (isUpper) word.uppercase() else word
                                onGlideWord(actual)
                                if (shiftState == ShiftState.ONE_SHOT) shiftState = ShiftState.OFF
                            }
                        )
                    }
                    BackspaceKey(
                        modifier = Modifier.weight(1.5f),
                        onBackspace = onBackspace,
                        onDeleteWord = onDeleteWord
                    )
                }
            }

            // ── Numbers / symbols ─────────────────────────────────────────────
            activePage == KeyboardPage.NUMBERS_SYMBOLS -> {
                StaticKeyRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), onKeyPress)
                StaticKeyRow(listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"), onKeyPress)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpecialKey("=\\<", Modifier.weight(1.5f), Slate700.copy(alpha = 0.55f)) {
                        onPageChange(KeyboardPage.ALT_SYMBOLS)
                    }
                    listOf("*", "\"", "'", ":", ";", "!", "?").forEach { k ->
                        StandardKey(k, Modifier.weight(1f), onClick = { onKeyPress(k) })
                    }
                    BackspaceKey(Modifier.weight(1.5f), onBackspace, onDeleteWord)
                }
            }

            // ── Alt symbols ───────────────────────────────────────────────────
            else -> {
                StaticKeyRow(listOf("~", "`", "|", "•", "√", "π", "÷", "×", "{", "}"), onKeyPress)
                StaticKeyRow(listOf("[", "]", "\\", "^", "_", "=", "<", ">", "¥"), onKeyPress)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SpecialKey("?123", Modifier.weight(1.5f), Slate700.copy(alpha = 0.55f)) {
                        onPageChange(KeyboardPage.NUMBERS_SYMBOLS)
                    }
                    listOf("¢", "£", "¤", "°", "¡", "¿", "§").forEach { k ->
                        StandardKey(k, Modifier.weight(1f), onClick = { onKeyPress(k) })
                    }
                    BackspaceKey(Modifier.weight(1.5f), onBackspace, onDeleteWord)
                }
            }
        }

        // ── Bottom row: ?123 | 🔒 | space | ↩ ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val symLabel = when (activePage) {
                KeyboardPage.NUMBERS_SYMBOLS, KeyboardPage.ALT_SYMBOLS -> "ABC"
                else -> "?123"
            }
            SpecialKey(
                label = symLabel,
                modifier = Modifier.weight(1.5f),
                bgColor = Slate700.copy(alpha = 0.55f),
                onClick = {
                    when (activePage) {
                        KeyboardPage.NUMBERS_SYMBOLS, KeyboardPage.ALT_SYMBOLS ->
                            onPageChange(KeyboardPage.LETTERS_LOWER)
                        else -> onPageChange(KeyboardPage.NUMBERS_SYMBOLS)
                    }
                }
            )
            // Cipher toggle key — glows when on, long press opens profile picker
            CipherKey(
                isCipherModeOn = isCipherModeOn,
                profileEmoji = coverProfileEmoji,
                modifier = Modifier.weight(1.5f),
                onTap = onToggleCipher,
                onLongPress = onLongPressCipher
            )
            LongPressKey(
                label = "space",
                longPressLabel = "⌨",
                modifier = Modifier.weight(3.5f),
                onTap = onSpace,
                onLongPress = onSwitchKeyboard
            )
            StandardKey(
                label = ".",
                modifier = Modifier.weight(1f),
                bgColor = Slate700.copy(alpha = 0.55f),
                onClick = { onKeyPress(".") }
            )
            SpecialKey(
                label = "↩",
                modifier = Modifier.weight(1.5f),
                bgColor = ImmersiveIndigo,
                textColor = Color.White,
                onClick = onEnter
            )

// ─────────────────────────────────────────────────────────────────────────────
// Shift key — shows ⇧ (off), filled ⇧ (one-shot), ⇪ (caps lock)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ShiftKey(
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val bg = when (shiftState) {
        ShiftState.OFF -> Slate700.copy(alpha = 0.55f)
        ShiftState.ONE_SHOT -> ImmersiveCyan.copy(alpha = 0.7f)
        ShiftState.CAPS_LOCK -> ImmersiveCyan
    }
    val textColor = when (shiftState) {
        ShiftState.OFF -> Slate100
        else -> Color.Black
    }
    val label = when (shiftState) {
        ShiftState.OFF -> "⇧"
        ShiftState.ONE_SHOT -> "⇧"
        ShiftState.CAPS_LOCK -> "⇪"
    }
    SpecialKey(label = label, modifier = modifier, bgColor = bg, textColor = textColor, onClick = onTap)
}

// ─────────────────────────────────────────────────────────────────────────────
// Top row with long-press numbers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberLongPressRow(
    letters: List<String>,
    numbers: List<String>,
    isUpper: Boolean,
    glideEnabled: Boolean,
    onKeyPress: (String) -> Unit,
    onGlideWord: (String) -> Unit
) {
    val tracker = remember { GlideKeyTracker() }
    var glideHighlightKey by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
            .glideRowGestures(
                tracker = tracker,
                enabled = glideEnabled,
                onGlideWord = onGlideWord,
                onHighlightKey = { glideHighlightKey = it }
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        letters.forEachIndexed { i, letter ->
            val number = numbers[i]
            val display = if (isUpper) letter.uppercase() else letter
            LongPressKey(
                label = display,
                longPressLabel = number,
                modifier = Modifier.weight(1f).trackGlideKey(display, tracker),
                glideHighlight = glideHighlightKey == display,
                onTap = { onKeyPress(display) },
                onLongPress = { onKeyPress(number) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Glide letter row (rows 2 and 3)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GlideLetterRow(
    keys: List<String>,
    isUpper: Boolean,
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
                onGlideWord = onGlideWord,
                onHighlightKey = { glideHighlightKey = it }
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            val display = if (isUpper) key.uppercase() else key
            StandardKey(
                label = display,
                modifier = Modifier.weight(1f).trackGlideKey(display, tracker),
                onClick = { onKeyPress(display) },
                glideHighlight = glideHighlightKey == display
            )
        }
    }
}

@Composable
private fun StaticKeyRow(
    keys: List<String>,
    onKeyPress: (String) -> Unit,
    rowPaddingStart: Dp = 4.dp,
    rowPaddingEnd: Dp = 4.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = rowPaddingStart, end = rowPaddingEnd, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            StandardKey(key, Modifier.weight(1f), onClick = { onKeyPress(key) })
        }
    }
}
