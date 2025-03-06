package com.example.eksiscraper.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.data.LocalDataSource
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EksiViewModel : ViewModel() {
    private val _topics = mutableStateOf<List<Topic>>(emptyList())
    val topics: State<List<Topic>> = _topics
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> get() = _error
    
    private val _isUsingLocalData = mutableStateOf(false)
    val isUsingLocalData: State<Boolean> get() = _isUsingLocalData
    
    // Add a new state for the currently selected topic
    private val _selectedTopic = mutableStateOf<Topic?>(null)
    val selectedTopic: State<Topic?> = _selectedTopic
    
    // Add a loading state specifically for the selected topic
    private val _isLoadingTopic = mutableStateOf(false)
    val isLoadingTopic: State<Boolean> = _isLoadingTopic

    // Add state for search results
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    private val _searchResult = mutableStateOf<Topic?>(null)
    val searchResult: State<Topic?> = _searchResult
    
    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage
    
    private val _isPageDialogVisible = mutableStateOf(false)
    val isPageDialogVisible: State<Boolean> = _isPageDialogVisible
    
    private val _selectedPage = mutableStateOf(1)
    val selectedPage: State<Int> = _selectedPage

    private val _redirectedUrl = mutableStateOf("")
    val redirectedUrl: State<String> = _redirectedUrl

    private val _totalPages = mutableStateOf(1)
    val totalPages: State<Int> = _totalPages

    // Add state for topic pagination
    private val _topicCurrentPage = mutableStateOf(1)
    val topicCurrentPage: State<Int> = _topicCurrentPage
    
    private val _topicTotalPages = mutableStateOf(1)
    val topicTotalPages: State<Int> = _topicTotalPages

    // Add state for home page pagination
    private val _homeCurrentPage = mutableStateOf(1)
    val homeCurrentPage: State<Int> = _homeCurrentPage
    
    private val _canLoadMoreTopics = mutableStateOf(true)
    val canLoadMoreTopics: State<Boolean> = _canLoadMoreTopics
    
    private val _isLoadingMoreTopics = mutableStateOf(false)
    val isLoadingMoreTopics: State<Boolean> = _isLoadingMoreTopics

    // Add state for saved topics
    private val _savedTopics = mutableStateOf<List<Topic>>(emptyList())
    val savedTopics: State<List<Topic>> = _savedTopics

    // Add a flag to track which screen is active
    private val _activeScreen = mutableStateOf<String>("home")
    val activeScreen: State<String> = _activeScreen
    
    // Separate state for saved topic viewing
    private val _viewingSavedTopic = mutableStateOf<Topic?>(null)
    val viewingSavedTopic: State<Topic?> = _viewingSavedTopic
    
    private val _isViewingSavedTopic = mutableStateOf(false)
    val isViewingSavedTopic: State<Boolean> = _isViewingSavedTopic
    
    private val _savedTopicCurrentPage = mutableStateOf(1)
    val savedTopicCurrentPage: State<Int> = _savedTopicCurrentPage

    init {
        fetchTopics()
    }

    fun loadLocalData() {
        _topics.value = LocalDataSource.getLocalData().home
        _isUsingLocalData.value = true
        _isLoading.value = false
        _error.value = null
    }
    
    fun fetchTopics(page: Int = 1) {
        if (page == 1) {
            _isLoading.value = true
            _error.value = null
            _isUsingLocalData.value = false
            _homeCurrentPage.value = 1
            _topics.value = emptyList()
        } else {
            _isLoadingMoreTopics.value = true
        }
        
        viewModelScope.launch {
            try {
                // Add a timeout mechanism
                val timeoutJob = viewModelScope.launch {
                    delay(30000) // 30 seconds timeout
                    if (page == 1 && _isLoading.value) {
                        _isLoading.value = false
                        _error.value = "Request timed out. Loading local data instead."
                        loadLocalData()
                    } else if (page > 1 && _isLoadingMoreTopics.value) {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                }
                
                println("EksiViewModel: Starting to fetch topics for page $page")
                val result = EksiService.getPopularTopics(page)
                
                // Cancel the timeout job since we got a response
                timeoutJob.cancel()
                
                if (result.isEmpty()) {
                    println("EksiViewModel: No topics found for page $page")
                    if (page == 1) {
                        _error.value = "No topics found. Please try again later."
                        loadLocalData()
                    } else {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                } else if (result.size == 1 && result[0].title.startsWith("Error fetching data")) {
                    println("EksiViewModel: Error in fetched data for page $page: ${result[0].title}")
                    if (page == 1) {
                        _error.value = result[0].title
                        loadLocalData()
                    } else {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                } else {
                    println("EksiViewModel: Successfully fetched ${result.size} topics for page $page")
                    if (page == 1) {
                        _topics.value = result
                        _homeCurrentPage.value = 1
                    } else {
                        _topics.value = _topics.value + result
                        _homeCurrentPage.value = page
                    }
                    _isUsingLocalData.value = false
                    
                    // If we received fewer than 50 topics, we've reached the last page
                    _canLoadMoreTopics.value = result.size >= 50
                    println("EksiViewModel: Can load more topics: ${_canLoadMoreTopics.value} (received ${result.size} topics)")
                }
                
                if (page == 1) {
                    _isLoading.value = false
                } else {
                    _isLoadingMoreTopics.value = false
                }
            } catch (e: Exception) {
                println("EksiViewModel: Error fetching topics for page $page: ${e.message}")
                if (page == 1) {
                    _error.value = "Failed to load data: ${e.message ?: "Unknown error"}"
                    _isLoading.value = false
                    // Fallback to local data if network request fails
                    loadLocalData()
                } else {
                    _isLoadingMoreTopics.value = false
                    _canLoadMoreTopics.value = false
                }
            }
        }
    }
    
    fun loadMoreTopics() {
        if (!_isLoading.value && !_isLoadingMoreTopics.value && _canLoadMoreTopics.value) {
            println("EksiViewModel: Loading more topics, current page: ${_homeCurrentPage.value}")
            fetchTopics(_homeCurrentPage.value + 1)
        } else {
            println("EksiViewModel: Cannot load more topics. isLoading: ${_isLoading.value}, isLoadingMoreTopics: ${_isLoadingMoreTopics.value}, canLoadMoreTopics: ${_canLoadMoreTopics.value}")
        }
    }
    
    // Add a method to set the selected topic directly from the list with pagination
    fun selectTopic(index: Int, page: Int = 1) {
        println("EksiViewModel: selectTopic called with index $index, page $page, topics size: ${_topics.value.size}")
        if (index >= 0 && index < _topics.value.size) {
            val topic = _topics.value[index]
            
            // Check if we're already viewing this topic and page
            if (_selectedTopic.value?.title == topic.title && _topicCurrentPage.value == page) {
                println("EksiViewModel: Already viewing topic '${topic.title}' on page $page, no need to reload")
                return
            }
            
            _selectedTopic.value = topic
            _topicCurrentPage.value = page
            println("EksiViewModel: Selected topic set to: ${_selectedTopic.value?.title}, page: $page")
            
            // Fetch entries if they haven't been loaded yet or if we're changing pages
            if (_selectedTopic.value != null && (!topic.entriesLoaded || !topic.loadedPages.contains(page))) {
                fetchEntriesForSelectedTopic(page)
            } else {
                println("EksiViewModel: Topic entries already loaded for page $page, no need to fetch again")
            }
        } else {
            println("EksiViewModel: Invalid index $index for topics size ${_topics.value.size}")
        }
    }
    
    // Update to support pagination
    private fun fetchEntriesForSelectedTopic(page: Int = 1) {
        val topic = _selectedTopic.value ?: return
        
        // Check if entries are already loaded for this page
        if (topic.entriesLoaded && topic.loadedPages.contains(page)) {
            println("EksiViewModel: Entries already loaded for topic '${topic.title}' on page $page, skipping fetch")
            return
        }
        
        _isLoadingTopic.value = true
        _topicCurrentPage.value = page
        
        viewModelScope.launch {
            try {
                println("EksiViewModel: Fetching entries for topic: ${topic.title}, page: $page")
                
                // Use the topic's original URL for fetching entries
                if (topic.url.isNotEmpty()) {
                    // Extract base URL and path from the topic's URL
                    val baseUrl = if (topic.url.startsWith("http")) {
                        val uri = java.net.URI(topic.url)
                        "${uri.scheme}://${uri.host}"
                    } else {
                        "https://eksisozluk.com"
                    }
                    
                    // Extract the path from the full URL or use the URL directly if it's just a path
                    val path = if (topic.url.startsWith("http")) {
                        java.net.URI(topic.url).path + java.net.URI(topic.url).query?.let { "?$it" } ?: ""
                    } else {
                        topic.url
                    }
                    
                    // Preserve the original URL parameters (like ?a=popular)
                    val originalPath = path
                    
                    // Modify the path to include page parameter if needed
                    val pageParam = if (page > 1) {
                        if (originalPath.contains("?")) {
                            "&p=$page"
                        } else {
                            "?p=$page"
                        }
                    } else ""
                    
                    // Construct the final path with all parameters
                    val pathWithPage = if (originalPath.contains("p=")) {
                        // Replace existing page parameter
                        originalPath.replaceFirst(Regex("p=\\d+"), "p=$page")
                    } else {
                        originalPath + pageParam
                    }
                    
                    println("EksiViewModel: Using URL: $baseUrl$pathWithPage")
                    
                    // Use the searchTopic method with the exact original path
                    val searchResult = EksiService.searchTopic(topic.title, page, originalPath)
                    
                    // Update total pages
                    _topicTotalPages.value = searchResult.totalPages
                    
                    // Create a new topic with the loaded entries
                    val updatedTopic = topic.copy(
                        entries = searchResult.entries, 
                        entriesLoaded = true,
                        loadedPages = topic.loadedPages + page,
                        totalPages = 999,  // Set a high default value to allow many pages
                        redirectedUrl = searchResult.redirectedUrl.ifEmpty { originalPath }
                    )
                    
                    // Update the selected topic
                    _selectedTopic.value = updatedTopic
                    
                    // Also update the topic in the list
                    val updatedTopics = _topics.value.toMutableList()
                    val index = updatedTopics.indexOfFirst { it.title == topic.title }
                    if (index != -1) {
                        updatedTopics[index] = updatedTopic
                        _topics.value = updatedTopics
                    }
                    
                    println("EksiViewModel: Successfully fetched ${searchResult.entries.size} entries for topic: ${topic.title}, page: $page, totalPages: ${searchResult.totalPages}")
                } else {
                    // Fallback to search by title if URL is empty
                    println("EksiViewModel: No URL available for topic, falling back to search by title")
                    val searchResult = EksiService.searchTopic(topic.title, page)
                    
                    // Update total pages
                    _topicTotalPages.value = searchResult.totalPages
                    
                    // Create a new topic with the loaded entries
                    val updatedTopic = topic.copy(
                        entries = searchResult.entries, 
                        entriesLoaded = true,
                        loadedPages = topic.loadedPages + page,
                        totalPages = 999,  // Set a high default value to allow many pages
                        redirectedUrl = searchResult.redirectedUrl
                    )
                    
                    // Update the selected topic
                    _selectedTopic.value = updatedTopic
                    
                    // Also update the topic in the list
                    val updatedTopics = _topics.value.toMutableList()
                    val index = updatedTopics.indexOfFirst { it.title == topic.title }
                    if (index != -1) {
                        updatedTopics[index] = updatedTopic
                        _topics.value = updatedTopics
                    }
                    
                    println("EksiViewModel: Successfully fetched ${searchResult.entries.size} entries for topic: ${topic.title}, page: $page, totalPages: ${searchResult.totalPages}")
                }
            } catch (e: Exception) {
                println("EksiViewModel: Error fetching entries: ${e.message}")
                
                // Update the selected topic with error message
                val errorEntries = listOf(Entry("Error loading entries: ${e.message ?: "Unknown error"}"))
                val updatedTopic = topic.copy(
                    entries = errorEntries,
                    entriesLoaded = true
                )
                _selectedTopic.value = updatedTopic
            } finally {
                _isLoadingTopic.value = false
            }
        }
    }
    
    // Add methods for search functionality
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun search(page: Int = 1) {
        if (_searchQuery.value.isBlank()) return
        
        // If changing pages, update the redirectedUrl to match the correct page
        val updatedRedirectedUrl = if (searchResult.value?.redirectedUrl?.isNotEmpty() == true) {
            if (searchResult.value?.redirectedUrl?.contains("p=") == true) {
                searchResult.value?.redirectedUrl?.replaceFirst(Regex("p=\\d+"), "p=$page") ?: ""
            } else if (searchResult.value?.redirectedUrl?.contains("?") == true) {
                "${searchResult.value?.redirectedUrl}&p=$page"
            } else {
                "${searchResult.value?.redirectedUrl}?p=$page"
            }
        } else {
            ""
        }
        
        // Then call searchTopic with the updated redirectedUrl
        viewModelScope.launch {
            _isSearching.value = true
            _currentPage.value = page
            _selectedPage.value = page
            
            try {
                val result = EksiService.searchTopic(
                    _searchQuery.value,
                    page,
                    updatedRedirectedUrl  // Use the updated redirectedUrl
                )
                _searchResult.value = result
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearch() {
        println("EksiViewModel: Clearing search state")
        _searchQuery.value = ""
        _searchResult.value = null
        _currentPage.value = 1
        _redirectedUrl.value = ""
        // Also clear any selected topic to prevent unwanted topic loading
        clearSelectedTopic()
    }
    
    // Add methods for page dialog
    fun showPageDialog() {
        // For topic details, use topicCurrentPage
        _selectedPage.value = if (_selectedTopic.value != null) {
            _topicCurrentPage.value
        } else {
            _currentPage.value
        }
        _isPageDialogVisible.value = true
    }
    
    fun hidePageDialog() {
        _isPageDialogVisible.value = false
    }
    
    fun updateSelectedPage(page: Int) {
        _selectedPage.value = page
    }
    
    fun applySelectedPage() {
        // For search results
        if (_searchResult.value != null) {
            // If we're viewing a saved topic in the You tab
            if (_searchResult.value?.isSaved == true) {
                viewSavedTopic(_searchResult.value!!, _selectedPage.value)
            } else {
                search(_selectedPage.value)
            }
        } 
        // For topic details
        else if (_selectedTopic.value != null) {
            navigateTopicToPage(_selectedPage.value)
        }
        hidePageDialog()
    }
    
    // Add method to navigate to a specific page for a topic
    fun navigateTopicToPage(page: Int) {
        println("EksiViewModel: navigateTopicToPage called with page $page")
        if (page >= 1 && _selectedTopic.value != null) {
            val topicIndex = _topics.value.indexOfFirst { it.title == _selectedTopic.value?.title }
            println("EksiViewModel: found topic index $topicIndex for ${_selectedTopic.value?.title}")
            if (topicIndex != -1) {
                println("EksiViewModel: calling selectTopic with index $topicIndex, page $page")
                selectTopic(topicIndex, page)
            }
        }
    }
    
    // Add method to save logs
    fun getDebugLog(): String {
        return EksiService.getDebugLog()
    }
    
    // Add method to get topics as JSON
    fun getTopicsAsJson(): String {
        return EksiService.getTopicsAsJson()
    }

    // Methods for saved topics
    fun saveTopic(topic: Topic) {
        println("EksiViewModel: Saving topic: ${topic.title}")
        
        // Check if the topic is already saved
        if (_savedTopics.value.any { it.title == topic.title }) {
            return
        }
        
        // Create a copy of the topic with isSaved set to true
        val savedTopic = topic.copy(isSaved = true)
        
        // Add to saved topics
        _savedTopics.value = _savedTopics.value + savedTopic
        
        // Also update the topic in the main list and selected topic if needed
        updateTopicSavedStatusInLists(topic.title, true)
        
        println("EksiViewModel: Total saved topics: ${_savedTopics.value.size}")
    }

    fun unsaveTopic(topic: Topic) {
        println("EksiViewModel: Removing saved topic: ${topic.title}")
        
        // Remove from saved topics
        _savedTopics.value = _savedTopics.value.filter { it.title != topic.title }
        
        // Update the topic in the main list and selected topic if needed
        updateTopicSavedStatusInLists(topic.title, false)
        
        println("EksiViewModel: Total saved topics: ${_savedTopics.value.size}")
    }

    fun isTopicSaved(title: String): Boolean {
        return _savedTopics.value.any { it.title == title }
    }

    private fun updateTopicSavedStatusInLists(title: String, isSaved: Boolean) {
        // Update in the main topics list
        val updatedTopics = _topics.value.map { 
            if (it.title == title) it.copy(isSaved = isSaved) else it 
        }
        _topics.value = updatedTopics
        
        // Update selected topic if it's the same one
        if (_selectedTopic.value?.title == title) {
            _selectedTopic.value = _selectedTopic.value?.copy(isSaved = isSaved)
        }
        
        // Update search result if it's the same one
        if (_searchResult.value?.title == title) {
            _searchResult.value = _searchResult.value?.copy(isSaved = isSaved)
        }
    }

    // Method to view a saved topic by using the same approach as search
    fun viewSavedTopic(topic: Topic, page: Int = 1) {
        println("EksiViewModel: Viewing saved topic: ${topic.title}")
        
        // Set the viewing saved topic state
        _viewingSavedTopic.value = topic
        _isViewingSavedTopic.value = true
        _savedTopicCurrentPage.value = page
        
        // Use the topic's redirectedUrl if available, but clean it up first
        var redirectedUrl = topic.redirectedUrl
        
        // Clean up the redirectedUrl by removing any query parameters
        if (redirectedUrl.contains("?")) {
            redirectedUrl = redirectedUrl.substringBefore("?")
            println("EksiViewModel: Cleaned redirectedUrl: $redirectedUrl")
        }
        
        // Call searchTopic with the cleaned redirectedUrl
        viewModelScope.launch {
            _isSearching.value = true
            
            try {
                val result = EksiService.searchTopic(
                    topic.title,
                    page,
                    redirectedUrl  // Use the cleaned redirectedUrl
                )
                
                // Update the viewingSavedTopic with the fetched entries
                _viewingSavedTopic.value = result.copy(isSaved = true)
            } catch (e: Exception) {
                // Handle error
                println("EksiViewModel: Error viewing saved topic: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    // Method to stop viewing a saved topic
    fun stopViewingSavedTopic() {
        _viewingSavedTopic.value = null
        _isViewingSavedTopic.value = false
        _savedTopicCurrentPage.value = 1
    }

    // Method to set the active screen
    fun setActiveScreen(screen: String) {
        // If we're already on this screen, don't do anything
        if (_activeScreen.value == screen) {
            println("EksiViewModel: Already on screen $screen, ignoring redundant setActiveScreen call")
            return
        }
        
        println("EksiViewModel: Setting active screen to $screen (previous: ${_activeScreen.value})")
        
        val previousScreen = _activeScreen.value
        
        // Set the active screen first to prevent redundant calls
        _activeScreen.value = screen
        
        // Always clear selected topic when navigating away from topic_detail
        if (previousScreen == "topic_detail" && screen != "topic_detail") {
            println("EksiViewModel: Clearing selected topic when navigating away from topic_detail")
            clearSelectedTopic()
        }
        
        // Always clear selected topic when navigating to search or profile
        else if (screen == "search" || screen == "profile") {
            println("EksiViewModel: Clearing selected topic when navigating to $screen")
            clearSelectedTopic()
        }
        
        // Additional cleanup based on the target screen
        when (screen) {
            "search" -> {
                // Don't clear search results to preserve search state
            }
            "profile" -> {
                // Don't clear saved topics state
            }
            "home" -> {
                // Don't clear home state
                // But ensure no topic is selected if coming from topic_detail
                if (previousScreen == "topic_detail") {
                    // Already handled above
                }
            }
        }
    }

    // Method to reset the Home screen to its initial state
    fun resetHomeScreen() {
        println("EksiViewModel: Resetting Home screen")
        
        // Clear any selected topic first
        clearSelectedTopic()
        
        // Reset to page 1
        _homeCurrentPage.value = 1
        _currentPage.value = 1
        
        // Only fetch fresh topics if the list is empty
        if (_topics.value.isEmpty()) {
            fetchTopics(1)
        }
    }
    
    
    // Method to reset the Profile screen to its initial state
    fun resetProfileScreen() {
        println("EksiViewModel: Resetting Profile screen")
        stopViewingSavedTopic()
        // Ensure we don't trigger any topic selection
        clearSelectedTopic()
    }

    // Method to clear the selected topic
    fun clearSelectedTopic() {
        // Only clear if there's actually a topic selected
        if (_selectedTopic.value == null) {
            println("EksiViewModel: No topic selected, ignoring clearSelectedTopic call")
            return
        }
        
        println("EksiViewModel: Clearing selected topic")
        _selectedTopic.value = null
        _isLoadingTopic.value = false
        _topicCurrentPage.value = 1
        _redirectedUrl.value = ""  // Clear the redirected URL as well
    }
} 