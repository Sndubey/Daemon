package com.shubham.myhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MyHubColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF002D6C),
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = AccentBlue,
    secondary = SecondaryBlue,
    onSecondary = Color(0xFF00325B),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun MyHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MyHubColorScheme,
        typography = Typography,
        content = content
    )
}
