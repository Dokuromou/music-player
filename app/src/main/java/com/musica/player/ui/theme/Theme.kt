package com.musica.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF7C4DFF)
val PurpleDark = Color(0xFF4527A0)
val BackgroundDark = Color(0xFF10131A)
val SurfaceDark = Color(0xFF1A1E28)
val SurfaceLight = Color(0xFFF6F4FA)
val Accent = Color(0xFFB388FF)

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Accent,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF232838),
    onPrimary = Color.White,
    onBackground = Color(0xFFECEAF4),
    onSurface = Color(0xFFECEAF4),
    onSurfaceVariant = Color(0xFF9A97AC)
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleDark,
    secondary = Purple,
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFEAE6F4),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1B22),
    onSurface = Color(0xFF1A1B22),
    onSurfaceVariant = Color(0xFF5B5870)
)

@Composable
fun MusicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
