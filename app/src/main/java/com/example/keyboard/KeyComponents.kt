package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyShape = RoundedCornerShape(10.dp)
private val KeyHeight = 46.dp   // +2dp from 44 — slightly more comfortable

// ── Standard key with popup on press ─────────────────────────────────────────
@Composable
fun StandardKey(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = KeyBg,
    pressedBgColor: Color = KeyBgPressed,
    textColor: Color = Slate100,
    onClick: (() -> Unit)? = null,
    glideHighlight: Boolean = false,
    showPopup: Boolean = true   // show letter popup above key when pressed
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // Spring animation for press
    val scale by animateFloatAsState(
        targetValue = when {
            glideHighlight -> 1.06f
            pressed -> 0.88f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "keyScale"
    )

    val displayBg = if (glideHighlight || pressed) pressedBgColor else bgColor

    Box(modifier = modifier.height(KeyHeight)) {
        // ── Popup above key when pressed ──────────────────────────────────────
        if (showPopup && pressed && label.length <= 2) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp)
                    .zIndex(10f)
                    .shadow(8.dp, RoundedCornerShape(8.dp))
                    .background(KeyPopupBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(),
                    color = KeyPopupText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Key itself ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(KeyShape)
                .background(displayBg, KeyShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.foundation.LocalIndication.current,
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
                fontSize = when {
                    label.length > 3 -> 10.sp
                    label.length > 1 -> 13.sp
                    else -> 18.sp
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Backspace — long press deletes word by word ───────────────────────────────
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
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "bsScale"
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
        Text("⌫", color = Slate300, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Special key (shift, ?123, ↩, etc.) ───────────────────────────────────────
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
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "spKeyScale"
    )

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(if (pressed) pressedBgColor else bgColor, KeyShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
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
            fontSize = when {
                label.length > 3 -> 11.sp
                else -> 14.sp
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Cipher key — glows cyan when active ──────────────────────────────────────
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

    // Pulsing glow when cipher is on
    val glowAlpha by animateFloatAsState(
        targetValue = if (isCipherModeOn) 0.6f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cipherGlow"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "cipherScale"
    )

    val bg = if (isCipherModeOn)
        Brush.radialGradient(listOf(ImmersiveCyanGlow, ImmersiveCyan.copy(alpha = 0.25f)))
    else
        Brush.linearGradient(listOf(KeySpecialBg, KeySpecialBg))

    Box(
        modifier = modifier
            .height(KeyHeight)
            .scale(scale)
            .clip(KeyShape)
            .background(bg)
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
            .testTag("key_cipher"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCipherModeOn) "🔒" else "🔓",
            fontSize = 16.sp
        )
        // Profile emoji hint
        Text(
            text = profileEmoji,
            fontSize = 8.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp),
            color = if (isCipherModeOn) ImmersiveCyan else Slate600
        )
    }
}

// ── Long press key (top row letters with number hints) ───────────────────────
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
            glideHighlight -> 1.06f
            pressed -> 0.88f
            else -> 1f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "lpKeyScale"
    )

    val bg = if (glideHighlight || pressed) KeyBgPressed else KeyBg

    Box(modifier = modifier.height(KeyHeight)) {
        // Popup on press
        if (pressed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp)
                    .zIndex(10f)
                    .shadow(8.dp, RoundedCornerShape(8.dp))
                    .background(KeyPopupBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(),
                    color = KeyPopupText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(RoundedCornerShape(10.dp))
                .background(bg, RoundedCornerShape(10.dp))
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
            Text(label, color = Slate100, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                longPressLabel,
                color = Slate500,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 4.dp)
            )
        }
    }
}
