package com.example.eksiscraper.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Query and autocomplete for the search screen. Running a search opens the topic screen
 * (ekşi redirects a query to its topic), so results, paging and favorites live there.
 */
class SearchViewModel(private val repository: EksiRepository) : ViewModel() {

    private val _query = mutableStateOf("")
    val query: State<String> = _query

    private val _suggestions = mutableStateOf<List<String>>(emptyList())
    val suggestions: State<List<String>> = _suggestions

    private val _isLoadingSuggestions = mutableStateOf(false)
    val isLoadingSuggestions: State<Boolean> = _isLoadingSuggestions

    private var suggestionJob: Job? = null

    fun updateQuery(query: String) {
        _query.value = query
        suggestionJob?.cancel()
        if (query.trim().length < 2) {
            _suggestions.value = emptyList()
            _isLoadingSuggestions.value = false
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(300) // debounce while typing
            _isLoadingSuggestions.value = true
            try {
                _suggestions.value = repository.getSearchSuggestions(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            } finally {
                _isLoadingSuggestions.value = false
            }
        }
    }

    fun clear() = updateQuery("")
}
