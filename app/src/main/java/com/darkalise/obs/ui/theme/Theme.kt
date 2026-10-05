package com.darkalise.obs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkAliseNeon,
    onPrimary = DarkAliseBlack,
    primaryContainer = DarkAlisePurpleDark,
    onPrimaryContainer = DarkAliseNeonGlow,
    secondary = DarkAliseBlue,
    onSecondary = DarkAliseText,
    background = DarkAliseBlack,
    onBackground = DarkAliseText,
    surface = DarkAliseSurface,
    onSurface = DarkAliseText,
    surfaceVariant = DarkAliseSurfaceVariant,
    onSurfaceVariant = DarkAliseTextMuted,
    outline = DarkAliseBorder,
    error = DarkAliseRed,
    onError = DarkAliseBlack
)

@Composable
fun DarkAliseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
