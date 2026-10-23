package com.thesozluk.app.settings

import android.content.Context
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A release on GitHub newer than the installed version. */
data class AppRelease(val tag: String, val name: String, val url: String)

/**
 * Checks GitHub for a newer release (the only request the app makes outside eksisozluk.com, and
 * only while "güncellemeleri kontrol et" is on). At most twice a day on its own; a release the
 * user dismissed is not offered again.
 */
object AppUpdates {
    const val REPO = "usreray/the-sozluk"
    private const val PREFS = "app_updates"
    private const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L

    private val _available = mutableStateOf<AppRelease?>(null)
    val available: State<AppRelease?> = _available

    /** Looks for a newer release; [force] skips the twice-a-day limit. Returns it, or null. */
    suspend fun check(context: Context, force: Boolean = false): AppRelease? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("lastCheck", 0L) < CHECK_EVERY_MS) return null
        val release = runCatching { latest() }.getOrNull() ?: return null
        prefs.edit().putLong("lastCheck", now).apply()
        val installed = installedVersion(context)
        if (!isNewer(release.tag, installed)) return null
        if (!force && prefs.getString("dismissed", null) == release.tag) return null
        _available.value = release
        return release
    }

    /** "sonra": this release is not offered again on its own. */
    fun dismiss(context: Context) {
        val release = _available.value ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("dismissed", release.tag).apply()
        _available.value = null
    }

    fun installedVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    private suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != 200) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").ifBlank { return@withContext null }
            AppRelease(tag, json.optString("name").ifBlank { tag }, json.optString("html_url"))
        } finally {
            connection.disconnect()
        }
    }

    /** "v3.1" against "3.0": compares the numbers part by part. */
    private fun isNewer(tag: String, installed: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").split('.', '-').map { it.toIntOrNull() ?: 0 }
        val a = parts(tag)
        val b = parts(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /**
     * A new GitHub issue, filled with the app version, Android version and device model so a
     * report starts with what is needed. Nothing personal goes in it; the user sees it before
     * sending.
     */
    fun issueUrl(context: Context): String {
        val body = """
            |**ne oldu?**
            |
            |
            |**ne bekliyordun?**
            |
            |
            |**nasıl tekrar edilir?**
            |1.
            |
            |---
            |uygulama: ${installedVersion(context)}
            |android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            |cihaz: ${Build.MANUFACTURER} ${Build.MODEL}
        """.trimMargin()
        return "https://github.com/$REPO/issues/new?body=" + URLEncoder.encode(body, "UTF-8")
    }
}
