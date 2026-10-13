package com.louaysadok.redgif.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.louaysadok.redgif.data.settings.ThemeMode

// Infinity-style palette: a saturated blue top bar and card-based feed on a light grey canvas.
private val InfinityBlue = Color(0xFF1565C0)
private val RedGifRed = Color(0xFFE53935)

private fun lightScheme() = lightColorScheme(
    primary = InfinityBlue,
    onPrimary = Color.White,
    primaryContainer = InfinityBlue,
    onPrimaryContainer = Color.White,
    secondary = RedGifRed,
    onSecondary = Color.White,
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE7EAF0),
    onSurfaceVariant = Color(0xFF5B6270),
    outline = Color(0xFFB7BCC6),
)

private fun darkScheme() = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFE6E6E6),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color(0xFF5C0000),
    background = Color(0xFF181818),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF232323),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFB8BCC4),
    outline = Color(0xFF5A5F68),
)

private fun blackScheme() = darkScheme().copy(
    background = Color.Black,
    surface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF111111),
)

@Composable
fun RedGifTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
    val scheme = when {
        mode == ThemeMode.BLACK -> blackScheme()
        dark -> darkScheme()
        else -> lightScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
