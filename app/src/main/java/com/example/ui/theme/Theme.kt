package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.data.ScrollSearchStateBus

private val DarkColorScheme = darkColorScheme(
    primary = TikTokRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF38101C),
    onPrimaryContainer = Color.White,
    secondary = TikTokCyan,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TikTokCyan,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkSurfaceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = TikTokRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE6EB),
    onPrimaryContainer = TikTokRed,
    secondary = TikTokCyanDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE2F9FB),
    onSecondaryContainer = Color(0xFF005863),
    tertiary = Color(0xFFFF8599),
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightSurfaceBorder
)

@Composable
fun ScrollSearchTheme(
    darkTheme: Boolean = ScrollSearchStateBus.isDarkTheme.collectAsState().value,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
