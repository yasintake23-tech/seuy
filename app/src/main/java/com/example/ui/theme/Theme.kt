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
  secondary = WarmGold,
  tertiary = SageGreen,
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onPrimary = WarmCreamSurface,
  onSecondary = DarkBackground,
  onTertiary = DarkTextPrimary,
  onBackground = DarkTextPrimary,
  onSurface = DarkTextPrimary,
  onSurfaceVariant = DarkTextSecondary,
  outline = BorderSoft
)

private val LightColorScheme = lightColorScheme(
  primary = SoftCoralPrimary,
  onPrimary = WarmCreamSurface,
  primaryContainer = SoftCoralContainer,
  onPrimaryContainer = SoftCoralDark,
  secondary = SageGreen,
  onSecondary = WarmCreamSurface,
  secondaryContainer = WarmCreamContainer,
  onSecondaryContainer = DeepCharcoal,
  tertiary = SlateNavy,
  background = WarmCreamBackground,
  surface = WarmCreamSurface,
  surfaceVariant = WarmCreamSurfaceVariant,
  onBackground = DeepCharcoal,
  onSurface = DeepCharcoal,
  onSurfaceVariant = SlateNavy,
  outline = BorderSoft
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep intentional soft romantic palette
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
