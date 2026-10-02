package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: EksiRepository
) : ViewModel() {

    private val _topics = mutableStateOf<List<Topic>>(emptyList())
    val topics: State<List<Topic>> = _topics

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage

    private val _selectedCategory = mutableStateOf("popular")
    val selectedCategory: State<String> = _selectedCategory

    private val _canLoadMoreTopics = mutableStateOf(true)
    val canLoadMoreTopics: State<Boolean> = _canLoadMoreTopics

    private val _isLoadingMoreTopics = mutableStateOf(false)
    val isLoadingMoreTopics: State<Boolean> = _isLoadingMoreTopics
    
    private val _scrollToTop = mutableStateOf(false)
    val scrollToTop: State<Boolean> = _scrollToTop

    // Scroll states
    val popularScrollState = LazyListState()
    val todayScrollState = LazyListState()
    val streamScrollState = LazyListState()

    fun getCategoryScrollState(category: String): LazyListState {
        return when (category) {
            "popular" -> popularScrollState
            "today" -> todayScrollState
            "stream" -> streamScrollState
            else -> popularScrollState
        }
    }

    fun fetchTopics(page: Int = 1) {
        if (page == 1) {
            _isLoading.value = true
            _error.value = null
            _currentPage.value = 1
            _topics.value = emptyList()
        } else {
            _isLoadingMoreTopics.value = true
        }

        viewModelScope.launch {
            try {
                // Timeout handling
                val timeoutJob = launch {
                    delay(30000)
                    if (page == 1 && _isLoading.value) {
                        _isLoading.value = false
                        _error.value = "Request timed out."
                    } else if (page > 1 && _isLoadingMoreTopics.value) {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                }

                val result = repository.getPopularTopics(page, _selectedCategory.value)
                timeoutJob.cancel()

                if (result.isEmpty()) {
                    if (page == 1) {
                        _error.value = "No topics found."
                    } else {
                        _canLoadMoreTopics.value = false
                    }
                } else if (result.size == 1 && result[0].title.startsWith("Error fetching data")) {
                    if (page == 1) {
                        _error.value = result[0].title
                    } else {
                        _canLoadMoreTopics.value = false
                    }
                } else {
                    if (page == 1) {
                        _topics.value = result
                    } else {
                        _topics.value = _topics.value + result
                    }
                    _currentPage.value = page
                    _canLoadMoreTopics.value = result.size >= 50
                }
            } catch (e: Exception) {
                if (page == 1) {
                    _error.value = e.message
                } else {
                    _canLoadMoreTopics.value = false
                }
            } finally {
                _isLoading.value = false
                _isLoadingMoreTopics.value = false
            }
        }
    }

    fun loadMoreTopics() {
        if (!_isLoading.value && !_isLoadingMoreTopics.value && _canLoadMoreTopics.value) {
            fetchTopics(_currentPage.value + 1)
        }
    }

    fun updateCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            fetchTopics(1)
        }
    }
    
    fun scrollToTop() {
        _scrollToTop.value = true
    }
    
    fun resetScrollToTop() {
        _scrollToTop.value = false
    }
    
    fun refreshTopics() {
        fetchTopics(1)
    }
}
