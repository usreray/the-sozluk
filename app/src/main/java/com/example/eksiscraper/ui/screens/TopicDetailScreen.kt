package com.example.eksiscraper.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.ui.components.EntriesDisplay
import com.example.eksiscraper.ui.components.ErrorDisplay
import com.example.eksiscraper.ui.components.LoadingIndicator
import com.example.eksiscraper.ui.components.PageSelectionDialog
import com.example.eksiscraper.ui.components.TopicHeader
import com.example.eksiscraper.viewmodel.EksiViewModel
import kotlin.math.ceil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopicDetailScreen(
    topicIndex: Int,
    navController: NavController,
    viewModel: EksiViewModel = viewModel()
) {
    val topics by viewModel.topics
    val isLoading by viewModel.isLoading
    val selectedTopic by viewModel.selectedTopic
    val currentPage by viewModel.topicCurrentPage
    val isPageDialogVisible by viewModel.isPageDialogVisible
    val selectedPage by viewModel.selectedPage
    val isLoadingTopic by viewModel.isLoadingTopic
    val activeScreen by viewModel.activeScreen
    
    // Set the active screen
    LaunchedEffect(Unit) {
        println("TopicDetailScreen: Setting active screen to topic_detail")
        viewModel.setActiveScreen("topic_detail")
        
        // Add a small delay to ensure the active screen state is updated before selecting a topic
        kotlinx.coroutines.delay(100)
        
        // Ensure the topic is selected only if we're on the topic detail screen and no topic is currently selected
        if (selectedTopic == null && topics.isNotEmpty() && topicIndex < topics.size && 
            viewModel.activeScreen.value == "topic_detail") {
            println("TopicDetailScreen: Selecting topic at index $topicIndex")
            viewModel.selectTopic(topicIndex, 1)
        } else if (selectedTopic != null) {
            // If a topic is already selected, just ensure we're on the topic detail screen
            println("TopicDetailScreen: Topic already selected: ${selectedTopic?.title ?: "Unknown"}, no need to reload")
        }
    }
    
    // Calculate max pages based on comment count (10 entries per page)
    val maxPages = selectedTopic?.let { 
        ceil(it.commentCount.toFloat() / 10).toInt().coerceAtLeast(1)
    } ?: 1
    
    // Page selection dialog
    if (isPageDialogVisible) {
        PageSelectionDialog(
            currentPage = currentPage,
            maxPages = maxPages,
            selectedPage = selectedPage,
            onPageSelected = { viewModel.updateSelectedPage(it) },
            onConfirm = { 
                viewModel.navigateTopicToPage(selectedPage)
                viewModel.hidePageDialog()
            },
            onDismiss = { viewModel.hidePageDialog() }
        )
    }
    
    // Handle back press to return to home screen
    BackHandler {
        viewModel.clearSelectedTopic()
        navController.popBackStack()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Loading indicator
        if (isLoadingTopic) {
            LoadingIndicator(message = "Loading entries...")
        } else if (selectedTopic != null) {
            // Display topic details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Topic title with page selector
                TopicHeader(
                    title = selectedTopic?.title ?: "",
                    currentPage = currentPage,
                    maxPages = maxPages,
                    onPreviousPage = { 
                        if (currentPage > 1) {
                            viewModel.selectTopic(topicIndex, currentPage - 1)
                        }
                    },
                    onNextPage = { 
                        if (currentPage < maxPages) {
                            viewModel.selectTopic(topicIndex, currentPage + 1)
                        }
                    },
                    onShowPageDialog = { viewModel.showPageDialog() },
                    isPreviousEnabled = currentPage > 1,
                    isNextEnabled = currentPage < maxPages,
                    isSaved = selectedTopic?.isSaved ?: false,
                    onSaveToggle = {
                        selectedTopic?.let { topic ->
                            if (topic.isSaved) {
                                viewModel.unsaveTopic(topic)
                            } else {
                                viewModel.saveTopic(topic)
                            }
                        }
                    }
                )
                
                // Entries
                selectedTopic?.let { topic ->
                    EntriesDisplay(
                        topic = topic,
                        currentPage = currentPage
                    )
                }
            }
        } else {
            // No topic selected or loading
            if (isLoading) {
                LoadingIndicator(message = "Loading...")
            } else if (activeScreen == "topic_detail") {
                // Only show error if we're actually on the topic detail screen
                // This prevents the error from flashing during navigation
                ErrorDisplay(message = "Topic not found")
            }
        }
    }
}