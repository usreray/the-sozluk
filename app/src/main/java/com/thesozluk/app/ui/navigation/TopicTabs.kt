package com.thesozluk.app.ui.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

data class TopicTab(val id: String, val title: String, val url: String, val page: Int, val preview: String)

object TopicTabs {
    val items = mutableStateListOf<TopicTab>()
    val requestedOverviewId = mutableStateOf<String?>(null)
    private lateinit var prefs: SharedPreferences
    private var initialized = false
    private var persistenceEnabled = true
    private var searchOriginId: String? = null
    val hasSearchOrigin: Boolean get() = searchOriginId != null

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
            val id = "${title.lowercase()}\u0000$url"
            restored.add(TopicTab(id, title, url, page, preview))
        }
        val order = if (prefs.getBoolean("tabsNewestFirst", false)) restored else restored.reversed()
        items.addAll(order)
    }

    fun setPersistenceEnabled(enabled: Boolean) {
        persistenceEnabled = enabled
        persist()
    }

    fun remember(title: String, url: String, page: Int, preview: String): TopicTab {
        val id = "${title.trim().lowercase()}\u0000$url"
        val index = items.indexOfFirst { it.id == id }
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
        if (searchOriginId == id) searchOriginId = null
        persist()
    }

    fun prepareNewTopicSearch(originId: String) {
        searchOriginId = originId
    }

    fun consumeSearchOrigin(): TopicTab? {
        val id = searchOriginId
        searchOriginId = null
        return items.firstOrNull { it.id == id }
    }

    fun requestOverview(id: String) {
        requestedOverviewId.value = id
    }

    fun consumeOverviewRequest(id: String) {
        if (requestedOverviewId.value == id) requestedOverviewId.value = null
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
                .put("title", tab.title)
                .put("url", tab.url)
                .put("page", tab.page)
                .put("preview", tab.preview))
        }
        prefs.edit().putString("tabs", saved.toString()).putBoolean("tabsNewestFirst", true).apply()
    }
}
