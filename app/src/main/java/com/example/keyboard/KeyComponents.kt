package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700

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
                } else {
                    Modifier
                }
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
