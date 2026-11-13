package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val KodeGitDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = LuminousViolet,
    onSecondary = OnLuminousViolet,
    secondaryContainer = LuminousVioletContainer,
    onSecondaryContainer = LuminousViolet,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldSuccess,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = GraphiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = GraphiteSurface,
    surfaceContainer = GraphiteElevated,
    surfaceContainerHigh = GraphiteHover,
    outline = BorderSubtle,
    outlineVariant = BorderStrong,
    error = CoralError,
    onError = ObsidianBg,
    errorContainer = CoralContainer,
    onErrorContainer = CoralError
)

private val KodeGitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun KodeGitTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = KodeGitDarkColorScheme,
        typography = KodeGitTypography,
        shapes = KodeGitShapes,
        content = content
    )
}
