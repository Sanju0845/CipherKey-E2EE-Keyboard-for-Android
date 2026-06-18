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
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    var isKeyboardFocusActive = true // By default, intercept custom keyboard keys to type into ChatGPT

    fun injectTextToActiveElement(text: String) {
        val wv = webView ?: return
        val escaped = text.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        val js = """
            (function() {
                var el = document.activeElement;
                if (!el) return;
                if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                    var start = el.selectionStart;
                    var end = el.selectionEnd;
                    var val = el.value;
                    el.value = val.substring(0, start) + '$escaped' + val.substring(end);
                    el.selectionStart = el.selectionEnd = start + ${escaped.length};
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                } else if (el.isContentEditable) {
                    var sel = window.getSelection();
                    if (sel.rangeCount > 0) {
                        var range = sel.getRangeAt(0);
                        range.deleteContents();
                        var textNode = document.createTextNode('$escaped');
                        range.insertNode(textNode);
                        range.setStartAfter(textNode);
                        range.setEndAfter(textNode);
                        sel.removeAllRanges();
                        sel.addRange(range);
                    } else {
                        el.innerText += '$escaped';
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                }
            })();
        """.trimIndent()
        wv.post {
            wv.evaluateJavascript(js, null)
        }
    }

    fun deleteCharFromActiveElement() {
        val wv = webView ?: return
        val js = """
            (function() {
                var el = document.activeElement;
                if (!el) return;
                if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                    var start = el.selectionStart;
                    var end = el.selectionEnd;
                    var val = el.value;
                    if (start === end) {
                        if (start > 0) {
                            el.value = val.substring(0, start - 1) + val.substring(end);
                            el.selectionStart = el.selectionEnd = start - 1;
                        }
                    } else {
                        el.value = val.substring(0, start) + val.substring(end);
                        el.selectionStart = el.selectionEnd = start;
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                } else if (el.isContentEditable) {
                    var sel = window.getSelection();
                    if (sel.rangeCount > 0) {
                        var range = sel.getRangeAt(0);
                        if (range.collapsed) {
                            document.execCommand('delete', false, null);
                        } else {
                            range.deleteContents();
                        }
                    } else {
                        document.execCommand('delete', false, null);
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                }
            })();
        """.trimIndent()
        wv.post {
            wv.evaluateJavascript(js, null)
        }
    }

    fun submitActiveElement() {
        val wv = webView ?: return
        val js = """
            (function() {
                var el = document.activeElement;
                if (!el) return;
                
                // Fire Enter keyboard events
                var kdown = new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true });
                el.dispatchEvent(kdown);
                
                var kup = new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true });
                el.dispatchEvent(kup);

                // Fallback: search for chatgpt's send button
                var buttons = document.querySelectorAll('button');
                for (var i = 0; i < buttons.length; i++) {
                    var btn = buttons[i];
                    if (btn.getAttribute('data-testid') === 'send-button' || (btn.querySelector('svg') && btn.innerText === '')) {
                        btn.click();
                        break;
                    }
                }
            })();
        """.trimIndent()
        wv.post {
            wv.evaluateJavascript(js, null)
        }
    }
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
    private var isFocusable = true // Start focusable by default so they can type immediately

    fun show() {
        if (floatingView != null) return

        savedStateOwner.init()
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(context)
        floatingView = composeView

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            // FLAG_HARDWARE_ACCELERATED ensures smooth, buttery transitions and eliminates typing lag!
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = (90 * context.resources.displayMetrics.density).toInt()
        }

        fun updateFocus(enabled: Boolean) {
            val view = floatingView ?: return
            val lp = view.layoutParams as? WindowManager.LayoutParams ?: return
            isFocusable = enabled
            if (enabled) {
                lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            try {
                windowManager.updateViewLayout(view, lp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        composeView.apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore get() = vmStore
            })
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                EncryptBoardTheme {
                    AiFloatingContent(
                        onDismiss = { dismiss() },
                        isInitiallyFocusable = isFocusable,
                        onFocusToggle = { focusable ->
                            updateFocus(focusable)
                        }
                    )
                }
            }
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

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun AiFloatingContent(
    onDismiss: () -> Unit,
    isInitiallyFocusable: Boolean,
    onFocusToggle: (Boolean) -> Unit
) {
    var isKeyboardFocusActive by remember { mutableStateOf(AiWebViewHolder.isKeyboardFocusActive) }

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
            // Header with status indicator & mode selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✦", fontSize = 14.sp, color = ImmersiveCyan)
                        Text("ChatGPT", color = Slate200, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Keyboard Focus Status Badge (Toggles Window Focus so they can swap between ChatGPT & back app seamlessly)
                    val badgeColor = if (isKeyboardFocusActive) ImmersiveCyan else Slate500
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .clickable {
                                isKeyboardFocusActive = !isKeyboardFocusActive
                                AiWebViewHolder.isKeyboardFocusActive = isKeyboardFocusActive
                                onFocusToggle(isKeyboardFocusActive)
                             }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(badgeColor, CircleShape)
                        )
                        Text(
                            text = if (isKeyboardFocusActive) "FOCUS: ACTIVE (TYPE HERE)" else "FOCUS: PASS-THROUGH",
                            color = if (isKeyboardFocusActive) Slate100 else Slate400,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
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

            // WebView with forced hardware acceleration & tactile page interaction focus hooks
            AndroidView(
                factory = { ctx ->
                    AiWebViewHolder.webView ?: WebView(ctx).also { wv ->
                        AiWebViewHolder.webView = wv
                        
                        // Force hardware layer for high-speed butter-smooth rendering & absolute zero typing lag
                        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        
                        wv.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                offscreenPreRaster = true
                            }
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

                        // Automatically focus the window when the user touches anywhere inside the web view
                        wv.setOnTouchListener { _, event ->
                            if (!isKeyboardFocusActive && event.action == android.view.MotionEvent.ACTION_DOWN) {
                                isKeyboardFocusActive = true
                                AiWebViewHolder.isKeyboardFocusActive = true
                                onFocusToggle(true)
                            }
                            false
                        }

                        if (!AiWebViewHolder.isLoaded) wv.loadUrl(AI_URL)
                    }
                },
                update = {},
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
        }
    }
}
