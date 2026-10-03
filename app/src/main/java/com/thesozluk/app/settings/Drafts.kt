package com.thesozluk.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

/** An unsent entry, kept on the device per topic. */
data class Draft(val title: String, val url: String, val text: String, val savedAt: Long)

/**
 * Entry drafts ("taslaklar"): the composer saves its text here when closed unsent and
 * restores it the next time the same topic is opened.
 */
object Drafts {
    private const val PREFS = "entry_drafts"
    private lateinit var prefs: SharedPreferences

    private val _all = mutableStateOf<List<Draft>>(emptyList())
    val all: State<List<Draft>> = _all

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        reload()
    }

    fun get(title: String): Draft? = _all.value.firstOrNull { it.title == title }

    fun save(title: String, url: String, text: String) {
        if (text.isBlank()) {
            delete(title)
            return
        }
        val json = JSONObject().put("url", url).put("text", text).put("savedAt", System.currentTimeMillis())
        prefs.edit().putString(title, json.toString()).apply()
        reload()
    }

    fun delete(title: String) {
        prefs.edit().remove(title).apply()
        reload()
    }

    private fun reload() {
        _all.value = prefs.all.mapNotNull { (title, value) ->
            val json = runCatching { JSONObject(value as String) }.getOrNull() ?: return@mapNotNull null
            Draft(title, json.optString("url"), json.optString("text"), json.optLong("savedAt"))
        }.sortedByDescending { it.savedAt }
    }
}
