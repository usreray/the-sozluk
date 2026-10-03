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

    /** Words that hide matching topics from every list (lowercase) */
    private val _blockedWords = mutableStateOf<Set<String>>(emptySet())
    val blockedWords: State<Set<String>> = _blockedWords

    /** Home tabs the user turned off, by name */
    private val _hiddenTabs = mutableStateOf<Set<String>>(emptySet())
    val hiddenTabs: State<Set<String>> = _hiddenTabs

    /** "#12" on each entry: its position in the topic */
    private val _showEntryNumbers = mutableStateOf(false)
    val showEntryNumbers: State<Boolean> = _showEntryNumbers

    private val _showAvatars = mutableStateOf(true)
    val showAvatars: State<Boolean> = _showAvatars

    /** Keep the screen on while reading a topic */
    private val _keepScreenOn = mutableStateOf(false)
    val keepScreenOn: State<Boolean> = _keepScreenOn

    /** Fetch the next topic page in the background before the reader gets there */
    private val _prefetchNextPage = mutableStateOf(true)
    val prefetchNextPage: State<Boolean> = _prefetchNextPage

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _blockedWords.value = prefs.getStringSet("blockedWords", emptySet()).orEmpty().toSet()
        _hiddenTabs.value = prefs.getStringSet("hiddenTabs", DEFAULT_HIDDEN_TABS).orEmpty().toSet()
        _showEntryNumbers.value = prefs.getBoolean("showEntryNumbers", false)
        _showAvatars.value = prefs.getBoolean("showAvatars", true)
        _keepScreenOn.value = prefs.getBoolean("keepScreenOn", false)
        _prefetchNextPage.value = prefs.getBoolean("prefetchNextPage", true)
        _notifications.value = prefs.getBoolean("notifications", true)
        _marqueeTitles.value = prefs.getBoolean("marqueeTitles", true)
        _themeMode.value = ThemeMode.entries.getOrElse(prefs.getInt("themeMode", 0)) { ThemeMode.System }
        _dynamicColor.value = prefs.getBoolean("dynamicColor", true)
        _pureBlack.value = prefs.getBoolean("pureBlack", false)
        _textScale.floatValue = prefs.getFloat("textScale", 1f)
        _seed.value = SeedColor.entries.getOrElse(prefs.getInt("seed", 0)) { SeedColor.Eksi }
        _palette.value = PaletteChoice.entries.getOrElse(prefs.getInt("palette", 0)) { PaletteChoice.TonalSpot }
        _contrast.value = ContrastChoice.entries.getOrElse(prefs.getInt("contrast", 0)) { ContrastChoice.Standard }
    }

    // Less common lists start hidden; they can be turned on in settings
    private val DEFAULT_HIDDEN_TABS = setOf("TakipFav", "Son", "Kenar", "Caylaklar", "Basiboslar")

    fun addBlockedWord(word: String) {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return
        _blockedWords.value = _blockedWords.value + clean
        prefs.edit().putStringSet("blockedWords", _blockedWords.value).apply()
    }

    fun removeBlockedWord(word: String) {
        _blockedWords.value = _blockedWords.value - word
        prefs.edit().putStringSet("blockedWords", _blockedWords.value).apply()
    }

    /** True if a topic title contains one of the blocked words. */
    fun isBlocked(title: String): Boolean {
        val words = _blockedWords.value
        if (words.isEmpty()) return false
        val lower = title.lowercase()
        return words.any { lower.contains(it) }
    }

    fun setTabVisible(name: String, visible: Boolean) {
        _hiddenTabs.value = if (visible) _hiddenTabs.value - name else _hiddenTabs.value + name
        prefs.edit().putStringSet("hiddenTabs", _hiddenTabs.value).apply()
    }

    fun setShowEntryNumbers(enabled: Boolean) {
        _showEntryNumbers.value = enabled
        prefs.edit().putBoolean("showEntryNumbers", enabled).apply()
    }

    fun setShowAvatars(enabled: Boolean) {
        _showAvatars.value = enabled
        prefs.edit().putBoolean("showAvatars", enabled).apply()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefs.edit().putBoolean("keepScreenOn", enabled).apply()
    }

    /** Bar titles too long for the bar scroll right to left */
    private val _marqueeTitles = mutableStateOf(true)
    val marqueeTitles: State<Boolean> = _marqueeTitles

    fun setMarqueeTitles(enabled: Boolean) {
        _marqueeTitles.value = enabled
        prefs.edit().putBoolean("marqueeTitles", enabled).apply()
    }

    /** Background check for new messages / followed-topic entries */
    private val _notifications = mutableStateOf(false)
    val notifications: State<Boolean> = _notifications

    /** Whether the app already asked for the notification permission on its own */
    fun takeNotificationPrompt(): Boolean {
        if (prefs.getBoolean("askedNotifications", false)) return false
        prefs.edit().putBoolean("askedNotifications", true).apply()
        return true
    }

    fun setNotifications(enabled: Boolean) {
        _notifications.value = enabled
        prefs.edit().putBoolean("notifications", enabled).apply()
    }

    fun setPrefetchNextPage(enabled: Boolean) {
        _prefetchNextPage.value = enabled
        prefs.edit().putBoolean("prefetchNextPage", enabled).apply()
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
