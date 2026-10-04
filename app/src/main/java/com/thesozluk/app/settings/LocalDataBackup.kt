package com.thesozluk.app.settings

import android.content.Context
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** User-owned local data only. Authentication and site status preferences are deliberately excluded. */
object LocalDataBackup {
    private const val FORMAT_VERSION = 1
    private val preferenceFiles = listOf(
        "app_settings", "search_history", "reading_history", "entry_bookmarks", "entry_drafts"
    )
    private val excludedSettings = setOf("notifications", "askedNotifications")

    suspend fun export(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val json = createJson(context)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("yedek dosyası açılamadı")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
    }

    suspend fun createJson(context: Context): String = withContext(Dispatchers.IO) {
        val preferences = JSONObject()
        preferenceFiles.forEach { name ->
            val values = JSONObject()
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all
                .filterKeys { name != "app_settings" || it !in excludedSettings }
                .filterValues { it != null }
                .forEach { (key, value) -> values.put(key, encode(requireNotNull(value))) }
            preferences.put(name, values)
        }
        val backup = JSONObject()
            .put("formatVersion", FORMAT_VERSION)
            .put("app", "the-sozluk")
            .put("createdAt", System.currentTimeMillis())
            .put("preferences", preferences)
        backup.toString(2)
    }

    suspend fun restore(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("yedek dosyası açılamadı")
        val json = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        restoreJson(context, json)
    }

    suspend fun restoreJson(context: Context, json: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        require(root.optInt("formatVersion") == FORMAT_VERSION && root.optString("app") == "the-sozluk") {
            "bu yedek dosyası desteklenmiyor"
        }
        val source = root.getJSONObject("preferences")
        val restored = preferenceFiles.associateWith { name ->
            val values = source.getJSONObject(name)
            values.keys().asSequence().associateWith { key -> decode(values.getJSONObject(key)) }
        }

        preferenceFiles.forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val protected = if (name == "app_settings") {
                prefs.all.filterKeys { it in excludedSettings }.filterValues { it != null }
                    .mapValues { requireNotNull(it.value) }
            } else emptyMap()
            val editor = prefs.edit().clear()
            (protected + restored.getValue(name)).forEach { (key, value) -> put(editor, key, requireNotNull(value)) }
            if (!editor.commit()) throw IOException("yedek verileri kaydedilemedi")
        }

        AppSettings.init(context)
        SearchHistory.init(context)
        ReadingHistory.init(context)
        EntryBookmarks.init(context)
        Drafts.init(context)
    }

    private fun encode(value: Any): JSONObject = when (value) {
        is String -> JSONObject().put("type", "string").put("value", value)
        is Boolean -> JSONObject().put("type", "boolean").put("value", value)
        is Int -> JSONObject().put("type", "int").put("value", value)
        is Long -> JSONObject().put("type", "long").put("value", value)
        is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
        is Set<*> -> JSONObject().put("type", "strings").put("value", JSONArray(value.toList()))
        else -> error("yedeklenemeyen veri türü")
    }

    private fun decode(value: JSONObject): Any = when (value.getString("type")) {
        "string" -> value.getString("value")
        "boolean" -> value.getBoolean("value")
        "int" -> value.getInt("value")
        "long" -> value.getLong("value")
        "float" -> value.getDouble("value").toFloat()
        "strings" -> value.getJSONArray("value").let { array ->
            (0 until array.length()).map { array.getString(it) }.toSet()
        }
        else -> throw IOException("yedek dosyasındaki veri türü tanınmıyor")
    }

    private fun put(editor: android.content.SharedPreferences.Editor, key: String, value: Any) {
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toMutableSet())
            else -> error("yedeklenemeyen veri türü")
        }
    }
}
