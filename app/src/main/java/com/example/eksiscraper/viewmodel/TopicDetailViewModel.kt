package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
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
        viewModelScope.launch {
            try {
                val result =
                        if (topic.url.isNotEmpty()) {
                            // Extract path and query
                            val uri =
                                    try {
                                        if (topic.url.startsWith("http")) URI(topic.url)
                                        else
                                                URI(
                                                        "https://eksisozluk.com" +
                                                                if (topic.url.startsWith("/")) ""
                                                                else "/" + topic.url
                                                )
                                    } catch (e: Exception) {
                                        null
                                    }

                            val path = uri?.path ?: topic.url
                            val query = uri?.query

                            val originalPath = if (query != null) "$path?$query" else path

                            // Modify path for page
                            val pathWithPage =
                                    if (originalPath.contains("p=")) {
                                        originalPath.replaceFirst(Regex("p=\\d+"), "p=$page")
                                    } else {
                                        if (originalPath.contains("?")) "$originalPath&p=$page"
                                        else "$originalPath?p=$page"
                                    }

                            repository.searchTopic(topic.title, page, pathWithPage)
                        } else {
                            repository.searchTopic(topic.title, page)
                        }

                _selectedTopic.value = result
                _totalPages.value = result.totalPages
                _currentPage.value = page
            } catch (e: Exception) {
                // Handle error
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

    fun toggleEntryFavorite(entryId: String, isFavorited: Boolean) {
        viewModelScope.launch {
            if (isFavorited) {
                repository.favoriteEntry(entryId)
            } else {
                repository.unfavoriteEntry(entryId)
            }
        }
    }

    private fun clearExpandedEntries() {
        _expandedEntries.clear()
    }

    fun favoriteEntry(entryId: String) {
        viewModelScope.launch {
            repository.favoriteEntry(entryId)
            // Ideally update the UI to reflect the change locally
        }
    }

    fun unfavoriteEntry(entryId: String) {
        viewModelScope.launch {
            repository.unfavoriteEntry(entryId)
            // Ideally update the UI to reflect the change locally
        }
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
