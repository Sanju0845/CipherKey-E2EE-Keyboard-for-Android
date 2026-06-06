package com.singularitysoftware.encryptboard.quick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.singularitysoftware.encryptboard.cipher.CipherClipboard
import com.singularitysoftware.encryptboard.cipher.CipherEngine
import com.singularitysoftware.encryptboard.cipher.CipherPrefs
import com.singularitysoftware.encryptboard.cipher.CipherScanner

/**
 * Small floating ENC / DEC buttons — works over WhatsApp without opening the keyboard.
 */
class QuickBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleRoot: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        showBubble()
    }

    override fun onDestroy() {
        removeBubble()
        CipherPrefs.setBubbleEnabled(this, false)
        super.onDestroy()
    }

    private fun showBubble() {
        if (bubbleRoot != null) return

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val enc = bubbleButton("ENC", 0xFF4F46E5.toInt()) {
            runQuickEncrypt()
        }
        val dec = bubbleButton("DEC", 0xFF06B6D4.toInt()) {
            runQuickDecrypt()
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(0xE6161920.toInt())
            addView(enc, LinearLayout.LayoutParams(dp(52), dp(40)).apply { marginEnd = dp(4) })
            addView(dec, LinearLayout.LayoutParams(dp(52), dp(40)))
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(8)
            y = dp(120)
        }

        var touchX = 0f
        var touchY = 0f
        var startX = 0
        var startY = 0

        row.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = event.rawX
                    touchY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - touchX).toInt()
                    lp.y = startY + (event.rawY - touchY).toInt()
                    windowManager?.updateViewLayout(row, lp)
                    true
                }
                else -> false
            }
        }

        row.setOnLongClickListener {
            stopSelf()
            true
        }

        windowManager?.addView(row, layoutParams)
        bubbleRoot = row
    }

    private fun bubbleButton(label: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setBackgroundColor(color)
            setOnClickListener { onClick() }
        }
    }

    private fun runQuickEncrypt() {
        val text = CipherClipboard.read(this)
        if (text.isNullOrBlank()) {
            CipherClipboard.toast(this, "Copy plain text first")
            return
        }
        if (CipherDetectorSafe.looksLikeCipher(text)) {
            CipherClipboard.toast(this, "Already encrypted")
            return
        }
        val useSymbols = CipherPrefs.getUseSymbols(this)
        val encrypted = CipherEngine.encrypt(this, text, useSymbols)
        CipherClipboard.write(this, "EncryptBoard", encrypted)
        CipherClipboard.toast(this, "Encrypted — paste in chat")
    }

    private fun runQuickDecrypt() {
        val text = CipherClipboard.read(this)
        if (text.isNullOrBlank()) {
            CipherClipboard.toast(this, "Copy cipher message first")
            return
        }
        val result = CipherScanner.tryDecrypt(this, text)
        if (result != null) {
            CipherClipboard.write(this, "EncryptBoard plain", result.plaintext)
            CipherClipboard.toast(this, "Decrypted — paste reply")
        } else {
            val msg = when (CipherScanner.diagnose(this, text)) {
                CipherScanner.FailureReason.DAMAGED -> "Message damaged — copy full block"
                CipherScanner.FailureReason.WRONG_KEY -> "Wrong passphrase"
                else -> "Not cipher text"
            }
            CipherClipboard.toast(this, msg)
        }
    }

    private fun removeBubble() {
        bubbleRoot?.let { windowManager?.removeView(it) }
        bubbleRoot = null
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EncryptBoard quick bubble",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, QuickTranslateActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("EncryptBoard bubble active")
            .setContentText("ENC/DEC use clipboard · long-press bubble to close")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "cipher_quick_bubble"
        private const val NOTIFICATION_ID = 4401

        fun start(context: Context) {
            val intent = Intent(context, QuickBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickBubbleService::class.java))
        }

        fun requestOverlayPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CipherClipboard.toast(context, "Allow overlay, then enable bubble again")
        }
    }
}

/** Avoid importing CipherDetector in service file cycle — thin wrapper */
private object CipherDetectorSafe {
    fun looksLikeCipher(text: String): Boolean =
        com.singularitysoftware.encryptboard.cipher.CipherDetector.looksLikeCipher(text)
}
