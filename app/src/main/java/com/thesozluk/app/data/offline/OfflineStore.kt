package com.thesozluk.app.data.offline

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.Topic
import com.thesozluk.app.network.EksiNetworkDataSource
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** A topic kept on the device for reading without internet. */
data class OfflineTopic(val title: String, val path: String, val pages: Int, val entries: Int, val savedAt: Long, val bytes: Long)

/** How far a download (or a backup) has come. */
data class Progress(val done: Int, val total: Int, val error: String? = null, val finished: Boolean = false)

/** Entry <-> JSON, for offline topics and backups. */
internal object EntryJson {
    fun write(e: Entry): JSONObject = JSONObject()
        .put("id", e.entryId).put("content", e.content).put("html", e.contentHtml)
        .put("author", e.author).put("authorId", e.authorId).put("date", e.date)
        .put("favorites", e.favoriteCount).put("comments", e.commentCount)
        .put("avatar", e.avatarUrl.orEmpty()).put("topicTitle", e.topicTitle).put("topicUrl", e.topicUrl)

    fun read(o: JSONObject, page: Int): Entry = Entry(
        content = o.optString("content"),
        contentHtml = o.optString("html"),
        author = o.optString("author"),
        authorId = o.optString("authorId"),
        date = o.optString("date"),
        favoriteCount = o.optInt("favorites"),
        commentCount = o.optInt("comments"),
        entryId = o.optString("id"),
        avatarUrl = o.optString("avatar").ifBlank { null },
        topicTitle = o.optString("topicTitle"),
        topicUrl = o.optString("topicUrl"),
        page = page
    )
}

/**
 * Whole topics saved for offline reading: one JSON file per topic under files/offline, holding
 * every page's entries. Downloads run page by page with a pause between requests so the site
 * isn't hammered, and keep going while the reader moves around the app.
 */
object OfflineStore {
    private lateinit var dir: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    private val _topics = mutableStateOf<List<OfflineTopic>>(emptyList())
    val topics: State<List<OfflineTopic>> = _topics

    /** Running downloads by topic path */
    val progress = mutableStateMapOf<String, Progress>()

    /** Pause between page requests */
    private const val PAGE_DELAY_MS = 900L

    fun init(context: Context) {
        dir = File(context.filesDir, "offline").apply { mkdirs() }
        reloadIndex()
    }

    /** "/slug--123" from a topic link (absolute or not, with or without query) */
    fun keyOf(url: String): String {
        val path = Uri.parse(if (url.startsWith("http")) url else "https://eksisozluk.com" + (if (url.startsWith("/")) "" else "/") + url).path.orEmpty()
        return path
    }

    fun get(url: String): OfflineTopic? = keyOf(url).let { key -> _topics.value.firstOrNull { it.path == key } }

    fun isDownloading(url: String): Boolean = progress[keyOf(url)]?.let { !it.finished } == true

    /** One page of a saved topic, as the network would have returned it; null if not saved. */
    suspend fun loadPage(url: String, page: Int): Topic? = withContext(Dispatchers.IO) {
        val file = fileFor(keyOf(url))
        if (!file.exists()) return@withContext null
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return@withContext null
        val pages = json.optJSONObject("pages") ?: return@withContext null
        val total = json.optInt("totalPages", 1)
        val wanted = page.coerceIn(1, total)
        val array = pages.optJSONArray(wanted.toString()) ?: return@withContext null
        Topic(
            title = json.optString("title"),
            url = "https://eksisozluk.com" + json.optString("path"),
            entries = List(array.length()) { EntryJson.read(array.getJSONObject(it), wanted) },
            entriesLoaded = true,
            totalPages = total,
            currentPage = wanted,
            topicPath = json.optString("path")
        )
    }

