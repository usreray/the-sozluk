package com.example.eksiscraper.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

enum class ThemeMode(val label: String) { System("sistem"), Light("açık"), Dark("koyu") }

/** Ready-made seed colors; each one generates a whole Material 3 palette. */
enum class SeedColor(val label: String, val argb: Long) {
    Eksi("ekşi", 0xFF81C14B),
    Ocean("okyanus", 0xFF1E88E5),
    Lavender("lavanta", 0xFF7E57C2),
    Rose("gül", 0xFFE91E63),
    Coral("mercan", 0xFFFF7043),
    Amber("kehribar", 0xFFFFB300),
    Mint("nane", 0xFF26A69A),
    Night("gece", 0xFF3949AB),
    Earth("toprak", 0xFF8D6E63),
    Cherry("kiraz", 0xFFD32F2F),
    Lime("limon", 0xFFC0CA33),
    Graphite("grafit", 0xFF607D8B)
}

/** How a palette is derived from its seed (material-kolor's PaletteStyle). */
enum class PaletteChoice(val label: String) {
    TonalSpot("tonal"),
    Vibrant("canlı"),
    Expressive("expressive"),
    Fidelity("sadık"),
    Content("içerik"),
    Rainbow("gökkuşağı"),
    FruitSalad("meyve salatası"),
    Neutral("nötr"),
    Monochrome("monokrom")
}

enum class ContrastChoice(val label: String, val level: Double) {
    Standard("standart", 0.0), Medium("orta", 0.5), High("yüksek", 1.0)
}

/** Appearance preferences, kept in SharedPreferences and exposed as Compose state. */
object AppSettings {
    private const val PREFS = "app_settings"
    private lateinit var prefs: SharedPreferences

    private val _themeMode = mutableStateOf(ThemeMode.System)
    val themeMode: State<ThemeMode> = _themeMode

    private val _dynamicColor = mutableStateOf(true)
    val dynamicColor: State<Boolean> = _dynamicColor

    /** Pure black surfaces in dark mode (saves battery on OLED screens) */
    private val _pureBlack = mutableStateOf(false)
    val pureBlack: State<Boolean> = _pureBlack

    private val _seed = mutableStateOf(SeedColor.Eksi)
    val seed: State<SeedColor> = _seed

    private val _palette = mutableStateOf(PaletteChoice.TonalSpot)
    val palette: State<PaletteChoice> = _palette

    private val _contrast = mutableStateOf(ContrastChoice.Standard)
    val contrast: State<ContrastChoice> = _contrast

    /** Multiplier for entry text size */
    private val _textScale = mutableFloatStateOf(1f)
    val textScale: State<Float> = _textScale

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _themeMode.value = ThemeMode.entries.getOrElse(prefs.getInt("themeMode", 0)) { ThemeMode.System }
        _dynamicColor.value = prefs.getBoolean("dynamicColor", true)
        _pureBlack.value = prefs.getBoolean("pureBlack", false)
        _textScale.floatValue = prefs.getFloat("textScale", 1f)
        _seed.value = SeedColor.entries.getOrElse(prefs.getInt("seed", 0)) { SeedColor.Eksi }
        _palette.value = PaletteChoice.entries.getOrElse(prefs.getInt("palette", 0)) { PaletteChoice.TonalSpot }
        _contrast.value = ContrastChoice.entries.getOrElse(prefs.getInt("contrast", 0)) { ContrastChoice.Standard }
    }

    fun setSeed(seed: SeedColor) {
        _seed.value = seed
        prefs.edit().putInt("seed", seed.ordinal).apply()
    }

    fun setPalette(palette: PaletteChoice) {
        _palette.value = palette
        prefs.edit().putInt("palette", palette.ordinal).apply()
    }

    fun setContrast(contrast: ContrastChoice) {
        _contrast.value = contrast
        prefs.edit().putInt("contrast", contrast.ordinal).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putInt("themeMode", mode.ordinal).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("dynamicColor", enabled).apply()
    }

    fun setPureBlack(enabled: Boolean) {
        _pureBlack.value = enabled
        prefs.edit().putBoolean("pureBlack", enabled).apply()
    }

    fun setTextScale(scale: Float) {
        _textScale.floatValue = scale
        prefs.edit().putFloat("textScale", scale).apply()
    }
}
