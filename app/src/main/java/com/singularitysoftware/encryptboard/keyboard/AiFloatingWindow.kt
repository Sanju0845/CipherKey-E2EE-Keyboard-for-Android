package com.singularitysoftware.encryptboard.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.compose.ui.platform.ComposeView
import com.singularitysoftware.encryptboard.ui.theme.EncryptBoardTheme
import com.singularitysoftware.encryptboard.ui.theme.*

private const val AI_URL = "https://chatgpt.com"

object AiWebViewHolder {
    var webView: WebView? = null
    var isLoaded = false
}

// Standalone owners that don't reference themselves
private class FloatingLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}

private class FloatingSavedStateOwner(
    private val lo: FloatingLifecycleOwner
) : SavedStateRegistryOwner {
    private val ctrl = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lo.lifecycle
    override val savedStateRegistry: SavedStateRegistry get() = ctrl.savedStateRegistry
    fun init() = ctrl.performRestore(null)
}

class AiFloatingWindow(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onDismiss: () -> Unit
) {
    private var floatingView: View? = null
    private val lifecycleOwner = FloatingLifecycleOwner()
    private val savedStateOwner = FloatingSavedStateOwner(lifecycleOwner)
    private val vmStore = ViewModelStore()

    fun show() {
        if (floatingView != null) return

        savedStateOwner.init()
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore get() = vmStore
            })
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                EncryptBoardTheme {
                    AiFloatingContent(onDismiss = { dismiss() })
                }
            }
        }

        floatingView = composeView

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            // NOT_FOCUSABLE = window can't steal focus from keyboard
            // NOT_TOUCH_MODAL = touches outside pass through
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            floatingView = null
        }
    }

    fun dismiss() {
        floatingView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            floatingView = null
        }
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        onDismiss()
    }

    fun isShowing() = floatingView != null

    fun destroy() {
        dismiss()
        AiWebViewHolder.webView?.destroy()
        AiWebViewHolder.webView = null
        AiWebViewHolder.isLoaded = false
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        vmStore.clear()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AiFloatingContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1117))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("✦", fontSize = 14.sp, color = ImmersiveCyan)
                    Text("ChatGPT", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF252B35))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↓", color = Slate400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // WebView — 280dp
            AndroidView(
                factory = { ctx ->
                    AiWebViewHolder.webView ?: WebView(ctx).also { wv ->
                        AiWebViewHolder.webView = wv
                        wv.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK  // use cache when available
                            javaScriptCanOpenWindowsAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            textZoom = 85
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/124.0.0.0 Mobile Safari/537.36"
                        }
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(wv, true)
                        }
                        wv.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                AiWebViewHolder.isLoaded = true
                                CookieManager.getInstance().flush()
                            }
                        }
                        wv.webChromeClient = WebChromeClient()
                        if (!AiWebViewHolder.isLoaded) wv.loadUrl(AI_URL)
                    }
                },
                update = {},
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
        }
    }
}
