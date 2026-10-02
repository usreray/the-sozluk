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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.CommentsUi
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
import kotlinx.coroutines.launch

private enum class TopicPhase { Loading, Error, Empty, Content }

/** Rows of the endless list: entries plus page markers and loading slots. */
private sealed interface TopicRow {
    val key: String
    data object Intro : TopicRow { override val key = "intro" }
    data object Previous : TopicRow { override val key = "previous" }
    data class PageMarker(val page: Int) : TopicRow { override val key = "page:$page" }
    data class EntryItem(val entry: Entry) : TopicRow { override val key = "entry:${entry.entryId}" }
    data object Footer : TopicRow { override val key = "footer" }
}

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
    val isLoadingNext by viewModel.isLoadingNext
    val isLoadingPrevious by viewModel.isLoadingPrevious
    val firstPage by viewModel.firstPage
    val lastPage by viewModel.lastPage
    val totalPages by viewModel.totalPages
    val isSubmitting by viewModel.isSubmitting
    val error by viewModel.error
    val message by viewModel.message
    val isLoggedIn by EksiSession.isLoggedIn
    val listState = viewModel.scrollState
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPagePicker by rememberSaveable { mutableStateOf(false) }
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var commentTarget by remember { mutableStateOf<Entry?>(null) }

    LaunchedEffect(title, url) { viewModel.loadTopic(title, url) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val loaded = topic?.entriesLoaded == true
    val canWrite = isLoggedIn && topic?.entryForm?.textFieldName != null
    val canComment = isLoggedIn && topic?.commentForm?.textFieldName != null
    val phase = when {
        error != null && !loaded -> TopicPhase.Error
        !loaded -> TopicPhase.Loading
        topic?.entries.isNullOrEmpty() -> TopicPhase.Empty
        else -> TopicPhase.Content
    }
    val displayTitle = topic?.title?.takeIf { it.isNotBlank() } ?: title

    // Entries with a marker wherever a new page starts
    val rows = remember(topic?.entries, firstPage, lastPage, totalPages, topic?.olderEntriesCount) {
        buildList {
            if ((topic?.olderEntriesCount ?: 0) > 0 && firstPage == 1) add(TopicRow.Intro)
            if (firstPage > 1) add(TopicRow.Previous)
            var page = -1
            topic?.entries.orEmpty().forEach { entry ->
                if (entry.page != page) {
                    page = entry.page
                    if (totalPages > 1) add(TopicRow.PageMarker(page))
                }
                add(TopicRow.EntryItem(entry))
            }
            add(TopicRow.Footer)
        }
    }
    // The page counter follows the entry at the top of the screen
    val visiblePage by remember(rows) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            rows.drop(index).firstNotNullOfOrNull { (it as? TopicRow.EntryItem)?.entry?.page }
                ?: rows.take(index + 1).lastOrNull { it is TopicRow.EntryItem }?.let { (it as TopicRow.EntryItem).entry.page }
                ?: firstPage
        }
    }
    // Endless scroll: next page near the end, previous page when reaching the top
    val nearEnd by remember(rows) {
        derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= rows.size - 4 }
    }
    LaunchedEffect(nearEnd, lastPage, phase) { if (nearEnd && phase == TopicPhase.Content) viewModel.loadNext() }
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    LaunchedEffect(atTop, firstPage, phase) { if (atTop && firstPage > 1 && phase == TopicPhase.Content) viewModel.loadPrevious() }

    fun goToPage(page: Int) {
        val index = rows.indexOfFirst { it is TopicRow.PageMarker && it.page == page }
        if (index >= 0) scope.launch { listState.animateScrollToItem(index) } else viewModel.jumpTo(page)
    }

    val requireLogin: (() -> Unit) -> Unit = { action -> if (isLoggedIn) action() else showLoginDialog = true }
    val actions = EntryActions(
        onToggleFavorite = { entry -> requireLogin { viewModel.toggleEntryFavorite(entry) } },
        onVote = { entry, rate -> requireLogin { viewModel.vote(entry, rate) } },
        onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
        onLink = { link -> navController.openEksiLink(link, uriHandler) },
        onDelete = if (topic?.deleteForm != null) viewModel::deleteEntry else null,
        comments = { entry ->
            val state = viewModel.comments(entry.entryId)
            CommentsUi(state.isOpen, state.isLoading, state.comments, state.error)
        },
        onToggleComments = viewModel::toggleComments,
        onVoteComment = { entry, comment, rate -> requireLogin { viewModel.voteComment(entry.entryId, comment, rate) } },
        onWriteComment = if (canComment) ({ entry -> commentTarget = entry }) else null
    )

    if (showPagePicker) {
        PagePickerDialog(
            currentPage = visiblePage,
            totalPages = totalPages,
            onPageSelected = {
                showPagePicker = false
                goToPage(it)
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
    commentTarget?.let { entry ->
        EntryComposerSheet(
            topicTitle = "${entry.author}: ${entry.content.take(80)}",
            heading = "yorum yaz",
            isSubmitting = isSubmitting,
            onSubmit = { text -> viewModel.submitComment(entry, text) { commentTarget = null } },
            onDismiss = { if (!isSubmitting) commentTarget = null }
        )
    }

    val barsVisible = listState.isScrollingUp()
    // Room under the floating header so the first row starts below it
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = phase, label = "topicPhase") { current ->
            when (current) {
                TopicPhase.Loading -> LoadingState(messages = listOf("entry'ler getiriliyor", "sayfa çevriliyor", "neredeyse hazır"))
                TopicPhase.Error -> ErrorState(message = error.orEmpty(), onRetry = viewModel::retry)
                TopicPhase.Empty -> MessageState(
                    icon = Icons.Rounded.SearchOff,
                    title = "Burada bir şey yok",
                    message = "Böyle bir başlık yok ya da gösterilecek entry kalmamış."
                )
                TopicPhase.Content -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = topInset, bottom = 128.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            TopicRow.Intro -> OlderEntriesCard(topic!!.olderEntriesCount, viewModel::showOlderEntries)
                            TopicRow.Previous -> Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                if (isLoadingPrevious) LoadingIndicator()
                                else TextButton(onClick = viewModel::loadPrevious) { Text("önceki sayfayı yükle") }
                            }
                            is TopicRow.PageMarker -> PageMarker(row.page, totalPages)
                            is TopicRow.EntryItem -> EntryCard(
                                entry = row.entry,
                                isExpanded = viewModel.isEntryExpanded(row.entry.entryId),
                                onToggleExpand = { viewModel.toggleEntryExpansion(row.entry.entryId) },
                                actions = actions,
                                modifier = Modifier.animateItem()
                            )
                            TopicRow.Footer -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                when {
                                    isLoadingNext -> LoadingIndicator()
                                    lastPage >= totalPages -> Text(
                                        "başlığın sonu",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingHeader(
            visible = barsVisible || phase != TopicPhase.Content,
            title = displayTitle,
            subtitle = when {
                !loaded -> null
                (topic?.olderEntriesCount ?: 0) > 0 -> "bugünün entry'leri"
                totalPages > 1 -> "$totalPages sayfa"
                else -> null
            },
            topic = topic,
            showActions = phase == TopicPhase.Content,
            onBack = { navController.popBackStack() },
            onSaveToggle = { saved -> if (saved) viewModel.saveTopic() else viewModel.unsaveTopic() },
            isLoading = isLoading && loaded,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        BottomToolbar(
            visible = phase == TopicPhase.Content && (totalPages > 1 || canWrite) && barsVisible,
            currentPage = visiblePage,
            totalPages = totalPages,
            canWrite = canWrite,
            onPrevious = { goToPage(visiblePage - 1) },
            onNext = { goToPage(visiblePage + 1) },
            onPickPage = { showPagePicker = true },
            onWrite = { showComposer = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
    }
}

/** The topic's title as a floating card instead of an app bar; slides away while reading. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingHeader(
    visible: Boolean,
    title: String,
    subtitle: String?,
    topic: Topic?,
    showActions: Boolean,
    onBack: () -> Unit,
    onSaveToggle: (Boolean) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (showActions) {
                        TopicShareButton(topic)
                        IconToggleButton(checked = topic?.isSaved == true, onCheckedChange = onSaveToggle) {
                            Icon(
                                imageVector = if (topic?.isSaved == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = if (topic?.isSaved == true) "Kaydedildi" else "Kaydet",
                                tint = if (topic?.isSaved == true) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (isLoading) LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun OlderEntriesCard(count: Int, onShow: () -> Unit) {
    FilledTonalButton(
        onClick = onShow,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(Icons.Rounded.History, contentDescription = null)
        Text("$count entry daha · baştan oku", modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun PageMarker(page: Int, totalPages: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            "sayfa $page / $totalPages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

/** Page jumps plus, for logged-in users, "write an entry"; uses the app's surface colors. */
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
            FloatingActionButton(onClick = onWrite) { Icon(Icons.Rounded.Edit, contentDescription = "entry gir") }
            return@AnimatedVisibility
        }
        val colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
        val pageControls: @Composable () -> Unit = {
            IconButton(onClick = onPrevious, enabled = currentPage > 1) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Önceki sayfa")
            }
            TextButton(onClick = onPickPage) {
                Text(
                    "$currentPage / $totalPages",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNext, enabled = currentPage < totalPages) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Sonraki sayfa")
            }
        }
        if (canWrite) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarDefaults.StandardFloatingActionButton(onClick = onWrite) {
                        Icon(Icons.Rounded.Edit, contentDescription = "entry gir")
                    }
                },
                colors = colors
            ) { pageControls() }
        } else {
            HorizontalFloatingToolbar(expanded = true, colors = colors) { pageControls() }
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
        Icon(Icons.Rounded.Share, contentDescription = "Başlığı paylaş", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** True while the user scrolls toward the top (or sits at the top): show the bars then. */
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
