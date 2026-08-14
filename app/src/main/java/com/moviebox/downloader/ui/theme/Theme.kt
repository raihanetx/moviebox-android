package com.moviebox.downloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFFE4E4E7),
    error = Color(0xFFEF4444),
    onError = Color.White,
    outline = Color(0xFF27272A),
)

@Composable
fun MovieBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackColorScheme,
        content = content
    )
}
