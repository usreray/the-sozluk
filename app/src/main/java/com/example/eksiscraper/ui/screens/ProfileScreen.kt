package com.example.eksiscraper.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.ui.components.EntriesDisplay
import com.example.eksiscraper.ui.components.EntriesSkeleton
import com.example.eksiscraper.ui.components.LoadingIndicator
import com.example.eksiscraper.ui.components.PageSelectionDialog
import com.example.eksiscraper.ui.components.SavedTopicsListSkeleton
import com.example.eksiscraper.ui.components.TopicHeader
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: EksiViewModel = viewModel()
) {
    val savedTopics by viewModel.savedTopics
    val isSearching by viewModel.isSearching
    val viewingSavedTopic by viewModel.viewingSavedTopic
    val isViewingSavedTopic by viewModel.isViewingSavedTopic
    val savedTopicCurrentPage by viewModel.savedTopicCurrentPage
    val isPageDialogVisible by viewModel.isPageDialogVisible
    val selectedPage by viewModel.selectedPage
    
    // Set the active screen
    LaunchedEffect(Unit) {
        println("ProfileScreen: Setting active screen to profile")
        viewModel.setActiveScreen("profile")
        // No need to call clearSelectedTopic here as setActiveScreen already does it
    }
    
    // Handle back press when viewing a saved topic
    BackHandler(enabled = isViewingSavedTopic) {
        viewModel.stopViewingSavedTopic()
    }
    
    // Calculate total pages from the viewing topic
    val totalPages = (viewingSavedTopic?.totalPages ?: 1).coerceAtLeast(savedTopicCurrentPage)
    
    // Page selection dialog
    if (isPageDialogVisible) {
        PageSelectionDialog(
            currentPage = savedTopicCurrentPage,
            maxPages = totalPages,
            selectedPage = selectedPage,
            onPageSelected = { viewModel.updateSelectedPage(it) },
            onConfirm = { 
                viewModel.applySelectedPage()
                viewModel.hidePageDialog()
            },
            onDismiss = { viewModel.hidePageDialog() }
        )
    }
    
    Scaffold(
        // No top app bar to match Home and Search screens
    ) { paddingValues ->
        if (isViewingSavedTopic) {
            // Show topic content without applying paddingValues to match TopicDetailScreen
            Column(
                modifier = Modifier.fillMaxSize()
                // Removed paddingValues to match TopicDetailScreen structure
            ) {
                if (isSearching) {
                    // Use the same approach as TopicDetailScreen for skeleton loading
                    EntriesSkeleton(itemCount = 5)
                } else if (viewingSavedTopic != null) {
                    // Topic title with page selector - no nested Column
                    TopicHeader(
                        title = viewingSavedTopic?.title ?: "",
                        currentPage = savedTopicCurrentPage,
                        maxPages = totalPages,
                        onPreviousPage = { 
                            if (savedTopicCurrentPage > 1) {
                                viewingSavedTopic?.let { topic ->
                                    viewModel.viewSavedTopic(topic, savedTopicCurrentPage - 1)
                                }
                            }
                        },
                        onNextPage = { 
                            viewingSavedTopic?.let { topic ->
                                viewModel.viewSavedTopic(topic, savedTopicCurrentPage + 1)
                            }
                        },
                        onShowPageDialog = { viewModel.showPageDialog() },
                        isPreviousEnabled = savedTopicCurrentPage > 1,
                        isNextEnabled = true,  // Always enable next page
                        isSaved = true,  // Always true since we're in the saved topics section
                        onSaveToggle = {
                            viewingSavedTopic?.let { topic ->
                                viewModel.unsaveTopic(topic)
                                // Return to the saved topics list if a topic is unsaved
                                viewModel.stopViewingSavedTopic()
                            }
                        }
                    )
                    
                    // Entries - directly match TopicDetailScreen structure
                    viewingSavedTopic?.let { topic ->
                        EntriesDisplay(
                            topic = topic,
                            currentPage = savedTopicCurrentPage,
                            viewModel = viewModel,
                            scrollState = viewModel.profileScrollState
                        )
                    }
                }
            }
        } else {
            // Show saved topics list or loading skeleton
            if (savedTopics.isEmpty()) {
                // Check if we're loading or truly empty
                val isLoading by viewModel.isLoading
                if (isLoading) {
                    // Show skeleton loading for saved topics
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        SavedTopicsListSkeleton(itemCount = 5)
                    }
                } else {
                    // Show empty state message
                    EmptySavedTopicsMessage(paddingValues)
                }
            } else {
                SavedTopicsList(
                    savedTopics = savedTopics,
                    onTopicSelected = { topic ->
                        viewModel.viewSavedTopic(topic)
                    },
                    viewModel = viewModel,
                    paddingValues = paddingValues,
                    onBackToList = { 
                        viewModel.stopViewingSavedTopic()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySavedTopicsMessage(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Saved Topics",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Topics you save will appear here for easy access",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedTopicsList(
    savedTopics: List<Topic>,
    onTopicSelected: (Topic) -> Unit,
    viewModel: EksiViewModel,
    paddingValues: PaddingValues,
    onBackToList: () -> Unit
) {
    LazyColumn(
        state = viewModel.profileScrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved Topics",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        
        items(savedTopics) { topic ->
            ElevatedCard(
                onClick = { onTopicSelected(topic) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = { viewModel.unsaveTopic(topic) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Unsave topic",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (topic.commentCount > 0) {
                        Text(
                            text = "${topic.commentCount} entries",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 