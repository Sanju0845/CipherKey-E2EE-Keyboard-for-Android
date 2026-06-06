package com.singularitysoftware.encryptboard.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import com.singularitysoftware.encryptboard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyShape = RoundedCornerShape(8.dp)
private val KeyHeight = 44.dp

// ── Standard key — simple fast tween, no popup (popup causes lag) ─────────────
@Composable
fun StandardKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = KeyBg,
    pressedBgColor: Color = KeyBgPressed,
    textColor: Color = Slate100,
    onClick: (() -> Unit)? = null,
    glideHighlight: Boolean = false,
    showPopup: Boolean = false   // disabled — too heavy on keyboard service
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (glideHighlight || pressed) 0.93f else 1f,
        animationSpec = tween(60),
        label = "ks"
    )

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(if (glideHighlight || pressed) pressedBgColor else bgColor, KeyShape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick()
                    }
                ) else Modifier
            )
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = when {
                label.length > 3 -> 10.sp
                label.length > 1 -> 13.sp
                else -> 17.sp
            },
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Backspace ─────────────────────────────────────────────────────────────────
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
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(60), label = "bk"
    )

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(KeySpecialBg, KeyShape)
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
                                delay(120)
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
        Text("⌫", color = Slate300, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Special key ───────────────────────────────────────────────────────────────
@Composable
fun SpecialKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    pressedBgColor: Color = bgColor.copy(alpha = 0.75f),
    textColor: Color = Slate200,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(60), label = "sk"
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
        Text(
            text = label,
            color = textColor,
            fontSize = when { label.length > 3 -> 11.sp; else -> 14.sp },
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Cipher key — flat color glow, no gradient (gradients are expensive) ───────
@Composable
fun CipherKey(
    isCipherModeOn: Boolean,
    profileEmoji: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(60), label = "ck"
    )

    val bg = if (isCipherModeOn) ImmersiveCyan.copy(alpha = 0.22f) else KeySpecialBg

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(bg, KeyShape)
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
                        if (released && job.isActive) { job.cancel(); onTap() }
                        else job.cancel()
                    }
                )
            }
            .testTag("key_cipher"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCipherModeOn) "🔒" else "🔓",
            fontSize = 15.sp
        )
        Text(
            text = profileEmoji,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp),
            color = if (isCipherModeOn) ImmersiveCyan else Slate600
        )
    }
}

// ── Long press key — no popup, just number hint ───────────────────────────────
@Composable
fun LongPressKey(
    label: String,
    longPressLabel: String,
    modifier: Modifier = Modifier,
    glideHighlight: Boolean = false,
    showPopup: Boolean = false,   // popup disabled for performance
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (glideHighlight || pressed) 0.93f else 1f,
        animationSpec = tween(60), label = "lk"
    )

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(if (glideHighlight || pressed) KeyBgPressed else KeyBg, KeyShape)
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
                        if (released && job.isActive) { job.cancel(); onTap() }
                        else job.cancel()
                    }
                )
            }
            .testTag("key_lp_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Slate100, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text(
            longPressLabel,
            color = Slate500,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 4.dp)
        )
    }
}
