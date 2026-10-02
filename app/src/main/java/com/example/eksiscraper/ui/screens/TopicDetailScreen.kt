package com.example.eksiscraper.ui.screens

import android.app.Application
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.EntryCard
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.LoginRequiredDialog
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.components.PagePickerDialog
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.TopicDetailViewModel

private enum class TopicPhase { Loading, Error, Empty, Content }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicDetailScreen(
    title: String,
    url: String,
    navController: NavController,
    viewModel: TopicDetailViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val topic by viewModel.selectedTopic
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val currentPage by viewModel.currentPage
    val totalPages by viewModel.totalPages
    val isLoggedIn by EksiSession.isLoggedIn
    val listState = viewModel.scrollState

    var showPagePicker by rememberSaveable { mutableStateOf(false) }
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(title, url) {
        if (topic == null) viewModel.loadTopic(title, url)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val loaded = topic?.entriesLoaded == true
    val phase = when {
        error != null && !loaded -> TopicPhase.Error
        !loaded -> TopicPhase.Loading
        topic?.entries.isNullOrEmpty() -> TopicPhase.Empty
        else -> TopicPhase.Content
    }

    if (showPagePicker) {
        PagePickerDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            onPageSelected = {
                showPagePicker = false
                viewModel.navigateToPage(it)
            },
            onDismiss = { showPagePicker = false }
        )
    }
    if (showLoginDialog) {
        LoginRequiredDialog(
            onLogin = {
                showLoginDialog = false
                navController.navigate(Screen.Login.route)
            },
            onDismiss = { showLoginDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = topic?.title?.takeIf { it.isNotBlank() } ?: title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = { if (loaded) Text(subtitle(topic, currentPage, totalPages)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Nothing to save or share for a topic that doesn't exist
                    if (loaded && !topic?.entries.isNullOrEmpty()) {
                        TopicShareButton(topic)
                        IconToggleButton(
                            checked = topic?.isSaved == true,
                            onCheckedChange = { saved ->
                                if (saved) viewModel.saveTopic() else viewModel.unsaveTopic()
                            }
                        ) {
                            Icon(
                                imageVector = if (topic?.isSaved == true) Icons.Rounded.Bookmark
                                else Icons.Rounded.BookmarkBorder,
                                contentDescription = if (topic?.isSaved == true) "Kaydedildi" else "Kaydet"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = phase, label = "topicPhase") { current ->
                when (current) {
                    TopicPhase.Loading -> LoadingState(
                        messages = listOf("entry'ler getiriliyor", "sayfa çevriliyor", "neredeyse hazır")
                    )
                    TopicPhase.Error -> ErrorState(message = error.orEmpty(), onRetry = viewModel::retry)
                    TopicPhase.Empty -> MessageState(
                        icon = Icons.Rounded.SearchOff,
                        title = "Burada bir şey yok",
                        message = "Böyle bir başlık yok ya da bu sayfada gösterilecek entry kalmamış."
                    )
                    TopicPhase.Content -> EntryList(
                        topic = topic!!,
                        currentPage = currentPage,
                        listState = listState,
                        // While another page loads, keep the old one dimmed instead of blanking
                        dimmed = isLoading,
                        isEntryExpanded = viewModel::isEntryExpanded,
                        onToggleExpand = viewModel::toggleEntryExpansion,
                        onToggleFavorite = { entryId ->
                            if (isLoggedIn) viewModel.toggleEntryFavorite(entryId) else showLoginDialog = true
                        },
                        onShowOlderEntries = viewModel::showOlderEntries
                    )
                }
            }

            if (isLoading && loaded) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            PageToolbar(
                visible = phase == TopicPhase.Content && totalPages > 1 && listState.isScrollingUp(),
                currentPage = currentPage,
                totalPages = totalPages,
                onPrevious = { viewModel.navigateToPage(currentPage - 1) },
                onNext = { viewModel.navigateToPage(currentPage + 1) },
                onPickPage = { showPagePicker = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun subtitle(topic: Topic?, currentPage: Int, totalPages: Int): String {
    val pages = if (totalPages > 1) "sayfa $currentPage / $totalPages" else "tek sayfa"
    // Only today's-entries pages (?a=popular / ?day=) carry the "N entry daha" count
    return if ((topic?.olderEntriesCount ?: 0) > 0) "bugünün entry'leri · $pages" else pages
}

@Composable
private fun EntryList(
    topic: Topic,
    currentPage: Int,
    listState: LazyListState,
    dimmed: Boolean,
    isEntryExpanded: (String) -> Boolean,
    onToggleExpand: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShowOlderEntries: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.5f else 1f),
        // Bottom room so the floating toolbar never covers the last entry
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (topic.olderEntriesCount > 0 && currentPage == 1) {
            item(key = "older") {
                FilledTonalButton(
                    onClick = onShowOlderEntries,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null)
                    Text(
                        "${topic.olderEntriesCount} entry daha · baştan oku",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        items(topic.entries, key = { it.entryId.ifEmpty { it.content.hashCode().toString() } }) { entry ->
            EntryCard(
                entry = entry,
                isExpanded = isEntryExpanded(entry.entryId),
                onToggleExpand = { onToggleExpand(entry.entryId) },
                onToggleFavorite = { onToggleFavorite(entry.entryId) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageToolbar(
    visible: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier.navigationBarsPadding().padding(bottom = 16.dp)
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        ) {
            IconButton(onClick = onPrevious, enabled = currentPage > 1) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Önceki sayfa")
            }
            TextButton(onClick = onPickPage) {
                Text(
                    "$currentPage / $totalPages",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onNext, enabled = currentPage < totalPages) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Sonraki sayfa")
            }
        }
    }
}

@Composable
private fun TopicShareButton(topic: Topic?) {
    val context = LocalContext.current
    val link = topic?.url?.let { if (it.startsWith("http")) it else EksiSession.BASE_URL + it } ?: return
    IconButton(onClick = {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "${topic.title}\n$link")
        context.startActivity(Intent.createChooser(send, null))
    }) {
        Icon(Icons.Rounded.Share, contentDescription = "Başlığı paylaş")
    }
}

/** True while the user scrolls toward the top (or sits at the top): show the toolbar then. */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            val up = if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousOffset >= firstVisibleItemScrollOffset
            }
            previousIndex = firstVisibleItemIndex
            previousOffset = firstVisibleItemScrollOffset
            up || !canScrollForward
        }
    }.value
}
