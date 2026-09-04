package com.khatibstudio.noyza.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Slate40,
    onSecondary = White,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Indigo40,
    onTertiary = White,
    tertiaryContainer = Indigo90,
    onTertiaryContainer = Indigo10,
    background = NeutralBg,
    onBackground = Slate10,
    surface = NeutralSurface,
    onSurface = Slate10,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Slate40,
    outline = OutlineLight,
    error = VeryLoudRed,
    onError = White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Indigo80,
    onTertiary = Indigo20,
    tertiaryContainer = Indigo20,
    onTertiaryContainer = Indigo90,
    background = NeutralBgDark,
    onBackground = Slate90,
    surface = NeutralSurfaceDark,
    onSurface = Slate90,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Slate80,
    outline = OutlineDark,
    error = VeryLoudRedLight,
    onError = Black,
)

@Composable
fun NoyZaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — we use our curated teal brand palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent — edge-to-edge handles the rest
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoyZaTypography,
        content = content
    )
}
