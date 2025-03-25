package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.mutableStateMapOf
import com.example.eksiscraper.viewmodel.EksiViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.res.painterResource
import com.example.eksiscraper.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically


/**
 * Clean the topic title by removing entry numbers at the end
 */
private fun cleanTopicTitle(title: String): String {
    // Pattern to match a number at the end of the title, possibly with spaces before it
    val numberPattern = Regex("\\s+\\d+$")
    return title.replace(numberPattern, "")
}

/**
 * Shared topic header with page navigation used in both TopicDetailScreen and SearchScreen
 */
@Composable
fun TopicHeader(
    title: String,
    currentPage: Int,
    maxPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onShowPageDialog: () -> Unit,
    isPreviousEnabled: Boolean,
    isNextEnabled: Boolean,
    isSaved: Boolean,
    onSaveToggle: () -> Unit,
    isScrolled: Boolean = false
) {
    // Clean the title by removing entry numbers at the end
    val cleanTitle = cleanTopicTitle(title)
    
    // Animation for page navigation visibility - use a more stable animation configuration
    val pageNavHeightPx by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 60.dp,  // Height when visible
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,  // Low bouncy for quick response
            stiffness = Spring.StiffnessHigh           // Faster response
        ),
        label = "pageNavHeight"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and save button section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Save/Unsave button
                IconButton(onClick = onSaveToggle) {
                    Icon(
                        painter = painterResource(
                            id = if (isSaved) R.drawable.bookmark_24px_filled 
                                else R.drawable.bookmark_24px
                        ),
                        contentDescription = if (isSaved) "Unsave Topic" else "Save Topic",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Only show page navigation with animation when visible
            AnimatedVisibility(
                visible = !isScrolled,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutLinearInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 150)
                )
            ) {
                Column {
                    // Add a divider to separate title from page navigation
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Divider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Page navigation section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous page button - just icon
                        IconButton(
                            onClick = onPreviousPage,
                            enabled = isPreviousEnabled,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (isPreviousEnabled) 
                                       MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        // Page info and change page button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = onShowPageDialog,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "$currentPage/$maxPages",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        // Next page button - just icon
                        IconButton(
                            onClick = onNextPage,
                            enabled = isNextEnabled,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Next Page",
                                tint = if (isNextEnabled) 
                                       MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared loading indicator used in both TopicDetailScreen and SearchScreen
 */
@Composable
fun LoadingIndicator(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Shared entries display used in both TopicDetailScreen and SearchScreen
 */
@Composable
fun EntriesDisplay(
    topic: Topic,
    currentPage: Int,
    viewModel: EksiViewModel,
    scrollState: LazyListState,
    onScroll: (Boolean) -> Unit = {}
) {
    // Track previous scroll position to determine scroll direction
    val previousScrollOffset = remember { mutableStateOf(0) }
    val previousFirstVisibleIndex = remember { mutableStateOf(0) }
    val isScrollingUp = remember { mutableStateOf(false) }
    
    // Check if we're at the top
    val isAtTop = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
    
    // Check if we're at the end of the list
    val isAtEnd = remember { mutableStateOf(false) }
    
    // Track if we're scrolling too fast
    val lastScrollUpdateTime = remember { mutableStateOf(System.currentTimeMillis()) }
    val isScrollingFast = remember { mutableStateOf(false) }
    
    // Track if we should hide controls to prevent flickering
    val shouldHideControls = remember { mutableStateOf(false) }

    // Use derivedStateOf to efficiently calculate if we're near the end of the list
    val reachedEnd = remember {
        derivedStateOf {
            if (topic.entries.isEmpty()) {
                false
            } else {
                val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItems = topic.entries.size
                
                // Check if the last item is visible and we're near the end
                lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
            }
        }
    }
    
    // Update isAtEnd based on reachedEnd derived state
    LaunchedEffect(reachedEnd.value) {
        isAtEnd.value = reachedEnd.value
        
        // If we reached the end while scrolling fast and not scrolling up,
        // ensure we keep controls hidden to prevent flickering
        if (reachedEnd.value && isScrollingFast.value && !isScrollingUp.value) {
            shouldHideControls.value = true
        }
    }
    
    // Create a derived state for scrolling down (opposite of scrolling up)
    // This helps us handle the fast scroll down case better
    val isScrollingDown = remember { mutableStateOf(false) }
    
    // Determine if we should show navigation controls
    // Show when at top or when scrolling up (but use our anti-flicker detection)
    val shouldShowControls = isAtTop || (isScrollingUp.value && !shouldHideControls.value)
    
    // Additional effect to handle when scroll jumps past our detection
    LaunchedEffect(scrollState.firstVisibleItemIndex) {
        // When item index changes significantly, it's a fast scroll
        val currentFirstVisibleIndex = scrollState.firstVisibleItemIndex
        val previousIndex = previousFirstVisibleIndex.value
        
        // If we jump by more than 2 items at once, consider it a fast scroll
        if (Math.abs(currentFirstVisibleIndex - previousIndex) > 2) {
            isScrollingFast.value = true
            
            // If we're scrolling down fast, update controls immediately
            if (currentFirstVisibleIndex > previousIndex) {
                isScrollingDown.value = true
                isScrollingUp.value = false
                shouldHideControls.value = true  // Set our anti-flicker flag
                onScroll(true) // Hide controls immediately on fast scroll down
            }
        } else {
            // Not scrolling fast
            isScrollingFast.value = false
            
            // If we're scrolling up, we can reset the anti-flicker flag
            if (currentFirstVisibleIndex < previousIndex) {
                shouldHideControls.value = false
            }
        }
    }
    
    // Detect scroll change and direction
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastScrollUpdateTime.value
        lastScrollUpdateTime.value = currentTime
        
        val currentOffset = scrollState.firstVisibleItemScrollOffset
        val currentFirstVisibleIndex = scrollState.firstVisibleItemIndex
        
        // Determine scroll direction with more reliable logic
        val movingUp = if (currentFirstVisibleIndex == previousFirstVisibleIndex.value) {
            // If we're on the same item, compare offsets
            currentOffset < previousScrollOffset.value
        } else {
            // If we've changed items, check if we're moving up in the list
            currentFirstVisibleIndex < previousFirstVisibleIndex.value
        }
        
        // Track both up and down directions explicitly
        isScrollingUp.value = movingUp
        isScrollingDown.value = !movingUp
        
        // Update our anti-flicker flag
        if (movingUp) {
            // If explicitly scrolling up, we can reset the anti-flicker flag
            // But only after a small delay to prevent flickering during momentum scrolls
            if (deltaTime > 100) {
                shouldHideControls.value = false
            }
        } else if (deltaTime < 50 || isScrollingFast.value) {
            // If scrolling down fast, set the anti-flicker flag
            shouldHideControls.value = true
        }
        
        // If scrolling down fast, make sure we hide controls
        if (!movingUp && (deltaTime < 50 || isScrollingFast.value)) {
            onScroll(true) // Hide controls on fast scroll down
        } else {
            // Normal scroll behavior
            onScroll(!shouldShowControls)
        }
        
        // Store current values for next comparison
        previousScrollOffset.value = currentOffset
        previousFirstVisibleIndex.value = currentFirstVisibleIndex
    }
    
    // Reset shouldHideControls when at the top
    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            // If we're at the top, always reset the anti-flicker flag
            shouldHideControls.value = false
            onScroll(false) // Force show controls at top
        }
    }
    
    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (topic.entries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No entries",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "No entries found for \"${topic.title}\" on page $currentPage",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Display entries
            itemsIndexed(topic.entries) { index, entry ->
                // Generate a unique key for each entry
                val entryKey = ((currentPage - 1) * 10) + index + 1
                
                EntryItem(
                    entry = entry, 
                    index = entryKey,
                    isExpanded = viewModel.isEntryExpanded(entryKey),
                    onExpandToggle = { viewModel.toggleEntryExpansion(entryKey) },
                    isFavorite = entry.isFavorited,
                    onFavoriteToggle = { entryId -> viewModel.toggleEntryFavorite(entryId, entry.isFavorited) }
                )
            }
            
            // Add empty entry card-sized space at the bottom to match the spacing
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Shared error display used in both TopicDetailScreen and SearchScreen
 */
@Composable
fun ErrorDisplay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Category tab bar for the home screen to switch between different topic categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "popular" to "Popular",
        "today" to "Today",
        "stream" to "Stream"
    )
    
    val coroutineScope = rememberCoroutineScope()
    
    // Determine if a swipe is in progress
    val isSwipeInProgress by remember {
        derivedStateOf {
            pagerState.currentPageOffsetFraction != 0f
        }
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab row for the category tabs
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                }
            ) {
                categories.forEachIndexed { index, (key, label) ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            // Only allow tab selection when not swiping
                            if (!isSwipeInProgress) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    }
} 