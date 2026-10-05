package com.thesozluk.app.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesozluk.app.model.Comment
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.Topic
import com.thesozluk.app.model.updateEntry
import com.thesozluk.app.model.withFavorite
import com.thesozluk.app.model.withVote
import com.thesozluk.app.network.TopicNotFoundException
import com.thesozluk.app.repository.EksiRepository
import com.thesozluk.app.settings.AppSettings
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Which entries of the topic are shown; [query] is appended to the topic path. */
sealed class TopicFilter(val label: String, val query: String?) {
    data object All : TopicFilter("tümü", null)
    data object Popular : TopicFilter("gündem", "a=popular")
    /** "bugün" links: today's entries only */
    data class Day(val param: String) : TopicFilter("bugün", param)
    /** A filter that came with the link: olay's new entries (a=tracked), detailed search results */
    data class Linked(val params: String, val name: String) : TopicFilter(name, params)
    data object Nice : TopicFilter("şükela", "a=nice")
    data object DailyNice : TopicFilter("bugünün şükelaları", "a=dailynice")
    data object Buddies : TopicFilter("takip ettiklerim", "a=buddyrecent")
    data object Rookies : TopicFilter("çaylaklar", "a=caylaklar")
    data object Images : TopicFilter("görseller", "a=gorseller")
    /** Entries with a web link in them (the site searches the topic for "http://") */
    data object Links : TopicFilter("linkler", "a=find&keywords=" + URLEncoder.encode("http://", "UTF-8"))
    /** Entries picked for an ekşi şeyler compilation */
    data object Seyler : TopicFilter("ekşi şeyler", "a=eksiseyler")
    /** The logged-in user's own entries in the topic */
    data class Mine(val nick: String) :
        TopicFilter("benimkiler", "a=search&author=" + URLEncoder.encode(nick, "UTF-8"))
    data class Find(val keywords: String) :
        TopicFilter("\"$keywords\"", "a=find&keywords=" + URLEncoder.encode(keywords, "UTF-8"))
    data class Author(val nick: String) :
        TopicFilter("@$nick", "a=search&author=" + URLEncoder.encode(nick, "UTF-8"))
}

data class CommentsState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isOpen: Boolean = false,
    val error: String? = null,
    val hasLoaded: Boolean = false
)

/**
 * One topic read as an endless list: pages are appended while scrolling down (and prepended
 * when scrolling up after a jump), so there is no page boundary to click through.
 */
class TopicDetailViewModel(private val repository: EksiRepository) : ViewModel() {

    /** Topic metadata plus every entry loaded so far, in page order */
    private val _selectedTopic = mutableStateOf<Topic?>(null)
    val selectedTopic: State<Topic?> = _selectedTopic

    /** First load or a jump to another page: the list is replaced */
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isLoadingNext = mutableStateOf(false)
    val isLoadingNext: State<Boolean> = _isLoadingNext

    private val _isLoadingPrevious = mutableStateOf(false)
    val isLoadingPrevious: State<Boolean> = _isLoadingPrevious

    private val _firstPage = mutableStateOf(1)
    val firstPage: State<Int> = _firstPage

    private val _lastPage = mutableStateOf(1)
    val lastPage: State<Int> = _lastPage

    private val _totalPages = mutableStateOf(1)
    val totalPages: State<Int> = _totalPages

    private val _expandedEntries = mutableStateMapOf<String, Boolean>()

    private val _comments = mutableStateMapOf<String, CommentsState>()

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _isSubmitting = mutableStateOf(false)
    val isSubmitting: State<Boolean> = _isSubmitting

    private val _isSavingDraft = mutableStateOf(false)
    val isSavingDraft: State<Boolean> = _isSavingDraft

    /** One-off feedback for a snackbar ("entry silindi", errors) */
    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message

    val scrollState = LazyListState()

    private val _filter = mutableStateOf<TopicFilter>(TopicFilter.All)
    val filter: State<TopicFilter> = _filter

    /** Showing just today's entries; the title then offers the whole topic */
    val isTodayOnly: Boolean get() = _filter.value is TopicFilter.Day

    /** Entry numbers only make sense for the whole topic read in order */
    val numbersEntries: Boolean
        get() = _filter.value == TopicFilter.All && _selectedTopic.value?.url?.contains("/entry/") != true

