package com.gamebiller.tvlock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF808080),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFF44336),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)

@Composable
fun GameBillerTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use dark theme for TV
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
