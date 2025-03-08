package com.example.eksiscraper.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eksiscraper.ui.components.EntriesDisplay
import com.example.eksiscraper.ui.components.EntriesSkeleton
import com.example.eksiscraper.ui.components.LoadingIndicator
import com.example.eksiscraper.ui.components.PageSelectionDialog
import com.example.eksiscraper.ui.components.TopicHeader
import com.example.eksiscraper.ui.components.TopicListSkeleton
import com.example.eksiscraper.viewmodel.EksiViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(viewModel: EksiViewModel = viewModel()) {
    val searchQuery by viewModel.searchQuery
    val searchResult by viewModel.searchResult
    val isSearching by viewModel.isSearching
    val currentPage by viewModel.currentPage
    val isPageDialogVisible by viewModel.isPageDialogVisible
    val selectedPage by viewModel.selectedPage
    val context = LocalContext.current
    
    // Set the active screen
    LaunchedEffect(Unit) {
        println("SearchScreen: Setting active screen to search")
        viewModel.setActiveScreen("search")
        // No need to call clearSelectedTopic here as setActiveScreen already does it
    }
    
    // Handle back gesture when search results are displayed
    BackHandler(enabled = searchResult != null) {
        println("SearchScreen: Back pressed, clearing search results")
        viewModel.clearSearch()
    }
    
    // Get total pages from the search result, with a minimum of the current page
    val totalPages = (searchResult?.totalPages ?: 1).coerceAtLeast(currentPage)
    
    // Page selection dialog
    if (isPageDialogVisible) {
        PageSelectionDialog(
            currentPage = currentPage,
            maxPages = totalPages,
            selectedPage = selectedPage,
            onPageSelected = { viewModel.updateSelectedPage(it) },
            onConfirm = { viewModel.applySelectedPage() },
            onDismiss = { viewModel.hidePageDialog() }
        )
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
        // No paddingValues applied here to ensure consistent positioning across all screens
    ) {
        // Only show search container if no results are displayed
        // Don't show it when changing pages of an existing search result
        if (searchResult == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Enter search term") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { viewModel.search() }
                            ),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        )
                        
                        OutlinedButton(
                            onClick = { viewModel.search() },
                            enabled = searchQuery.isNotEmpty() && !isSearching,
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (searchQuery.isNotEmpty() && !isSearching) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outline
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (searchQuery.isNotEmpty() && !isSearching)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            Text("Search")
                        }
                    }
                }
            }
            
            // Show loading indicator for initial search
            if (isSearching) {
                // Replace LoadingIndicator with TopicListSkeleton for initial search
                TopicListSkeleton(itemCount = 5)
            }
        } else {
            // Display search results
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isSearching) {
                    // Use the same approach as TopicDetailScreen and ProfileScreen for skeleton loading
                    EntriesSkeleton(itemCount = 5)
                } else {
                    // Topic title with page selector - no nested Column
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
                        onShowPageDialog = { viewModel.showPageDialog() },
                        isPreviousEnabled = currentPage > 1,
                        isNextEnabled = true,  // Always enable next page for search results
                        isSaved = searchResult?.isSaved ?: false,
                        onSaveToggle = {
                            searchResult?.let { topic ->
                                if (topic.isSaved) {
                                    viewModel.unsaveTopic(topic)
                                } else {
                                    viewModel.saveTopic(topic)
                                }
                            }
                        }
                    )
                    
                    // Entries
                    searchResult?.let { result ->
                        EntriesDisplay(
                            topic = result,
                            currentPage = currentPage
                        )
                    }
                }
            }
        }
    }
} 