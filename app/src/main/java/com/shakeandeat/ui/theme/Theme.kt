package com.shakeandeat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Orange is a fill colour (navy text on it passes contrast). On light backgrounds, orange
// text and outlines use `tertiary` (the darker orange) so they stay readable.
private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Navy,
    secondary = Navy,
    onSecondary = Cream,
    tertiary = OrangeDark,
    onTertiary = Cream,
    background = Cream,
    onBackground = Navy,
    surface = CreamSurface,
    onSurface = Navy,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = Slate,
    outline = Slate,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = Navy,
    secondary = Mist,
    onSecondary = Navy,
    tertiary = Orange,
    onTertiary = Navy,
    background = Navy,
    onBackground = Sand,
    surface = NavyLight,
    onSurface = Sand,
    surfaceVariant = NavyLight,
    onSurfaceVariant = Mist,
    outline = SlateLight,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun ShakeAndEatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
