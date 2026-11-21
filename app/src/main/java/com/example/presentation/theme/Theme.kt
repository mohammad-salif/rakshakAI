package com.example.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RakshakAccessibilityMetrics(
    val elderlyMode: Boolean = false,
    val minTouchTarget: Dp = 48.dp,
    val reducedMotion: Boolean = false
)

val LocalRakshakMetrics = compositionLocalOf { RakshakAccessibilityMetrics() }

private val StandardLightScheme = lightColorScheme(
    primary = RakshakColors.DeepNavy,
    onPrimary = Color.White,
    primaryContainer = RakshakColors.DeepNavyContainer,
    onPrimaryContainer = Color.White,
    secondary = RakshakColors.TealPrimary,
    onSecondary = Color.White,
    secondaryContainer = RakshakColors.TealSoft,
    onSecondaryContainer = RakshakColors.DeepNavy,
    tertiary = RakshakColors.CautionAmber,
    background = RakshakColors.CanvasLight,
    onBackground = RakshakColors.InkPrimary,
    surface = RakshakColors.SurfaceWhite,
    onSurface = RakshakColors.InkPrimary,
    surfaceVariant = RakshakColors.SurfaceMuted,
    onSurfaceVariant = RakshakColors.InkSecondary,
    outline = RakshakColors.OutlineSoft,
    error = RakshakColors.DangerRed,
    onError = Color.White,
    errorContainer = RakshakColors.DangerSoft,
    onErrorContainer = RakshakColors.DangerRed
)

private val ElderlyHighContrastScheme = lightColorScheme(
    primary = RakshakColors.DeepNavy,
    onPrimary = Color.White,
    primaryContainer = RakshakColors.DeepNavy,
    onPrimaryContainer = Color.White,
    secondary = RakshakColors.TealPrimary,
    onSecondary = Color.White,
    secondaryContainer = RakshakColors.TealSoft,
    onSecondaryContainer = RakshakColors.ElderlyInk,
    tertiary = RakshakColors.CautionAmber,
    background = RakshakColors.ElderlyCanvas,
    onBackground = RakshakColors.ElderlyInk,
    surface = RakshakColors.ElderlySurface,
    onSurface = RakshakColors.ElderlyInk,
    surfaceVariant = RakshakColors.SurfaceMuted,
    onSurfaceVariant = RakshakColors.ElderlyInk,
    outline = RakshakColors.ElderlyBorder,
    error = RakshakColors.DangerRed,
    onError = Color.White,
    errorContainer = RakshakColors.DangerSoft,
    onErrorContainer = RakshakColors.CriticalDarkRed
)

private val RakshakShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun RakshakTheme(
    elderlyMode: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (elderlyMode) ElderlyHighContrastScheme else StandardLightScheme
    val metrics = RakshakAccessibilityMetrics(
        elderlyMode = elderlyMode,
        minTouchTarget = if (elderlyMode) 56.dp else 48.dp,
        reducedMotion = reducedMotion
    )

    CompositionLocalProvider(LocalRakshakMetrics provides metrics) {
        MaterialTheme(
            colorScheme = scheme,
            typography = rakshakTypography(elderlyMode),
            shapes = RakshakShapes,
            content = content
        )
    }
}
