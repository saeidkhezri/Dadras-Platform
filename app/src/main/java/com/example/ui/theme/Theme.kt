package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium iOS 26 Liquid Glass Color Schemes
private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlueDark,
    secondary = SlateNavyLight,
    tertiary = EmeraldDark,
    background = GraphiteBlack,
    surface = GlassSurfaceDarkMode,
    error = CoralRedDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = AppleBlueLight,
    secondary = SlateNavyMedium,
    tertiary = SoftGreenLight,
    background = Color(0xFFF8FAFC), // Off-white/pure background for apple look
    surface = GlassSurfaceLightMode,
    error = SoftRedLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // default is true for modern luxury dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
