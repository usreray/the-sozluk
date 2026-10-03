package com.thesozluk.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.thesozluk.app.model.Entry
import org.json.JSONArray
import org.json.JSONObject

/** Recent search terms, newest first, kept on this device. */
object SearchHistory {
    private const val PREFS = "search_history"
    private const val KEY = "items"
    private const val LIMIT = 20
    private lateinit var prefs: SharedPreferences

    private val _items = mutableStateOf<List<String>>(emptyList())
    val items: State<List<String>> = _items

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        _items.value = List(array.length()) { array.optString(it) }.filter(String::isNotBlank).take(LIMIT)
    }

    fun add(query: String) {
        val term = query.trim().takeIf(String::isNotEmpty) ?: return
        _items.value = (listOf(term) + _items.value.filterNot { it.equals(term, ignoreCase = true) }).take(LIMIT)
        persist()
    }

    fun remove(query: String) {
        _items.value = _items.value.filterNot { it == query }
        persist()
    }

    private fun persist() {
        prefs.edit().putString(KEY, JSONArray(_items.value).toString()).apply()
    }
}

/** A topic opened recently. */
data class HistoryItem(val title: String, val url: String, val openedAt: Long)

/** Recently opened topics, newest first, kept on the device (at most [LIMIT]). */
object ReadingHistory {
    private const val PREFS = "reading_history"
    private const val KEY = "items"
    private const val LIMIT = 100
    private lateinit var prefs: SharedPreferences

    private val _items = mutableStateOf<List<HistoryItem>>(emptyList())
    val items: State<List<HistoryItem>> = _items

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        _items.value = List(array.length()) { i ->
            val o = array.getJSONObject(i)
            HistoryItem(o.optString("title"), o.optString("url"), o.optLong("openedAt"))
        }
    }

    /** Puts a topic at the top (once per topic); does nothing when history is turned off. */
    fun record(title: String, url: String) {
        if (!AppSettings.historyEnabled.value || title.isBlank()) return
        val item = HistoryItem(title, url, System.currentTimeMillis())
        _items.value = (listOf(item) + _items.value.filterNot { it.title == title }).take(LIMIT)
        save()
    }

    fun remove(title: String) {
        _items.value = _items.value.filterNot { it.title == title }
        save()
    }

    fun clear() {
        _items.value = emptyList()
        save()
    }

    private fun save() {
        val array = JSONArray()
        _items.value.forEach { array.put(JSONObject().put("title", it.title).put("url", it.url).put("openedAt", it.openedAt)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }
}

/** A single entry kept by the user, with an optional note. */
data class SavedEntry(val entry: Entry, val note: String, val savedAt: Long)

/** Bookmarked entries ("kaydedilen entry'ler"), newest first, kept on the device. */
object EntryBookmarks {
    private const val PREFS = "entry_bookmarks"
    private const val KEY = "items"
    private lateinit var prefs: SharedPreferences

    private val _items = mutableStateOf<List<SavedEntry>>(emptyList())
    val items: State<List<SavedEntry>> = _items

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        _items.value = List(array.length()) { i ->
            val o = array.getJSONObject(i)
            SavedEntry(
                entry = Entry(
                    content = o.optString("content"),
                    contentHtml = o.optString("html"),
                    author = o.optString("author"),
                    date = o.optString("date"),
                    entryId = o.optString("id"),
                    favoriteCount = o.optInt("favorites"),
                    topicTitle = o.optString("topicTitle"),
                    topicUrl = o.optString("topicUrl"),
                    avatarUrl = o.optString("avatar").ifBlank { null }
                ),
                note = o.optString("note"),
                savedAt = o.optLong("savedAt")
            )
        }
    }

    fun isSaved(entryId: String): Boolean = _items.value.any { it.entry.entryId == entryId }

    fun note(entryId: String): String = _items.value.firstOrNull { it.entry.entryId == entryId }?.note.orEmpty()

    /** Saves (or updates the note of) an entry; [topicTitle] fills in entries read inside a topic. */
    fun save(entry: Entry, topicTitle: String, topicUrl: String, note: String = note(entry.entryId)) {
        val kept = entry.copy(
            topicTitle = entry.topicTitle.ifBlank { topicTitle },
            topicUrl = entry.topicUrl.ifBlank { topicUrl }
        )
        val existing = _items.value.firstOrNull { it.entry.entryId == entry.entryId }
        val item = SavedEntry(kept, note, existing?.savedAt ?: System.currentTimeMillis())
        _items.value = if (existing != null) _items.value.map { if (it.entry.entryId == entry.entryId) item else it }
        else listOf(item) + _items.value
        persist()
    }

    fun remove(entryId: String) {
        _items.value = _items.value.filterNot { it.entry.entryId == entryId }
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        _items.value.forEach { item ->
            val e = item.entry
            array.put(
                JSONObject()
                    .put("id", e.entryId).put("content", e.content).put("html", e.contentHtml)
                    .put("author", e.author).put("date", e.date).put("favorites", e.favoriteCount)
                    .put("topicTitle", e.topicTitle).put("topicUrl", e.topicUrl).put("avatar", e.avatarUrl.orEmpty())
                    .put("note", item.note).put("savedAt", item.savedAt)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }
}
