package com.thesozluk.app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesozluk.app.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Autocomplete for the search screen (the search field owns the text). Running a search opens the topic screen
 * (ekşi redirects a query to its topic), so results, paging and favorites live there.
 */
class SearchViewModel(private val repository: EksiRepository) : ViewModel() {

    private val _suggestions = mutableStateOf<List<String>>(emptyList())
    val suggestions: State<List<String>> = _suggestions
    private val _authors = mutableStateOf<List<String>>(emptyList())
    val authors: State<List<String>> = _authors

    private val _isLoadingSuggestions = mutableStateOf(false)
    val isLoadingSuggestions: State<Boolean> = _isLoadingSuggestions

    private var suggestionJob: Job? = null

    fun updateQuery(query: String) {
        suggestionJob?.cancel()
        if (query.trim().length < 2) {
            _suggestions.value = emptyList()
            _authors.value = emptyList()
            _isLoadingSuggestions.value = false
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(300) // debounce while typing
            _isLoadingSuggestions.value = true
            try {
                val (titles, authors) = repository.getSearchSuggestions(query)
                _suggestions.value = titles
                _authors.value = authors
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _suggestions.value = emptyList()
                _authors.value = emptyList()
            } finally {
                _isLoadingSuggestions.value = false
            }
        }
    }
}
