package com.example.eksiscraper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.eksiscraper.settings.AppSettings
import com.example.eksiscraper.settings.PaletteChoice
import com.example.eksiscraper.settings.SeedColor
import com.example.eksiscraper.settings.ThemeMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/** Whether the app is in dark mode, following the user's theme setting. */
@Composable
fun isAppInDarkTheme(): Boolean = when (AppSettings.themeMode.value) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

fun PaletteChoice.toStyle(): PaletteStyle = when (this) {
    PaletteChoice.TonalSpot -> PaletteStyle.TonalSpot
    PaletteChoice.Vibrant -> PaletteStyle.Vibrant
    PaletteChoice.Expressive -> PaletteStyle.Expressive
    PaletteChoice.Fidelity -> PaletteStyle.Fidelity
    PaletteChoice.Content -> PaletteStyle.Content
    PaletteChoice.Rainbow -> PaletteStyle.Rainbow
    PaletteChoice.FruitSalad -> PaletteStyle.FruitSalad
    PaletteChoice.Neutral -> PaletteStyle.Neutral
    PaletteChoice.Monochrome -> PaletteStyle.Monochrome
}

/**
 * A full Material 3 scheme generated from a seed color with Google's color algorithm (the 2025
 * spec that M3 Expressive uses), in the chosen palette style and contrast.
 */
@Composable
fun rememberSeedScheme(
    seed: SeedColor,
    palette: PaletteChoice,
    darkTheme: Boolean,
    pureBlack: Boolean,
    contrastLevel: Double
): ColorScheme = rememberDynamicColorScheme(
    seedColor = Color(seed.argb),
    isDark = darkTheme,
    isAmoled = pureBlack,
    style = palette.toStyle(),
    contrastLevel = contrastLevel,
    specVersion = ColorSpec.SpecVersion.SPEC_2025
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EksiScraperTheme(content: @Composable () -> Unit) {
    val darkTheme = isAppInDarkTheme()
    val pureBlack = darkTheme && AppSettings.pureBlack.value
    // Material You colors from the wallpaper (Android 12+); otherwise the chosen palette
    val useWallpaper = AppSettings.dynamicColor.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = if (useWallpaper) {
        val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (pureBlack) scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0E0E0E),
            surfaceContainer = Color(0xFF141414),
            surfaceContainerHigh = Color(0xFF1B1B1B),
            surfaceContainerHighest = Color(0xFF232323)
        ) else scheme
    } else {
        rememberSeedScheme(
            seed = AppSettings.seed.value,
            palette = AppSettings.palette.value,
            darkTheme = darkTheme,
            pureBlack = pureBlack,
            contrastLevel = AppSettings.contrast.value.level
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        // Springy, slightly overshooting motion for every M3 component
        motionScheme = MotionScheme.expressive(),
        typography = scaledTypography(AppSettings.textScale.value),
        content = content
    )
}
