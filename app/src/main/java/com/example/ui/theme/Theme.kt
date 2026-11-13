package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val YounkTunerColorScheme = darkColorScheme(
    primary = HondaRed,
    onPrimary = TextPrimary,
    primaryContainer = HondaRedContainer,
    onPrimaryContainer = HondaRedBright,
    secondary = TelemetryCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = TelemetryCyanContainer,
    onSecondaryContainer = TelemetryCyan,
    tertiary = EmeraldOk,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldOk,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonElevated,
    onSurfaceVariant = TextSecondary,
    outline = CarbonBorder,
    error = HondaRedBright,
    onError = ObsidianBg
)

val YounkTunerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun YounkTunerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YounkTunerColorScheme,
        typography = Typography,
        shapes = YounkTunerShapes,
        content = content
    )
}
