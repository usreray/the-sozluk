package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.updateEntry
import com.example.eksiscraper.model.withFavorite
import com.example.eksiscraper.model.withVote
import com.example.eksiscraper.network.TopicNotFoundException
import com.example.eksiscraper.repository.EksiRepository
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class TopicDetailViewModel(private val repository: EksiRepository) : ViewModel() {

    private val _selectedTopic = mutableStateOf<Topic?>(null)
    val selectedTopic: State<Topic?> = _selectedTopic

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage

    private val _totalPages = mutableStateOf(1)
    val totalPages: State<Int> = _totalPages

    private val _expandedEntries = mutableStateMapOf<String, Boolean>()
    val expandedEntries: Map<String, Boolean> = _expandedEntries

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

    fun loadTopic(title: String, url: String, page: Int = 1) {
        val initialTopic =
                Topic(
                        title = title,
                        url = url,
                        commentCount = 0,
                        entries = emptyList(),
                        entriesLoaded = false
                )

        // Check if saved
        viewModelScope.launch {
            val isSaved = repository.isTopicSaved(title)
            val topicWithSavedStatus = initialTopic.copy(isSaved = isSaved)
            selectTopic(topicWithSavedStatus, page)
        }
    }

    fun selectTopic(topic: Topic, page: Int = 1) {
        // Check if we're already viewing this topic and page
        if (_selectedTopic.value?.title == topic.title && _currentPage.value == page) {
            return
        }

        // Clear expanded entries when switching to a different page or topic
        if (_selectedTopic.value?.title != topic.title || _currentPage.value != page) {
            clearExpandedEntries()
            viewModelScope.launch { scrollState.scrollToItem(0) }
        }

        _selectedTopic.value = topic
        _currentPage.value = page

        // Fetch entries
        fetchEntries(topic, page)
    }

    /** Leaves today's entries for the full topic, starting from its first entry. */
    fun showOlderEntries() {
        val topic = _selectedTopic.value ?: return
        showAllEntries = true
        clearExpandedEntries()
        _currentPage.value = 1
        viewModelScope.launch { scrollState.scrollToItem(0) }
        fetchEntries(topic, 1)
    }

    private fun fetchEntries(topic: Topic, page: Int) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                show(topic, page, request(topic, page))
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

    private fun show(topic: Topic, page: Int, result: Topic) {
        // Keep the link we were opened with so paging stays in the same (popular/full) mode
        _selectedTopic.value = if (topic.url.isNotEmpty()) result.copy(url = topic.url) else result
        _totalPages.value = result.totalPages
        _currentPage.value = page
    }

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

    /** Whether the page offered an entry form, i.e. the user may write here. */
    val canWrite: Boolean get() = _selectedTopic.value?.entryForm?.textFieldName != null

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
                var result = request(topic, maxOf(1, _totalPages.value))
                if (result.totalPages > _totalPages.value) result = request(topic, result.totalPages)
                show(topic, result.currentPage, result)
                scrollState.animateScrollToItem(maxOf(0, result.entries.size))
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

    fun consumeMessage() {
        _message.value = null
    }

    fun retry() {
        _selectedTopic.value?.let { fetchEntries(it, _currentPage.value) }
    }

    fun navigateToPage(page: Int) {
        _selectedTopic.value?.let { topic -> selectTopic(topic, page) }
    }

    fun isEntryExpanded(entryId: String): Boolean {
        return _expandedEntries[entryId] ?: false
    }

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

    private fun clearExpandedEntries() {
        _expandedEntries.clear()
    }

    fun saveTopic() {
        _selectedTopic.value?.let { topic ->
            viewModelScope.launch {
                repository.saveTopic(topic)
                _selectedTopic.value = topic.copy(isSaved = true)
            }
        }
    }

    fun unsaveTopic() {
        _selectedTopic.value?.let { topic ->
            viewModelScope.launch {
                repository.unsaveTopic(topic)
                _selectedTopic.value = topic.copy(isSaved = false)
            }
        }
    }
}
