package com.example.quick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipher.CipherClipboard
import com.example.cipher.CipherEngine
import com.example.cipher.CipherPrefs
import com.example.cipher.CipherScanner
import com.example.ui.theme.*

/**
 * Translator-style screen: paste / copy from clipboard, one tap encrypt or decrypt.
 * Optimized for rapid WhatsApp-style chat (no keyboard switching).
 */
class QuickTranslateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                QuickTranslateScreen(
                    onBack = { finish() },
                    onOpenBubble = {
                        if (android.provider.Settings.canDrawOverlays(this)) {
                            QuickBubbleService.start(this)
                            CipherPrefs.setBubbleEnabled(this, true)
                        } else {
                            QuickBubbleService.requestOverlayPermission(this)
                        }
                    }
                )
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(
                Intent(context, QuickTranslateActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}

@Composable
fun QuickTranslateScreen(onBack: () -> Unit, onOpenBubble: () -> Unit) {
    val context = LocalContext.current
    var plain by remember { mutableStateOf("") }
    var cipher by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var useSymbols by remember { mutableStateOf(CipherPrefs.getUseSymbols(context)) }

    LaunchedEffect(Unit) {
        val clip = CipherClipboard.read(context) ?: return@LaunchedEffect
        if (CipherScanner.tryDecrypt(context, clip) != null) {
            cipher = clip
            status = "Loaded cipher from clipboard"
        } else {
            plain = clip
            status = "Loaded plain text from clipboard"
        }
    }

    fun copyOut(text: String, label: String) {
        CipherClipboard.write(context, label, text)
        CipherClipboard.toast(context, "Copied — paste in chat")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("← Back", color = Slate400) }
            TextButton(onClick = onOpenBubble) {
                Text("Floating bubble", color = ImmersiveCyan, fontSize = 12.sp)
            }
        }

        Text(
            text = "Quick translator",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ImmersiveCyan
        )
        Text(
            text = "Copy a chat message → open here → Decrypt. Type plain → Encrypt → paste send.",
            fontSize = 12.sp,
            color = Slate400,
            lineHeight = 18.sp
        )

        status?.let {
            Text(text = it, fontSize = 11.sp, color = ImmersiveCyan)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = useSymbols,
                onClick = {
                    useSymbols = true
                    CipherPrefs.setUseSymbols(context, true)
                },
                label = { Text("Symbols") }
            )
            FilterChip(
                selected = !useSymbols,
                onClick = {
                    useSymbols = false
                    CipherPrefs.setUseSymbols(context, false)
                },
                label = { Text("Emoji") }
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val clip = CipherClipboard.read(context)
                if (clip == null) {
                    status = "Clipboard empty"
                } else if (CipherScanner.tryDecrypt(context, clip) != null) {
                    cipher = clip
                    status = "Pasted cipher from clipboard"
                } else {
                    plain = clip
                    status = "Pasted plain from clipboard"
                }
            }) {
                Text("Paste clip", color = Slate300, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = plain,
            onValueChange = { plain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Plain (your message)", color = Slate500) },
            minLines = 2,
            colors = fieldColors()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (plain.isBlank()) {
                        status = "Enter plain text first"
                        return@Button
                    }
                    cipher = CipherEngine.encrypt(context, plain, useSymbols)
                    copyOut(cipher, "CipherKey encrypted")
                    status = "Encrypted — copied"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigo)
            ) {
                Text("Encrypt → copy", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (cipher.isBlank()) {
                        status = "Paste cipher text first"
                        return@Button
                    }
                    val result = CipherScanner.tryDecrypt(context, cipher)
                    if (result != null) {
                        plain = result.plaintext
                        copyOut(plain, "CipherKey decrypted")
                        status = "Decrypted — copied"
                    } else {
                        status = when (CipherScanner.diagnose(context, cipher)) {
                            CipherScanner.FailureReason.DAMAGED ->
                                "Damaged in transit — copy full message, use Symbols"
                            CipherScanner.FailureReason.WRONG_KEY ->
                                "Wrong passphrase — match Key in main app"
                            else -> "Not cipher text — copy ꧁…꧂ block only"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveCyan, contentColor = Color.Black)
            ) {
                Text("Decrypt → copy", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(
            value = cipher,
            onValueChange = { cipher = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cipher (from chat)", color = Slate500) },
            minLines = 3,
            colors = fieldColors()
        )

        Text(
            text = "Tip: keep Symbols mode on both phones for WhatsApp. After encrypt, paste once in WhatsApp and send.",
            fontSize = 11.sp,
            color = Slate500,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Slate100,
    unfocusedTextColor = Slate100,
    focusedBorderColor = ImmersiveCyan,
    unfocusedBorderColor = Slate700,
    focusedContainerColor = ImmersiveSurface,
    unfocusedContainerColor = ImmersiveSurface
)
