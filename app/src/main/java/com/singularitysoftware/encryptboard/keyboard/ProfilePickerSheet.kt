package com.singularitysoftware.encryptboard.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singularitysoftware.encryptboard.cipher.CoverProfile
import com.singularitysoftware.encryptboard.ui.theme.*

/**
 * Horizontal scrollable profile picker shown above the keyboard strip
 * when the user long-presses the 🔒 cipher toggle button.
 *
 * Each chip shows the profile emoji + name.
 * Selected profile gets a cyan highlight border.
 * Dismiss by tapping outside (handled by caller setting showProfilePicker = false).
 */
@Composable
fun ProfilePickerSheet(
    currentProfile: CoverProfile,
    onSelectProfile: (CoverProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ImmersiveGlass)
            .padding(vertical = 8.dp)
    ) {
        Text(
            "Cover Profile  ·  Long press 🔒 to dismiss",
            modifier = Modifier.padding(horizontal = 14.dp),
            color = Slate500,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CoverProfile.entries) { profile ->
                ProfileChip(
                    profile = profile,
                    isSelected = profile == currentProfile,
                    onClick = { onSelectProfile(profile) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ProfileChip(
    profile: CoverProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ImmersiveCyan.copy(alpha = 0.15f) else Color(0xFF141920),
        animationSpec = tween(200), label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) ImmersiveCyan else Slate400,
        animationSpec = tween(200), label = "chipText"
    )

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(1.dp, ImmersiveCyan, RoundedCornerShape(8.dp))
                else Modifier.border(1.dp, Slate800, RoundedCornerShape(8.dp))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(profile.emoji, fontSize = 14.sp)
        Text(
            profile.displayName,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
