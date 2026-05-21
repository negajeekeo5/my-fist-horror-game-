package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HorrorColorScheme = darkColorScheme(
    primary = HorrorGlowRed,
    onPrimary = Color.White,
    secondary = HorrorRed,
    onSecondary = HorrorWhite,
    tertiary = HorrorGold,
    background = HorrorCoal,
    onBackground = HorrorWhite,
    surface = HorrorAsh,
    onSurface = HorrorWhite,
    surfaceVariant = HorrorAsh,
    onSurfaceVariant = HorrorMist,
    error = HorrorGlowRed,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for atmospheric horror
    dynamicColor: Boolean = false, // Disable dynamic light schemes to preserve curated blood-crimson vibe
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HorrorColorScheme,
        typography = Typography,
        content = content
    )
}
