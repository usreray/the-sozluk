package com.example.eksiscraper.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

enum class ThemeMode(val label: String) { System("sistem"), Light("açık"), Dark("koyu") }

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

    /** Multiplier for entry text size */
    private val _textScale = mutableFloatStateOf(1f)
    val textScale: State<Float> = _textScale

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _themeMode.value = ThemeMode.entries.getOrElse(prefs.getInt("themeMode", 0)) { ThemeMode.System }
        _dynamicColor.value = prefs.getBoolean("dynamicColor", true)
        _pureBlack.value = prefs.getBoolean("pureBlack", false)
        _textScale.floatValue = prefs.getFloat("textScale", 1f)
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
