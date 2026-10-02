package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Topic

/** Display a list of entries for a topic */
@Composable
fun EntriesDisplay(
        topic: Topic,
        currentPage: Int,
        scrollState: LazyListState,
        onScroll: (Boolean) -> Unit,
        isEntryExpanded: (String) -> Boolean,
        onToggleEntryExpansion: (String) -> Unit,
        onToggleEntryFavorite: (String, Boolean) -> Unit
) {
    // Track scroll state to hide/show navigation
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        // Simple logic: if we've scrolled past the first item or have significant offset, consider
        // it scrolled
        // This can be refined based on scroll direction if needed
        val isScrolled =
                scrollState.firstVisibleItemIndex > 0 ||
                        scrollState.firstVisibleItemScrollOffset > 50
        onScroll(isScrolled)
    }

    LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                            bottom = 80.dp
                    ) // Space for FAB or bottom nav
    ) {
        itemsIndexed(topic.entries) { index, entry ->
            EntryItem(
                    entry = entry,
                    index = (currentPage - 1) * 10 + index + 1, // Calculate global index
                    isExpanded = isEntryExpanded(entry.entryId),
                    onExpandToggle = { onToggleEntryExpansion(entry.entryId) },
                    isFavorite = entry.isFavorited,
                    onFavoriteToggle = { _ ->
                        // The EntryItem handles the toggle logic internally and calls this callback
                        // We just need to pass it through if the parent needs to know
                        onToggleEntryFavorite(entry.entryId, !entry.isFavorited)
                    }
            )
        }
    }
}
