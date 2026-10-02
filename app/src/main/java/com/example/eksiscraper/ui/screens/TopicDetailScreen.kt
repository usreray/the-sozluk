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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.EntryActions
import com.example.eksiscraper.ui.components.EntryCard
import com.example.eksiscraper.ui.components.EntryComposerSheet
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.LoginRequiredDialog
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.components.PagePickerDialog
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.ui.navigation.openEksiLink
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
    val isSubmitting by viewModel.isSubmitting
    val error by viewModel.error
    val message by viewModel.message
    val currentPage by viewModel.currentPage
    val totalPages by viewModel.totalPages
    val isLoggedIn by EksiSession.isLoggedIn
    val listState = viewModel.scrollState
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }

    var showPagePicker by rememberSaveable { mutableStateOf(false) }
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(title, url) {
        if (topic == null) viewModel.loadTopic(title, url)
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val loaded = topic?.entriesLoaded == true
    val canWrite = isLoggedIn && topic?.entryForm?.textFieldName != null
    val phase = when {
        error != null && !loaded -> TopicPhase.Error
        !loaded -> TopicPhase.Loading
        topic?.entries.isNullOrEmpty() -> TopicPhase.Empty
        else -> TopicPhase.Content
    }
    // The bar shows the title only once the big heading has scrolled away
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val displayTitle = topic?.title?.takeIf { it.isNotBlank() } ?: title

    val requireLogin: (() -> Unit) -> Unit = { action -> if (isLoggedIn) action() else showLoginDialog = true }
    val actions = EntryActions(
        onToggleFavorite = { entry -> requireLogin { viewModel.toggleEntryFavorite(entry) } },
        onVote = { entry, rate -> requireLogin { viewModel.vote(entry, rate) } },
        onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
        onLink = { link -> navController.openEksiLink(link, uriHandler) },
        onDelete = if (topic?.deleteForm != null) viewModel::deleteEntry else null
    )

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
    if (showComposer) {
        EntryComposerSheet(
            topicTitle = displayTitle,
            isSubmitting = isSubmitting,
            onSubmit = { text -> viewModel.submitEntry(text) { showComposer = false } },
            onDismiss = { if (!isSubmitting) showComposer = false }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = headerGone, enter = fadeIn(), exit = fadeOut()) {
                        Text(displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Nothing to save or share for a topic that doesn't exist
                    if (phase == TopicPhase.Content) {
                        TopicShareButton(topic)
                        IconToggleButton(
                            checked = topic?.isSaved == true,
                            onCheckedChange = { saved -> if (saved) viewModel.saveTopic() else viewModel.unsaveTopic() }
                        ) {
                            Icon(
                                imageVector = if (topic?.isSaved == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
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
                    TopicPhase.Content -> LazyColumn(
                        state = listState,
                        // While another page loads, keep the old one dimmed instead of blanking
                        modifier = Modifier.fillMaxSize().alpha(if (isLoading) 0.5f else 1f),
                        // Bottom room so the floating toolbar never covers the last entry
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "header") {
                            TopicHeader(
                                topic = topic!!,
                                title = displayTitle,
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPickPage = { showPagePicker = true },
                                onShowOlderEntries = viewModel::showOlderEntries
                            )
                        }
                        items(topic!!.entries, key = { it.entryId.ifEmpty { it.content.hashCode().toString() } }) { entry ->
                            EntryCard(
                                entry = entry,
                                isExpanded = viewModel.isEntryExpanded(entry.entryId),
                                onToggleExpand = { viewModel.toggleEntryExpansion(entry.entryId) },
                                actions = actions,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            if (isLoading && loaded) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            BottomToolbar(
                visible = phase == TopicPhase.Content && (totalPages > 1 || canWrite) && listState.isScrollingUp(),
                currentPage = currentPage,
                totalPages = totalPages,
                canWrite = canWrite,
                onPrevious = { viewModel.navigateToPage(currentPage - 1) },
                onNext = { viewModel.navigateToPage(currentPage + 1) },
                onPickPage = { showPagePicker = true },
                onWrite = { showComposer = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** Big title with the reading state as chips, instead of cramming it into the app bar. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopicHeader(
    topic: Topic,
    title: String,
    currentPage: Int,
    totalPages: Int,
    onPickPage: () -> Unit,
    onShowOlderEntries: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMediumEmphasized)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Only today's-entries pages (?a=popular / ?day=) carry the "N entry daha" count
            if (topic.olderEntriesCount > 0) {
                AssistChip(
                    onClick = onShowOlderEntries,
                    label = { Text("bugünün entry'leri") },
                    leadingIcon = { Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) }
                )
            }
            AssistChip(
                onClick = onPickPage,
                enabled = totalPages > 1,
                label = { Text(if (totalPages > 1) "sayfa $currentPage / $totalPages" else "tek sayfa") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                }
            )
        }
        if (topic.olderEntriesCount > 0 && currentPage == 1) {
            FilledTonalButton(
                onClick = onShowOlderEntries,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Rounded.History, contentDescription = null)
                Text("${topic.olderEntriesCount} entry daha · baştan oku", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Page controls plus, for logged-in users, the "write an entry" button. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomToolbar(
    visible: Boolean,
    currentPage: Int,
    totalPages: Int,
    canWrite: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickPage: () -> Unit,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier.navigationBarsPadding().padding(bottom = 16.dp)
    ) {
        if (totalPages <= 1) {
            FloatingActionButton(onClick = onWrite) {
                Icon(Icons.Rounded.Edit, contentDescription = "entry gir")
            }
            return@AnimatedVisibility
        }
        val pageControls: @Composable () -> Unit = {
            IconButton(onClick = onPrevious, enabled = currentPage > 1) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Önceki sayfa")
            }
            TextButton(onClick = onPickPage) {
                Text("$currentPage / $totalPages", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            IconButton(onClick = onNext, enabled = currentPage < totalPages) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Sonraki sayfa")
            }
        }
        if (canWrite) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onWrite) {
                        Icon(Icons.Rounded.Edit, contentDescription = "entry gir")
                    }
                },
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            ) { pageControls() }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            ) { pageControls() }
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
fun LazyListState.isScrollingUp(): Boolean {
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
