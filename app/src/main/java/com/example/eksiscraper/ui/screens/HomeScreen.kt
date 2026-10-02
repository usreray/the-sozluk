
package com.example.eksiscraper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.eksiscraper.ui.components.CategoryTabBar
import com.example.eksiscraper.ui.components.TopicListItem
import com.example.eksiscraper.ui.components.TopicListSkeleton
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.HomeViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val topics by viewModel.topics
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val isLoadingMoreTopics by viewModel.isLoadingMoreTopics
    val canLoadMoreTopics by viewModel.canLoadMoreTopics
    val scrollToTop by viewModel.scrollToTop
    val selectedCategory by viewModel.selectedCategory
    
    val swipeRefreshState = rememberSwipeRefreshState(isLoading)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Define categories
    val categories = listOf(
        "popular" to "Popular",
        "today" to "Today",
        "stream" to "Stream"
    )
    
    // Find the initial page based on the selected category
    val initialPage = categories.indexOfFirst { it.first == selectedCategory }.coerceAtLeast(0)
    
    // Create pager state for horizontal swipes with custom fling behavior
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { categories.size }
    )
    
    // Custom fling behavior to ensure we only change pages when swipe is completed
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1)
    )
    
    // Sync pager state with selected category when the selected category changes externally
    LaunchedEffect(selectedCategory) {
        val index = categories.indexOfFirst { it.first == selectedCategory }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    // Only update the selected category when page change has settled
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, offset) ->
                // Only update when the swipe has settled (offset is 0)
                if (offset == 0f) {
                    val category = categories.getOrNull(page)?.first ?: return@collect
                    if (category != selectedCategory) {
                        viewModel.updateCategory(category)
                    }
                }
            }
    }
    
    // Initialize the first page of content when the screen becomes visible
    LaunchedEffect(Unit) {
        if (topics.isEmpty() && !isLoading) {
            viewModel.fetchTopics()
        }
    }
    
    // Observe the scrollToTop state and scroll to top when it changes to true
    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            // Get the correct scroll state for the current page
            val currentCategory = categories[pagerState.currentPage].first
            val scrollState = viewModel.getCategoryScrollState(currentCategory)
            
            // Scroll to the top of the list
            scrollState.animateScrollToItem(0)
            
            // Reset the flag
            viewModel.resetScrollToTop()
        }
    }
    
    // Show FAB only when scrolled down
    val showFab by remember {
        derivedStateOf {
            // Get the correct scroll state for the current page
            val currentCategory = categories[pagerState.currentPage].first
            val scrollState = viewModel.getCategoryScrollState(currentCategory)
            
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0
        }
    }
    
    // Detect when we've scrolled to the bottom of the list
    val isAtBottom by remember {
        derivedStateOf {
            // Get the correct scroll state for the current page
            val currentCategory = categories[pagerState.currentPage].first
            val scrollState = viewModel.getCategoryScrollState(currentCategory)
            
            if (topics.isEmpty()) {
                false
            } else {
                val layoutInfo = scrollState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                
                if (visibleItemsInfo.isEmpty()) {
                    false
                } else {
                    val lastVisibleItem = visibleItemsInfo.last()
                    val lastVisibleItemIndex = lastVisibleItem.index
                    val totalItemsCount = topics.size
                    
                    // Consider we're at the bottom if we can see the last item
                    // or we're within 3 items of the end
                    lastVisibleItemIndex >= totalItemsCount - 3
                }
            }
        }
    }
    
    // Load more topics when we reach the bottom
    LaunchedEffect(isAtBottom, isLoading, isLoadingMoreTopics, canLoadMoreTopics) {
        if (isAtBottom && !isLoading && !isLoadingMoreTopics && canLoadMoreTopics) {
            viewModel.loadMoreTopics()
        }
    }
    
    // Method to scroll to the top of the home screen without reloading data
    fun scrollHomeToTop() {
        // Get the correct scroll state for the current category
        val currentCategory = categories[pagerState.currentPage].first
        val scrollState = viewModel.getCategoryScrollState(currentCategory)
        
        // Scroll to the top of the list
        scope.launch {
            scrollState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                FloatingActionButton(
                    onClick = { 
                        scrollHomeToTop()
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Add the CategoryTabBar at the top
            CategoryTabBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { category -> viewModel.updateCategory(category) },
                pagerState = pagerState
            )
            
            // Use HorizontalPager for the main content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                flingBehavior = flingBehavior
            ) { page ->
                // Get the category for this page
                val pageCategory = categories[page].first
                
                // Get the scroll state directly from the ViewModel
                val pageScrollState = viewModel.getCategoryScrollState(pageCategory)
                
                // The content inside each page is the same, it's just filtered by the selected category
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshTopics() },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Show loading indicator at the top while loading
                            AnimatedVisibility(
                                visible = isLoading && topics.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            
                            // Topic list
                            if (topics.isEmpty() && !isLoading) {
                                // Empty state
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No topics available.\nPull down to refresh.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = pageScrollState,  // Use the ViewModel's scroll state
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 8.dp,
                                        bottom = 88.dp // Extra padding for bottom nav
                                    )
                                ) {
                                    itemsIndexed(topics) { index, topic ->
                                        TopicListItem(
                                            topic = topic,
                                            onClick = { 
                                                val encodedTitle = URLEncoder.encode(topic.title, StandardCharsets.UTF_8.toString())
                                                val encodedUrl = URLEncoder.encode(topic.url, StandardCharsets.UTF_8.toString())
                                                navController.navigate("${Screen.TopicDetail.route}/$encodedTitle/$encodedUrl")
                                            }
                                        )
                                    }
                                    
                                    // Show loading indicator at the bottom when loading more topics
                                    if (isLoadingMoreTopics) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(32.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Show end of list message when no more topics can be loaded
                                    if (!canLoadMoreTopics && topics.isNotEmpty() && !isLoadingMoreTopics) {
                                        item {
                                            Text(
                                                text = "End of topics reached",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Center loading indicator when initially loading
                        if (isLoading && topics.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TopicListSkeleton(itemCount = 10)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
