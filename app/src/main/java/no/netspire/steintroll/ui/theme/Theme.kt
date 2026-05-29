package no.netspire.steintroll.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    background = Color(0xFF0F1115),
    surface = Color(0xFF1C1F26),
    onSurface = Color(0xFFE5E7EB),
)
private val LightColors = lightColorScheme(primary = Color(0xFF2563EB))

@Composable
fun SteintrollTheme(useDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (useDark) DarkColors else LightColors, content = content)
}
