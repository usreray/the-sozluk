package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: EksiRepository) : ViewModel() {

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _searchResult = mutableStateOf<Topic?>(null)
    val searchResult: State<Topic?> = _searchResult

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _suggestions = mutableStateOf<List<String>>(emptyList())
    val suggestions: State<List<String>> = _suggestions

    // Query that produced the current result; used to tell a new search from a page change
    private var lastSearchedQuery: String? = null
    private var suggestionJob: Job? = null

    val scrollState = LazyListState()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadSuggestions(query)
    }

    private fun loadSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.trim().length < 2) {
            _suggestions.value = emptyList()
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(300) // debounce while typing
            _suggestions.value = try {
                repository.getSearchSuggestions(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun selectSuggestion(suggestion: String) {
        _searchQuery.value = suggestion
        search()
    }

    fun search(page: Int = 1) {
        if (_searchQuery.value.isBlank()) return

        suggestionJob?.cancel()
        _suggestions.value = emptyList()

        val query = _searchQuery.value.trim()
        val isNewSearch = _searchResult.value == null || query != lastSearchedQuery

        if (_currentPage.value != page || isNewSearch) {
            viewModelScope.launch { scrollState.scrollToItem(0) }
        }

        if (isNewSearch) {
            _currentPage.value = 1
        } else {
            _currentPage.value = page
        }

        // The data source applies the page number to the redirected topic URL
        val redirectedUrl = if (isNewSearch) "" else _searchResult.value?.redirectedUrl.orEmpty()

        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            try {
                val result =
                        repository.searchTopic(query, if (isNewSearch) 1 else page, redirectedUrl)
                _searchResult.value = result
                lastSearchedQuery = query
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Entry states
    private val _expandedEntryIds = mutableStateMapOf<String, Boolean>()
    private val _favoritedEntryIds = mutableStateMapOf<String, Boolean>()

    fun isEntryExpanded(entryId: String): Boolean {
        return _expandedEntryIds[entryId] ?: false
    }

    fun toggleEntryExpansion(entryId: String) {
        val current = _expandedEntryIds[entryId] ?: false
        _expandedEntryIds[entryId] = !current
    }

    fun toggleEntryFavorite(entryId: String, isFavorited: Boolean) {
        _favoritedEntryIds[entryId] = isFavorited
        viewModelScope.launch {
            if (isFavorited) {
                repository.favoriteEntry(entryId)
            } else {
                repository.unfavoriteEntry(entryId)
            }
        }
    }

    fun saveTopic(topic: Topic) {
        viewModelScope.launch {
            repository.saveTopic(topic)
            // Update the local topic state to reflect the change
            _searchResult.value = _searchResult.value?.copy(isSaved = true)
        }
    }

    fun unsaveTopic(topic: Topic) {
        viewModelScope.launch {
            repository.unsaveTopic(topic)
            // Update the local topic state
            _searchResult.value = _searchResult.value?.copy(isSaved = false)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResult.value = null
        _currentPage.value = 1
        _error.value = null
        _suggestions.value = emptyList()
        lastSearchedQuery = null
        _expandedEntryIds.clear()
    }
}
