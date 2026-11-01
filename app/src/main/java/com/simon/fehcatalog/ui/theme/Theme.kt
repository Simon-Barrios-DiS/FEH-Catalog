package com.simon.fehcatalog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = FehGold,
    onPrimary = Color(0xFF372C00),
    primaryContainer = Color(0xFF544400),
    onPrimaryContainer = Color(0xFFFFE9A8),
    secondary = Color(0xFFB9C6D8),
    onSecondary = Color(0xFF233143),
    secondaryContainer = Color(0xFF3D3417),
    onSecondaryContainer = Color(0xFFFFE9A8),
    tertiary = Color(0xFFD9C48A),
    onTertiary = Color(0xFF3A2F0A),
    background = FehNavy,
    onBackground = FehOnSurface,
    surface = FehSurface,
    onSurface = FehOnSurface,
    surfaceVariant = FehSurfaceVariant,
    onSurfaceVariant = FehMuted,
    outline = Color(0xFF6B7788),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF765A12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E1A5),
    onPrimaryContainer = Color(0xFF251B00),
    secondary = Color(0xFF55606F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E1A5),
    onSecondaryContainer = Color(0xFF251B00),
    tertiary = Color(0xFF6A5A2E),
    onTertiary = Color.White,
    background = FehCream,
    onBackground = FehInk,
    surface = Color(0xFFFFFCF3),
    onSurface = FehInk,
    surfaceVariant = Color(0xFFEDE4D0),
    onSurfaceVariant = Color(0xFF4A4A44),
    outline = Color(0xFF7C7668),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun FEHCatalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FehTypography,
        content = content
    )
}
