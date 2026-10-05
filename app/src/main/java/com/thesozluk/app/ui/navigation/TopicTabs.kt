package com.thesozluk.app.ui.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class TopicTab(val id: String, val title: String, val url: String, val page: Int, val preview: String)

object TopicTabs {
    val items = mutableStateListOf<TopicTab>()
    private lateinit var prefs: SharedPreferences
    private var initialized = false
    private var persistenceEnabled = true

    fun init(context: Context, enabled: Boolean) {
        if (initialized) return
        initialized = true
        persistenceEnabled = enabled
        prefs = context.applicationContext.getSharedPreferences("open_topic_tabs", Context.MODE_PRIVATE)
        if (!enabled) {
            prefs.edit().clear().apply()
            return
        }

        val saved = runCatching { JSONArray(prefs.getString("tabs", "[]")) }.getOrNull() ?: return
        val restored = mutableListOf<TopicTab>()
        for (index in 0 until saved.length()) {
            val json = saved.optJSONObject(index) ?: continue
            val title = json.optString("title").trim()
            if (title.isEmpty()) continue
            val url = json.optString("url")
            val page = json.optInt("page", 1).coerceAtLeast(1)
            val preview = json.optString("preview")
            val id = json.optString("id").ifBlank { tabId(title, url) }
            restored.add(TopicTab(id, title, url, page, preview))
        }
        val order = if (prefs.getBoolean("tabsNewestFirst", false)) restored else restored.reversed()
        items.addAll(order)
    }

    fun setPersistenceEnabled(enabled: Boolean) {
        persistenceEnabled = enabled
        persist()
    }

    /** Built from the route the topic was opened with, so it stays the same once the site's title loads. */
    fun tabId(routeTitle: String, url: String) = "${routeTitle.trim().lowercase()}\u0000$url"

    fun remember(id: String, title: String, url: String, page: Int, preview: String): TopicTab {
        // The same topic opened another way (e.g. from an /entry/ link) takes over its older tab
        val duplicate = items.indexOfFirst { it.id != id && it.title.trim().equals(title.trim(), ignoreCase = true) }
        var index = items.indexOfFirst { it.id == id }
        if (duplicate >= 0) {
            if (index < 0) index = duplicate else items.removeAt(duplicate).also { if (duplicate < index) index-- }
        }
        val savedPreview = preview.ifBlank { items.getOrNull(index)?.preview.orEmpty() }
        val tab = TopicTab(id, title, url, page.coerceAtLeast(1), savedPreview)
        if (index < 0) items.add(0, tab) else items[index] = tab
        persist()
        return tab
    }

    fun close(id: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        items.removeAt(index)
        persist()
    }

    fun updatePreview(id: String, preview: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0 || preview.isBlank()) return
        items[index] = items[index].copy(preview = preview)
        persist()
    }

    private fun persist() {
        if (!::prefs.isInitialized) return
        if (!persistenceEnabled) {
            prefs.edit().remove("tabs").apply()
            return
        }
        val saved = JSONArray()
        items.forEach { tab ->
            saved.put(JSONObject()
                .put("id", tab.id)
                .put("title", tab.title)
                .put("url", tab.url)
                .put("page", tab.page)
                .put("preview", tab.preview))
        }
        prefs.edit().putString("tabs", saved.toString()).putBoolean("tabsNewestFirst", true).apply()
    }
}
