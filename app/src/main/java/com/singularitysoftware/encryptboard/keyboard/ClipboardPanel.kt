package com.singularitysoftware.encryptboard.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singularitysoftware.encryptboard.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ClipboardEntry(
    val id: Int,
    val text: String,
    val isCipher: Boolean
)

// Result from inline decrypt preview — includes integrity status
data class DecryptPreviewResult(
    val plaintext: String,
    val integrityOk: Boolean
)

@Composable
fun ClipboardPanel(
    entries: List<ClipboardEntry>,
    onBackToKeyboard: () -> Unit,
    onPasteRaw: (String) -> Unit,
    onEncryptAndPaste: (String) -> Unit,
    onEncryptWithProfile: (String, com.singularitysoftware.encryptboard.cipher.CoverProfile) -> Unit = { text, _ -> onEncryptAndPaste(text) },
    onDecryptAndPaste: (String) -> Unit,
    onDecryptPreview: (String) -> DecryptPreviewResult,
    onDelete: (Int) -> Unit,
    currentProfile: com.singularitysoftware.encryptboard.cipher.CoverProfile = com.singularitysoftware.encryptboard.cipher.CoverProfile.SYMBOLS,
    availableProfiles: List<com.singularitysoftware.encryptboard.cipher.CoverProfile> = com.singularitysoftware.encryptboard.cipher.CoverProfile.entries,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ImmersiveClipBg)
    ) {
        Text(
            text = "Tap to paste  ·  🔒 encrypt  ·  🔓 preview",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = Slate600,
            fontSize = 11.sp
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Nothing copied yet", color = Slate500, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                        ) + fadeIn(tween(200))
                    ) {
                        SwipeToDeleteWrapper(onDelete = { onDelete(entry.id) }) {
                            ClipCard(
                                entry = entry,
                                currentProfile = currentProfile,
                                availableProfiles = availableProfiles,
                                onPasteRaw = { onPasteRaw(entry.text) },
                                onEncrypt = { profile -> onEncryptWithProfile(entry.text, profile) },
                                onDecryptPreview = { onDecryptPreview(entry.text) },
                                onDecryptAndPaste = { plain -> onDecryptAndPaste(plain) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipCard(
    entry: ClipboardEntry,
    currentProfile: com.singularitysoftware.encryptboard.cipher.CoverProfile,
    availableProfiles: List<com.singularitysoftware.encryptboard.cipher.CoverProfile>,
    onPasteRaw: () -> Unit,
    onEncrypt: (com.singularitysoftware.encryptboard.cipher.CoverProfile) -> Unit,
    onDecryptPreview: () -> DecryptPreviewResult,
    onDecryptAndPaste: (String) -> Unit
) {
    var decryptedPreview by remember(entry.id) { mutableStateOf<DecryptPreviewResult?>(null) }
    var showEncryptPicker by remember(entry.id) { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1E24))
    ) {
        // ── Main card row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = onPasteRaw
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text preview
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp)
            ) {
                if (entry.isCipher) {
                    Text(
                        "🔒 encrypted",
                        color = ImmersiveCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = entry.text,
                    color = if (entry.isCipher) Slate400 else Slate200,
                    fontSize = 13.sp,
                    maxLines = if (entry.isCipher) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            // Action button: encrypt for plain text, decrypt-preview for cipher text
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                    .background(
                        if (entry.isCipher) ImmersiveCyan.copy(alpha = 0.10f)
                        else if (showEncryptPicker) ImmersiveIndigo.copy(alpha = 0.30f)
                        else ImmersiveIndigo.copy(alpha = 0.15f)
                    )
                    .pointerInput(entry.id) {
                        detectTapGestures(
                            onTap = {
                                if (entry.isCipher) {
                                    if (decryptedPreview != null) decryptedPreview = null
                                    else decryptedPreview = onDecryptPreview().let { result ->
                                        if (result.plaintext.isEmpty())
                                            DecryptPreviewResult("⚠ Could not decrypt — wrong passphrase?", false)
                                        else result
                                    }
                                } else {
                                    onEncrypt(currentProfile)
                                }
                            },
                            onLongPress = {
                                if (!entry.isCipher) showEncryptPicker = !showEncryptPicker
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (entry.isCipher) {
                            if (decryptedPreview != null) "🔒" else "🔓"
                        } else "🔒",
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (entry.isCipher) {
                            if (decryptedPreview != null) "hide" else "read"
                        } else "enc",
                        color = if (entry.isCipher) ImmersiveCyan else ImmersiveIndigoLight,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Long-press hint for plain text cards
                    if (!entry.isCipher) {
                        Text("hold=profile", color = Slate700, fontSize = 7.sp)
                    }
                }
            }
        }

        // ── Encrypt profile picker (shown on long press of 🔒 button) ──────────
        AnimatedVisibility(
            visible = showEncryptPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117))
                    .padding(8.dp)
            ) {
                Text("Choose encrypt profile:", color = Slate500, fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableProfiles.filter {
                        it != com.singularitysoftware.encryptboard.cipher.CoverProfile.SYMBOLS &&
                        it != com.singularitysoftware.encryptboard.cipher.CoverProfile.EMOJIS || true
                    }) { profile ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (profile == currentProfile) ImmersiveCyan.copy(alpha = 0.15f)
                                    else Color(0xFF1A1E26)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    onClick = {
                                        onEncrypt(profile)
                                        showEncryptPicker = false
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${profile.emoji} ${profile.displayName}",
                                color = if (profile == currentProfile) ImmersiveCyan else Slate300,
                                fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── Decrypt preview panel ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = decryptedPreview != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111520))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Integrity badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔓 Decrypted:", color = ImmersiveCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    if (decryptedPreview?.integrityOk == true) {
                        Text(
                            "✓ Verified",
                            color = Color(0xFF4CAF50),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "⚠ Integrity Failed",
                            color = Color(0xFFFF6B6B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = decryptedPreview?.plaintext ?: "",
                    color = if (decryptedPreview?.integrityOk == true) Slate100 else Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip(
                        label = "Copy",
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("decrypted", decryptedPreview?.plaintext ?: ""))
                            decryptedPreview = null
                        }
                    )
                    if (decryptedPreview?.integrityOk == true) {
                        ActionChip(
                            label = "Paste into field",
                            accent = true,
                            onClick = {
                                decryptedPreview?.plaintext?.let { onDecryptAndPaste(it) }
                                decryptedPreview = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (accent) ImmersiveIndigo else Slate800)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (accent) Color.White else Slate300,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Wraps content with a swipe-left-to-delete gesture.
 * Dragging left reveals a red "🗑 Delete" background.
 * Swipe past 40% of width → item deletes with a slide-out animation.
 * Release before 40% → snaps back.
 */
@Composable
fun SwipeToDeleteWrapper(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var itemWidth by remember { mutableStateOf(1f) }
    // How far left the user has swiped as a fraction 0..1
    val swipeFraction = (-offsetX.value / itemWidth).coerceIn(0f, 1f)
    // Red bg intensity based on swipe
    val deleteBgAlpha = (swipeFraction * 1.8f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> itemWidth = coords.size.width.toFloat() }
    ) {
        // ── Red delete background ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFB00020).copy(alpha = deleteBgAlpha)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🗑", fontSize = 16.sp)
                if (swipeFraction > 0.2f) {
                    Text(
                        "Delete",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Draggable content ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (-offsetX.value > itemWidth * 0.4f) {
                                    // Committed — slide fully out then delete
                                    offsetX.animateTo(-itemWidth, tween(200))
                                    onDelete()
                                    offsetX.snapTo(0f)
                                } else {
                                    // Not far enough — snap back
                                    offsetX.animateTo(0f, tween(250))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, tween(250)) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                // Only allow left swipe (negative drag)
                                val newVal = (offsetX.value + dragAmount).coerceIn(-itemWidth, 0f)
                                offsetX.snapTo(newVal)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
