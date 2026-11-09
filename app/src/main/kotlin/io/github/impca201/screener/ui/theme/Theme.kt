package io.github.impca201.screener.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Snoozer's palette (sister app): the blue of the app icon, with the icon's teal as the
// tertiary accent. Material 3 tonal values from seed #3E7BDA.
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF0D5CB9),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD7E2FF),
        onPrimaryContainer = Color(0xFF004590),
        secondary = Color(0xFF565E71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDAE2F9),
        onSecondaryContainer = Color(0xFF3F4759),
        tertiary = Color(0xFF2D6861),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFB3EEE5),
        onTertiaryContainer = Color(0xFF0F4F49),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        background = Color(0xFFFAF9FD),
        onBackground = Color(0xFF1A1B1F),
        surface = Color(0xFFFAF9FD),
        onSurface = Color(0xFF1A1B1F),
        surfaceVariant = Color(0xFFE0E2EC),
        onSurfaceVariant = Color(0xFF44474E),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C6D0),
        inverseSurface = Color(0xFF2F3033),
        inverseOnSurface = Color(0xFFF2F0F4),
        inversePrimary = Color(0xFFACC7FF),
        surfaceDim = Color(0xFFDBD9DD),
        surfaceBright = Color(0xFFFAF9FD),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F3F7),
        surfaceContainer = Color(0xFFEFEDF1),
        surfaceContainerHigh = Color(0xFFE9E7EC),
        surfaceContainerHighest = Color(0xFFE3E2E6),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFACC7FF),
        onPrimary = Color(0xFF002F66),
        primaryContainer = Color(0xFF004590),
        onPrimaryContainer = Color(0xFFD7E2FF),
        secondary = Color(0xFFBEC6DC),
        onSecondary = Color(0xFF283041),
        secondaryContainer = Color(0xFF3F4759),
        onSecondaryContainer = Color(0xFFDAE2F9),
        tertiary = Color(0xFF97D2C9),
        onTertiary = Color(0xFF003732),
        tertiaryContainer = Color(0xFF0F4F49),
        onTertiaryContainer = Color(0xFFB3EEE5),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF121316),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF121316),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF44474E),
        onSurfaceVariant = Color(0xFFC4C6D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474E),
        inverseSurface = Color(0xFFE3E2E6),
        inverseOnSurface = Color(0xFF2F3033),
        inversePrimary = Color(0xFF0D5CB9),
        surfaceDim = Color(0xFF121316),
        surfaceBright = Color(0xFF38393C),
        surfaceContainerLowest = Color(0xFF0D0E11),
        surfaceContainerLow = Color(0xFF1A1B1F),
        surfaceContainer = Color(0xFF1F1F23),
        surfaceContainerHigh = Color(0xFF292A2D),
        surfaceContainerHighest = Color(0xFF343538),
    )

/**
 * Screener's theme: Snoozer's colours, following the phone's light/dark setting. Screener is
 * a popup without a settings screen, so there is no theme or dynamic color choice (DESIGN.md).
 */
@Composable
fun ScreenerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
