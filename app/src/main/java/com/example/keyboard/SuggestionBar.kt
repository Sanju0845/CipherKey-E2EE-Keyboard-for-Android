package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

private val ToolbarHeight = 44.dp
private val ChipShape = RoundedCornerShape(8.dp)

@Composable
fun SuggestionBar(
    decryptedText: String?,
    composingDraft: String,
    isCipherModeOn: Boolean,
    useSymbols: Boolean,
    onToggleCipherMode: () -> Unit,
    onToggleUseSymbols: () -> Unit,
    onCopyDecrypted: () -> Unit,
    onReplaceDecrypted: () -> Unit,
    onReplyEncrypted: () -> Unit,
    onEncryptActiveField: () -> Unit,
    onDecryptScan: () -> Unit,
    onOpenConfig: () -> Unit,
    hasActiveTextToEncrypt: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ImmersiveToggleStripBg,
        modifier = modifier
            .fillMaxWidth()
            .height(ToolbarHeight)
            .testTag("suggestion_bar_surface")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (decryptedText != null) {
                DecryptToolbarContent(
                    decryptedText = decryptedText,
                    onCopy = onCopyDecrypted,
                    onReplace = onReplaceDecrypted,
                    onReply = onReplyEncrypted
                )
            } else if (isCipherModeOn) {
                CipherToolbarContent(
                    composingDraft = composingDraft,
                    hasActiveTextToEncrypt = hasActiveTextToEncrypt,
                    onEncryptField = onEncryptActiveField,
                    onDecryptScan = onDecryptScan
                )
            } else {
                NormalToolbarContent(
                    useSymbols = useSymbols,
                    hasActiveTextToEncrypt = hasActiveTextToEncrypt,
                    onToggleCipherMode = onToggleCipherMode,
                    onToggleUseSymbols = onToggleUseSymbols,
                    onOpenConfig = onOpenConfig,
                    onEncryptField = onEncryptActiveField,
                    onDecryptScan = onDecryptScan
                )
            }
        }
    }
}

@Composable
private fun RowScope.DecryptToolbarContent(
    decryptedText: String,
    onCopy: () -> Unit,
    onReplace: () -> Unit,
    onReply: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(end = 4.dp)
            .testTag("decrypt_preview_bar")
    ) {
        Text(
            text = "DECRYPTED",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = ImmersiveCyan,
            letterSpacing = 0.8.sp
        )
        Text(
            text = decryptedText,
            fontSize = 12.sp,
            color = Slate100,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    ToolbarChip("Copy", onClick = onCopy, modifier = Modifier.testTag("btn_copy_decrypted"))
    ToolbarChip("Paste", onClick = onReplace, modifier = Modifier.testTag("btn_replace_decrypted"))
    ToolbarChip("Reply", onClick = onReply, accent = true, modifier = Modifier.testTag("btn_reply_encrypted"))
}

@Composable
private fun RowScope.CipherToolbarContent(
    composingDraft: String,
    hasActiveTextToEncrypt: Boolean,
    onEncryptField: () -> Unit,
    onDecryptScan: () -> Unit
) {
    StatusChip(
        label = "LOCK ON",
        accent = true,
        modifier = Modifier.testTag("cipher_active_chip")
    )

    Text(
        text = when {
            composingDraft.isNotEmpty() -> composingDraft
            hasActiveTextToEncrypt -> "Tap Encrypt or type to send"
            else -> "Copy msg → tap Read to decrypt"
        },
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp),
        color = if (composingDraft.isNotEmpty()) ImmersiveCyan else Slate400,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Row(
        modifier = Modifier.width(108.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (composingDraft.isEmpty()) {
            ToolbarChip(
                label = "Read",
                onClick = onDecryptScan,
                modifier = Modifier.testTag("btn_decrypt_scan")
            )
        }
        if (hasActiveTextToEncrypt) {
            ToolbarChip(
                label = "Encrypt",
                onClick = onEncryptField,
                accent = true,
                modifier = Modifier.testTag("btn_encrypt_active_field")
            )
        }
    }
}

@Composable
private fun RowScope.NormalToolbarContent(
    useSymbols: Boolean,
    hasActiveTextToEncrypt: Boolean,
    onToggleCipherMode: () -> Unit,
    onToggleUseSymbols: () -> Unit,
    onOpenConfig: () -> Unit,
    onEncryptField: () -> Unit,
    onDecryptScan: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ToolbarChip("Read", onClick = onDecryptScan, accent = true, modifier = Modifier.testTag("btn_decrypt_scan"))
        ToolbarChip("Lock", onClick = onToggleCipherMode)
        ToolbarChip(
            label = if (useSymbols) "Symbols" else "Emoji",
            onClick = onToggleUseSymbols
        )
        ToolbarChip("Key", onClick = onOpenConfig)
        if (hasActiveTextToEncrypt) {
            ToolbarChip("Encrypt", onClick = onEncryptField, accent = true)
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (accent) ImmersiveCyanTransparent else Slate800
    val border = if (accent) ImmersiveCyan.copy(alpha = 0.5f) else Slate700
    val textColor = if (accent) ImmersiveCyan else Slate300

    Box(
        modifier = modifier
            .height(28.dp)
            .background(bg, ChipShape)
            .border(1.dp, border, ChipShape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (accent) ImmersiveIndigo else Slate800
    val textColor = if (accent) Color.White else Slate300

    Surface(
        onClick = onClick,
        color = bg,
        shape = ChipShape,
        modifier = modifier.height(28.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
