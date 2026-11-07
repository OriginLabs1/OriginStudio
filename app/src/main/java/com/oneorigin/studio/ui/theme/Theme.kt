package com.oneorigin.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OriginStudioColors = darkColorScheme(
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
fun OriginStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OriginStudioColors,
        typography = OriginStudioTypography,
        content = content,
    )
}
