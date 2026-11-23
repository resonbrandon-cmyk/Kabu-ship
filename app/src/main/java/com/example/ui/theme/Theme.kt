package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = PureWhite,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = TealSecondary,
    onSecondary = PureWhite,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = CoralTertiary,
    onTertiary = PureWhite,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = OnCoralContainer,
    background = AlabasterCanvas,
    onBackground = DeepSlateInk,
    surface = PureWhite,
    onSurface = DeepSlateInk,
    surfaceVariant = WarmPaper,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = AlabasterCanvas,
    surfaceContainer = WarmPaper,
    surfaceContainerHigh = SoftStone,
    outline = OutlineStone,
    outlineVariant = SoftStone,
    error = ErrorCrimson,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = CobaltLight,
    onPrimary = NightNavy,
    primaryContainer = CobaltDarkContainer,
    onPrimaryContainer = SoftIceInk,
    secondary = TealLight,
    onSecondary = NightNavy,
    secondaryContainer = TealDarkContainer,
    onSecondaryContainer = SoftIceInk,
    tertiary = CoralLight,
    onTertiary = NightNavy,
    tertiaryContainer = CoralDarkContainer,
    onTertiaryContainer = SoftIceInk,
    background = NightNavy,
    onBackground = SoftIceInk,
    surface = DarkSurface,
    onSurface = SoftIceInk,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = MutedIce,
    surfaceContainerLowest = NightNavy,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkOutline,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceElevated,
    error = CoralLight,
    onError = NightNavy
)

val SocialShipShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SocialShipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SocialShipTypography,
        shapes = SocialShipShapes,
        content = content
    )
}
