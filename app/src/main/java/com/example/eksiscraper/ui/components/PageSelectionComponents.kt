package com.example.eksiscraper.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Modern page selection dialog with vertically scrollable page selector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageSelectionDialog(
    currentPage: Int,
    maxPages: Int,
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Track whether scrolling is currently in progress
    val isScrollInProgress = listState.isScrollInProgress
    
    // Track whether we should snap to center
    val shouldSnapToCenter = remember { mutableStateOf(false) }
    
    // Track the last user-selected page to prevent continuous auto-selection
    val lastSelectedPage = remember { mutableStateOf(selectedPage) }
    
    // Track whether this is a programmatic scroll to prevent feedback loops
    val isProgrammaticScroll = remember { mutableStateOf(false) }
    
    // Prevent immediate auto-selection when dialog first opens
    val hasInitialized = remember { mutableStateOf(false) }
    
    // Add a debounce flag to prevent rapid selection changes
    val isSelectionDebouncing = remember { mutableStateOf(false) }
    
    // Add internal state to store the actual displayed selection
    val internalSelectedPage = remember { mutableStateOf(selectedPage) }
    
    // Add a state to track the last time scrolling stopped
    val lastScrollStopTime = remember { mutableStateOf(0L) }
    
    // Add a state to track if we need forced snapping
    val needsSnapping = remember { mutableStateOf(false) }
    
    // Update internal state when external selection changes
    LaunchedEffect(selectedPage) {
        internalSelectedPage.value = selectedPage
    }
    
    // Set initial scroll position when dialog opens with animation
    LaunchedEffect(Unit) {
        // For wrapped list, we position at virtual center (actual page + middle offset)
        val initialIndex = ((Int.MAX_VALUE / 2) / maxPages) * maxPages + (selectedPage - 1)
        isProgrammaticScroll.value = true
        
        // Use animateScrollToItem for smooth initial animation
        listState.scrollToItem(index = initialIndex)
        delay(30) // Brief delay
        hasInitialized.value = true
        isProgrammaticScroll.value = false
    }
    
    // Effect to center selected page when it changes
    LaunchedEffect(internalSelectedPage.value) {
        if (hasInitialized.value && !isProgrammaticScroll.value) {
            // Only animate if this wasn't caused by our own scrolling detection
            isProgrammaticScroll.value = true
            
            try {
                // Apply a debounce period
                isSelectionDebouncing.value = true
                
                // Get current first visible item
                val currentIndex = listState.firstVisibleItemIndex
                // Calculate the offset to maintain same relative position in the virtual list
                val baseIndex = (currentIndex / maxPages) * maxPages
                // Calculate target index in the infinite list
                val targetIndex = baseIndex + (internalSelectedPage.value - 1)
                
                // Smoothly animate to center the selected page
                listState.animateScrollToItem(
                    index = targetIndex
                )
                
                delay(150) // Wait for animation to complete
            } finally {
                // Make sure we reset these states even if something fails
                delay(50) // Extra delay to ensure stability
                isProgrammaticScroll.value = false
                isSelectionDebouncing.value = false
            }
        }
    }
    
    // Detect when scrolling stops and select centered page
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            // Reset the snapping flag when scrolling starts again
            needsSnapping.value = true
        } else if (hasInitialized.value && !isProgrammaticScroll.value && 
            !isSelectionDebouncing.value && listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            
            // Record the time when scrolling stopped
            val currentTime = System.currentTimeMillis()
            lastScrollStopTime.value = currentTime
            
            // Add a delay before processing to ensure scroll has truly settled
            delay(50)
            
            // Only proceed if this is still the most recent scroll stop
            if (currentTime == lastScrollStopTime.value && needsSnapping.value) {
                needsSnapping.value = false
                
                // Find visible center position
                val layoutInfo = listState.layoutInfo
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val viewportCenter = layoutInfo.viewportStartOffset + (viewportHeight / 2)
                
                // Find the item closest to the center of the viewport
                val visibleItems = layoutInfo.visibleItemsInfo
                val centerItem = visibleItems.minByOrNull { 
                    abs((it.offset + (it.size / 2)) - viewportCenter)
                }
                
                // Process the center item if found
                if (centerItem != null) {
                    // For infinite list, convert virtual index to page number
                    val virtualPageNumber = (centerItem.index % maxPages) + 1
                    
                    // Check if we need to snap to the center more precisely
                    val itemCenter = centerItem.offset + (centerItem.size / 2)
                    val distanceFromCenter = abs(itemCenter - viewportCenter)
                    
                    // Handle different centering scenarios
                    when {
                        // If we're way off center, snap to it first
                        distanceFromCenter > centerItem.size * 0.2 -> {  // If more than 20% off center
                            isProgrammaticScroll.value = true
                            isSelectionDebouncing.value = true
                            
                            coroutineScope.launch {
                                // Snap to the nearest item properly
                                listState.animateScrollToItem(
                                    index = centerItem.index
                                )
                                delay(100)
                                
                                // Now update the selection after snapping
                                internalSelectedPage.value = virtualPageNumber
                                onPageSelected(virtualPageNumber)
                                
                                // Reset states
                                delay(50)
                                isProgrammaticScroll.value = false
                                isSelectionDebouncing.value = false
                            }
                        }
                        
                        // If we're centered but need to update selection
                        virtualPageNumber != internalSelectedPage.value -> {
                            isSelectionDebouncing.value = true
                            internalSelectedPage.value = virtualPageNumber
                            onPageSelected(virtualPageNumber)
                            
                            // Special handling for manual scroll between last and first page
                            // After scroll has stopped, ensure the scroll position is reset to a good range
                            // Find the item closest to the middle of the virtual list range
                            val optimalBaseIndex = ((Int.MAX_VALUE / 2) / maxPages) * maxPages
                            val middleCycleIndex = optimalBaseIndex + (virtualPageNumber - 1)
                            
                            // If we are far from optimal position, silently scroll back to optimal range
                            val currentCycleIndex = (centerItem.index / maxPages)
                            val optimalCycleIndex = (optimalBaseIndex / maxPages)
                            val cycleDifference = abs(currentCycleIndex - optimalCycleIndex)
                            
                            if (cycleDifference > 10) {  // If we're more than 10 cycles from optimal
                                coroutineScope.launch {
                                    isProgrammaticScroll.value = true
                                    // Instant jump to the optimal range without animation
                                    listState.scrollToItem(index = middleCycleIndex)
                                    delay(30)
                                    isProgrammaticScroll.value = false
                                }
                            }
                            
                            // Reset debounce after a short delay
                            delay(100)
                            isSelectionDebouncing.value = false
                        }
                        
                        // Otherwise, we're centered and selection is correct - do nothing
                        else -> { /* No action needed */ }
                    }
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Page",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page selector
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Center indicator
                    Card(
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(0.8f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) { }
                    
                    // Simple lazy column with actual page numbers - wrapped for infinite scrolling
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 72.dp)
                    ) {
                        // Use a large number of items to simulate infinite scrolling
                        // Start from a large offset to allow scrolling "infinitely" in both directions
                        items(Int.MAX_VALUE) { virtualIndex ->
                            // Map the virtual index to a real page number
                            val pageNumber = (virtualIndex % maxPages) + 1
                            val isSelected = pageNumber == internalSelectedPage.value
                            
                            PageNumberItem(
                                pageNumber = pageNumber,
                                isSelected = isSelected,
                                onClick = { 
                                    // Simplify the click handler to avoid race conditions
                                    if (!isSelectionDebouncing.value) {
                                        isSelectionDebouncing.value = true
                                        internalSelectedPage.value = pageNumber
                                        onPageSelected(pageNumber)
                                        
                                        // Programmatically scroll to this position
                                        coroutineScope.launch {
                                            isProgrammaticScroll.value = true
                                            
                                            try {
                                                // Find the closest instance of this page number
                                                val currentIndex = listState.firstVisibleItemIndex
                                                val baseIndex = (currentIndex / maxPages) * maxPages
                                                val targetIndex = baseIndex + (pageNumber - 1)
                                                
                                                // Special case for max page to first page and vice versa to make transitions smoother
                                                val currentVisiblePageNumber = (currentIndex % maxPages) + 1
                                                
                                                // If we're jumping between max page and first page or vice versa, use a different cycle
                                                if ((currentVisiblePageNumber == maxPages && pageNumber == 1) || 
                                                    (currentVisiblePageNumber == 1 && pageNumber == maxPages)) {
                                                    
                                                    // For transition from max to 1, go to the next cycle's first page
                                                    // For transition from 1 to max, go to the previous cycle's max page
                                                    val newBaseIndex = if (currentVisiblePageNumber == maxPages) {
                                                        baseIndex + maxPages // Next cycle
                                                    } else {
                                                        baseIndex - maxPages // Previous cycle
                                                    }
                                                    
                                                    val newTargetIndex = newBaseIndex + (pageNumber - 1)
                                                    listState.animateScrollToItem(
                                                        index = newTargetIndex
                                                    )
                                                } else {
                                                    // Normal case - same cycle
                                                    listState.animateScrollToItem(
                                                        index = targetIndex
                                                    )
                                                }
                                                
                                                delay(150) // Allow animation to complete
                                            } finally {
                                                // Ensure flags are reset
                                                isProgrammaticScroll.value = false
                                                isSelectionDebouncing.value = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Center both buttons with Row layout
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        enabled = internalSelectedPage.value >= 1 && internalSelectedPage.value <= maxPages && internalSelectedPage.value != currentPage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        },
        dismissButton = null
    )
}

/**
 * Individual page number item for the page selector
 */
@Composable
private fun PageNumberItem(
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(
            durationMillis = 100,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "scale"
    )
    
    // Use a simple color selection instead of animation
    val color = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Removes ripple effect
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pageNumber.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
} 