package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    activePanel: ActivePanel = ActivePanel.KEYBOARD,
    onToggleCipherMode: () -> Unit,
    onOpenClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StripBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: suggestions or cipher draft ────────────────────────────────
        if (isCipherModeOn) {
            Text(
                text = if (composingDraft.isNotEmpty()) "✏️  $composingDraft"
                       else "🔒 Cipher ON  ·  type to compose",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                color = if (composingDraft.isNotEmpty()) ImmersiveCyan else Slate500,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                                .height(18.dp)
                                .background(Color(0xFF2A3040))
                        )
                    }
                }
            } else {
                Text(
                    "CipherKey · type to get suggestions",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    color = Slate600,
                    fontSize = 11.sp
                )
            }
        }

        // ── Right: ↓ arrow opens panel selector ──────────────────────────────
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when (activePanel) {
                        ActivePanel.AI -> ImmersiveCyan.copy(alpha = 0.15f)
                        ActivePanel.CLIPBOARD -> ImmersiveIndigo.copy(alpha = 0.15f)
                        else -> Color(0xFF1E2530)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onOpenClipboard  // onOpenClipboard now opens the panel selector
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (activePanel) {
                    ActivePanel.AI -> "✦"
                    ActivePanel.CLIPBOARD -> "📋"
                    else -> "↓"
                },
                color = when (activePanel) {
                    ActivePanel.AI -> ImmersiveCyan
                    ActivePanel.CLIPBOARD -> ImmersiveIndigoLight
                    else -> Slate300
                },
                fontSize = if (activePanel == ActivePanel.KEYBOARD) 19.sp else 17.sp,
                fontWeight = FontWeight.Normal
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
                    .height(26.dp)
                    .widthIn(min = 56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF252B35)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    word,
                    color = Slate100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        } else {
            Text(word, color = Slate400, fontSize = 13.sp)
        }
    }
}
