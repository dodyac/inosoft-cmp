package com.ahmaddody.newsreader.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF17223B)
private val Ocean = Color(0xFF006C74)
private val Aqua = Color(0xFF80D4D9)
private val Mist = Color(0xFFF2F7F7)
private val WarmWhite = Color(0xFFFCFDFC)
private val Coral = Color(0xFFB3261E)

// Material fills in any token left unset from its baseline palette, which is purple-tinted and
// clashes with this scheme. Every token the UI reads is therefore declared explicitly, and the
// values are mirrored in iOS `DesignSystem.swift`.
private val LightColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F5),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Ink,
    onSecondary = Color.White,
    background = WarmWhite,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF4A5453),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F7F7),
    surfaceContainer = Color(0xFFEDF2F2),
    surfaceContainerHigh = Color(0xFFE7EDED),
    surfaceContainerHighest = Color(0xFFE1E8E8),
    outline = Color(0xFFD3DEDD),
    outlineVariant = Color(0xFFE3EAEA),
    inverseSurface = Color(0xFF2B3133),
    inverseOnSurface = Color(0xFFEFF1F1),
    inversePrimary = Aqua,
    error = Coral,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Aqua,
    onPrimary = Color(0xFF00373B),
    primaryContainer = Color(0xFF005057),
    onPrimaryContainer = Color(0xFF9CF1F5),
    secondary = Color(0xFFBCC7E7),
    background = Color(0xFF101416),
    onBackground = Color(0xFFE0E3E5),
    surface = Color(0xFF101416),
    onSurface = Color(0xFFE0E3E5),
    surfaceVariant = Color(0xFF253033),
    onSurfaceVariant = Color(0xFFBFC8C7),
    surfaceContainerLowest = Color(0xFF0B0F11),
    surfaceContainerLow = Color(0xFF171C1E),
    surfaceContainer = Color(0xFF1B2124),
    surfaceContainerHigh = Color(0xFF262C2F),
    surfaceContainerHighest = Color(0xFF313739),
    outline = Color(0xFF2F3B3D),
    outlineVariant = Color(0xFF212B2D),
    inverseSurface = Color(0xFFE0E3E5),
    inverseOnSurface = Color(0xFF2B3133),
    inversePrimary = Ocean,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun NewsReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NewsTypography,
        content = content,
    )
}

