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

private val DarkColorScheme = darkColorScheme(
  primary = DarkRosePrimary,
  secondary = CoralLight,
  tertiary = WineLight,
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onPrimary = DarkTextPrimary,
  onSecondary = DarkBackground,
  onTertiary = DarkTextPrimary,
  onBackground = DarkTextPrimary,
  onSurface = DarkTextPrimary,
  onSurfaceVariant = DarkTextSecondary,
  outline = BorderLight
)

private val LightColorScheme = lightColorScheme(
  primary = RosePrimary,
  onPrimary = RoseSurface,
  primaryContainer = RoseSoft,
  onPrimaryContainer = RoseDark,
  secondary = CoralSecondary,
  onSecondary = RoseSurface,
  secondaryContainer = CoralContainer,
  onSecondaryContainer = WineTertiary,
  tertiary = WineTertiary,
  background = RoseBackground,
  surface = RoseSurface,
  surfaceVariant = RoseSoft,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  onSurfaceVariant = TextSecondary,
  outline = BorderLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep intentional romantic aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

