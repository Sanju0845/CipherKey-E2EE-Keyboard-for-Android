package com.example.keyboard

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

/**
 * AI panel — embeds ChatGPT (or any AI URL) inside the keyboard footprint.
 *
 * The WebView is kept alive in memory and never reloaded while the keyboard service
 * is running, so mid-conversation state is preserved when switching to keyboard/clipboard
 * and back.
 *
 * Cache strategy:
 *   - LOAD_DEFAULT: uses cache when available, network when stale
 *   - DOM storage + cookies enabled so login sessions persist
 *   - JavaScript enabled
 */

private const val AI_URL = "https://chatgpt.com"

// Singleton holder so the WebView survives panel switches
object AiWebViewHolder {
    var webView: WebView? = null
    var isLoaded = false
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AiPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
    ) {
        AndroidView(
            factory = { ctx ->
                // Reuse cached WebView if available
                AiWebViewHolder.webView ?: WebView(ctx).also { wv ->
                    AiWebViewHolder.webView = wv

                    wv.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        javaScriptCanOpenWindowsAutomatically = true
                        mediaPlaybackRequiresUserGesture = false
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        // Spoof a mobile Chrome UA so ChatGPT renders the mobile UI
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    // Enable persistent cookies
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

                    if (!AiWebViewHolder.isLoaded) {
                        wv.loadUrl(AI_URL)
                    }
                }
            },
            update = { /* WebView manages its own state */ },
            modifier = Modifier.fillMaxSize()
        )
    }
}
