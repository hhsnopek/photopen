package dev.hhsnopek.photopen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Photos and videos are judged against black, so the viewer never follows the
// system light theme. Chrome is muted so it disappears next to content.
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8AB4F8),
        onPrimary = Color(0xFF0B1B33),
        secondary = Color(0xFF9AA0A6),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE8EAED),
        surface = Color(0xFF101418),
        onSurface = Color(0xFFE8EAED),
        surfaceVariant = Color(0xFF1F2429),
        onSurfaceVariant = Color(0xFFBDC1C6),
    )

@Composable
fun PhotoPenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
