package dev.hydranet.worldtime

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6650A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
)

internal fun worldTimeColorScheme(context: Context, dark: Boolean): ColorScheme {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    return if (dark) colors else colors.copy(
        surfaceContainerLow = colors.surfaceContainer,
        surfaceContainer = colors.surfaceContainerHigh,
        surfaceContainerHigh = colors.surfaceContainerHighest,
    )
}

internal fun ColorScheme.timelineBandColors(hour: Int): Pair<Color, Color> {
    if (surface.luminance() > 0.5f) {
        return when (hour) {
            in 6..8 -> secondaryContainer to onSecondaryContainer
            in 9..17 -> surfaceBright to onSurface
            in 18..21 -> primaryContainer to onPrimaryContainer
            else -> primary to onPrimary
        }
    }
    return when (hour) {
        in 6..8 -> secondaryContainer.copy(alpha = 0.62f) to onSecondaryContainer
        in 9..17 -> surfaceVariant.copy(alpha = 0.28f) to onSurface
        in 18..21 -> primaryContainer.copy(alpha = 0.58f) to onPrimaryContainer
        else -> primaryContainer.copy(alpha = 0.92f) to onPrimaryContainer
    }
}

internal fun ColorScheme.timelineDetailColor(foreground: Color): Color =
    if (surface.luminance() > 0.5f) foreground else foreground.copy(alpha = 0.78f)

@Composable
fun WorldTimeTheme(content: @Composable () -> Unit) {
    val colors = worldTimeColorScheme(LocalContext.current, isSystemInDarkTheme())
    MaterialTheme(colorScheme = colors, content = content)
}
