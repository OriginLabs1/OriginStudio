package com.oneorigin.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Origin StudioColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnAccent,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Card,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Field,
    onSurfaceVariant = Palette.TextDim,
    outline = Palette.Border,
)

@Composable
fun Origin StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Origin StudioColors,
        typography = Origin StudioTypography,
        content = content,
    )
}
