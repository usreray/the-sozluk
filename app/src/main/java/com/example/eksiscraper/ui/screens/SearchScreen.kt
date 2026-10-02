package com.example.eksiscraper.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.ui.components.EntriesDisplay
import com.example.eksiscraper.ui.components.EntriesSkeleton
import com.example.eksiscraper.ui.components.PageSelectionDialog
import com.example.eksiscraper.ui.components.TopicHeader
import com.example.eksiscraper.ui.components.TopicListSkeleton
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
        navController: NavController? = null, // Optional for now if not used for navigation yet
        viewModel: SearchViewModel =
                viewModel(
                        factory =
                                EksiViewModelFactory(
                                        LocalContext.current.applicationContext as Application
                                )
                )
) {
        val searchQuery by viewModel.searchQuery
        val searchResult by viewModel.searchResult
        val isSearching by viewModel.isSearching
        val currentPage by viewModel.currentPage
        val suggestions by viewModel.suggestions

        // Local state for page dialog since it's UI specific
        val isPageDialogVisible = remember { mutableStateOf(false) }
        val selectedPage = remember { mutableStateOf(1) }

        // Add state to track scroll position
        val isScrolledState = remember { mutableStateOf(false) }
        val isScrolled = isScrolledState.value

        // Handle back gesture when search results are displayed
        BackHandler(enabled = searchResult != null) { viewModel.clearSearch() }

        // Get total pages from the search result, with a minimum of the current page
        val totalPages = (searchResult?.totalPages ?: 1).coerceAtLeast(currentPage)

        // Page selection dialog
        if (isPageDialogVisible.value) {
                PageSelectionDialog(
                        currentPage = currentPage,
                        maxPages = totalPages,
                        selectedPage = selectedPage.value,
                        onPageSelected = { selectedPage.value = it },
                        onConfirm = {
                                viewModel.search(selectedPage.value)
                                isPageDialogVisible.value = false
                        },
                        onDismiss = { isPageDialogVisible.value = false }
                )
        }

        Column(modifier = Modifier.fillMaxSize()) {
                // Only show search container if no results are displayed
                // Don't show it when changing pages of an existing search result
                if (searchResult == null) {
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                                text = "Search",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                OutlinedTextField(
                                                        value = searchQuery,
                                                        onValueChange = {
                                                                viewModel.updateSearchQuery(it)
                                                        },
                                                        label = { Text("Enter search term") },
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .padding(end = 8.dp),
                                                        singleLine = true,
                                                        keyboardOptions =
                                                                KeyboardOptions(
                                                                        imeAction = ImeAction.Search
                                                                ),
                                                        keyboardActions =
                                                                KeyboardActions(
                                                                        onSearch = {
                                                                                viewModel.search()
                                                                        }
                                                                ),
                                                        colors =
                                                                TextFieldDefaults.colors(
                                                                        focusedIndicatorColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        unfocusedIndicatorColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline,
                                                                        focusedLabelColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        cursorColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                ),
                                                        leadingIcon = {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .Search,
                                                                        contentDescription =
                                                                                "Search",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.7f
                                                                                        )
                                                                )
                                                        }
                                                )

                                                OutlinedButton(
                                                        onClick = { viewModel.search() },
                                                        enabled =
                                                                searchQuery.isNotEmpty() &&
                                                                        !isSearching,
                                                        border =
                                                                ButtonDefaults.outlinedButtonBorder
                                                                        .copy(
                                                                                brush =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .SolidColor(
                                                                                                        if (searchQuery
                                                                                                                        .isNotEmpty() &&
                                                                                                                        !isSearching
                                                                                                        )
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary
                                                                                                        else
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .outline
                                                                                                )
                                                                        ),
                                                        colors =
                                                                ButtonDefaults.outlinedButtonColors(
                                                                        contentColor =
                                                                                if (searchQuery
                                                                                                .isNotEmpty() &&
                                                                                                !isSearching
                                                                                )
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.38f
                                                                                                )
                                                                )
                                                ) { Text("Search") }
                                        }

                                        if (suggestions.isNotEmpty() && !isSearching) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                                                        items(suggestions) { suggestion ->
                                                                Text(
                                                                        text = suggestion,
                                                                        style =
                                                                                MaterialTheme.typography
                                                                                        .bodyLarge,
                                                                        color =
                                                                                MaterialTheme.colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .clickable {
                                                                                                viewModel
                                                                                                        .selectSuggestion(
                                                                                                                suggestion
                                                                                                        )
                                                                                        }
                                                                                        .padding(
                                                                                                vertical =
                                                                                                        12.dp,
                                                                                                horizontal =
                                                                                                        4.dp
                                                                                        )
                                                                )
                                                                HorizontalDivider()
                                                        }
                                                }
                                        }
                                }
                        }

                        // Show loading indicator for initial search
                        if (isSearching) {
                                TopicListSkeleton(itemCount = 5)
                        }
                } else {
                        // Display search results
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                if (isSearching) {
                                        EntriesSkeleton(itemCount = 5)
                                } else {
                                        // Topic title with page selector
                                        TopicHeader(
                                                title = searchResult?.title ?: "",
                                                currentPage = currentPage,
                                                maxPages = totalPages,
                                                onPreviousPage = {
                                                        if (currentPage > 1) {
                                                                viewModel.search(currentPage - 1)
                                                        }
                                                },
                                                onNextPage = { viewModel.search(currentPage + 1) },
                                                onShowPageDialog = {
                                                        selectedPage.value = currentPage
                                                        isPageDialogVisible.value = true
                                                },
                                                isPreviousEnabled = currentPage > 1,
                                                isNextEnabled =
                                                        true, // Always enable next page for search
                                                // results
                                                isSaved = searchResult?.isSaved ?: false,
                                                onSaveToggle = {
                                                        searchResult?.let { topic ->
                                                                if (topic.isSaved) {
                                                                        viewModel.unsaveTopic(topic)
                                                                } else {
                                                                        viewModel.saveTopic(topic)
                                                                }
                                                        }
                                                },
                                                isScrolled = isScrolled
                                        )

                                        // Entries
                                        searchResult?.let { result ->
                                                EntriesDisplay(
                                                        topic = result,
                                                        currentPage = currentPage,
                                                        scrollState = viewModel.scrollState,
                                                        onScroll = { scrolled ->
                                                                isScrolledState.value = scrolled
                                                        },
                                                        isEntryExpanded = { entryId ->
                                                                viewModel.isEntryExpanded(entryId)
                                                        },
                                                        onToggleEntryExpansion = { entryId ->
                                                                viewModel.toggleEntryExpansion(
                                                                        entryId
                                                                )
                                                        },
                                                        onToggleEntryFavorite = { entryId ->
                                                                viewModel.toggleEntryFavorite(entryId)
                                                        },
                                                        onLoginRequest = {
                                                                navController?.navigate(Screen.Login.route)
                                                        }
                                                )
                                        }
                                }
                        }
                }
        }
}
