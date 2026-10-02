package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Comment
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.model.updateEntry
import com.example.eksiscraper.model.withFavorite
import com.example.eksiscraper.model.withVote
import com.example.eksiscraper.network.TopicNotFoundException
import com.example.eksiscraper.repository.EksiRepository
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class CommentsState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isOpen: Boolean = false,
    val error: String? = null
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

    /** One-off feedback for a snackbar ("entry silindi", errors) */
    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message

    val scrollState = LazyListState()

    // False: only today's entries (?a=popular / ?day=); true: the whole topic
    private var showAllEntries = false
    private var pageJob: Job? = null

    fun loadTopic(title: String, url: String) {
        if (_selectedTopic.value != null) return
        _selectedTopic.value = Topic(title = title, url = url)
        viewModelScope.launch {
            val isSaved = repository.isTopicSaved(title)
            _selectedTopic.value = _selectedTopic.value?.copy(isSaved = isSaved)
            jumpTo(1)
        }
    }

    /** Replaces the list with [page] (page picker, first load, retry). */
    fun jumpTo(page: Int) {
        val topic = _selectedTopic.value ?: return
        pageJob?.cancel()
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
                scrollState.scrollToItem(0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TopicNotFoundException) {
                // Not a connection problem: show the "no entries" state instead of an error
                _selectedTopic.value = topic.copy(entries = emptyList(), entriesLoaded = true)
            } catch (e: Exception) {
                _error.value = e.message ?: "Başlık yüklenemedi"
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
                val result = request(topic, page)
                val current = _selectedTopic.value ?: return@launch
                val known = current.entries.map { it.entryId }.toSet()
                _selectedTopic.value = merge(current, result).copy(
                    entries = current.entries + result.entries.withPage(page).filterNot { it.entryId in known }
                )
                _lastPage.value = page
                _totalPages.value = maxOf(result.totalPages, page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Sonraki sayfa yüklenemedi"
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
                _message.value = "Önceki sayfa yüklenemedi"
            } finally {
                _isLoadingPrevious.value = false
            }
        }
    }

    /** Leaves today's entries for the full topic, starting from its first entry. */
    fun showOlderEntries() {
        showAllEntries = true
        jumpTo(1)
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
        // Gündem (?a=popular) and bugün (?day=YYYY-MM-DD) links show only today's
        // entries, like the site and other clients; "N entry daha" switches to the full
        // topic. Other params (an old ?p=, ?focusto=) are dropped: the data source adds
        // the page itself.
        val filter = if (showAllEntries) null
        else uri?.query?.split("&")?.firstOrNull { it == "a=popular" || it.startsWith("day=") }
        return if (path.isNullOrBlank() || path == "/") {
            // No topic path (e.g. a search or an old saved search URL): search by title
            repository.searchTopic(topic.title, page)
        } else {
            repository.searchTopic(topic.title, page, if (filter != null) "$path?$filter" else path)
        }
    }

    /** Takes the fresh page's metadata but keeps the link we were opened with and the saved flag. */
    private fun merge(current: Topic, result: Topic): Topic = result.copy(
        url = current.url.ifEmpty { result.url },
        isSaved = current.isSaved,
        // The "N entry daha" count only belongs to the first page of today's entries
        olderEntriesCount = if (showAllEntries) 0 else maxOf(current.olderEntriesCount, result.olderEntriesCount)
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
                _message.value = "Oy kaydedilemedi"
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
                // The new entry is at the end of the whole topic
                showAllEntries = true
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

    /** Deletes one of the user's own entries with the page's delete form. */
    fun deleteEntry(entry: Entry) {
        val form = _selectedTopic.value?.deleteForm
        if (form == null) {
            _message.value = "Silme formu bulunamadı; sayfayı yenileyip tekrar dene"
            return
        }
        val idField = form.fields.keys.firstOrNull { it.equals("id", ignoreCase = true) } ?: "id"
        viewModelScope.launch {
            val error = repository.submitForm(form, mapOf(idField to entry.entryId))
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

    // --- comments ---

    fun comments(entryId: String): CommentsState = _comments[entryId] ?: CommentsState()

    /** Opens or closes an entry's comments, loading them the first time. */
    fun toggleComments(entry: Entry) {
        val state = comments(entry.entryId)
        if (state.isOpen) {
            _comments[entry.entryId] = state.copy(isOpen = false)
            return
        }
        _comments[entry.entryId] = state.copy(isOpen = true)
        if (state.comments.isEmpty()) loadComments(entry.entryId)
    }

    private fun loadComments(entryId: String) {
        _comments[entryId] = comments(entryId).copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val list = repository.getComments(entryId)
                _comments[entryId] = comments(entryId).copy(comments = list, isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _comments[entryId] = comments(entryId).copy(isLoading = false, error = e.message ?: "Yorumlar yüklenemedi")
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
                _message.value = "Oy kaydedilemedi"
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
                _message.value = "Favori kaydedilemedi"
            }
        }
    }

    fun saveTopic() {
        _selectedTopic.value?.let { topic ->
            viewModelScope.launch {
                repository.saveTopic(topic)
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
