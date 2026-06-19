package com.example.ui.theme

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
    primary = androidx.compose.ui.graphics.Color(0xFF38BDF8),       // Sky Blue
    secondary = androidx.compose.ui.graphics.Color(0xFF2DD4BF),     // Bright Teal
    tertiary = androidx.compose.ui.graphics.Color(0xFF818CF8),      // Indigo Accent
    background = androidx.compose.ui.graphics.Color(0xFF0F172A),    // Dark slate background
    surface = androidx.compose.ui.graphics.Color(0xFF1E293B),       // Slate surface cards
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0284C7),       // Sky Blue Primary
    secondary = androidx.compose.ui.graphics.Color(0xFF0F766E),     // Deep Teal
    tertiary = androidx.compose.ui.graphics.Color(0xFF4F46E5),      // Indigo
    background = androidx.compose.ui.graphics.Color(0xFFF8FAFC),    // Cool Off-white Slate background
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),       // Clear white elements
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F172A)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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
