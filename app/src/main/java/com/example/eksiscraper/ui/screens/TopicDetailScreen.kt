package com.example.eksiscraper.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.ui.components.EntriesDisplay
import com.example.eksiscraper.ui.components.EntriesSkeleton
import com.example.eksiscraper.ui.components.ErrorDisplay
import com.example.eksiscraper.ui.components.PageSelectionDialog
import com.example.eksiscraper.ui.components.TopicHeader
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.TopicDetailViewModel

@Composable
fun TopicDetailScreen(
        title: String,
        url: String,
        navController: NavController,
        viewModel: TopicDetailViewModel =
                viewModel(
                        factory =
                                EksiViewModelFactory(
                                        LocalContext.current.applicationContext as Application
                                )
                )
) {
    val selectedTopic by viewModel.selectedTopic
    val isLoading by viewModel.isLoading
    val currentPage by viewModel.currentPage
    val totalPages by viewModel.totalPages

    // Add state to track scroll position and hide/show navigation
    val isScrolledState = remember { mutableStateOf(false) }
    val isScrolled = isScrolledState.value

    // Page selection dialog state
    val isPageDialogVisible = remember { mutableStateOf(false) }
    val selectedPage = remember { mutableStateOf(1) }

    // Load topic on start
    LaunchedEffect(title, url) {
        if (selectedTopic?.title != title) {
            viewModel.loadTopic(title, url)
        }
    }

    // Page selection dialog
    if (isPageDialogVisible.value) {
        PageSelectionDialog(
                currentPage = currentPage,
                maxPages = totalPages,
                selectedPage = selectedPage.value,
                onPageSelected = { selectedPage.value = it },
                onConfirm = {
                    viewModel.navigateToPage(selectedPage.value)
                    isPageDialogVisible.value = false
                },
                onDismiss = { isPageDialogVisible.value = false }
        )
    }

    // Handle back press to return to home screen
    BackHandler { navController.popBackStack() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Loading indicator
        if (isLoading && selectedTopic?.entries?.isEmpty() == true) {
            EntriesSkeleton(itemCount = 5)
        } else if (selectedTopic != null) {
            // Topic title with page selector
            TopicHeader(
                    title = selectedTopic?.title ?: "",
                    currentPage = currentPage,
                    maxPages = totalPages,
                    onPreviousPage = {
                        if (currentPage > 1) {
                            viewModel.navigateToPage(currentPage - 1)
                        }
                    },
                    onNextPage = {
                        if (currentPage < totalPages) {
                            viewModel.navigateToPage(currentPage + 1)
                        }
                    },
                    onShowPageDialog = {
                        selectedPage.value = currentPage
                        isPageDialogVisible.value = true
                    },
                    isPreviousEnabled = currentPage > 1,
                    isNextEnabled = currentPage < totalPages,
                    isSaved = selectedTopic?.isSaved ?: false,
                    onSaveToggle = {
                        if (selectedTopic?.isSaved == true) {
                            viewModel.unsaveTopic()
                        } else {
                            viewModel.saveTopic()
                        }
                    },
                    isScrolled = isScrolled
            )

            // Entries
            selectedTopic?.let { topic ->
                EntriesDisplay(
                        topic = topic,
                        currentPage = currentPage,
                        scrollState = viewModel.scrollState,
                        onScroll = { scrolled -> isScrolledState.value = scrolled },
                        isEntryExpanded = { entryId -> viewModel.isEntryExpanded(entryId) },
                        onToggleEntryExpansion = { entryId ->
                            viewModel.toggleEntryExpansion(entryId)
                        },
                        onToggleEntryFavorite = { entryId -> viewModel.toggleEntryFavorite(entryId) },
                        onLoginRequest = { navController.navigate(Screen.Login.route) }
                )
            }
        } else {
            // No topic selected or loading
            if (isLoading) {
                EntriesSkeleton(itemCount = 5)
            } else {
                ErrorDisplay(message = "Topic not found")
            }
        }
    }
}
