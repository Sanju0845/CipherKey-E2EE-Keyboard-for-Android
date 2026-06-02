package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cipher.CipherEngine
import com.example.cipher.CipherPrefs
import com.example.quick.QuickBubbleService
import com.example.quick.QuickTranslateActivity
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    OnboardingScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(ImmersiveBg) // Cosmic Immersive Dark Theme
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Keyboards System State Checking
    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    // Read stored Passphrase
    var passphrase by remember { mutableStateOf("") }
    
    // Sandbox / Playground state
    var playgroundInput by remember { mutableStateOf(TextFieldValue("")) }
    var playgroundEncryptedResult by remember { mutableStateOf("") }
    var playgroundDecryptedResult by remember { mutableStateOf("") }
    var sandboxUseSymbols by remember { mutableStateOf(true) }

    // Periodically sync keyboard status when returning to app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    val stored = CipherEngine.getStoredPassphrase(context)
                    passphrase = if (stored == CipherEngine.DEFAULT_PASSPHRASE) "" else stored
                    isEnabled = isKeyboardEnabled(context)
                    isSelected = isKeyboardSelected(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to sync keyboard status on resume: ${e.message}")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dynamic checks
    val isSystemReady = isEnabled && isSelected

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("onboarding_root"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Identity Header
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = "CIPHERKEY",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ImmersiveCyan, // Immersive Cyan Neon Accent
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Futuristic Local Symbolic Crypto-Keyboard",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate300
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Type safely across any social platform. Messages are dynamic, hidden behind complex math, customizable emojis, and invisible entropy characters.",
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 18.sp
            )
        }

        QuickChatCard(context = context)

        // Horizontal status warning pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSystemReady) ImmersiveCyanTransparent else Color(0x15FF6B6B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_indicator")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSystemReady) "● STEALTH OVERLAY READY" else "○ ACTION REQUIRED FOR STEALTH",
                    color = if (isSystemReady) ImmersiveCyan else Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // STEP 1: Enable Keyboard in settings
        OnboardingStepCard(
            stepNumber = "1",
            title = "Activate Keyboard",
            description = "Register CipherKey as a system input method. Navigate to System Settings and enable 'CipherKey'.",
            isCompleted = isEnabled,
            actionLabel = "Enable Keyboard",
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to open Input Method Settings: ${e.message}", e)
                }
            },
            modifier = Modifier.testTag("step_1_card")
        )

        // STEP 2: Choose active keyboard
        OnboardingStepCard(
            stepNumber = "2",
            title = "Set as Active",
            description = "Switch your system active input method to CipherKey. Click configure to bring up the picker options.",
            isCompleted = isSelected,
            actionLabel = "Switch Keyboards",
            onAction = {
                try {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to show Input Method Picker: ${e.message}", e)
                }
            },
            enabled = isEnabled,
            modifier = Modifier.testTag("step_2_card")
        )

        // STEP 3: Passphrase Config Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .testTag("passphrase_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(ImmersiveIndigo, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("3", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Group Encryption Password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                }

                Text(
                    text = "All CipherKey clean installs share an instant out-of-the-box key. Define a custom passphrase below to create private communication subgroups.",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Subgroup Passphrase", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = ImmersiveCyan,
                        unfocusedBorderColor = Slate700,
                        focusedContainerColor = ImmersiveSurface,
                        unfocusedContainerColor = ImmersiveSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passphrase_input")
                )
                
                Text(
                    text = when {
                        CipherEngine.isUsingDefaultPassphrase(context) ->
                            "Using default shared key (same on all fresh installs)."
                        else -> "Custom passphrase active — must match on every device."
                    },
                    fontSize = 11.sp,
                    color = if (CipherEngine.isUsingDefaultPassphrase(context)) Slate500 else ImmersiveCyan
                )

                Button(
                    onClick = {
                        CipherEngine.setStoredPassphrase(context, passphrase)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveCyan, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_save_passphrase")
                ) {
                    Text("Save passphrase", fontWeight = FontWeight.Bold)
                }
            }
        }

        // PLAYGROUND: Static interactive playground to try before switching
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .testTag("playground_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔒 Live Crypto Playground",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Type text below and simulate how the keyboard performs instant local transformations.",
                    fontSize = 11.sp,
                    color = Slate400
                )

                OutlinedTextField(
                    value = playgroundInput,
                    onValueChange = { playgroundInput = it },
                    placeholder = { Text("Write cleartext message...", color = Slate500) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = ImmersiveCyan,
                        unfocusedBorderColor = Slate700,
                        focusedContainerColor = ImmersiveSurface,
                        unfocusedContainerColor = ImmersiveSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playground_text_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            sandboxUseSymbols = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sandboxUseSymbols) ImmersiveCyan else Slate700,
                            contentColor = if (sandboxUseSymbols) Color.Black else Slate300
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("👽 Symbols", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            sandboxUseSymbols = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!sandboxUseSymbols) ImmersiveIndigoLight else Slate700,
                            contentColor = if (!sandboxUseSymbols) Color.White else Slate300
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🪐 Emojis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val encrypted = CipherEngine.encrypt(context, playgroundInput.text, sandboxUseSymbols)
                                playgroundEncryptedResult = encrypted
                                playgroundDecryptedResult = ""
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Playground encrypt failed: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_playground_encrypt")
                    ) {
                        Text("🔒 Encrypt", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            try {
                                val decrypted = CipherEngine.decrypt(context, playgroundEncryptedResult)
                                playgroundDecryptedResult = decrypted ?: "Decryption Failed - Check key alignment"
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Playground decrypt failed: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate700,
                            disabledContainerColor = Slate800,
                            contentColor = Color.White,
                            disabledContentColor = Slate500
                        ),
                        enabled = playgroundEncryptedResult.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_playground_decrypt")
                    ) {
                        Text("🔓 Decrypt", fontWeight = FontWeight.Bold)
                    }
                }

                if (playgroundEncryptedResult.isNotEmpty()) {
                    Text(
                        text = "Encrypted Output stream:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveCyan
                    )
                    Surface(
                        color = ImmersiveBg,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = playgroundEncryptedResult,
                            color = Slate300,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(8.dp)
                                .testTag("playground_encrypted_output")
                        )
                    }
                }

                if (playgroundDecryptedResult.isNotEmpty()) {
                    Text(
                        text = "Decrypted Output proof:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveCyan
                    )
                    Surface(
                        color = ImmersiveBg,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = playgroundDecryptedResult,
                            color = Slate100,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(8.dp)
                                .testTag("playground_decrypted_output")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickChatCard(context: Context) {
    var bubbleOn by remember { mutableStateOf(CipherPrefs.isBubbleEnabled(context)) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .testTag("quick_chat_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ Rapid chat mode",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveCyan
            )
            Text(
                text = "Copy → ENC/DEC → paste. No Lock mode needed. Use Symbols on both phones.",
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { QuickTranslateActivity.launch(context) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigo)
                ) {
                    Text("Translator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            QuickBubbleService.requestOverlayPermission(context)
                            return@Button
                        }
                        if (bubbleOn) {
                            QuickBubbleService.stop(context)
                            CipherPrefs.setBubbleEnabled(context, false)
                            bubbleOn = false
                        } else {
                            QuickBubbleService.start(context)
                            bubbleOn = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bubbleOn) ImmersiveCyan else Slate700,
                        contentColor = if (bubbleOn) Color.Black else Color.White
                    )
                ) {
                    Text(
                        if (bubbleOn) "Bubble ON" else "Bubble",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isCompleted) ImmersiveCyan else Slate700,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            color = if (isCompleted) Color.Black else Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                }

                Text(
                    text = if (isCompleted) "✓ ACTIVE" else "PENDING",
                    color = if (isCompleted) ImmersiveCyan else Slate500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 16.sp
            )

            Button(
                onClick = onAction,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Slate700 else ImmersiveIndigo,
                    contentColor = if (isCompleted) Slate100 else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// System keyboard query managers
private fun isKeyboardEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        enabledList.any { it.packageName == context.packageName }
    } catch (e: Exception) {
        false
    }
}

private fun isKeyboardSelected(context: Context): Boolean {
    return try {
        val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        currentIme != null && currentIme.contains(context.packageName)
    } catch (e: Exception) {
        false
    }
}
