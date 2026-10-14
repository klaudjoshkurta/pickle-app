package com.shkurta.pickle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private fun PickleColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = teal,
        onPrimary = onBrand,
        primaryContainer = tealSoft,
        onPrimaryContainer = ink,
        secondary = tomato,
        onSecondary = onBrand,
        secondaryContainer = tomato,
        onSecondaryContainer = onBrand,
        tertiary = mustard,
        onTertiary = onMustard,
        tertiaryContainer = mustard,
        onTertiaryContainer = onMustard,
        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = sunken,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = sunken,
        inverseSurface = ink,
        inverseOnSurface = inkInverse,
        outline = lineStrong,
        outlineVariant = lineSoft,
        error = tomatoDeep,
        onError = onBrand,
    )
}

private val PickleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PickleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPickleColors else LightPickleColors

    CompositionLocalProvider(LocalPickleColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = Typography,
            shapes = PickleShapes,
            content = content
        )
    }
}

/** Brand color roles that don't map 1:1 onto Material's scheme. */
object Pickle {
    val colors: PickleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPickleColors.current
}
