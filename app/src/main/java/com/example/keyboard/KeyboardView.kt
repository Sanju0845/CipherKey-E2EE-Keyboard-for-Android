package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onToggleCipher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardBgColor = ImmersiveSurface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(keyboardBgColor)
            .padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
            .testTag("keyboard_keys_layout")
    ) {
        when (activePage) {
            KeyboardPage.LETTERS_LOWER -> {
                 // First QWERTY row: q w e r t y u i o p
                 KeyboardRow(
                     keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                     onKeyPress = onKeyPress
                 )
                 // Second QWERTY row: a s d f g h j k l
                 KeyboardRow(
                     keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                     rowPaddingStart = 12.dp,
                     rowPaddingEnd = 12.dp,
                     onKeyPress = onKeyPress
                 )
                 // Third QWERTY row with Shift and Delete
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
                         bgColor = Slate700.copy(alpha = 0.5f)
                     )
                     
                     listOf("z", "x", "c", "v", "b", "n", "m").forEach { key ->
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
                         bgColor = Slate700.copy(alpha = 0.5f)
                     )
                 }
            }
            KeyboardPage.LETTERS_UPPER -> {
                 KeyboardRow(
                     keys = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                     onKeyPress = onKeyPress
                 )
                 KeyboardRow(
                     keys = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                     rowPaddingStart = 12.dp,
                     rowPaddingEnd = 12.dp,
                     onKeyPress = onKeyPress
                 )
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(horizontal = 4.dp, vertical = 3.dp),
                     horizontalArrangement = Arrangement.spacedBy(4.dp)
                 ) {
                     SpecialKey(
                         label = "⇪", // Double caps representation active
                         modifier = Modifier.weight(1.5f),
                         onClick = { onPageChange(KeyboardPage.LETTERS_LOWER) },
                         bgColor = ImmersiveCyan, // Highlighted active caps in themed Cyan
                         textColor = Color.Black
                     )
                     
                     listOf("Z", "X", "C", "V", "B", "N", "M").forEach { key ->
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
                         bgColor = Color(0xFF202327)
                     )
                 }
            }
            KeyboardPage.NUMBERS_SYMBOLS -> {
                 KeyboardRow(
                     keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                     onKeyPress = onKeyPress
                 )
                 KeyboardRow(
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
                         bgColor = Slate700.copy(alpha = 0.5f)
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
                         bgColor = Color(0xFF202327)
                     )
                 }
            }
            KeyboardPage.ALT_SYMBOLS -> {
                 KeyboardRow(
                     keys = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "{", "}"),
                     onKeyPress = onKeyPress
                 )
                 KeyboardRow(
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
                         bgColor = Slate700.copy(alpha = 0.5f)
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
                         bgColor = Color(0xFF202327)
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
            val symbolToggleLabel = if (activePage == KeyboardPage.LETTERS_LOWER || activePage == KeyboardPage.LETTERS_UPPER) "?123" else "ABC"
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
                bgColor = Slate700.copy(alpha = 0.5f)
            )

            // Dynamic Cipher Mode Toggle Button directly on QWERTY layout
            SpecialKey(
                label = if (isCipherModeOn) "🔒 ON" else "🔓 OFF",
                modifier = Modifier.weight(1.5f),
                onClick = onToggleCipher,
                bgColor = if (isCipherModeOn) {
                    if (useSymbols) ImmersiveCyan else ImmersiveIndigoLight
                } else {
                    Slate700.copy(alpha = 0.5f)
                },
                textColor = if (isCipherModeOn) Color.Black else Slate100
            )

            StandardKey(
                label = "space",
                modifier = Modifier.weight(3.5f),
                onClick = onSpace,
                bgColor = if (isCipherModeOn) Slate700 else Slate800
            )

            // Enter key
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
fun KeyboardRow(
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

private val KeyShape = RoundedCornerShape(7.dp)

@Composable
fun StandardKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF252A33),
    textColor: Color = Slate100,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .height(42.dp)
            .shadow(0.5.dp, KeyShape)
            .background(bgColor, KeyShape)
            .clip(KeyShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (label.length > 2) 11.sp else 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpecialKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedBgColor = if (bgColor == Color(0xFF202327)) Slate700.copy(alpha = 0.5f) else bgColor
    
    Box(
        modifier = modifier
            .height(42.dp)
            .shadow(0.5.dp, KeyShape)
            .background(resolvedBgColor, KeyShape)
            .clip(KeyShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("key_special_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
