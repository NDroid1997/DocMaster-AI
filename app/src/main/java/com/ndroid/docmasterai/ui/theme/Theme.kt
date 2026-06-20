package com.ndroid.docmasterai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryContainer,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryContainer,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryContainer,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    error = ErrorDark,
    onError = OnErrorContainer,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = OutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = BackgroundLight,
    primaryContainer = OnPrimaryContainer,
    onPrimaryContainer = PrimaryContainer,
    secondary = SecondaryLight,
    onSecondary = BackgroundLight,
    secondaryContainer = OnSecondaryContainer,
    onSecondaryContainer = SecondaryContainer,
    tertiary = TertiaryLight,
    onTertiary = BackgroundLight,
    tertiaryContainer = OnTertiaryContainer,
    onTertiaryContainer = TertiaryContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    error = ErrorLight,
    onError = BackgroundLight,
    errorContainer = OnErrorContainer,
    onErrorContainer = ErrorContainer,
    outline = OutlineLight,
)

@Composable
fun DocMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DocMasterTypography,
        shapes = DocMasterShapes,
        content = content
    )
}