    /** Downloads every page of a topic; progress shows in [progress]. */
    fun download(title: String, url: String) {
        val key = keyOf(url)
        if (key.isBlank() || jobs[key]?.isActive == true) return
        progress[key] = Progress(0, 0)
        jobs[key] = scope.launch {
            val pages = JSONObject()
            var total = 1
            var count = 0
            try {
                var page = 1
                while (page <= total) {
                    val topic = EksiNetworkDataSource.searchTopic(title, page, key)
                    total = maxOf(topic.totalPages, 1)
                    val array = JSONArray()
                    topic.entries.forEach { array.put(EntryJson.write(it)) }
                    pages.put(page.toString(), array)
                    count += topic.entries.size
                    progress[key] = Progress(page, total)
                    page++
                    if (page <= total) delay(PAGE_DELAY_MS)
                }
                val json = JSONObject()
                    .put("title", title).put("path", key).put("totalPages", total)
                    .put("entries", count).put("savedAt", System.currentTimeMillis()).put("pages", pages)
                fileFor(key).writeText(json.toString())
                reloadIndex()
                progress[key] = Progress(total, total, finished = true)
            } catch (e: CancellationException) {
                progress.remove(key)
                throw e
            } catch (e: Exception) {
                progress[key] = Progress(progress[key]?.done ?: 0, total, error = e.message ?: "indirilemedi", finished = true)
            }
        }
    }

    fun cancel(url: String) {
        val key = keyOf(url)
        jobs.remove(key)?.cancel()
        progress.remove(key)
    }

    fun delete(url: String) {
        val key = keyOf(url)
        cancel(url)
        fileFor(key).delete()
        reloadIndex()
    }

    fun clearAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        progress.clear()
        dir.listFiles()?.forEach { it.delete() }
        reloadIndex()
    }

    fun totalBytes(): Long = _topics.value.sumOf { it.bytes }

    private fun fileFor(key: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.json")
    }

    private fun reloadIndex() {
        _topics.value = dir.listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull { file ->
            // Only the header fields; the pages stay on disk until read
            val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return@mapNotNull null
            OfflineTopic(
                title = json.optString("title"),
                path = json.optString("path"),
                pages = json.optInt("totalPages"),
                entries = json.optInt("entries"),
                savedAt = json.optLong("savedAt"),
                bytes = file.length()
            )
        }.sortedByDescending { it.savedAt }
    }
}

/**
 * Backup of an author's (the user's own) entries: every page of their latest entries, written
 * as Markdown to a file the user picked. Runs in the background like offline downloads.
 */
object EntryBackup {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _progress = mutableStateOf<Progress?>(null)
    val progress: State<Progress?> = _progress

    private const val PAGE_DELAY_MS = 700L

    val isRunning: Boolean get() = job?.isActive == true

    fun start(context: Context, nick: String, target: Uri) {
        if (isRunning) return
        val resolver = context.applicationContext.contentResolver
        _progress.value = Progress(0, 0)
        job = scope.launch {
            try {
                val entries = mutableListOf<Entry>()
                var page = 1
                while (true) {
                    val batch = EksiNetworkDataSource.fetchUserEntries(nick, "son-entryleri", page)
                    if (batch.isEmpty()) break
                    val known = entries.map { it.entryId }.toSet()
                    val fresh = batch.filterNot { it.entryId in known }
                    if (fresh.isEmpty()) break
                    entries += fresh
                    _progress.value = Progress(entries.size, 0)
                    page++
                    delay(PAGE_DELAY_MS)
                }
                val stamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
                val text = buildString {
                    appendLine("# $nick · entry yedeği")
                    appendLine()
                    appendLine("$stamp · ${entries.size} entry")
                    entries.forEach { e ->
                        appendLine()
                        appendLine("---")
                        appendLine()
                        appendLine("## ${e.topicTitle}")
                        appendLine()
                        appendLine(e.content)
                        appendLine()
                        appendLine("_${e.date} · https://eksisozluk.com/entry/${e.entryId}_")
                    }
                }
                resolver.openOutputStream(target)?.use { it.write(text.toByteArray()) }
                    ?: throw IllegalStateException("dosyaya yazılamadı")
                _progress.value = Progress(entries.size, entries.size, finished = true)
            } catch (e: CancellationException) {
                _progress.value = null
                throw e
            } catch (e: Exception) {
                _progress.value = Progress(_progress.value?.done ?: 0, 0, error = e.message ?: "yedek alınamadı", finished = true)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _progress.value = null
    }
}
