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
    primary = Color(0xFF0F172A), // Elite Obsidian Navy for a premium luxurious feel
    secondary = Color(0xFF334155), // Slate Gray
    tertiary = Color(0xFF0369A1), // Deep Ocean Blue
    background = Color(0xFFF8FAFC), // Elegant light silk-gray background
    surface = Color(0xFFFFFFFF), // Pure polished white card surface
    error = Color(0xFFDC2626), // Crimson error
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
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
