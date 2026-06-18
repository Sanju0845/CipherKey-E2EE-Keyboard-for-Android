package com.singularitysoftware.encryptboard.ui.theme

import androidx.compose.ui.graphics.Color

// ── AMOLED Dark & Light Adaptive Palette ───────────────────────────────────────
val ImmersiveBg: Color get() = if (ThemeState.isDark) Color(0xFF000000) else Color(0xFFFDFFFF)
val ImmersiveSurface: Color get() = if (ThemeState.isDark) Color(0xFF0A0D10) else Color(0xFFFFFFFF)
val ImmersiveCardBg: Color get() = if (ThemeState.isDark) Color(0xFF111418) else Color(0xFFFFFFFF)
val ImmersiveElevated: Color get() = if (ThemeState.isDark) Color(0xFF161B22) else Color(0xFFF1F5F9)
val ImmersiveGlass: Color get() = if (ThemeState.isDark) Color(0xCC0D1117) else Color(0xCCF1F5F9)
val ImmersiveGlassBorder: Color get() = if (ThemeState.isDark) Color(0x33FFFFFF) else Color(0x15000000)
val ImmersiveStripBg: Color get() = if (ThemeState.isDark) Color(0xFF080B0E) else Color(0xFFFFFFFF)
val ImmersiveClipBg: Color get() = if (ThemeState.isDark) Color(0xFF0A0E12) else Color(0xFFF1F5F9)
val ImmersivePreviewBg: Color get() = if (ThemeState.isDark) Color(0xFF0D1117) else Color(0xFFF8FAFC)

// ── Accent Colors ─────────────────────────────────────────────────────────────
val ImmersiveIndigo: Color get() = if (ThemeState.isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
val ImmersiveIndigoLight: Color get() = if (ThemeState.isDark) Color(0xFF818CF8) else Color(0xFF4338CA)
val ImmersiveIndigoGlow: Color get() = if (ThemeState.isDark) Color(0x556366F1) else Color(0x224F46E5)
val ImmersiveIndigoTransparent: Color get() = if (ThemeState.isDark) Color(0x336366F1) else Color(0x124F46E5)
val ImmersiveCyan: Color get() = if (ThemeState.isDark) Color(0xFF22D3EE) else Color(0xFF0891B2)
val ImmersiveCyanTransparent: Color get() = if (ThemeState.isDark) Color(0x3322D3EE) else Color(0x120891B2)
val ImmersiveCyanGlow: Color get() = if (ThemeState.isDark) Color(0x4422D3EE) else Color(0x220891B2)
val ImmersiveGreen: Color get() = if (ThemeState.isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
val ImmersiveRed: Color get() = if (ThemeState.isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

// ── Neutral Slates ────────────────────────────────────────────────────────────
val Slate100: Color get() = if (ThemeState.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
val Slate200: Color get() = if (ThemeState.isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
val Slate300: Color get() = if (ThemeState.isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
val Slate400: Color get() = if (ThemeState.isDark) Color(0xFF94A3B8) else Color(0xFF475569)
val Slate500: Color get() = if (ThemeState.isDark) Color(0xFF64748B) else Color(0xFF64748B)
val Slate600: Color get() = if (ThemeState.isDark) Color(0xFF475569) else Color(0xFF94A3B8)
val Slate700: Color get() = if (ThemeState.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
val Slate800: Color get() = if (ThemeState.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
val Slate900: Color get() = if (ThemeState.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

// ── Key Colors ────────────────────────────────────────────────────────────────
val KeyBg: Color get() = if (ThemeState.isDark) Color(0xFF1C2128) else Color(0xFFE2E8F0)
val KeyBgPressed: Color get() = if (ThemeState.isDark) Color(0xFF2D3748) else Color(0xFFCBD5E1)
val KeySpecialBg: Color get() = if (ThemeState.isDark) Color(0xFF141A21) else Color(0xFFCBD5E1)
val KeyPopupBg: Color get() = if (ThemeState.isDark) Color(0xFF2D3748) else Color(0xFF475569)
val KeyPopupText: Color get() = Color(0xFFF8FAFC)
