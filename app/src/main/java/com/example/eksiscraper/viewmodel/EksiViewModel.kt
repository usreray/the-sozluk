package com.example.eksiscraper.viewmodel

import android.app.Application
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.data.room.EksiDatabase
import com.example.eksiscraper.data.room.SavedTopicRepository
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EksiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SavedTopicRepository
    
    private val _topics = mutableStateOf<List<Topic>>(emptyList())
    val topics: State<List<Topic>> = _topics
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> get() = _error
    
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

    // Track expanded entries across the entire app
    private val _expandedEntries = mutableStateMapOf<Int, Boolean>()
    val expandedEntries: Map<Int, Boolean> = _expandedEntries
    
    // Add state for scroll to top
    private val _homeScrollToTop = mutableStateOf(false)
    val homeScrollToTop: State<Boolean> = _homeScrollToTop
    
    // Add state for home screen categories
    private val _selectedHomeCategory = mutableStateOf("popular")
    val selectedHomeCategory: State<String> = _selectedHomeCategory
    
    // Add rememberLazyListState for each screen to maintain scroll position across tab switches
    val homeScrollState = LazyListState()
    val searchScrollState = LazyListState()
    val profileScrollState = LazyListState()
    // Add a separate scroll state for topic details
    val topicDetailScrollState = LazyListState()
    
    // Add persistent scroll states for each category tab
    val popularScrollState = LazyListState()
    val todayScrollState = LazyListState()
    val streamScrollState = LazyListState()
    
    // Function to get the appropriate scroll state for a given category
    fun getCategoryScrollState(category: String): LazyListState {
        return when (category) {
            "popular" -> popularScrollState
            "today" -> todayScrollState
            "stream" -> streamScrollState
            else -> homeScrollState // Fallback
        }
    }
    
    // Add a flag to track if the first page has been loaded
    private val _hasInitializedHomePage = mutableStateOf(false)
    val hasInitializedHomePage: State<Boolean> = _hasInitializedHomePage

    init {
        val database = EksiDatabase.getDatabase(application)
        repository = SavedTopicRepository(database.savedTopicDao())
        
        // Load saved topics from the database
        viewModelScope.launch {
            repository.allSavedTopics.collectLatest { savedTopics ->
                _savedTopics.value = savedTopics
                
                // Update saved status in the main topics list
                updateSavedStatusInTopicsList()
            }
        }
        
        // We don't auto-fetch topics anymore - this will now be initiated by the UI
        // when the app is first visible
    }
    
    // Update saved status in the main topics list
    private fun updateSavedStatusInTopicsList() {
        val savedTopicTitles = _savedTopics.value.map { it.title }
        
        // Update topics in the main list
        _topics.value = _topics.value.map { topic ->
            if (savedTopicTitles.contains(topic.title)) {
                topic.copy(isSaved = true)
            } else {
                topic.copy(isSaved = false)
            }
        }
        
        // Update selected topic if needed
        _selectedTopic.value = _selectedTopic.value?.let { topic ->
            if (savedTopicTitles.contains(topic.title)) {
                topic.copy(isSaved = true)
            } else {
                topic.copy(isSaved = false)
            }
        }
        
        // Update search result if needed
        _searchResult.value = _searchResult.value?.let { topic ->
            if (savedTopicTitles.contains(topic.title)) {
                topic.copy(isSaved = true)
            } else {
                topic.copy(isSaved = false)
            }
        }
        
        // Update viewing saved topic if needed
        _viewingSavedTopic.value = _viewingSavedTopic.value?.let { topic ->
            if (savedTopicTitles.contains(topic.title)) {
                topic.copy(isSaved = true)
            } else {
                topic.copy(isSaved = false)
            }
        }
    }

    fun fetchTopics(page: Int = 1) {
        if (page == 1) {
            _isLoading.value = true
            _error.value = null
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
                        _error.value = "Request timed out."
                    } else if (page > 1 && _isLoadingMoreTopics.value) {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                }
                
                // Pass the selected category to getPopularTopics
                val result = EksiService.getPopularTopics(page, _selectedHomeCategory.value)
                
                // Cancel the timeout job since we got a response
                timeoutJob.cancel()
                
                if (result.isEmpty()) {
                    if (page == 1) {
                        _error.value = "No topics found. Please try again later."
                    } else {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                } else if (result.size == 1 && result[0].title.startsWith("Error fetching data")) {
                    if (page == 1) {
                        _error.value = result[0].title
                    } else {
                        _isLoadingMoreTopics.value = false
                        _canLoadMoreTopics.value = false
                    }
                } else {
                    // Update saved status for each topic
                    val updatedResult = result.map { topic ->
                        updateTopicSavedStatus(topic)
                    }
                    
                    if (page == 1) {
                        _topics.value = updatedResult
                        _homeCurrentPage.value = 1
                    } else {
                        _topics.value = _topics.value + updatedResult
                        _homeCurrentPage.value = page
                    }
                    
                    // If we received fewer than 50 topics, we've reached the last page
                    _canLoadMoreTopics.value = result.size >= 50
                }
                
                if (page == 1) {
                    _isLoading.value = false
                } else {
                    _isLoadingMoreTopics.value = false
                }
            } catch (e: Exception) {
                if (page == 1) {
                    _error.value = "Failed to load data: ${e.message ?: "Unknown error"}"
                    _isLoading.value = false
                } else {
                    _isLoadingMoreTopics.value = false
                    _canLoadMoreTopics.value = false
                }
            }
        }
    }
    
    fun loadMoreTopics() {
        if (!_isLoading.value && !_isLoadingMoreTopics.value && _canLoadMoreTopics.value) {
            fetchTopics(_homeCurrentPage.value + 1)
        }
    }
    
    // Add a method to set the selected topic directly from the list with pagination
    fun selectTopic(index: Int, page: Int = 1) {
        if (index >= 0 && index < _topics.value.size) {
            val topic = _topics.value[index]
            
            // Update saved status before setting as selected topic
            val updatedTopic = updateTopicSavedStatus(topic)
            
            // Check if we're already viewing this topic and page
            if (_selectedTopic.value?.title == updatedTopic.title && _topicCurrentPage.value == page) {
                return
            }
            
            // Clear expanded entries when switching to a different page
            if (_selectedTopic.value?.title == updatedTopic.title && _topicCurrentPage.value != page) {
                clearExpandedEntries()
                
                // Reset scroll position to top when changing pages within the same topic
                viewModelScope.launch {
                    topicDetailScrollState.scrollToItem(0)
                }
            }
            
            // Always reset scroll position to top when selecting a topic
            viewModelScope.launch {
                topicDetailScrollState.scrollToItem(0)
            }
            
            _selectedTopic.value = updatedTopic
            _topicCurrentPage.value = page
            
            // Always fetch entries for the selected topic and page
            fetchEntriesForSelectedTopic(page)
        }
    }
    
    // Update to support pagination
    private fun fetchEntriesForSelectedTopic(page: Int = 1) {
        val topic = _selectedTopic.value ?: return
        
        _isLoadingTopic.value = true
        _topicCurrentPage.value = page
        
        viewModelScope.launch {
            try {
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
                } else {
                    // Fallback to search by title if URL is empty
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
                }
            } catch (e: Exception) {
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
        
        // Check if this is a new search or just a page change
        val isNewSearch = _searchResult.value == null || 
                         (_searchResult.value != null && page == 1 && 
                          _searchResult.value?.title?.lowercase() != _searchQuery.value.lowercase().trim())
                          
        // Clear expanded entries when changing pages or performing a new search
        if (_currentPage.value != page || isNewSearch) {
            clearExpandedEntries()
            
            // Reset scroll position to top when changing pages or performing a new search
            viewModelScope.launch {
                searchScrollState.scrollToItem(0)
            }
        }
        
        // For a new search, reset the current page to 1
        if (isNewSearch) {
            _currentPage.value = 1
        } else {
            _currentPage.value = page
        }
        
        // If changing pages, update the redirectedUrl to match the correct page
        val updatedRedirectedUrl = if (searchResult.value?.redirectedUrl?.isNotEmpty() == true && !isNewSearch) {
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
            
            try {
                val result = EksiService.searchTopic(
                    _searchQuery.value,
                    if (isNewSearch) 1 else page,  // Use page 1 for new searches
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
        _searchQuery.value = ""
        _searchResult.value = null
        _currentPage.value = 1
        _redirectedUrl.value = ""
        // Make sure we stay on the search screen
        if (_activeScreen.value != "search") {
            _activeScreen.value = "search"
        }
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
        if (page >= 1 && _selectedTopic.value != null) {
            val topicIndex = _topics.value.indexOfFirst { it.title == _selectedTopic.value?.title }
            if (topicIndex != -1) {
                // Clear expanded entries when changing pages
                if (_topicCurrentPage.value != page) {
                    clearExpandedEntries()
                    
                    // Reset scroll position to top when changing pages
                    viewModelScope.launch {
                        topicDetailScrollState.scrollToItem(0)
                    }
                }
                
                selectTopic(topicIndex, page)
            }
        }
    }
    
    // Add method to get topics as JSON
    fun getTopicsAsJson(): String {
        return EksiService.getTopicsAsJson()
    }

    // Methods for saved topics
    fun saveTopic(topic: Topic) {
        // Update the topic in the UI immediately
        updateTopicSavedStatusInLists(topic.title, true)
        
        // Save to Room database via repository
        viewModelScope.launch {
            repository.saveTopic(topic)
        }
    }

    fun unsaveTopic(topic: Topic) {
        // Update the topic in the UI immediately
        updateTopicSavedStatusInLists(topic.title, false)
        
        // Remove from Room database via repository
        viewModelScope.launch {
            repository.unsaveTopic(topic)
            
            // If we're viewing this topic in the You tab, stop viewing it
            if (_isViewingSavedTopic.value && _viewingSavedTopic.value?.title == topic.title) {
                stopViewingSavedTopic()
            }
        }
    }

    fun isTopicSaved(title: String): Boolean {
        // This is now just a UI helper - the actual data comes from the database
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
        
        // Update viewing saved topic if it's the same one
        if (_viewingSavedTopic.value?.title == title) {
            _viewingSavedTopic.value = _viewingSavedTopic.value?.copy(isSaved = isSaved)
        }
    }

    // Method to view a saved topic by using the same approach as search
    fun viewSavedTopic(topic: Topic, page: Int = 1) {
        // Clear expanded entries when changing pages
        if (_savedTopicCurrentPage.value != page || _viewingSavedTopic.value?.title != topic.title) {
            clearExpandedEntries()
            
            // Reset scroll position to top when changing pages
            viewModelScope.launch {
                profileScrollState.scrollToItem(0)
            }
        }
        
        // Set the viewing saved topic state
        _viewingSavedTopic.value = topic
        _isViewingSavedTopic.value = true
        _savedTopicCurrentPage.value = page
        
        // Use the topic's redirectedUrl if available, but clean it up first
        var redirectedUrl = topic.redirectedUrl
        
        // Clean up the redirectedUrl by removing any query parameters
        if (redirectedUrl.contains("?")) {
            redirectedUrl = redirectedUrl.substringBefore("?")
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

    // Method to check and update the saved status of the search result
    fun checkAndUpdateSearchResultSavedStatus() {
        viewModelScope.launch {
            _searchResult.value?.let { result ->
                val isSaved = repository.isTopicSaved(result.title)
                if (result.isSaved != isSaved) {
                    _searchResult.value = result.copy(isSaved = isSaved)
                }
            }
        }
    }

    // Method to set the active screen
    fun setActiveScreen(screen: String) {
        // If we're already on this screen, don't do anything
        if (_activeScreen.value == screen) {
            return
        }
        
        val previousScreen = _activeScreen.value
        
        // Set the active screen first to prevent redundant calls
        _activeScreen.value = screen
        
        // Special handling for navigating to home screen
        if (screen == "home") {
            // If coming from topic_detail, we want to preserve the selected topic
            // so we can show it when navigating back to home
            if (previousScreen == "topic_detail") {
                // Don't clear the selected topic
            } else if (_selectedTopic.value != null && previousScreen != "topic_detail") {
                // If we have a selected topic but we're not coming from topic_detail,
                // we should preserve it
            }
            
            return
        }
        
        // When navigating to topic_detail, we want to preserve the selected topic
        if (screen == "topic_detail") {
            return
        }
        
        // When navigating to search or profile, we don't want to clear the selected topic
        // so it can be restored when navigating back to home
        if (screen == "search" || screen == "profile") {
            return
        }
    }

    // Method to reset the Home screen to its initial state
    fun resetHomeScreen() {
        // When the Home tab is pressed again, we want to go to the home page
        // regardless of whether a topic is selected
        if (_selectedTopic.value != null) {
            clearSelectedTopic()
        }
        
        // Reset to page 1
        _homeCurrentPage.value = 1
        _currentPage.value = 1
        
        // Ensure we're on the home screen
        _activeScreen.value = "home"
        
        // Only fetch fresh topics if the list is empty
        if (_topics.value.isEmpty()) {
            fetchTopics(1)
        }
    }
    
    
    // Method to reset the Profile screen to its initial state
    fun resetProfileScreen() {
        stopViewingSavedTopic()
        // Don't clear the selected topic to preserve the topic detail view
        // when navigating back to home
    }

    // Method to clear the selected topic
    fun clearSelectedTopic() {
        // Only clear if there's actually a topic selected
        if (_selectedTopic.value == null) {
            return
        }
        
        _selectedTopic.value = null
        _isLoadingTopic.value = false
        _topicCurrentPage.value = 1
        _redirectedUrl.value = ""  // Clear the redirected URL as well
    }

    // Method to scroll to the top of the home screen without reloading data
    fun scrollHomeToTop() {
        // We don't need to reload data, just signal that we want to scroll to top
        // This will be observed in the HomeScreen composable
        _homeScrollToTop.value = true
    }
    
    // Reset the scroll to top flag after it's been consumed
    fun resetHomeScrollToTop() {
        _homeScrollToTop.value = false
    }

    // Method to change the selected home category
    fun changeHomeCategory(category: String) {
        if (_selectedHomeCategory.value != category) {
            _selectedHomeCategory.value = category
            
            // Reset page and fetch new topics for the selected category
            _homeCurrentPage.value = 1
            _topics.value = emptyList()
            
            // Reset scroll position to top for this specific category
            viewModelScope.launch {
                getCategoryScrollState(category).scrollToItem(0)
            }
            
            // Fetch topics for the selected category
            fetchTopics(1)
        }
    }

    // Add this method to ensure topics are updated when they're loaded
    fun updateTopicSavedStatus(topic: Topic): Topic {
        return if (_savedTopics.value.any { it.title == topic.title }) {
            topic.copy(isSaved = true)
        } else {
            topic.copy(isSaved = false)
        }
    }

    // Method to toggle entry expansion state
    fun toggleEntryExpansion(entryKey: Int) {
        _expandedEntries[entryKey] = !(_expandedEntries[entryKey] ?: false)
    }

    // Method to check if an entry is expanded
    fun isEntryExpanded(entryKey: Int): Boolean {
        return _expandedEntries[entryKey] ?: false
    }
    
    // Method to toggle entry favorite state
    fun toggleEntryFavorite(entryId: String, isFavorited: Boolean = false) {
        // Don't update local state, just send the request to the appropriate endpoint
        viewModelScope.launch {
            try {
                val success = EksiService.toggleEntryFavorite(entryId, isFavorited)
                // We don't update any local state - we'll rely on the server data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // For backward compatibility
    fun toggleEntryFavorite(entryId: String) {
        toggleEntryFavorite(entryId, false)
    }
    
    // Method to check if an entry is favorited
    fun isEntryFavorited(entryId: String): Boolean {
        return false  // Always return false - we'll use the Entry.isFavorited from server instead
    }
    
    // Method to clear all expanded entries
    fun clearExpandedEntries() {
        _expandedEntries.clear()
    }
    
    // Method to initialize the home page if it hasn't been loaded yet
    fun initializeHomePageIfNeeded() {
        if (!_hasInitializedHomePage.value) {
            _hasInitializedHomePage.value = true
            fetchTopics()
        }
    }

    // Method to preload content for a category without updating the UI
    fun preloadCategoryContent(category: String) {
        // Don't preload if it's the current category
        if (category == _selectedHomeCategory.value) {
            return
        }
        
        // Don't preload if we're already loading something
        if (_isLoading.value || _isLoadingMoreTopics.value) {
            return
        }
        
        // Don't eagerly preload during initial app launch
        // Only preload if the home page has been initialized
        if (!_hasInitializedHomePage.value) {
            return
        }
        
        // Start a background task to load the content
        viewModelScope.launch {
            try {
                // Fetch the data but don't update the UI
                val preloadedTopics = EksiService.getPopularTopics(1, category)
                
                // Store the preloaded data in a cache (if needed)
                // You could add a preloadedTopicsCache map here if you want to store and reuse this data
            } catch (e: Exception) {
                // Just log errors, don't show to the user since this is a background operation
            }
        }
    }
} 