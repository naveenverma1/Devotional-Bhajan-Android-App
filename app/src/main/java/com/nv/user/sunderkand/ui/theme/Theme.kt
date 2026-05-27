package com.nv.user.sunderkand.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette — fixed, not dynamic.
//
// Saffron / maroon over a paper-cream sepia surface for the default
// light/sepia mode. The dark mode keeps the warmth (no cool blue-black,
// which would feel wrong for devotional content).

internal val Saffron = Color(0xFFE87722)
internal val DeepMaroon = Color(0xFF7A1F1F)
internal val Gold = Color(0xFFC9A227)

internal val SepiaSurface = Color(0xFFFBF3E2)
internal val SepiaInk = Color(0xFF2B1B11)
internal val SepiaInkMuted = Color(0xFF6B4A33)

internal val DarkSurface = Color(0xFF1A140F)
internal val DarkInk = Color(0xFFF3DDB0)
internal val DarkInkMuted = Color(0xFFAB8E5C)

private val SepiaLightScheme = lightColorScheme(
    primary = Saffron,
    onPrimary = Color.White,
    primaryContainer = Saffron.copy(alpha = 0.15f),
    onPrimaryContainer = DeepMaroon,
    secondary = DeepMaroon,
    onSecondary = Color.White,
    secondaryContainer = DeepMaroon.copy(alpha = 0.15f),
    onSecondaryContainer = DeepMaroon,
    tertiary = Gold,
    onTertiary = SepiaInk,
    background = SepiaSurface,
    onBackground = SepiaInk,
    surface = SepiaSurface,
    onSurface = SepiaInk,
    surfaceVariant = Color(0xFFF1E5C9),
    onSurfaceVariant = SepiaInkMuted,
    outline = SepiaInkMuted,
)

private val WarmDarkScheme = darkColorScheme(
    primary = Saffron,
    onPrimary = Color.Black,
    primaryContainer = Saffron.copy(alpha = 0.25f),
    onPrimaryContainer = DarkInk,
    secondary = Gold,
    onSecondary = Color.Black,
    secondaryContainer = DeepMaroon,
    onSecondaryContainer = DarkInk,
    tertiary = Gold,
    onTertiary = Color.Black,
    background = DarkSurface,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF2A1F17),
    onSurfaceVariant = DarkInkMuted,
    outline = DarkInkMuted,
)

@Composable
fun SunderkandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) WarmDarkScheme else SepiaLightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = SunderkandTypography,
        content = content,
    )
}
