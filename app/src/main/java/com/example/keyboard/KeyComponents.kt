package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyShape = RoundedCornerShape(8.dp)
private val KeyHeight = 44.dp

@Composable
fun StandardKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF2A3038),
    pressedBgColor: Color = Color(0xFF3D4654),
    textColor: Color = Slate100,
    onClick: (() -> Unit)? = null,
    glideHighlight: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = when {
            glideHighlight -> 1.05f
            pressed -> 0.94f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 70),
        label = "keyScale"
    )

    val displayBg = when {
        glideHighlight -> pressedBgColor
        pressed -> pressedBgColor
        else -> bgColor
    }

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(displayBg, KeyShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onClick()
                        }
                    )
                } else Modifier
            )
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (label.length > 2) 11.sp else 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Backspace key with:
 *  - Single tap  → delete one char (or the current selection if any)
 *  - Long press  → repeatedly delete word-by-word every 300ms while held
 */
@Composable
fun BackspaceKey(
    modifier: Modifier = Modifier,
    onBackspace: () -> Unit,
    onDeleteWord: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var isLongPressing by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(70),
        label = "backspaceScale"
    )

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(Slate700.copy(alpha = 0.55f), KeyShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        // Start long-press timer
                        val job = scope.launch {
                            delay(400) // initial delay before repeat starts
                            isLongPressing = true
                            while (isLongPressing) {
                                onDeleteWord()
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                delay(150) // repeat interval
                            }
                        }
                        tryAwaitRelease()
                        pressed = false
                        if (isLongPressing) {
                            isLongPressing = false
                            job.cancel()
                        } else {
                            job.cancel()
                            onBackspace() // normal single tap
                        }
                    }
                )
            }
            .testTag("key_backspace"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⌫",
            color = Slate100,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SpecialKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    pressedBgColor: Color = bgColor.copy(alpha = 0.85f),
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(70),
        label = "specialKeyScale"
    )

    val resolvedBg = if (bgColor == Color(0xFF202327)) Slate700.copy(alpha = 0.55f) else bgColor

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(if (pressed) pressedBgColor else resolvedBg, KeyShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
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

/**
 * Key that shows a small number hint in the top-right corner.
 * Tap = letter, long press = number.
 */
@Composable
fun LongPressKey(
    label: String,
    longPressLabel: String,
    modifier: Modifier = Modifier,
    glideHighlight: Boolean = false,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            glideHighlight -> 1.05f
            pressed -> 0.94f
            else -> 1f
        },
        animationSpec = tween(70),
        label = "lpKeyScale"
    )

    val bg = if (glideHighlight || pressed) Color(0xFF3D4654) else Color(0xFF2A3038)

    Box(
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val job = scope.launch {
                            delay(350)
                            // Long press triggered
                            pressed = false
                            onLongPress()
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released && job.isActive) {
                            job.cancel()
                            onTap()
                        } else {
                            job.cancel()
                        }
                    }
                )
            }
            .testTag("key_lp_$label"),
        contentAlignment = Alignment.Center
    ) {
        // Main letter
        androidx.compose.material3.Text(
            text = label,
            color = Slate100,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        // Small number hint top-right
        androidx.compose.material3.Text(
            text = longPressLabel,
            color = Slate500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp)
        )
    }
}
