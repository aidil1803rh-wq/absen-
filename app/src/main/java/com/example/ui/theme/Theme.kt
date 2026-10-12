package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PksOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = PksOrangeDark,
    onPrimaryContainer = PksOrangeLight,
    secondary = PksAmberAccent,
    background = Color(0xFF121827),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
)

private val LightColorScheme = lightColorScheme(
    primary = PksOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = PksOrangeLight,
    onPrimaryContainer = PksOrangeDark,
    secondary = PksAmberAccent,
    onSecondary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariant,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
