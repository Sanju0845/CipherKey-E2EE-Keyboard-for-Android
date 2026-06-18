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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.singularitysoftware.encryptboard.cipher.CipherEngine
import com.singularitysoftware.encryptboard.cipher.CipherPrefs
import com.singularitysoftware.encryptboard.cipher.CoverProfile
import com.singularitysoftware.encryptboard.cipher.CoverEncoder
import com.singularitysoftware.encryptboard.cipher.CipherScanner
import com.singularitysoftware.encryptboard.cipher.CipherClipboard
import com.singularitysoftware.encryptboard.quick.QuickBubbleService
import com.singularitysoftware.encryptboard.quick.QuickTranslateActivity
import com.singularitysoftware.encryptboard.ui.theme.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// ── Prefs key ─────────────────────────────────────────────────────────────────
private const val PREFS_NAME = "encryptboard_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

// ── Root: decides onboarding vs main app ─────────────────────────────────────
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var themeMode by remember { mutableStateOf(CipherPrefs.getThemeMode(context)) }
    val isDark = when(themeMode) {
        "light" -> false
        "dark"  -> true
        else    -> isSystemInDarkTheme()
    }
    var onboardingDone by remember {
        mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    }

    EncryptBoardTheme(darkTheme = isDark) {
        AnimatedContent(
            targetState = onboardingDone,
            transitionSpec = {
                (fadeIn(animationSpec = tween(700, easing = LinearOutSlowInEasing)) + 
                 scaleIn(initialScale = 1.05f, animationSpec = tween(700, easing = LinearOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(400, easing = FastOutLinearInEasing)) + 
                        scaleOut(targetScale = 0.95f, animationSpec = tween(400, easing = FastOutLinearInEasing))
                    )
            },
            label = "app_root_transition"
        ) { done ->
            if (!done) {
                OnboardingFlow {
                    prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                    onboardingDone = true
                }
            } else {
                MainApp(themeMode = themeMode, onThemeChange = { newMode ->
                    CipherPrefs.setThemeMode(context, newMode)
                    themeMode = newMode
                })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

data class OnboardPage(
    val title: String,
    val subtitle: String,
    val accent: Color
)

private val onboardPages = listOf(
    OnboardPage(
        "End-to-End Encrypted",
        "Every message you type is encrypted on-device using AES-128 before it leaves your keyboard. Nobody reads your messages — not even us.",
        Color(0xFF22D3EE)
    ),
    OnboardPage(
        "Invisible in Plain Sight",
        "Encrypted text disguises itself as symbols, emojis, or normal-looking text. Your chats look ordinary. The secrets stay secret.",
        Color(0xFF818CF8)
    ),
    OnboardPage(
        "Works in Every App",
        "EncryptBoard is a system keyboard — encrypt and decrypt directly inside WhatsApp, Telegram, Gmail, or any app without switching.",
        Color(0xFF4ADE80)
    )
)

@Composable
fun OnboardingIllustration(pageIndex: Int, accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "illustration")

    // Subtle pulsing wave
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Transit phase for data packet flow
    val dataOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "data_flow"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .background(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.15f), Color.Transparent)
                ),
                shape = CircleShape
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2

            when (pageIndex) {
                0 -> {
                    // Page 1: Encryption Shield & Padlock Cyber Fusion
                    // Outer dashed telemetry ring
                    drawCircle(
                        color = accent.copy(alpha = 0.2f),
                        radius = cx * 0.95f * pulseScale,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 15f), 0f)
                        )
                    )

                    // Shield curve path
                    val shieldPath = Path().apply {
                        moveTo(cx, cy - cy * 0.65f)
                        quadraticTo(cx + cx * 0.55f, cy - cy * 0.65f, cx + cx * 0.55f, cy - cy * 0.1f)
                        quadraticTo(cx + cx * 0.55f, cy + cy * 0.45f, cx, cy + cy * 0.75f)
                        quadraticTo(cx - cx * 0.55f, cy + cy * 0.45f, cx - cx * 0.55f, cy - cy * 0.1f)
                        quadraticTo(cx - cx * 0.55f, cy - cy * 0.65f, cx, cy - cy * 0.65f)
                        close()
                    }
                    drawPath(
                        path = shieldPath,
                        color = accent,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Padlock shackle
                    val shackleWidth = cx * 0.24f
                    val shackleHeight = cy * 0.28f
                    drawRoundRect(
                        color = accent.copy(alpha = 0.6f),
                        topLeft = Offset(cx - shackleWidth / 2, cy - cy * 0.38f),
                        size = Size(shackleWidth, shackleHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Padlock solid body
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(cx - cx * 0.25f, cy - cy * 0.12f),
                        size = Size(cx * 0.5f, cy * 0.35f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Security central dot representation
                    drawCircle(
                        color = Color(0xFF030712), // High contrast deep dark
                        radius = 3.dp.toPx(),
                        center = Offset(cx, cy + cy * 0.05f)
                    )
                }
                1 -> {
                    // Page 2: Stealth Invisible Camouflage Transformation Stream (Plain sight disguise)
                    // Rotating or stationary telemetry ring
                    drawCircle(
                        color = accent.copy(alpha = 0.15f),
                        radius = cx * 0.85f,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Abstract stealth look curves
                    val upperLash = Path().apply {
                        moveTo(cx - cx * 0.65f, cy)
                        quadraticTo(cx, cy - cy * 0.55f * pulseScale, cx + cx * 0.65f, cy)
                    }
                    val lowerLash = Path().apply {
                        moveTo(cx - cx * 0.65f, cy)
                        quadraticTo(cx, cy + cy * 0.55f * pulseScale, cx + cx * 0.65f, cy)
                    }
                    drawPath(upperLash, color = accent, style = Stroke(width = 2.dp.toPx()))
                    drawPath(lowerLash, color = accent, style = Stroke(width = 2.dp.toPx()))

                    // Central secure core ring
                    drawCircle(
                        color = accent.copy(alpha = 0.4f),
                        radius = cx * 0.28f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = accent,
                        radius = cx * 0.1f
                    )

                    // Decoy / camo nodes floating subtly
                    val shift = 16.dp.toPx() * (pulseScale - 1f) * 4f
                    drawCircle(color = accent.copy(alpha = 0.5f), radius = 2.2.dp.toPx(), center = Offset(cx - cx * 0.45f, cy - 12.dp.toPx() + shift))
                    drawCircle(color = accent.copy(alpha = 0.7f), radius = 3.2.dp.toPx(), center = Offset(cx + cx * 0.45f, cy + 12.dp.toPx() - shift))
                    drawCircle(color = accent.copy(alpha = 0.4f), radius = 1.8.dp.toPx(), center = Offset(cx - cx * 0.3f, cy + 16.dp.toPx() + shift * 0.5f))
                }
                2 -> {
                    // Page 3: Works in Every App Secure multi-hub bridge
                    val nodeA = Offset(cx, cy - cy * 0.55f)
                    val nodeB = Offset(cx - cx * 0.55f, cy + cy * 0.35f)
                    val nodeC = Offset(cx + cx * 0.55f, cy + cy * 0.35f)

                    // Dash tunnels
                    val tunnelSpec = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                    )
                    drawLine(color = accent.copy(alpha = 0.5f), start = nodeA, end = nodeB, strokeWidth = 1.2.dp.toPx())
                    drawLine(color = accent.copy(alpha = 0.5f), start = nodeA, end = nodeC, strokeWidth = 1.2.dp.toPx())
                    drawLine(color = accent.copy(alpha = 0.5f), start = nodeB, end = nodeC, strokeWidth = 1.2.dp.toPx())

                    // App nodes (A, B, C)
                    drawCircle(color = accent, radius = 7.dp.toPx(), center = nodeA)
                    drawCircle(color = Color(0xFF030712), radius = 3.dp.toPx(), center = nodeA)

                    drawCircle(color = accent.copy(alpha = 0.85f), radius = 6.dp.toPx(), center = nodeB)
                    drawCircle(color = Color(0xFF030712), radius = 2.5.dp.toPx(), center = nodeB)

                    drawCircle(color = accent.copy(alpha = 0.85f), radius = 6.dp.toPx(), center = nodeC)
                    drawCircle(color = Color(0xFF030712), radius = 2.5.dp.toPx(), center = nodeC)

                    // Pulse packets
                    val abX = nodeA.x + (nodeB.x - nodeA.x) * dataOffset
                    val abY = nodeA.y + (nodeB.y - nodeA.y) * dataOffset
                    drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(abX, abY))

                    val bcX = nodeB.x + (nodeC.x - nodeB.x) * dataOffset
                    val bcY = nodeB.y + (nodeC.y - nodeB.y) * dataOffset
                    drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(bcX, bcY))
                }
            }
        }
    }
}

