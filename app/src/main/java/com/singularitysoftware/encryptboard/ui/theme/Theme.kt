package com.singularitysoftware.encryptboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersiveIndigo,
    secondary = Slate400,
    tertiary = ImmersiveCyan,
    background = ImmersiveBg,
    surface = ImmersiveSurface,
    onPrimary = Slate100,
    onSecondary = Slate800,
    onBackground = Slate100,
    onSurface = Slate100
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ImmersiveIndigo,
    secondary = Slate500,
    tertiary = ImmersiveCyan,
    background = ImmersiveBg,
    surface = ImmersiveSurface,
    onPrimary = Slate100,
    onSecondary = Slate800,
    onBackground = Slate100,
    onSurface = Slate100
  )

@Composable
fun EncryptBoardTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to preserve the hand-picked Immersive UI palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
