package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.ImmersiveCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Shared constants ──────────────────────────────────────────────────────────
val KeyShape = RoundedCornerShape(10.dp)
val KeyHeight = 48.dp

@Composable
fun StandardKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF252C36),
    pressedBgColor: Color = Color(0xFF3D4654),
    textColor: Color = Slate100,
    onClick: (() -> Unit)? = null,
    glideHighlight: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // Popup effect: key grows slightly on press
    val scale by animateFloatAsState(
        targetValue = when {
            glideHighlight -> 1.06f
            pressed -> 1.08f   // grows up (popup feel)
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "keyScale"
    )

    val displayBg = when {
        glideHighlight || pressed -> pressedBgColor
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
            fontSize = if (label.length > 3) 11.sp else 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

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
        targetValue = if (pressed) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
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
                        val job = scope.launch {
                            delay(400)
                            isLongPressing = true
                            while (isLongPressing) {
                                onDeleteWord()
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                delay(150)
                            }
                        }
                        tryAwaitRelease()
                        pressed = false
                        if (isLongPressing) {
                            isLongPressing = false
                            job.cancel()
                        } else {
                            job.cancel()
                            onBackspace()
                        }
                    }
                )
            }
            .testTag("key_backspace"),
        contentAlignment = Alignment.Center
    ) {
        Text("⌫", color = Slate100, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        targetValue = if (pressed) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "specialKeyScale"
    )

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(if (pressed) pressedBgColor else bgColor, KeyShape)
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
        Text(text = label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LongPressKey(
    label: String,
    longPressLabel: String,
    modifier: Modifier = Modifier,
    glideHighlight: Boolean = false,
    bgColor: Color = Color(0xFF252C36),
    textColor: Color = Slate100,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            glideHighlight -> 1.06f
            pressed -> 1.08f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "lpKeyScale"
    )

    val resolvedBg = if (glideHighlight || pressed) Color(0xFF3D4654) else bgColor

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(resolvedBg, KeyShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val job = scope.launch {
                            delay(350)
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
        Text(text = label, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text(
            text = longPressLabel,
            color = if (textColor == Color.Black) Color.Black.copy(alpha = 0.4f) else Slate500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp)
        )
    }
}