@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardPages.size })
    val coroutineScope = rememberCoroutineScope()
    val current = onboardPages[pagerState.currentPage]

    val animatedAccentColor by animateColorAsState(
        targetValue = current.accent,
        animationSpec = tween(500),
        label = "glow_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        // Top-Right Skip Button using safe status bar padding
        if (pagerState.currentPage < onboardPages.size - 1) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp)
            ) {
                Text(
                    text = "Skip",
                    color = Slate500,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // HorizontalPager spanning the screen, centering items vertically on any device
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pg = onboardPages[pageIndex]

            // Calculate current swipe offset fraction for sleek custom parallax & transform animations
            val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
            val absOffset = kotlin.math.abs(pageOffset)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        // Advanced scale & fade transitions matching swipe velocity
                        val scale = 1.0f - (absOffset * 0.15f).coerceIn(0f, 0.15f)
                        val alpha = 1.0f - (absOffset * 0.85f).coerceIn(0f, 1.0f)
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        
                        // Subtle horizontal parallax movement
                        translationX = pageOffset * size.width * 0.18f
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pulsing custom-drawn technical vector illustration
                    OnboardingIllustration(pageIndex = pageIndex, accent = pg.accent)

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

        // Bottom static layout: indicators & premium navigation controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Elegant linear navigation page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardPages.forEachIndexed { i, _ ->
                    val isActive = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) animatedAccentColor else Slate700)
                    )
                }
            }

            // High-contrast, stylish pill-shaped CTA button
            Button(
                onClick = {
                    if (pagerState.currentPage < onboardPages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedAccentColor,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < onboardPages.size - 1) "Continue" else "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN APP — bottom nav
// ─────────────────────────────────────────────────────────────────────────────

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TRANSLATE("Playground", Icons.Default.Lock),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainApp(
    themeMode: String = "dark",
    onThemeChange: (String) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(NavTab.HOME) }
    var prefillPassphrase by remember { mutableStateOf<String?>(null) }
    var shouldFocusPlaygroundInput by remember { mutableStateOf(false) }
    val view = LocalView.current
    val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
    val bottomInsetDp = with(androidx.compose.ui.platform.LocalDensity.current) {
        insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom.toDp()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(ImmersiveBg)
        .systemBarsPadding()) {
        // Content area - full-bleed scrolling behind the floating footer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (fadeIn(animationSpec = tween(220, delayMillis = 60)) + 
                     slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { direction * it / 5 })
                    .togetherWith(
                     fadeOut(animationSpec = tween(150)) + 
                     slideOutHorizontally(animationSpec = tween(150)) { -direction * it / 5 }
                    )
                }, label = "tab"
            ) { tab ->
                when (tab) {
                    NavTab.HOME     -> HomeTab(
                        onNavigateToSettings = { activeTab = NavTab.SETTINGS },
                        onNavigateToTranslate = { 
                            shouldFocusPlaygroundInput = false
                            activeTab = NavTab.TRANSLATE 
                        },
                        onNavigateToTranslateAndFocus = {
                            shouldFocusPlaygroundInput = true
                            activeTab = NavTab.TRANSLATE
                        },
                        onGenerateAndSetPassphrase = { 
                            val newPass = com.singularitysoftware.encryptboard.cipher.CipherEngine.generateStrongPassphrase()
                            prefillPassphrase = newPass
                            activeTab = NavTab.SETTINGS 
                        }
                    )
                    NavTab.TRANSLATE -> TranslateTab(
                        shouldFocusInput = shouldFocusPlaygroundInput,
                        onFocusReset = { shouldFocusPlaygroundInput = false }
                    )
                    NavTab.SETTINGS -> SettingsTab(
                        themeMode = themeMode,
                        onThemeChange = onThemeChange,
                        prefillPassphrase = prefillPassphrase,
                        onClearPrefill = { prefillPassphrase = null }
                    )
                }
            }
        }

        // Smooth black background fade with a low feather brush going behind the footer bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(125.dp + bottomInsetDp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ImmersiveBg.copy(alpha = 0.4f),
                            ImmersiveBg.copy(alpha = 0.85f),
                            ImmersiveBg
                        )
                    )
                )
        )

        // Floating capsule nav bar
        FloatingNavBar(
            activeTab = activeTab,
            onTabSelect = { activeTab = it },
            bottomInsetDp = bottomInsetDp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FloatingNavBar(
    activeTab: NavTab,
    onTabSelect: (NavTab) -> Unit,
    bottomInsetDp: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(bottom = 24.dp + bottomInsetDp)
            .width(310.dp)
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.8f)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD90C111A), // Translucent dark glass top
                        Color(0xC00C111A)  // Translucent dark glass bottom
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x66FFFFFF),       // High specular white reflection at top edge
                        Color(0x13FFFFFF),       // Clear fade in the middle
                        Color(0x228B5CF6)        // Subtle purple ambient glow at bottom edge
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isActive) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pill_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isActive) ImmersiveCyan.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navbg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isActive) ImmersiveCyan else Slate400,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navtext"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ImmersiveCyan.copy(alpha = 0.4f),
                                ImmersiveIndigo.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                modifier = Modifier.size(19.dp),
                tint = textColor
            )
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(120)) + expandHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(tween(80)) + shrinkHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Text(
                    text = tab.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SetupPermissionItem(
    title: String,
    description: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val completedGreen = Color(0xFF10B981)
    val pendingRed = Color(0xFFEF4444)
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6081218)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = if (isCompleted) completedGreen.copy(alpha = 0.25f) else pendingRed.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isCompleted) completedGreen else pendingRed, CircleShape)
                    )
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                }
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 15.sp
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) completedGreen.copy(alpha = 0.12f) else pendingRed.copy(alpha = 0.12f),
                    contentColor = if (isCompleted) completedGreen else pendingRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCompleted) completedGreen.copy(alpha = 0.4f) else pendingRed.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isCompleted) "✓ COMPLETED" else "⚠ PENDING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeTab(
    onNavigateToSettings: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    onNavigateToTranslateAndFocus: () -> Unit,
    onGenerateAndSetPassphrase: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }
    var showSecurityPolicy by remember { mutableStateOf(false) }
    val isReady = isEnabled && isSelected

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = isKeyboardEnabled(context)
                isSelected = isKeyboardSelected(context)
                hasOverlay = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Immersive Background image
        Image(
            painter = painterResource(id = R.drawable.home_bg),
            contentDescription = "Home background/wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark semi-transparent overlay gradient to guarantee accessibility and high-contrast text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            ImmersiveBg.copy(alpha = 0.88f),
                            ImmersiveBg
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                    "EncryptBoard",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ImmersiveCyan,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(ImmersiveCyan.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                        .border(1.dp, ImmersiveCyan.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        "C I P H E R K E Y",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ImmersiveCyan,
                        letterSpacing = 3.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your words. Your rules. Encrypted.",
                    fontSize = 13.sp,
                    color = Slate400
                )
            }

            // ── Keyboard Status Card ──────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE6081218)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isReady) ImmersiveCyan.copy(alpha = 0.3f) else Slate800, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Shield icon circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                if (isReady) ImmersiveCyan.copy(alpha = 0.12f) else Slate800.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .border(1.5.dp, if (isReady) ImmersiveCyan.copy(alpha = 0.5f) else Slate700, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.5f, 2.dp.toPx())
                                quadraticTo(w * 0.85f, 2.dp.toPx(), w * 0.85f, h * 0.35f)
                                quadraticTo(w * 0.85f, h * 0.75f, w * 0.5f, h * 0.95f)
                                quadraticTo(w * 0.15f, h * 0.75f, w * 0.15f, h * 0.35f)
                                quadraticTo(w * 0.15f, 2.dp.toPx(), w * 0.5f, 2.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = path,
                                color = if (isReady) ImmersiveCyan else Slate500,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            if (isReady) {
                                val checkPath = Path().apply {
                                    moveTo(w * 0.35f, h * 0.5f)
                                    lineTo(w * 0.47f, h * 0.62f)
                                    lineTo(w * 0.68f, h * 0.38f)
                                }
                                drawPath(
                                    path = checkPath,
                                    color = ImmersiveCyan,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            } else {
                                val lockSh = Path().apply {
                                    moveTo(w * 0.5f, h * 0.38f)
                                    lineTo(w * 0.5f, h * 0.62f)
                                }
                                drawPath(
                                    path = lockSh,
                                    color = Slate500,
                                    style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "KEYBOARD STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveCyan,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            if (isReady) "Keyboard Enabled" else "Keyboard Disabled",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate100
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (isReady) "Cipher Mode: Ready" else "Setup Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isReady) ImmersiveCyan else Color(0xFFFF6B6B)
                            )
                            if (isReady) Text("●", fontSize = 8.sp, color = ImmersiveGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "All keystrokes processed in secure tunnel",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                        Text(
                            "AES-128  •  Zero-Knowledge  •  Local Only",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                    Text("›", fontSize = 22.sp, color = Slate600)
                }
            }

            // ── SYSTEM CONFIGURATION STEPS ────────────────────────────────────
            Text(
                "SYSTEM CONFIGURATION STEPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 1.5.sp
            )

            SetupPermissionItem(
                title = "1. Enable Keyboard",
                description = "Activate EncryptBoard inside languages and input in your system parameters.",
                isCompleted = isEnabled,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )

            SetupPermissionItem(
                title = "2. Turn On Keyboard",
                description = "Select and set EncryptBoard as your primary active typing input method.",
                isCompleted = isSelected,
                onClick = {
                    runCatching {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                }
            )

            SetupPermissionItem(
                title = "3. Display Over Other Apps",
                description = "Grant floating overlay access to draw the live secure bubbles above chat clients.",
                isCompleted = hasOverlay,
                onClick = {
                    runCatching {
                        QuickBubbleService.requestOverlayPermission(context)
                    }
                }
            )

            // ── OPEN ENCRYPTBOARD CTA ─────────────────────────────────────────
            Button(
                onClick = onNavigateToTranslateAndFocus,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(ImmersiveCyan, ImmersiveIndigo)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val accentColorColor = Color.Black
                            drawRoundRect(
                                color = accentColorColor,
                                topLeft = Offset(0f, 2.dp.toPx()),
                                size = Size(size.width, size.height - 4.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            val y1 = size.height * 0.45f
                            val y2 = size.height * 0.7f
                            drawLine(color = accentColorColor.copy(alpha = 0.5f), start = Offset(1.5.dp.toPx(), y1), end = Offset(size.width - 1.5.dp.toPx(), y1), strokeWidth = 1.dp.toPx())
                            drawLine(color = accentColorColor.copy(alpha = 0.5f), start = Offset(1.5.dp.toPx(), y2), end = Offset(size.width - 1.5.dp.toPx(), y2), strokeWidth = 1.dp.toPx())
                            val spaceY = size.height * 0.75f
                            drawRoundRect(
                                color = accentColorColor,
                                topLeft = Offset(size.width * 0.3f, spaceY + 0.5.dp.toPx()),
                                size = Size(size.width * 0.4f, 2.dp.toPx()),
                                cornerRadius = CornerRadius(0.5.dp.toPx(), 0.5.dp.toPx())
                            )
                        }
                        Text(
                            "OPEN ENCRYPTBOARD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                        Text("›", fontSize = 18.sp, color = Color.Black)
                    }
                }
            }
            Text(
                "Secure. Private. Under Your Control.",
                fontSize = 11.sp,
                color = Slate500,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // ── TOOLS & PASSPHRASE ────────────────────────────────────────────
            Text(
                "KEY & CAMOUFLAGE CAPABILITIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 1.5.sp
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE6081218)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(20.dp))
            ) {
                Column {
                    ToolRowItem(
                        iconBg = ImmersiveCyan.copy(alpha = 0.12f),
                        iconTint = ImmersiveCyan,
                        title = "Camouflage Sandbox",
                        subtitle = "Simulate stealth text profiles & camouflage",
                        onClick = onNavigateToTranslate,
                        iconDraw = {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val w = size.width
                                val h = size.height
                                drawRoundRect(
                                    color = ImmersiveCyan,
                                    topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
                                    size = Size(w - 4.dp.toPx(), h - 8.dp.toPx()),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                drawLine(
                                    color = ImmersiveCyan,
                                    start = Offset(w * 0.4f, h - 5.dp.toPx()),
                                    end = Offset(w * 0.4f, h),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = ImmersiveCyan,
                                    start = Offset(w * 0.6f, h - 5.dp.toPx()),
                                    end = Offset(w * 0.6f, h),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = ImmersiveCyan,
                                    start = Offset(w * 0.25f, h),
                                    end = Offset(w * 0.75f, h),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawCircle(color = ImmersiveCyan, radius = 1.dp.toPx(), center = Offset(w * 0.25f, h * 0.4f))
                                drawCircle(color = ImmersiveCyan, radius = 1.dp.toPx(), center = Offset(w * 0.5f, h * 0.4f))
                                drawCircle(color = ImmersiveCyan, radius = 1.dp.toPx(), center = Offset(w * 0.75f, h * 0.4f))
                                drawCircle(color = ImmersiveCyan, radius = 1.dp.toPx(), center = Offset(w * 0.35f, h * 0.65f))
                                drawCircle(color = ImmersiveCyan, radius = 1.dp.toPx(), center = Offset(w * 0.65f, h * 0.65f))
                            }
                        }
                    )
                    Divider(color = Slate800, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    ToolRowItem(
                        iconBg = ImmersiveIndigo.copy(alpha = 0.15f),
                        iconTint = ImmersiveIndigo,
                        title = "Offline Key Architect",
                        subtitle = "Generate locally 128-bit secure keys",
                        onClick = onGenerateAndSetPassphrase,
                        iconDraw = {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val w = size.width
                                val h = size.height
                                drawCircle(
                                    color = ImmersiveIndigo,
                                    radius = 7.dp.toPx(),
                                    center = Offset(w * 0.5f, h * 0.5f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                drawCircle(
                                    color = ImmersiveIndigo.copy(alpha = 0.5f),
                                    radius = 3.dp.toPx(),
                                    center = Offset(w * 0.5f, h * 0.5f)
                                )
                                for (i in 0 until 8) {
                                    val angle = i * Math.PI / 4
                                    val dx1 = (w * 0.5f) + 7.dp.toPx() * Math.cos(angle).toFloat()
                                    val dy1 = (h * 0.5f) + 7.dp.toPx() * Math.sin(angle).toFloat()
                                    val dx2 = (w * 0.5f) + 9.5.dp.toPx() * Math.cos(angle).toFloat()
                                    val dy2 = (h * 0.5f) + 9.5.dp.toPx() * Math.sin(angle).toFloat()
                                    drawLine(
                                        color = ImmersiveIndigo,
                                        start = Offset(dx1, dy1),
                                        end = Offset(dx2, dy2),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }
                        }
                    )
                    Divider(color = Slate800, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    ToolRowItem(
                        iconBg = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                        iconTint = Color(0xFF8B5CF6),
                        title = "Zero-Knowledge Safety Policy",
                        subtitle = "Review on-device cryptographic protection",
                        onClick = { showSecurityPolicy = true },
                        iconDraw = {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(w * 0.5f, 2.dp.toPx())
                                    quadraticTo(w * 0.85f, 2.dp.toPx(), w * 0.85f, h * 0.35f)
                                    quadraticTo(w * 0.85f, h * 0.75f, w * 0.5f, h * 0.95f)
                                    quadraticTo(w * 0.15f, h * 0.75f, w * 0.15f, h * 0.35f)
                                    quadraticTo(w * 0.15f, 2.dp.toPx(), w * 0.5f, 2.dp.toPx())
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF8B5CF6),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(w * 0.5f, h * 0.5f)
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(140.dp))
        } // end Column

        if (showSecurityPolicy) {
            AlertDialog(
                onDismissRequest = { showSecurityPolicy = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.5f, 2.dp.toPx())
                                quadraticTo(w * 0.85f, 2.dp.toPx(), w * 0.85f, h * 0.35f)
                                quadraticTo(w * 0.85f, h * 0.75f, w * 0.5f, h * 0.95f)
                                quadraticTo(w * 0.15f, h * 0.75f, w * 0.15f, h * 0.35f)
                                quadraticTo(w * 0.15f, 2.dp.toPx(), w * 0.5f, 2.dp.toPx())
                                close()
                            }
                            drawPath(path = path, color = ImmersiveCyan, style = Stroke(width = 2.dp.toPx()))
                        }
                        Text("Zero-Knowledge Security", color = Slate100, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "All encryption and decryption happens fully offline, directly on your physical hardware.",
                            color = Slate300,
                            fontSize = 13.sp
                        )
                        Text(
                            "• No servers, cloud backups, or remote APIs.\n" +
                            "• All decryption is locally processed via military-grade AES-128 GCM.\n" +
                            "• The keyboard operates entirely inside a secure device sandbox without internet access.",
                            color = Slate400,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSecurityPolicy = false }) {
                        Text("ACKNOWLEDGEMENT", color = ImmersiveCyan, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFE0F1922)
            )
        }
    } // end Box
}

@Composable
fun StepMiniCard(
    number: String,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconDraw: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6081218)),
        modifier = modifier
            .border(1.dp, if (isCompleted) accentColor.copy(alpha = 0.4f) else Slate800, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                    .align(Alignment.Start),
                contentAlignment = Alignment.Center
            ) {
                Text(number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                iconDraw()
            }
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Text(
                subtitle,
                fontSize = 9.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun ToolRowItem(
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconDraw: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            iconDraw()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate100)
            Text(subtitle, fontSize = 11.sp, color = Slate500)
        }
        Text("›", fontSize = 18.sp, color = Slate600)
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
fun TranslateTab(shouldFocusInput: Boolean = false, onFocusReset: () -> Unit = {}) {
    val context = LocalContext.current
    var bubbleOn by remember { mutableStateOf(CipherPrefs.isBubbleEnabled(context)) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(shouldFocusInput) {
        if (shouldFocusInput) {
            delay(300)
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusReset()
        }
    }

    // Live Encrypter states
    var plainText by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf(CoverProfile.SYMBOLS) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Derive encrypted output dynamically so it's always live!
    val encryptedOutput = remember(plainText, selectedProfile, refreshTrigger) {
        if (plainText.isBlank()) ""
        else {
            try {
                CipherEngine.encrypt(context, plainText, useSymbols = true, coverProfile = selectedProfile)
            } catch (e: Exception) {
                ""
            }
        }
    }

    // Live Decrypter states
    var cipherInput by remember { mutableStateOf("") }

    // Derive decrypted result dynamically so it's instantly decoded
    val scanResult = remember(cipherInput) {
        if (cipherInput.isBlank()) null
        else CipherScanner.tryDecrypt(context, cipherInput)
    }

    val failureReason = remember(cipherInput, scanResult) {
        if (cipherInput.isBlank() || scanResult != null) CipherScanner.FailureReason.NONE
        else CipherScanner.diagnose(context, cipherInput)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "playground_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "neon_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 44.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // App Title & Cyber Security Badge Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Cipher Playground",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = ImmersiveCyan,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Stealth camouflage sandbox & realtime stego analysis.",
                    fontSize = 12.sp,
                    color = Slate500,
                    lineHeight = 16.sp
                )
            }

            // Real-time server status badge
            Box(
                modifier = Modifier
                    .background(ImmersiveGreen.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                    .border(1.dp, ImmersiveGreen.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { alpha = pulseAlpha }
                            .background(ImmersiveGreen, CircleShape)
                    )
                    Text(
                        "ONLINE SECURE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ImmersiveGreen,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        // SECTION 1: STEALTH ENCRYPTER (DISGUISER)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xEE0B101D), Color(0xDD070B14))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x3BFFFFFF),
                            Color(0x06FFFFFF),
                            ImmersiveCyan.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ImmersiveIndigo.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .border(1.dp, ImmersiveIndigo.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypt",
                            tint = ImmersiveIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            "Stealth Encrypter (Disguiser)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            "Inject secret bytes invisibly inside daily casual texts.",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                // Full Width Input Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "SECRET INPUT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = plainText,
                        onValueChange = { plainText = it },
                        placeholder = { Text("Type clean message...", color = Slate600, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = ImmersiveCyan,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Color(0x33000000),
                            unfocusedContainerColor = Color(0x22000000)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("playground_plain_input"),
                        minLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }

                // Choose Covert Camouflage Format
                Text(
                    "CHOOSE COVERT CAMOUFLAGE FORMAT:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.5.sp
                )

                // Horizontal list of Profiles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoverProfile.entries.forEach { profile ->
                        val isSelected = selectedProfile == profile
                        val borderColor = if (isSelected) ImmersiveCyan else Slate800
                        val bgColor = if (isSelected) ImmersiveCyan.copy(alpha = 0.15f) else Color(0x26000000)
                        val textColor = if (isSelected) ImmersiveCyan else Slate300

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            modifier = Modifier
                                .width(125.dp)
                                .clickable { selectedProfile = profile }
                                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(profile.emoji, fontSize = 22.sp)
                                Text(
                                    profile.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = when (profile) {
                                        CoverProfile.SYMBOLS -> "Stealth Glitch"
                                        CoverProfile.EMOJIS -> "Stealth Emojis"
                                        CoverProfile.CRICKET -> "Sports Feed"
                                        CoverProfile.SHOPPING -> "Grocery List"
                                        CoverProfile.NOTES -> "Project Notes"
                                        CoverProfile.MOVIE -> "Movie Review"
                                        CoverProfile.TECH -> "Tech Logs"
                                        CoverProfile.TANGLISH -> "Conversational"
                                    },
                                    fontSize = 9.sp,
                                    color = Slate500,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Interactive Multi-metrics Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x22000000), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ENTROPY STATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text(
                            if (plainText.isEmpty()) "0 bits" else "${plainText.length * 8} bits",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ImmersiveCyan
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0x1AFFFFFF)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALGORITHM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text("AES-128 GCM", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ImmersiveIndigoLight)
                    }
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0x1AFFFFFF)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CAMOUFLAGE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text(
                            when (selectedProfile) {
                                CoverProfile.SYMBOLS -> "Visual"
                                CoverProfile.EMOJIS -> "Emoji Cover"
                                else -> "Structural"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ImmersiveGreen
                        )
                    }
                }

                // Full Width Camouflaged Output Box
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "CAMOUFLAGE OUTPUT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveGreen,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF02060F), RoundedCornerShape(12.dp))
                            .border(1.dp, if (encryptedOutput.isNotEmpty()) ImmersiveGreen.copy(alpha = 0.25f) else Slate800, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        val hasOutput = encryptedOutput.isNotEmpty()
                        if (hasOutput) {
                            Text(
                                text = encryptedOutput,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ImmersiveGreen,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Pending secret input above...",
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = Slate600,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Clean action buttons placed beautifully at the bottom of Section 1 inside Stealth Encrypter
                val hasOutput = encryptedOutput.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Copy Button
                    Button(
                        onClick = {
                            if (hasOutput) {
                                CipherClipboard.write(context, "Stealth Cipher", encryptedOutput)
                                CipherClipboard.toast(context, "Disguised cipher copied to clipboard.")
                            }
                        },
                        enabled = hasOutput,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveIndigo,
                            disabledContainerColor = Slate800.copy(alpha = 0.5f),
                            disabledContentColor = Slate600
                        ),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copy",
                            tint = if (hasOutput) Color.White else Slate600,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Regenerate Button for randomized profiles
                    if (selectedProfile != CoverProfile.SYMBOLS && selectedProfile != CoverProfile.EMOJIS) {
                        Button(
                            onClick = { refreshTrigger++ },
                            enabled = hasOutput,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Slate800,
                                disabledContainerColor = Slate900,
                                disabledContentColor = Slate700
                            ),
                            modifier = Modifier.weight(1.1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scramble",
                                tint = if (hasOutput) Color.White else Slate600,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Scramble", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test Sandbox Button (Transfers ciphertext to decrypter instantly)
                    Button(
                        onClick = {
                            if (hasOutput) {
                                cipherInput = encryptedOutput
                                CipherClipboard.toast(context, "Payload sent to Decrypter Sandbox")
                            }
                        },
                        enabled = hasOutput,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = Slate800.copy(alpha = 0.3f),
                            disabledContentColor = Slate600
                        ),
                        modifier = Modifier.weight(1.3f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Sandbox",
                            tint = if (hasOutput) Color.Black else Slate600,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Test Sandbox", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SECTION 2: STEALTH DECRYPTER (EXTRACTOR)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xEE0B101D), Color(0xDD070B14))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x3BFFFFFF),
                            Color(0x06FFFFFF),
                            ImmersiveCyan.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ImmersiveCyan.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .border(1.dp, ImmersiveCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Decrypt",
                            tint = ImmersiveCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            "Stealth Decrypter (Extractor)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            "Paste dynamic cover blocks to strip steganography and extract original secret.",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                // Cipher Input field with instant helpers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ENTER CIPHER COVER BLOCK:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    TextButton(
                        onClick = {
                            val clipboardText = CipherClipboard.read(context)
                            if (clipboardText != null) {
                                cipherInput = clipboardText
                                CipherClipboard.toast(context, "Pasted content from clipboard.")
                            } else {
                                CipherClipboard.toast(context, "Clipboard is empty")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Paste",
                            tint = ImmersiveCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Paste Clipboard", fontSize = 11.sp, color = ImmersiveCyan, fontWeight = FontWeight.Bold)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = cipherInput,
                        onValueChange = { cipherInput = it },
                        placeholder = { Text("Paste disguised text/emojis/cricket/glitch logs here...", color = Slate600, fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = ImmersiveCyan,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Color(0x33000000),
                            unfocusedContainerColor = Color(0x22000000)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playground_cipher_input"),
                        minLines = 3,
                        trailingIcon = {
                            if (cipherInput.isNotEmpty()) {
                                IconButton(onClick = { cipherInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                // Real-Time Output Decryption Telemetry Console
                when {
                    scanResult != null -> {
                        // Success block with beautiful custom terminal look
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF03070E), RoundedCornerShape(12.dp))
                                .border(1.dp, ImmersiveGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            // Verified header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = ImmersiveGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "DECRYPTION TUNNEL SUCCESSFUL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ImmersiveGreen,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                val isSteg = CoverEncoder.isCoverText(cipherInput)
                                val labelText = if (isSteg) {
                                    val match = when {
                                        cipherInput.contains("Movie:") -> "Movie Cover"
                                        cipherInput.contains("INFO ") || cipherInput.contains("Latency") -> "System Logs"
                                        cipherInput.contains("Ov ") || cipherInput.contains("Target") -> "Cricket Feed"
                                        cipherInput.contains("x1") || cipherInput.contains("x2") || cipherInput.contains("x3") || cipherInput.contains("x4") -> "Groceries"
                                        cipherInput.contains("Project Ideas") || cipherInput.contains("Meeting Notes") || cipherInput.contains("Task List") -> "Task List"
                                        cipherInput.contains("bro") || cipherInput.contains("ra") || cipherInput.contains("mawa") || cipherInput.contains("anna") -> "Tenglish Cover"
                                        else -> "Steganography"
                                    }
                                    "Stg: $match"
                                } else {
                                    "Format: Unicode GLYPHS"
                                }

                                Text(
                                    labelText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                            }

                            // Dynamic live analysis logs
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x7F000000), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("ANALYSIS REPORT LOGS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate500, fontFamily = FontFamily.Monospace)
                                Text("● [INIT]: Extraction scanning core initialized...", fontSize = 9.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                                Text("● [PARSE]: Discovered Cover Payload size: ${cipherInput.length} symbols", fontSize = 9.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                                Text("● [DECRYPT]: Ciphertext extracted, parsing AES-128-GCM structure", fontSize = 9.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                                Text("● [INTEGRITY]: CRC checksum valid, key signature verified", fontSize = 9.sp, color = ImmersiveGreen, fontFamily = FontFamily.Monospace)
                            }

                            Divider(color = Slate800, thickness = 0.5.dp)

                            // Plaintext message view
                            Text(
                                "EXTRACTED ORIGINAL SECRET:",
                                fontSize = 9.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = scanResult.plaintext,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate100,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick copy decrypted text
                            Button(
                                onClick = {
                                    CipherClipboard.write(context, "Decrypted text", scanResult.plaintext)
                                    CipherClipboard.toast(context, "Decrypted message copied.")
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGreen, contentColor = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy",
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Copy Decrypted Message", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    failureReason == CipherScanner.FailureReason.WRONG_KEY -> {
                        // Handled Key Mismatch Error
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ImmersiveRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, ImmersiveRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = ImmersiveRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    "PASSPHRASE KEY MISMATCH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveRed
                                )
                                Text(
                                    "This stealth block was encrypted with a different key. Please synchronize your key passphrase in Settings.",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    failureReason == CipherScanner.FailureReason.DAMAGED -> {
                        // Handled transit damage error
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF97316).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFF97316).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    "CORRUPT TRANSIT BLOCK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF97316)
                                )
                                Text(
                                    "The unicode steganography or visual glyph sequence is fragmented. Ensure you copy the entire chat message block completely.",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    cipherInput.isNotEmpty() -> {
                        // Not an encrypted text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1F000000), RoundedCornerShape(12.dp))
                                .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No secret payloads detected. To test, enter or generate a hidden cipher block first.",
                                fontSize = 11.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        // Waiting state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1A000000), RoundedCornerShape(12.dp))
                                .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Secure Extractor Core Ready",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                                Text(
                                    "Pasting any text automatically checks, detects, and extracts hidden AES packages instantly.",
                                    fontSize = 11.sp,
                                    color = Slate600,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // COHESIVE SYSTEM CONTROLS (FLOATING BUBBLE SERVICE)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xEE0B101D), Color(0xDD070B14))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x3BFFFFFF),
                            Color(0x06FFFFFF),
                            ImmersiveCyan.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (bubbleOn) ImmersiveCyan.copy(alpha = 0.18f) else Slate800, RoundedCornerShape(10.dp))
                            .border(1.dp, if (bubbleOn) ImmersiveCyan.copy(alpha = 0.35f) else Slate700, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Floating Bubble Service",
                            tint = if (bubbleOn) ImmersiveCyan else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Floating Bubble Overlay",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            if (bubbleOn) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .graphicsLayer { alpha = pulseAlpha }
                                        .background(ImmersiveCyan, CircleShape)
                                )
                            }
                        }
                        Text(
                            "Render quick-actions overlay bubbles above any application.",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
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
                            CipherPrefs.setBubbleEnabled(context, true)
                            bubbleOn = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bubbleOn) ImmersiveIndigo else ImmersiveCyan,
                        contentColor = if (bubbleOn) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        if (bubbleOn) "Deactivate Floating Bubble" else "Activate Floating Bubble",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(140.dp))
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
fun SettingsTab(
    themeMode: String,
    onThemeChange: (String) -> Unit,
    prefillPassphrase: String? = null,
    onClearPrefill: () -> Unit = {}
) {
    val context = LocalContext.current
    var passphrase by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://ibb.co/hJvChz9W")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val regex = Regex("""https://i\.ibb\.co/[a-zA-Z0-9]+/[a-zA-Z0-9_\-\.]+\.(png|jpe?g|webp)""")
                val match = regex.find(text)
                if (match != null) {
                    logoUrl = match.value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit, prefillPassphrase) {
        if (prefillPassphrase != null) {
            passphrase = prefillPassphrase
            onClearPrefill()
        } else {
            val stored = CipherEngine.getStoredPassphrase(context)
            passphrase = if (stored == CipherEngine.DEFAULT_PASSPHRASE) "" else stored
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Identity Header
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle app logo with nice gradient styling matching design guidelines
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(ImmersiveIndigoLight, ImmersiveIndigo)
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, ImmersiveCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = logoUrl,
                                contentScale = ContentScale.Crop
                            ),
                            contentDescription = "Logo Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "EncryptBoard Logo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "EncryptBoard - CipherKey",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate100
                    )
                    Text(
                        text = "Real-time Stealth Message Safeguard",
                        fontSize = 12.sp,
                        color = ImmersiveCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Active & Guarded · v1.0.0",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }
            }
        }

        // Settings list entries
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION: App UI Theme Toggle
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ImmersiveCyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, ImmersiveCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "UI Theme Selection",
                                tint = ImmersiveCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "App UI Theme",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                "Choose dark mode vs light mode",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    // Theme selector capsule switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ImmersiveSurface, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("dark" to "Dark Mode", "light" to "Light Mode", "system" to "System Theme").forEach { (modeValue, modeLabel) ->
                            val isSelected = themeMode == modeValue
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ImmersiveIndigo else Color.Transparent)
                                    .clickable { onThemeChange(modeValue) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeLabel,
                                    color = if (isSelected) Color.White else Slate500,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Divider(color = Slate800, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                // SECTION: Group Passphrase configuration (inline inside a clean preference entry)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ImmersiveIndigo.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, ImmersiveIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Group Passphrase",
                                tint = ImmersiveIndigoLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "Group Passphrase",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                "Shared secret for group-wide cryptography",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase", color = Slate500) },
                        placeholder = { Text("Leave blank for default key", color = Slate600) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = ImmersiveCyan,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = ImmersiveSurface,
                            unfocusedContainerColor = ImmersiveSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passphrase_input")
                    )

                    Text(
                        text = if (CipherEngine.isUsingDefaultPassphrase(context))
                            "Using default shared key — same on all fresh installs."
                        else "Custom passphrase active — must match on peer devices.",
                        fontSize = 11.sp,
                        color = if (CipherEngine.isUsingDefaultPassphrase(context)) Slate600 else ImmersiveCyan
                    )

                    Button(
                        onClick = { 
                            CipherEngine.setStoredPassphrase(context, passphrase)
                            CipherClipboard.toast(context, "Group Passphrase saved.")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_save_passphrase")
                    ) {
                        Text("Save Passphrase", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Legal footer
        LegalFooter()

        Spacer(Modifier.height(140.dp))
    }
}

@Composable
fun LegalFooter() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Privacy Policy",
            fontSize = 12.sp,
            color = Slate500,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://gist.github.com/Sanju0845/eddff5c3ff4adf6cfaad562e20cbe1c7"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
        Text("  ·  ", fontSize = 12.sp, color = Slate700)
        Text(
            text = "Terms & Conditions",
            fontSize = 12.sp,
            color = Slate500,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://gist.github.com/Sanju0845/1763d661d41157fdb27441a588847d3b"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
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

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF081218)
@Composable
fun PreviewMainApp() {
    EncryptBoardTheme {
        MainApp()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF081218)
@Composable
fun PreviewHomeTab() {
    EncryptBoardTheme {
        HomeTab(
            onNavigateToSettings = {},
            onNavigateToTranslate = {},
            onNavigateToTranslateAndFocus = {},
            onGenerateAndSetPassphrase = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF081218)
@Composable
fun PreviewOnboarding() {
    EncryptBoardTheme {
        OnboardingFlow {}
    }
}
