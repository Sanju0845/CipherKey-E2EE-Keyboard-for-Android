package com.singularitysoftware.encryptboard.keyboard

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singularitysoftware.encryptboard.ui.theme.*

enum class ActivePanel { KEYBOARD, CLIPBOARD, AI }

/**
 * Grid panel shown when user taps the ↓ arrow button.
 * Looks similar to Gboard's "more options" screen.
 * Shows: Keyboard | Clipboard | AI
 */
@Composable
fun PanelSelector(
    currentPanel: ActivePanel,
    onSelectPanel: (ActivePanel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0D11))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Switch panel",
            color = Slate500,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelTile(
                icon = "⌨",
                label = "Keyboard",
                isActive = currentPanel == ActivePanel.KEYBOARD,
                onClick = { onSelectPanel(ActivePanel.KEYBOARD) },
                modifier = Modifier.weight(1f)
            )
            PanelTile(
                icon = "📋",
                label = "Clipboard",
                isActive = currentPanel == ActivePanel.CLIPBOARD,
                onClick = { onSelectPanel(ActivePanel.CLIPBOARD) },
                modifier = Modifier.weight(1f)
            )
            PanelTile(
                icon = "✦",
                label = "AI",
                isActive = currentPanel == ActivePanel.AI,
                onClick = { onSelectPanel(ActivePanel.AI) },
                accentColor = ImmersiveCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PanelTile(
    icon: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color = Slate300,
    modifier: Modifier = Modifier
) {
    val bg = if (isActive) accentColor.copy(alpha = 0.15f) else Color(0xFF161A20)
    val textColor = if (isActive) accentColor else Slate300

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, fontSize = 22.sp)
        Text(
            label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
