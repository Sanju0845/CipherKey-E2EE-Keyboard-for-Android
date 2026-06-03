package com.example.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun KeyboardStrip(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    isCipherModeOn: Boolean,
    composingDraft: String,
    showClipboard: Boolean,
    onToggleCipherMode: () -> Unit,
    onOpenClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate bg color: darker when cipher is on
    val stripBg by animateColorAsState(
        targetValue = if (isCipherModeOn) Color(0xFF060A0E) else ImmersiveStripBg,
        animationSpec = tween(300),
        label = "stripBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)   // 48dp — Material minimum touch target
            .background(stripBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: word suggestions or cipher draft ────────────────────────────
        if (isCipherModeOn) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pulsing lock indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ImmersiveCyan)
                )
                Text(
                    text = if (composingDraft.isNotEmpty()) composingDraft
                           else "Cipher ON  ·  type to compose",
                    color = if (composingDraft.isNotEmpty()) ImmersiveCyan else Slate500,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            if (suggestions.isNotEmpty()) {
                suggestions.forEachIndexed { i, word ->
                    WordChip(
                        word = word,
                        isCenter = i == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { onSuggestionClick(word) }
                    )
                    if (i < suggestions.size - 1) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(Slate800)
                        )
                    }
                }
            } else {
                Text(
                    "CipherKey",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    color = Slate700,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Right: clipboard/keyboard toggle — 48dp touch target ──────────────
        val iconBg by animateColorAsState(
            targetValue = if (showClipboard) ImmersiveCyan.copy(alpha = 0.15f) else Color(0xFF111820),
            animationSpec = tween(250),
            label = "iconBg"
        )
        val iconColor by animateColorAsState(
            targetValue = if (showClipboard) ImmersiveCyan else Slate400,
            animationSpec = tween(250),
            label = "iconColor"
        )

        Box(
            modifier = Modifier
                .size(48.dp)   // full 48dp touch target
                .padding(8.dp) // 8dp padding → 32dp visual icon
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onOpenClipboard
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showClipboard) "⌨" else "❑",
                color = iconColor,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun WordChip(
    word: String,
    isCenter: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCenter) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(min = 60.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF1C2535)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    word,
                    color = Slate100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        } else {
            Text(word, color = Slate400, fontSize = 13.sp)
        }
    }
}
