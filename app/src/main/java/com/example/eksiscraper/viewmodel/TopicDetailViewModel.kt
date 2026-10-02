package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.model.withFavorite
import com.example.eksiscraper.repository.EksiRepository
import java.net.URI
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

    val scrollState = LazyListState()

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

    private fun fetchEntries(topic: Topic, page: Int) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Open the topic itself from its first entry: drop query parameters such as
                // ?a=popular (today's entries only) or an old ?p=; the data source adds the page
                val path = try {
                    if (topic.url.isEmpty()) null
                    else URI(if (topic.url.startsWith("http")) topic.url else "https://eksisozluk.com" + (if (topic.url.startsWith("/")) "" else "/") + topic.url).path
                } catch (e: Exception) {
                    null
                }
                val result =
                        if (path.isNullOrBlank() || path == "/") {
                            // No topic path (e.g. an old saved search URL): search by title
                            repository.searchTopic(topic.title, page)
                        } else {
                            repository.searchTopic(topic.title, page, path)
                        }

                _selectedTopic.value = result
                _totalPages.value = result.totalPages
                _currentPage.value = page
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load the topic"
            } finally {
                _isLoading.value = false
            }
        }
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
    fun toggleEntryFavorite(entryId: String) {
        val entry = _selectedTopic.value?.entries?.firstOrNull { it.entryId == entryId } ?: return
        val target = !entry.isFavorited
        _selectedTopic.value = _selectedTopic.value?.withFavorite(entryId, target)
        viewModelScope.launch {
            if (!repository.setFavorite(entryId, target)) {
                _selectedTopic.value = _selectedTopic.value?.withFavorite(entryId, !target)
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
