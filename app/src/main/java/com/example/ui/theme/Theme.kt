package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    secondary = AccentGreen,
    tertiary = AccentOrange,
    background = NavyDarkBg,
    surface = NavySurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = TextPrimary,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Mode
    dynamicColor: Boolean = false, // Force custom premium theme colors
    content: @Composable () -> Unit,
) {
    // We always use DarkColorScheme as requested by the user
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

