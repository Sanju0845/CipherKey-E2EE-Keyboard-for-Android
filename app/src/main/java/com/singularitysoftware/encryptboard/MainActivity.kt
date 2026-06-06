package com.singularitysoftware.encryptboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.singularitysoftware.encryptboard.cipher.CipherEngine
import com.singularitysoftware.encryptboard.cipher.CipherPrefs
import com.singularitysoftware.encryptboard.quick.QuickBubbleService
import com.singularitysoftware.encryptboard.quick.QuickTranslateActivity
import com.singularitysoftware.encryptboard.ui.theme.*

// ── Prefs key ─────────────────────────────────────────────────────────────────
private const val PREFS_NAME = "encryptboard_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EncryptBoardTheme {
                AppRoot()
            }
        }
    }
}

// ── Root: decides onboarding vs main app ─────────────────────────────────────
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var onboardingDone by remember {
        mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    }

    if (!onboardingDone) {
        OnboardingFlow {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
            // Request overlay permission after onboarding
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            onboardingDone = true
        }
    } else {
        MainApp()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

data class OnboardPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

private val onboardPages = listOf(
    OnboardPage(
        "🔒",
        "End-to-End Encrypted",
        "Every message you type is encrypted on-device using AES-128 before it leaves your keyboard. Nobody reads your messages — not even us.",
        Color(0xFF22D3EE)
    ),
    OnboardPage(
        "👾",
        "Invisible in Plain Sight",
        "Encrypted text disguises itself as symbols, emojis, or normal-looking text. Your chats look ordinary. The secrets stay secret.",
        Color(0xFF818CF8)
    ),
    OnboardPage(
        "⚡",
        "Works in Every App",
        "EncryptBoard is a system keyboard — encrypt and decrypt directly inside WhatsApp, Telegram, Gmail, or any app without switching.",
        Color(0xFF4ADE80)
    )
)

@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = onboardPages[page]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        // Glow blob behind emoji
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .blur(80.dp)
                .background(current.accent.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 100.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: emoji + text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        fadeIn(tween(400)) + slideInHorizontally { it / 3 } togetherWith
                        fadeOut(tween(200)) + slideOutHorizontally { -it / 3 }
                    }, label = "page"
                ) { p ->
                    val pg = onboardPages[p]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Big emoji in a glowing circle
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(pg.accent.copy(alpha = 0.2f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, pg.accent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pg.emoji, fontSize = 56.sp)
                        }

                        Spacer(Modifier.height(40.dp))

                        Text(
                            text = pg.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate100,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = pg.subtitle,
                            fontSize = 15.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Bottom: dots + button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onboardPages.forEachIndexed { i, _ ->
                        val isActive = i == page
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isActive) 24.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isActive) current.accent else Slate700)
                        )
                    }
                }

                // CTA button
                Button(
                    onClick = {
                        if (page < onboardPages.size - 1) page++ else onFinish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = current.accent,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (page < onboardPages.size - 1) "Continue" else "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Skip
                if (page < onboardPages.size - 1) {
                    TextButton(onClick = onFinish) {
                        Text("Skip", color = Slate500, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN APP — bottom nav
// ─────────────────────────────────────────────────────────────────────────────

enum class NavTab(val emoji: String, val label: String) {
    HOME("🏠", "Home"),
    TRANSLATE("⚡", "Translate"),
    SETTINGS("⚙", "Settings")
}

@Composable
fun MainApp() {
    var activeTab by remember { mutableStateOf(NavTab.HOME) }

    Box(modifier = Modifier.fillMaxSize().background(ImmersiveBg)) {
        // Content area — padded above nav bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                }, label = "tab"
            ) { tab ->
                when (tab) {
                    NavTab.HOME     -> HomeTab()
                    NavTab.TRANSLATE -> TranslateTab()
                    NavTab.SETTINGS -> SettingsTab()
                }
            }
        }

        // Floating capsule nav bar
        FloatingNavBar(
            activeTab = activeTab,
            onTabSelect = { activeTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FloatingNavBar(
    activeTab: NavTab,
    onTabSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 32.dp, vertical = 20.dp)
            .fillMaxWidth()
            .height(60.dp)
            .shadow(24.dp, RoundedCornerShape(30.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF0D1117), Color(0xFF161B22))
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(ImmersiveCyan.copy(alpha = 0.3f), ImmersiveIndigo.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(30.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                NavPill(
                    tab = tab,
                    isActive = tab == activeTab,
                    onClick = { onTabSelect(tab) }
                )
            }
        }
    }
}

@Composable
fun NavPill(tab: NavTab, isActive: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) ImmersiveCyan.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200), label = "navbg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) ImmersiveCyan else Slate500,
        animationSpec = tween(200), label = "navtext"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(tab.emoji, fontSize = 16.sp)
            AnimatedVisibility(visible = isActive) {
                Text(
                    tab.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeTab() {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }
    val isReady = isEnabled && isSelected

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = isKeyboardEnabled(context)
                isSelected = isKeyboardSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                "EncryptBoard",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ImmersiveCyan,
                letterSpacing = 1.sp
            )
            Text(
                "Crypto-powered keyboard",
                fontSize = 13.sp,
                color = Slate500,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Status banner
        StatusBanner(isReady)

        // Setup steps
        SetupStepCard(
            number = 1,
            icon = "⌨️",
            title = "Enable Keyboard",
            description = "Add EncryptBoard to your system keyboards in Settings.",
            isCompleted = isEnabled,
            buttonLabel = "Open Settings",
            enabled = true,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )

        SetupStepCard(
            number = 2,
            icon = "✅",
            title = "Set as Active",
            description = "Switch your active input method to EncryptBoard.",
            isCompleted = isSelected,
            buttonLabel = "Switch Keyboard",
            enabled = isEnabled,
            onClick = {
                runCatching {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                }
            }
        )

        // Overlay permission card
        OverlayPermissionCard(context)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun StatusBanner(isReady: Boolean) {
    val bg = if (isReady) ImmersiveCyanTransparent else Color(0x18FF6B6B)
    val accent = if (isReady) ImmersiveCyan else Color(0xFFFF6B6B)
    val dot = if (isReady) "●" else "○"
    val text = if (isReady) "Keyboard active · Encryption ready" else "Setup required · Follow steps below"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(dot, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SetupStepCard(
    number: Int,
    icon: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isCompleted) ImmersiveCyan.copy(alpha = 0.15f) else ImmersiveIndigo.copy(alpha = 0.12f),
                        CircleShape
                    )
                    .border(1.dp, if (isCompleted) ImmersiveCyan.copy(alpha = 0.5f) else ImmersiveIndigo.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCompleted) "✓" else icon,
                    fontSize = 20.sp,
                    color = if (isCompleted) ImmersiveCyan else ImmersiveIndigoLight
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate100)
                    Text(
                        if (isCompleted) "DONE" else "PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) ImmersiveCyan else Slate600
                    )
                }
                Text(description, fontSize = 12.sp, color = Slate500, lineHeight = 16.sp)

                Button(
                    onClick = onClick,
                    enabled = enabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Slate800 else ImmersiveIndigo,
                        contentColor = if (isCompleted) Slate400 else Color.White,
                        disabledContainerColor = Slate900,
                        disabledContentColor = Slate700
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(buttonLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun OverlayPermissionCard(context: Context) {
    val hasOverlay = Settings.canDrawOverlays(context)
    if (hasOverlay) return

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A10)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ImmersiveGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ImmersiveGreen.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, ImmersiveGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🫧", fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Overlay Permission", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate100)
                Text("Required for the floating ENC/DEC bubble over chat apps.", fontSize = 12.sp, color = Slate500, lineHeight = 16.sp)
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveGreen.copy(alpha = 0.15f),
                        contentColor = ImmersiveGreen
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Grant Permission", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRANSLATE TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TranslateTab() {
    val context = LocalContext.current
    var bubbleOn by remember { mutableStateOf(CipherPrefs.isBubbleEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Quick Tools",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ImmersiveCyan,
            letterSpacing = 1.sp
        )
        Text("Encrypt & decrypt without switching keyboards.", fontSize = 13.sp, color = Slate500)

        Spacer(Modifier.height(4.dp))

        // Translator card
        ToolCard(
            icon = "🔄",
            title = "Translator",
            subtitle = "Paste cipher → decrypt. Type plain → encrypt → copy & send.",
            accentColor = ImmersiveIndigo,
            buttonLabel = "Open Translator",
            onClick = { QuickTranslateActivity.launch(context) }
        )

        // Bubble card
        ToolCard(
            icon = "🫧",
            title = "Floating Bubble",
            subtitle = "ENC/DEC buttons float over any app. Copy → tap → paste.",
            accentColor = if (bubbleOn) ImmersiveCyan else Slate700,
            buttonLabel = if (bubbleOn) "Turn Bubble OFF" else "Turn Bubble ON",
            onClick = {
                if (!Settings.canDrawOverlays(context)) {
                    QuickBubbleService.requestOverlayPermission(context)
                    return@ToolCard
                }
                if (bubbleOn) {
                    QuickBubbleService.stop(context)
                    CipherPrefs.setBubbleEnabled(context, false)
                    bubbleOn = false
                } else {
                    QuickBubbleService.start(context)
                    CipherPrefs.setBubbleEnabled(context, true)
                    bubbleOn = true
                }
            }
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ToolCard(
    icon: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 20.sp)
                }
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate100)
                    Text(subtitle, fontSize = 12.sp, color = Slate500, lineHeight = 16.sp)
                }
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (accentColor == ImmersiveCyan) Color.Black else Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(buttonLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS TAB
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab() {
    val context = LocalContext.current
    var passphrase by remember { mutableStateOf("") }
    var sandboxInput by remember { mutableStateOf(TextFieldValue("")) }
    var encryptedOut by remember { mutableStateOf("") }
    var decryptedOut by remember { mutableStateOf("") }
    var useSymbols by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val stored = CipherEngine.getStoredPassphrase(context)
        passphrase = if (stored == CipherEngine.DEFAULT_PASSPHRASE) "" else stored
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ImmersiveCyan,
            letterSpacing = 1.sp
        )

        // Passphrase card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ImmersiveIndigo.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .border(1.dp, ImmersiveIndigo.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🔑", fontSize = 18.sp) }
                    Column {
                        Text("Group Passphrase", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text("Shared secret for your group", fontSize = 11.sp, color = Slate500)
                    }
                }

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase", color = Slate500) },
                    placeholder = { Text("Leave blank for default key", color = Slate600) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = ImmersiveCyan,
                        unfocusedBorderColor = Slate800,
                        focusedContainerColor = ImmersiveSurface,
                        unfocusedContainerColor = ImmersiveSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("passphrase_input")
                )

                Text(
                    text = if (CipherEngine.isUsingDefaultPassphrase(context))
                        "Using default shared key — same on all fresh installs."
                    else "Custom passphrase active — must match on every device.",
                    fontSize = 11.sp,
                    color = if (CipherEngine.isUsingDefaultPassphrase(context)) Slate600 else ImmersiveCyan
                )

                Button(
                    onClick = { CipherEngine.setStoredPassphrase(context, passphrase) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigo),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_save_passphrase")
                ) {
                    Text("Save Passphrase", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Crypto playground card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ImmersiveCyan.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, ImmersiveCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🧪", fontSize = 18.sp) }
                    Column {
                        Text("Crypto Playground", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text("Test encrypt/decrypt live", fontSize = 11.sp, color = Slate500)
                    }
                }

                OutlinedTextField(
                    value = sandboxInput,
                    onValueChange = { sandboxInput = it },
                    placeholder = { Text("Type a test message...", color = Slate600) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = ImmersiveCyan,
                        unfocusedBorderColor = Slate800,
                        focusedContainerColor = ImmersiveSurface,
                        unfocusedContainerColor = ImmersiveSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("playground_text_field")
                )

                // Mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(true to "👽 Symbols", false to "🪐 Emojis").forEach { (sym, label) ->
                        val active = useSymbols == sym
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) ImmersiveIndigo else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { useSymbols = sym }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else Slate500
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            runCatching {
                                encryptedOut = CipherEngine.encrypt(context, sandboxInput.text, useSymbols)
                                decryptedOut = ""
                            }.onFailure { Log.e("Playground", "encrypt failed: ${it.message}") }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigo),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("btn_playground_encrypt")
                    ) {
                        Text("🔒 Encrypt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            runCatching {
                                decryptedOut = CipherEngine.decrypt(context, encryptedOut)
                                    ?: "Failed — check passphrase"
                            }.onFailure { Log.e("Playground", "decrypt failed: ${it.message}") }
                        },
                        enabled = encryptedOut.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate800,
                            contentColor = Slate100,
                            disabledContainerColor = Slate900,
                            disabledContentColor = Slate700
                        ),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("btn_playground_decrypt")
                    ) {
                        Text("🔓 Decrypt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (encryptedOut.isNotEmpty()) {
                    PlaygroundOutput(label = "Encrypted", value = encryptedOut, accent = ImmersiveCyan)
                }
                if (decryptedOut.isNotEmpty()) {
                    PlaygroundOutput(label = "Decrypted", value = decryptedOut, accent = ImmersiveGreen)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PlaygroundOutput(label: String, value: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
        Surface(
            color = ImmersiveSurface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                color = Slate300,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun isKeyboardEnabled(context: Context): Boolean = runCatching {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.enabledInputMethodList?.any { it.packageName == context.packageName } == true
}.getOrDefault(false)

private fun isKeyboardSelected(context: Context): Boolean = runCatching {
    val cur = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    cur != null && cur.contains(context.packageName)
}.getOrDefault(false)
