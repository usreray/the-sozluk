package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiSession

/** Display a list of entries for a topic */
@Composable
fun EntriesDisplay(
        topic: Topic,
        currentPage: Int,
        scrollState: LazyListState,
        onScroll: (Boolean) -> Unit,
        isEntryExpanded: (String) -> Boolean,
        onToggleEntryExpansion: (String) -> Unit,
        onToggleEntryFavorite: (String) -> Unit,
        onLoginRequest: () -> Unit
) {
    val isLoggedIn by EksiSession.isLoggedIn
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        AlertDialog(
                onDismissRequest = { showLoginDialog = false },
                title = { Text("Giriş yapın") },
                text = { Text("Entry'leri favorilemek için ekşi sözlük hesabınızla giriş yapmanız gerekiyor.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showLoginDialog = false
                                onLoginRequest()
                            }
                    ) { Text("Giriş yap") }
                },
                dismissButton = { TextButton(onClick = { showLoginDialog = false }) { Text("Vazgeç") } }
        )
    }

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
                    onFavoriteToggle = {
                        if (isLoggedIn) onToggleEntryFavorite(entry.entryId)
                        else showLoginDialog = true
                    }
            )
        }
    }
}