    private var pageJob: Job? = null

    // The page after the last loaded one, fetched ahead of time
    private var prefetch: Pair<Int, Deferred<Topic>>? = null

    fun loadTopic(title: String, url: String, startPage: Int = 1) {
        if (_selectedTopic.value != null) return
        // "bugün" and list-filter links open the matching entries, like the site.
        val params = url.substringAfter("?", "").replace("&amp;", "&").split("&")
            .filter { it.isNotBlank() && !it.startsWith("p=") && !it.startsWith("focusto=") }
        val day = params.firstOrNull { it.startsWith("day=") }
        _filter.value = when {
            day != null -> TopicFilter.Day(day)
            params.any { it == "a=popular" } -> TopicFilter.Popular
            params.any { it == "a=caylaklar" } -> TopicFilter.Rookies
            params.any { it == "a=eksiseyler" } -> TopicFilter.Seyler
            params.any { it == "a=tracked" } -> TopicFilter.Linked(params.joinToString("&"), "yeni entry'ler")
            params.any { it.startsWith("searchform.", ignoreCase = true) } ->
                TopicFilter.Linked(params.joinToString("&"), "arama sonucu")
            else -> TopicFilter.All
        }
        _selectedTopic.value = Topic(title = title, url = url)
        viewModelScope.launch {
            val isSaved = repository.isTopicSaved(title)
            _selectedTopic.value = _selectedTopic.value?.copy(isSaved = isSaved)
            // Saved topics reopen where the reader left off
            jumpTo(startPage.coerceAtLeast(1))
        }
    }
    /** Replaces the list with [page] (page picker, first load, retry). */
    fun jumpTo(page: Int) {
        val topic = _selectedTopic.value ?: return
        pageJob?.cancel()
        clearPrefetch()
        _isLoading.value = true
        _isLoadingNext.value = false
        _isLoadingPrevious.value = false
        _error.value = null
        pageJob = viewModelScope.launch {
            try {
                val result = request(topic, page)
                val loadedPage = result.currentPage
                _expandedEntries.clear()
                _selectedTopic.value = merge(topic, result).copy(entries = result.entries.withPage(loadedPage))
                _firstPage.value = loadedPage
                _lastPage.value = loadedPage
                _totalPages.value = result.totalPages
                // A jumped-to page opens at its own start (its page marker)
                scrollState.scrollToItem(0)
                prefetchAfter(loadedPage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TopicNotFoundException) {
                // Not a connection problem: show the "no entries" state instead of an error
                _selectedTopic.value = topic.copy(entries = emptyList(), entriesLoaded = true)
            } catch (e: Exception) {
                _error.value = e.message ?: "başlık yüklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Appends the next page; called as the list nears its end. */
    fun loadNext() {
        val topic = _selectedTopic.value ?: return
        if (!topic.entriesLoaded || _isLoading.value || _isLoadingNext.value) return
        if (_lastPage.value >= _totalPages.value) return
        val page = _lastPage.value + 1
        _isLoadingNext.value = true
        viewModelScope.launch {
            try {
                val result = takePrefetched(page) ?: request(topic, page)
                val current = _selectedTopic.value ?: return@launch
                val known = current.entries.map { it.entryId }.toSet()
                _selectedTopic.value = merge(current, result).copy(
                    entries = current.entries + result.entries.withPage(page).filterNot { it.entryId in known }
                )
                _lastPage.value = page
                _totalPages.value = maxOf(result.totalPages, page)
                prefetchAfter(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "sonraki sayfa yüklenemedi"
            } finally {
                _isLoadingNext.value = false
            }
        }
    }

    /** Prepends the page before the first loaded one (after jumping into the middle). */
    fun loadPrevious() {
        val topic = _selectedTopic.value ?: return
        if (!topic.entriesLoaded || _isLoading.value || _isLoadingPrevious.value || _firstPage.value <= 1) return
        val page = _firstPage.value - 1
        _isLoadingPrevious.value = true
        viewModelScope.launch {
            try {
                val result = request(topic, page)
                val current = _selectedTopic.value ?: return@launch
                val known = current.entries.map { it.entryId }.toSet()
                _selectedTopic.value = current.copy(
                    entries = result.entries.withPage(page).filterNot { it.entryId in known } + current.entries
                )
                _firstPage.value = page
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "önceki sayfa yüklenemedi"
            } finally {
                _isLoadingPrevious.value = false
            }
        }
    }

    /** Opens the whole topic from its first page (from a single entry, today's entries, or the title). */
    fun showOlderEntries() = applyFilter(TopicFilter.All)

    /** Shows the topic through [filter] (şükela, search, an author...) from its first page. */
    fun applyFilter(filter: TopicFilter) {
        val topic = _selectedTopic.value ?: return
        if (topic.topicPath.isNotBlank()) _selectedTopic.value = topic.copy(url = topic.topicPath, olderEntriesCount = 0)
        _filter.value = filter
        jumpTo(1)
    }

    private fun prefetchAfter(page: Int) {
        clearPrefetch()
        val topic = _selectedTopic.value ?: return
        if (!AppSettings.prefetchNextPage.value || page >= _totalPages.value) return
        val next = page + 1
        prefetch = next to viewModelScope.async { request(topic, next) }
    }

    private suspend fun takePrefetched(page: Int): Topic? {
        val pending = prefetch?.takeIf { it.first == page }?.second ?: return null
        prefetch = null
        return try {
            pending.await()
        } catch (e: CancellationException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun clearPrefetch() {
        prefetch?.second?.cancel()
        prefetch = null
    }

    fun retry() = jumpTo(_firstPage.value)

    private suspend fun request(topic: Topic, page: Int): Topic {
        val uri = try {
            if (topic.url.isEmpty()) null
            else URI(if (topic.url.startsWith("http")) topic.url else "https://eksisozluk.com" + (if (topic.url.startsWith("/")) "" else "/") + topic.url)
        } catch (e: Exception) {
            null
        }
        val path = uri?.path
        // The filter decides the query (?day=, ?a=nice, ?a=find&keywords=...). Other params
        // of the link (an old ?p=, ?focusto=) are dropped: the data source adds the page itself.
        val filter = _filter.value.query
        return if (path.isNullOrBlank() || path == "/") {
            // No topic path (e.g. a search or an old saved search URL): search by title
            repository.searchTopic(topic.title, page)
        } else {
            try {
                repository.searchTopic(topic.title, page, if (filter != null) "$path?$filter" else path)
            } catch (e: java.io.IOException) {
                if (e is TopicNotFoundException || filter != null) throw e
                // No connection: the copy saved for offline reading, if there is one
                val offline = com.thesozluk.app.data.offline.OfflineStore.loadPage(path, page) ?: throw e
                if (!shownOfflineNotice) {
                    shownOfflineNotice = true
                    _message.value = "bağlantı yok; çevrimdışı kopya gösteriliyor"
                }
                offline.copy(isSaved = topic.isSaved)
            }
        }
    }

    private var shownOfflineNotice = false

    /** Takes the fresh page's metadata but keeps the link we were opened with and the saved flag. */
    private fun merge(current: Topic, result: Topic): Topic = result.copy(
        url = current.url.ifEmpty { result.url },
        isSaved = current.isSaved,
        // "N entry daha" leads from a single entry or today's entries to the whole topic
        olderEntriesCount = if (current.url.contains("/entry/") || isTodayOnly || _filter.value == TopicFilter.Popular) result.olderEntriesCount else 0
    )

    private fun List<Entry>.withPage(page: Int) = map { it.copy(page = page) }

    /** +1 şükela, -1 çok kötü, 0 takes the vote back; reverts if the site rejects it. */
    fun vote(entry: Entry, rate: Int) {
        val previous = if (entry.isLiked) 1 else if (entry.isDisliked) -1 else 0
        if (previous == rate) return
        _selectedTopic.value = _selectedTopic.value?.updateEntry(entry.entryId) { it.withVote(rate) }
        viewModelScope.launch {
            if (!repository.vote(entry.entryId, entry.authorId, rate, previous)) {
                _selectedTopic.value = _selectedTopic.value?.updateEntry(entry.entryId) { it.withVote(previous) }
                _message.value = "oy kaydedilemedi"
            }
        }
    }

    /** Posts a new entry through the topic's own form, then shows the last page where it lands. */
    fun submitEntry(text: String, onSuccess: () -> Unit) {
        val topic = _selectedTopic.value ?: return
        val form = topic.entryForm ?: return
        val field = form.textFieldName ?: return
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val error = repository.submitForm(form, mapOf(field to text))
                if (error != null) {
                    _message.value = error
                    return@launch
                }
                onSuccess()
                _message.value = "entry girildi"
                com.thesozluk.app.settings.Drafts.delete(topic.title)
                // The new entry is at the end of the whole topic
                _filter.value = TopicFilter.All
                val last = request(topic, maxOf(1, _totalPages.value))
                jumpTo(last.totalPages)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "entry girilemedi"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /** Uploads a picture for the entry being written; returns its link or throws with the reason. */
    suspend fun uploadImage(bytes: ByteArray, fileName: String, mimeType: String): String {
        val topic = _selectedTopic.value ?: throw java.io.IOException("başlık yüklenmedi")
        val form = topic.imageUploadForm ?: throw java.io.IOException("görsel yükleme bu hesapta açık değil")
        return repository.uploadImage(form, bytes, fileName, mimeType, topic.topicPath.ifBlank { topic.url })
    }

    /** Saves an entry to the site's "kenar" list; local draft remains if the site rejects it. */
    fun saveSiteDraft(text: String, onSuccess: () -> Unit) {
        val topic = _selectedTopic.value ?: return
        val form = topic.entryForm ?: return
        _isSubmitting.value = true
        _isSavingDraft.value = true
        viewModelScope.launch {
            try {
                val path = topic.topicPath.ifBlank { topic.url }
                val error = repository.saveSiteDraft(path, form, text)
                if (error != null) {
                    _message.value = error
                } else {
                    com.thesozluk.app.settings.Drafts.delete(topic.title)
                    _message.value = "kenara kaydedildi"
                    onSuccess()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "kenara kaydedilemedi"
            } finally {
                _isSubmitting.value = false
                _isSavingDraft.value = false
            }
        }
    }

    /** Deletes one of the user's own entries with the page's delete form. */
    fun deleteEntry(entry: Entry) {
viewModelScope.launch {
            val error = repository.deleteEntry(entry.entryId)
            if (error == null) {
                _selectedTopic.value = _selectedTopic.value?.let { topic ->
                    topic.copy(entries = topic.entries.filterNot { it.entryId == entry.entryId })
                }
                _message.value = "entry silindi"
            } else {
                _message.value = error
            }
        }
    }

    /** The "düzelt" form of an entry, with its current text. */
    suspend fun editForm(entry: Entry): com.thesozluk.app.model.FormSpec = repository.getEditForm(entry.entryId)

    /** Saves an edited entry, then reloads the pages on screen to show it. */
    fun submitEdit(form: com.thesozluk.app.model.FormSpec, text: String, onSuccess: () -> Unit) {
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val error = repository.editEntry(form, text)
                if (error != null) {
                    _message.value = error
                } else {
                    onSuccess()
                    _message.value = "entry düzeltildi"
                    jumpTo(_firstPage.value)
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // --- comments ---

    fun comments(entryId: String): CommentsState = _comments[entryId] ?: CommentsState()

    /** Automatically opens comments for entries that have them. */
    fun showComments(entry: Entry) {
        if (entry.commentCount <= 0) return
        val state = comments(entry.entryId)
        if (!state.isOpen) _comments[entry.entryId] = state.copy(isOpen = true)
        if (!state.hasLoaded && !state.isLoading) loadComments(entry.entryId)
    }

    private fun loadComments(entryId: String) {
        _comments[entryId] = comments(entryId).copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val list = repository.getComments(entryId)
                _comments[entryId] = comments(entryId).copy(comments = list, isLoading = false, hasLoaded = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _comments[entryId] = comments(entryId).copy(isLoading = false, error = e.message ?: "yorumlar yüklenemedi")
            }
        }
    }

    fun voteComment(entryId: String, comment: Comment, rate: Int) {
        val previous = if (comment.isLiked) 1 else if (comment.isDisliked) -1 else 0
        if (previous == rate) return
        fun apply(r: Int) {
            _comments[entryId] = comments(entryId).let { s ->
                s.copy(comments = s.comments.map { if (it.id == comment.id) it.withVote(r, previous) else it })
            }
        }
        apply(rate)
        viewModelScope.launch {
            if (!repository.voteComment(comment.id, comment.authorId, rate, previous)) {
                apply(previous)
                _message.value = "oy kaydedilemedi"
            }
        }
    }

    /** Whether the page offered a comment form (logged in, comments enabled). */
    val canComment: Boolean get() = _selectedTopic.value?.commentForm?.textFieldName != null

    fun submitComment(entry: Entry, text: String, onSuccess: () -> Unit) {
        val form = _selectedTopic.value?.commentForm ?: return
        val field = form.textFieldName ?: return
        val idField = form.fields.keys.firstOrNull { it.equals("id", ignoreCase = true) } ?: "Id"
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val error = repository.submitForm(form, mapOf(idField to entry.entryId, field to text))
                if (error != null) {
                    _message.value = error
                } else {
                    onSuccess()
                    _message.value = "yorum eklendi"
                    _selectedTopic.value = _selectedTopic.value?.updateEntry(entry.entryId) {
                        it.copy(commentCount = it.commentCount + 1)
                    }
                    loadComments(entry.entryId)
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    suspend fun favoriters(entryId: String, rookies: Boolean = false): List<String> = repository.getFavoriters(entryId, rookies)

    /** "Başlığı açan": who opened the topic and when. */
    suspend fun topicCreator(): com.thesozluk.app.model.TopicCreator {
        val id = _selectedTopic.value?.topicId?.ifBlank { null } ?: throw IllegalStateException("başlık bilgisi yok")
        return repository.getTopicCreator(id)
    }

    /** Follows or stops following the topic (new entries then show up in "olay"). */
    fun toggleTrack() {
        val topic = _selectedTopic.value ?: return
        val url = (if (topic.isTracked) topic.untrackUrl else topic.trackUrl) ?: return
        val target = !topic.isTracked
        _selectedTopic.value = topic.copy(isTracked = target)
        viewModelScope.launch {
            val error = repository.postAction(url)
            if (error != null) {
                _selectedTopic.value = _selectedTopic.value?.copy(isTracked = !target)
                _message.value = error
            } else {
                _message.value = if (target) "başlık takibe alındı" else "başlık takibi bırakıldı"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun isEntryExpanded(entryId: String): Boolean = _expandedEntries[entryId] ?: false

    fun toggleEntryExpansion(entryId: String) {
        _expandedEntries[entryId] = !(_expandedEntries[entryId] ?: false)
    }

    /** Optimistically flips the favorite; reverts if eksisozluk.com rejects it. */
    fun toggleEntryFavorite(entry: Entry) {
        val target = !entry.isFavorited
        _selectedTopic.value = _selectedTopic.value?.withFavorite(entry.entryId, target)
        viewModelScope.launch {
            if (!repository.setFavorite(entry.entryId, target)) {
                _selectedTopic.value = _selectedTopic.value?.withFavorite(entry.entryId, !target)
                _message.value = "favori kaydedilemedi"
            }
        }
    }

    /** Remembers the page being read for saved topics (whole topic only, not filters). */
    fun rememberPage(page: Int) {
        val topic = _selectedTopic.value ?: return
        if (!topic.isSaved || _filter.value != TopicFilter.All || topic.url.contains("/entry/")) return
        viewModelScope.launch { repository.updateLastPage(topic.title, page) }
    }

    fun saveTopic(page: Int = _firstPage.value) {
        _selectedTopic.value?.let { topic ->
            viewModelScope.launch {
                // Only a position in the whole topic is worth coming back to
                val position = if (_filter.value == TopicFilter.All && !topic.url.contains("/entry/")) page else 1
                repository.saveTopic(topic.copy(currentPage = position))
                _selectedTopic.value = _selectedTopic.value?.copy(isSaved = true)
            }
        }
    }

    fun unsaveTopic() {
        _selectedTopic.value?.let { topic ->
            viewModelScope.launch {
                repository.unsaveTopic(topic)
                _selectedTopic.value = _selectedTopic.value?.copy(isSaved = false)
            }
        }
    }
}
