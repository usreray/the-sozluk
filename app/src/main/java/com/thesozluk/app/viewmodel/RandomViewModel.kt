package com.thesozluk.app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.Topic
import com.thesozluk.app.model.withVote
import com.thesozluk.app.repository.EksiRepository
import com.thesozluk.app.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Where a random entry is picked from. */
enum class RandomMode(val label: String, val description: String) {
    /** debe, or a şükela page of a gündem topic: almost always worth reading */
    Good("şükela", "debe'den ve gündemin en beğenilenlerinden"),
    /** Any entry number on the site: mostly ordinary, sometimes a find */
    Deep("derinlerden", "sözlüğün herhangi bir köşesinden"),
    /** Mostly şükela, now and then one from the depths */
    Mixed("karışık", "çoğunlukla şükela, arada bir derinlerden")
}

/** A picked entry with the topic it belongs to and where it came from. */
data class RandomPick(val entry: Entry, val topicTitle: String, val topicPath: String, val source: String)

class RandomViewModel(private val repository: EksiRepository) : ViewModel() {
    /** Every pick of this session, oldest first: the screen reads them like a chat */
    val picks = mutableStateListOf<RandomPick>()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message

    // Lists fetched once per session: debe for good picks, gündem topics for şükela pages,
    // and the newest entry number seen (the upper end for "derinlerden")
    private var debe: List<Topic>? = null
    private var gundem: List<Topic>? = null
    private var maxEntryId = 0L

    private var job: Job? = null

    fun next(mode: RandomMode) {
        job?.cancel()
        _isLoading.value = true
        _error.value = null
        job = viewModelScope.launch {
            try {
                val deep = when (mode) {
                    RandomMode.Good -> false
                    RandomMode.Deep -> true
                    RandomMode.Mixed -> Random.nextInt(5) == 0
                }
                val pick = (if (deep) pickDeep() else pickGood()) ?: throw IllegalStateException("bir şey bulunamadı")
                if (picks.none { it.entry.entryId == pick.entry.entryId }) picks.add(pick)
                if (picks.size > 50) picks.removeRange(0, picks.size - 50)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "rastgele entry getirilemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun ensureLists() {
        if (debe == null) debe = runCatching { repository.getPopularTopics(1, "debe") }.getOrDefault(emptyList())
        if (gundem == null) gundem = runCatching { repository.getPopularTopics(1, "popular") }.getOrDefault(emptyList())
        if (maxEntryId == 0L) {
            maxEntryId = debe.orEmpty().mapNotNull { entryIdOf(it.url) }.maxOrNull() ?: 0L
        }
    }

    private fun entryIdOf(url: String): Long? = Regex("/entry/(\\d+)").find(url)?.groupValues?.get(1)?.toLongOrNull()

    private fun allowed(title: String) = !AppSettings.isBlocked(title)

    /** debe (half the time) or a random entry from the şükela page of a gündem topic. */
    private suspend fun pickGood(): RandomPick? {
        ensureLists()
        val fromDebe = debe.orEmpty().filter { allowed(it.title) && entryIdOf(it.url) != null }
        if (fromDebe.isNotEmpty() && (Random.nextBoolean() || gundem.isNullOrEmpty())) {
            val item = fromDebe.random()
            return loadEntry(entryIdOf(item.url)!!, "debe")
        }
        val topics = gundem.orEmpty().filter { allowed(it.title) }
        repeat(3) {
            val topic = topics.randomOrNull() ?: return null
            val path = topic.url.substringBefore("?") + "?a=nice"
            val page = runCatching { repository.searchTopic(topic.title, 1, path) }.getOrNull() ?: return@repeat
            val entry = page.entries.randomOrNull() ?: return@repeat
            return RandomPick(entry, page.title, page.topicPath.ifBlank { topic.url.substringBefore("?") }, "gündem · şükela")
        }
        return null
    }

    /** Any entry number up to the newest one; deleted ones are skipped. */
    private suspend fun pickDeep(): RandomPick? {
        ensureLists()
        val upper = if (maxEntryId > 1000) maxEntryId else 170_000_000L
        repeat(6) {
            val id = Random.nextLong(1, upper)
            val pick = runCatching { loadEntry(id, "derinlerden") }.getOrNull()
            if (pick != null && allowed(pick.topicTitle)) return pick
        }
        throw IllegalStateException("denk gelen entry'ler silinmiş, tekrar dene")
    }

    private suspend fun loadEntry(id: Long, source: String): RandomPick? {
        val topic = repository.searchTopic("#$id", 1, "/entry/$id")
        val entry = topic.entries.firstOrNull { it.entryId == id.toString() } ?: topic.entries.firstOrNull() ?: return null
        if (topic.title.isBlank()) return null
        return RandomPick(entry, topic.title, topic.topicPath, source)
    }

    /** The page of its topic the entry is on (the site's ?focusto= answers with that page). */
    suspend fun pageOf(pick: RandomPick): Int {
        if (pick.topicPath.isBlank()) return 1
        val path = pick.topicPath.substringBefore("?")
        return runCatching { repository.searchTopic(pick.topicTitle, 1, "$path?focusto=${pick.entry.entryId}").currentPage }
            .getOrDefault(1)
            .coerceAtLeast(1)
    }

    fun toggleFavorite(entry: Entry) {
        val target = !entry.isFavorited
        update(entry.entryId) {
            it.copy(isFavorited = target, favoriteCount = (it.favoriteCount + if (target) 1 else -1).coerceAtLeast(0))
        }
        viewModelScope.launch {
            if (!repository.setFavorite(entry.entryId, target)) {
                update(entry.entryId) { it.copy(isFavorited = !target, favoriteCount = entry.favoriteCount) }
                _message.value = "favori kaydedilemedi"
            }
        }
    }

    fun vote(entry: Entry, rate: Int) {
        val previous = if (entry.isLiked) 1 else if (entry.isDisliked) -1 else 0
        if (previous == rate) return
        update(entry.entryId) { it.withVote(rate) }
        viewModelScope.launch {
            if (!repository.vote(entry.entryId, entry.authorId, rate, previous)) {
                update(entry.entryId) { it.withVote(previous) }
                _message.value = "oy kaydedilemedi"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun update(entryId: String, change: (Entry) -> Entry) {
        val index = picks.indexOfFirst { it.entry.entryId == entryId }
        if (index >= 0) picks[index] = picks[index].copy(entry = change(picks[index].entry))
    }
}
