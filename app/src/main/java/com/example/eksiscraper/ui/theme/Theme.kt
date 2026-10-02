package com.example.eksiscraper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.eksiscraper.settings.AppSettings
import com.example.eksiscraper.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    surface = LightSurface,
    background = LightSurface
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    surface = DarkSurface,
    background = DarkSurface
)

/** Whether the app is in dark mode, following the user's theme setting. */
@Composable
fun isAppInDarkTheme(): Boolean = when (AppSettings.themeMode.value) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EksiScraperTheme(content: @Composable () -> Unit) {
    val darkTheme = isAppInDarkTheme()
    // Material You colors from the wallpaper (Android 12+); ekşi green otherwise
    val dynamicColor = AppSettings.dynamicColor.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val colorScheme = if (darkTheme && AppSettings.pureBlack.value) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0E0E0E),
            surfaceContainer = Color(0xFF141414),
            surfaceContainerHigh = Color(0xFF1B1B1B),
            surfaceContainerHighest = Color(0xFF232323)
        )
    } else baseScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        // Springy, slightly overshooting motion for every M3 component
        motionScheme = MotionScheme.expressive(),
        typography = scaledTypography(AppSettings.textScale.value),
        content = content
    )
}
