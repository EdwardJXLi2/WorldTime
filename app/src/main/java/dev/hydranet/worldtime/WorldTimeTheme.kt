package dev.hydranet.worldtime

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

internal fun worldTimeColorScheme(context: Context, dark: Boolean) = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
    dark -> DarkColors
    else -> LightColors
}

@Composable
fun WorldTimeTheme(content: @Composable () -> Unit) {
    val colors = worldTimeColorScheme(LocalContext.current, isSystemInDarkTheme())
    MaterialTheme(colorScheme = colors, content = content)
}
