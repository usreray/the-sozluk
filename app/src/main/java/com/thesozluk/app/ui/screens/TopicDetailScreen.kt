package com.thesozluk.app.ui.screens

import com.thesozluk.app.ui.components.ImageActions
import com.thesozluk.app.ui.components.TopicFilterSheet
import com.thesozluk.app.ui.components.ShareEntryImageSheet
import com.thesozluk.app.settings.ReadingHistory
import com.thesozluk.app.data.offline.OfflineStore
import com.thesozluk.app.settings.EntryBookmarks
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import com.thesozluk.app.ui.components.RevealPullToRefresh
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.DragInteraction
import com.thesozluk.app.ui.components.EntryListSkeleton
import android.app.Application
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.Topic
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.ui.components.CommentsUi
import com.thesozluk.app.ui.components.EntryActions
import com.thesozluk.app.ui.components.EntryCard
import com.thesozluk.app.ui.components.EntryComposerSheet
import com.thesozluk.app.ui.components.ErrorState
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.floatingTopBarInset
import com.thesozluk.app.ui.components.isScrollingUp
import com.thesozluk.app.ui.components.LoadingState
import com.thesozluk.app.ui.components.LoginRequiredDialog
import com.thesozluk.app.ui.components.MessageState
import com.thesozluk.app.ui.components.PagePickerDialog
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.TopicTabs
import com.thesozluk.app.ui.navigation.openEksiLink
import com.thesozluk.app.viewmodel.EksiViewModelFactory
import com.thesozluk.app.viewmodel.TopicDetailViewModel
import com.thesozluk.app.viewmodel.TopicFilter
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.settings.Drafts
import com.thesozluk.app.ui.components.TextPromptDialog
import com.thesozluk.app.ui.components.TopicCreatorDialog
import com.thesozluk.app.ui.components.TopicMenu
import com.thesozluk.app.ui.components.TopicMenuAction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import kotlinx.coroutines.launch

private enum class TopicPhase { Loading, Error, Empty, Content }


/** Rows of the endless list: entries plus page markers and loading slots. */
private sealed interface TopicRow {
    val key: String
    data object Intro : TopicRow { override val key = "intro" }
    data object Header : TopicRow { override val key = "header" }
    data object FilterBanner : TopicRow { override val key = "filter" }
    data class PageMarker(val page: Int) : TopicRow { override val key = "page:$page" }
    data class EntryItem(val entry: Entry, val number: Int?) : TopicRow { override val key = "entry:${entry.entryId}" }
    data object Footer : TopicRow { override val key = "footer" }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicDetailScreen(
    title: String,
    url: String,
    navController: NavController,
    startPage: Int = 1,
    openComposer: Boolean = false,
    /** In the tablet layout's right pane: closing clears the pane instead of leaving the screen */
    onClose: (() -> Unit)? = null,
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
    val isSavingDraft by viewModel.isSavingDraft
    val error by viewModel.error
    val message by viewModel.message
    val isLoggedIn by EksiSession.isLoggedIn
    val listState = viewModel.scrollState
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPagePicker by rememberSaveable { mutableStateOf(false) }
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var favoritersOf by remember { mutableStateOf<com.thesozluk.app.model.Entry?>(null) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var autoOpenedComposer by rememberSaveable { mutableStateOf(false) }
    var commentTarget by remember { mutableStateOf<Entry?>(null) }
    var shareImageOf by remember { mutableStateOf<Entry?>(null) }
    // Entry opened full screen (swipe sideways for the others), null when closed
    var pagerEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    // Entry being edited, with its "düzelt" form
    var editTarget by remember { mutableStateOf<Pair<Entry, com.thesozluk.app.model.FormSpec>?>(null) }
    var prompt by remember { mutableStateOf<TopicMenuAction?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showCreator by remember { mutableStateOf(false) }
    val filter by viewModel.filter

    // Keep the screen awake while reading, if chosen in settings
    val keepScreenOn by AppSettings.keepScreenOn
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(title, url) { viewModel.loadTopic(title, url, startPage) }
    // Offline copy of this topic: on the device, downloading, or none
    val offlineKey = topic?.let { it.topicPath.ifBlank { it.url } } ?: url
    val offlineProgress = OfflineStore.progress[OfflineStore.keyOf(offlineKey)]
    val offlineState = when {
        offlineProgress != null && !offlineProgress.finished ->
            if (offlineProgress.total > 0) "indiriliyor ${offlineProgress.done} / ${offlineProgress.total} · durdur" else "indiriliyor · durdur"
        OfflineStore.topics.value.any { it.path == OfflineStore.keyOf(offlineKey) } -> "kayıtlı"
        else -> null
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val loaded = topic?.entriesLoaded == true
    val canWrite = isLoggedIn && topic?.entryForm?.textFieldName != null
    val canComment = isLoggedIn && topic?.commentForm?.textFieldName != null
    LaunchedEffect(topic?.entries) {
        topic?.entries.orEmpty().filter { it.commentCount > 0 }.forEach(viewModel::showComments)
    }
    LaunchedEffect(openComposer, loaded, canWrite) {
        if (openComposer && loaded && canWrite && !autoOpenedComposer) {
            autoOpenedComposer = true
            showComposer = true
        }
    }
    val phase = when {
        error != null && !loaded -> TopicPhase.Error
        !loaded -> TopicPhase.Loading
        topic?.entries.isNullOrEmpty() -> TopicPhase.Empty
        else -> TopicPhase.Content
    }
    val displayTitle = topic?.title?.takeIf { it.isNotBlank() } ?: title
    var showTabsOverview by remember { mutableStateOf(false) }

    // Entries with a marker wherever a new page starts
    val rows = remember(topic?.entries, firstPage, lastPage, totalPages, topic?.olderEntriesCount, filter) {
        val numbered = viewModel.numbersEntries
        buildList {
            // The full title, however long, above page 1; the bar shows a short copy once this
            // scrolls away, and on a page jumped to in the middle (no header there)
            if (firstPage == 1) add(TopicRow.Header)
            // Above a page in the middle only its page marker sits on top. Earlier pages are
            // added above it while reading, and the list keeps the row on screen by its key; a
            // fixed row at the very top (as the old "previous page" slot was) would break that
            if (firstPage == 1 && filter !is TopicFilter.All && filter !is TopicFilter.Day && filter != TopicFilter.Popular) add(TopicRow.FilterBanner)
            if ((topic?.olderEntriesCount ?: 0) > 0 && firstPage == 1) add(TopicRow.Intro)
            var page = -1
            var indexInPage = 0
            // An entry can come twice (e.g. pinned on the page and in the list); list keys must be unique
            topic?.entries.orEmpty().distinctBy { it.entryId }.forEach { entry ->
                if (entry.page != page) {
                    page = entry.page
                    indexInPage = 0
                    if (totalPages > 1) add(TopicRow.PageMarker(page))
                }
                // The site shows 10 entries a page, so the position follows from the page
                add(TopicRow.EntryItem(entry, if (numbered) (page - 1) * 10 + indexInPage + 1 else null))
                indexInPage++
            }
            add(TopicRow.Footer)
        }
    }
    // The page counter follows the entry being read: the one across the upper quarter of the
    // screen, not whatever sliver is at the very top (often the end of the page before)
    val visiblePage by remember(rows) {
        derivedStateOf {
            val info = listState.layoutInfo
            val line = (info.viewportEndOffset - info.viewportStartOffset) / 4
            val rowsByKey = rows.associateBy { it.key }
            val reading = info.visibleItemsInfo.firstOrNull { it.offset + it.size > line }
            val index = reading?.let { item -> rows.indexOfFirst { it.key == item.key } } ?: listState.firstVisibleItemIndex
            when (val row = reading?.key?.let(rowsByKey::get)) {
                is TopicRow.EntryItem -> row.entry.page
                is TopicRow.PageMarker -> row.page
                else -> rows.drop(index.coerceAtLeast(0)).firstNotNullOfOrNull { (it as? TopicRow.EntryItem)?.entry?.page }
            } ?: firstPage
        }
    }
    val currentTabId = TopicTabs.tabId(title, url)
    val tabPreview = topic?.entries?.firstOrNull { it.page == visiblePage }?.content
        ?: topic?.entries?.firstOrNull()?.content.orEmpty()
    LaunchedEffect(currentTabId, displayTitle, visiblePage, tabPreview, onClose) {
        if (onClose == null && displayTitle.isNotBlank()) TopicTabs.remember(currentTabId, displayTitle, url, visiblePage, tabPreview)
    }
    // Endless scroll both ways, like long threads in forum apps: the next page comes in near the
    // end, the previous one whenever the top of the loaded pages is close. The previous page is
    // added above the rows on screen; the list keeps the row being read in place by its key, so
    // nothing moves and the top is then far away again (no run back through the pages).
    val nearEnd by remember(rows) {
        derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= rows.size - 4 }
    }
    LaunchedEffect(nearEnd, lastPage, phase) { if (nearEnd && phase == TopicPhase.Content) viewModel.loadNext() }
    val nearTop by remember { derivedStateOf { listState.firstVisibleItemIndex <= 2 } }
    LaunchedEffect(nearTop, firstPage, phase, isLoading) {
        if (nearTop && !isLoading && firstPage > 1 && phase == TopicPhase.Content) viewModel.loadPrevious()
    }

    // Saved topics keep the reading position, written once the reader settles on a page
    LaunchedEffect(visiblePage, topic?.isSaved) {
        if (topic?.isSaved == true && phase == TopicPhase.Content) {
            kotlinx.coroutines.delay(800)
            viewModel.rememberPage(visiblePage)
        }
    }

    // Page buttons scroll the list themselves; that is not the reader scrolling down, so the
    // bars stay put until the next drag
    var pinBars by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) pinBars = false }
    }

    fun goToPage(page: Int) {
        pinBars = true
        val index = rows.indexOfFirst { it is TopicRow.PageMarker && it.page == page }
        if (index >= 0) scope.launch { listState.animateScrollToItem(index) } else viewModel.jumpTo(page)
    }

    val requireLogin: (() -> Unit) -> Unit = { action -> if (isLoggedIn) action() else showLoginDialog = true }
    favoritersOf?.let { entry ->
        com.thesozluk.app.ui.components.FavoritersSheet(
            entry = entry,
            load = { id, rookies -> viewModel.favoriters(id, rookies) },
            onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
            onDismiss = { favoritersOf = null }
        )
    }
    val actions = EntryActions(
        onShowFavoriters = { entry -> requireLogin { favoritersOf = entry } },
        onToggleFavorite = { entry -> requireLogin { viewModel.toggleEntryFavorite(entry) } },
        onVote = { entry, rate -> requireLogin { viewModel.vote(entry, rate) } },
        onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
        onLink = { link ->
            if (link is com.thesozluk.app.ui.components.EksiLink.Image) {
                val images = topic?.entries.orEmpty().flatMap { entry ->
                    org.jsoup.Jsoup.parseBodyFragment(entry.contentHtml).select("a[href]").mapNotNull { anchor ->
                        val href = anchor.attr("href")
                        (com.thesozluk.app.ui.components.eksiLinkFor(href, anchor.text()) as? com.thesozluk.app.ui.components.EksiLink.Image)?.ref
                    }
                }.distinct()
                com.thesozluk.app.ui.screens.ImageGallery.refs = (images + link.ref).distinct()
            }
            navController.openEksiLink(link, uriHandler)
        },
        onDelete = if (topic?.deleteForm != null) viewModel::deleteEntry else null,
        comments = { entry ->
            val state = viewModel.comments(entry.entryId)
            CommentsUi(state.isOpen, state.isLoading, state.comments, state.error)
        },
        onVoteComment = { entry, comment, rate -> requireLogin { viewModel.voteComment(entry.entryId, comment, rate) } },
        onWriteComment = if (canComment) ({ entry -> commentTarget = entry }) else null,
        onAuthorInTopic = { entry -> viewModel.applyFilter(TopicFilter.Author(entry.author)) },
        onBookmark = { entry ->
            if (EntryBookmarks.isSaved(entry.entryId)) {
                EntryBookmarks.remove(entry.entryId)
                scope.launch { snackbar.showSnackbar("kayıttan çıkarıldı") }
            } else {
                EntryBookmarks.save(entry, displayTitle, topic?.topicPath?.ifBlank { null } ?: topic?.url.orEmpty())
                scope.launch { snackbar.showSnackbar("entry kaydedildi; profilde \"kaydedilen entry'ler\"de") }
            }
        },
        onShareImage = { entry -> shareImageOf = entry },
        onEdit = { entry ->
            scope.launch {
                try {
                    editTarget = entry to viewModel.editForm(entry)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snackbar.showSnackbar(e.message ?: "düzeltme sayfası açılamadı")
                }
            }
        }
    )
    val topicReferrer = topic?.topicPath?.ifBlank { null } ?: topic?.url.orEmpty()
    val imageActions: (com.thesozluk.app.model.ImageUploader?) -> ImageActions? = { uploader ->
        uploader?.let { u ->
            ImageActions(
                upload = { bytes, name, type -> viewModel.uploadImage(u, bytes, name, type, topicReferrer) },
                delete = if (u.deleteUrl != null) ({ key -> viewModel.deleteImage(u, key) }) else null
            )
        }
    }
    editTarget?.let { (entry, form) ->
        EntryComposerSheet(
            topicTitle = displayTitle,
            heading = "entry'yi düzelt",
            isSubmitting = isSubmitting,
            initialText = form.textValue.ifBlank { entry.content },
            // The düzelt page's own uploader, else the one under the topic's entry box
            onUploadImage = imageActions(form.imageUploader ?: topic?.imageUploader)?.upload,
            onDeleteImage = imageActions(form.imageUploader ?: topic?.imageUploader)?.delete,
            onSubmit = { text -> viewModel.submitEdit(form, text) { editTarget = null } },
            onDismiss = { if (!isSubmitting) editTarget = null }
        )
    }
    if (showFilters) {
        TopicFilterSheet(
            current = filter,
            isLoggedIn = isLoggedIn,
            nick = EksiSession.nick.value,
            onPick = { picked ->
                showFilters = false
                viewModel.applyFilter(picked)
            },
            onDismiss = { showFilters = false }
        )
    }
    val onMenu: (TopicMenuAction) -> Unit = { action ->
        when (action) {
            is TopicMenuAction.Filter -> viewModel.applyFilter(action.filter)
            TopicMenuAction.Filters -> showFilters = true
            TopicMenuAction.Creator -> showCreator = true
            TopicMenuAction.Share -> topic?.let { shareTopic(context, it) }
            TopicMenuAction.SaveOffline -> topic?.let {
                val path = it.topicPath.ifBlank { it.url }
                OfflineStore.download(it.title, path)
                // An offline copy also goes to the saved topics
                if (!it.isSaved) viewModel.saveTopic(visiblePage)
                scope.launch { snackbar.showSnackbar("indirme başladı; ilerlemesi ⋮ menüsünde ve profilde") }
            }
            TopicMenuAction.DeleteOffline -> topic?.let {
                OfflineStore.delete(it.topicPath.ifBlank { it.url })
                scope.launch { snackbar.showSnackbar("çevrimdışı kopya silindi") }
            }
            else -> prompt = action
        }
    }
    when (prompt) {
        TopicMenuAction.SearchInTopic -> TextPromptDialog(
            title = "başlıkta ara",
            placeholder = "kelime",
            initial = (filter as? TopicFilter.Find)?.keywords.orEmpty(),
            onConfirm = {
                prompt = null
                viewModel.applyFilter(TopicFilter.Find(it))
            },
            onDismiss = { prompt = null }
        )
        TopicMenuAction.SearchAuthor -> TextPromptDialog(
            title = "yazarın bu başlıktaki entry'leri",
            placeholder = "yazar",
            initial = (filter as? TopicFilter.Author)?.nick.orEmpty(),
            onConfirm = {
                prompt = null
                viewModel.applyFilter(TopicFilter.Author(it.removePrefix("@")))
            },
            onDismiss = { prompt = null }
        )
        else -> Unit
    }
    if (showCreator) {
        TopicCreatorDialog(
            load = viewModel::topicCreator,
            onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
            onDismiss = { showCreator = false }
        )
    }

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
        // Continue a draft saved on this device, or one left "kenarda" on the site
        val draftKey = topic?.title ?: displayTitle
        EntryComposerSheet(
            topicTitle = displayTitle,
            isSubmitting = isSubmitting,
            onSubmit = { text -> viewModel.submitEntry(text) { showComposer = false } },
            onSaveToSite = { text -> viewModel.saveSiteDraft(text) { showComposer = false } },
            isSavingDraft = isSavingDraft,
            onUploadImage = imageActions(topic?.imageUploader)?.upload,
            onDeleteImage = imageActions(topic?.imageUploader)?.delete,
            onDismiss = { if (!isSubmitting) showComposer = false },
            initialText = remember { Drafts.get(draftKey)?.text ?: topic?.entryForm?.textValue.orEmpty() },
            onTextChange = { text -> Drafts.save(draftKey, topic?.topicPath?.ifBlank { null } ?: topic?.url.orEmpty(), text) }
        )
    }
    shareImageOf?.let { entry ->
        ShareEntryImageSheet(entry = entry, topicTitle = displayTitle, onDismiss = { shareImageOf = null })
    }
    // Recently opened topics (settings: içerik ve gizlilik)
    LaunchedEffect(loaded, topic?.title) {
        val t = topic ?: return@LaunchedEffect
        if (loaded && t.title.isNotBlank()) ReadingHistory.record(t.title, t.topicPath.ifBlank { t.url })
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

    val barsVisible = !AppSettings.hideBarsOnScroll.value || listState.isScrollingUp() || pinBars
    // Only a pull opens the refresh gap; page jumps load the same way but show in the bar
    var pullRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) { if (!isLoading) pullRefreshing = false }
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || firstPage > 1 } }
    // Room under the floating header so the first row starts below it
    val topInset = floatingTopBarInset()

    // A surface at the root sets the background and the default text color (no Scaffold here)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = phase, label = "topicPhase") { current ->
            when (current) {
                TopicPhase.Loading -> Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = topInset)) {
                    if (startPage <= 1) TopicHeader(displayTitle, onClick = null)
                    EntryListSkeleton(contentPadding = PaddingValues(top = 12.dp))
                }
                TopicPhase.Error -> ErrorState(message = error.orEmpty(), onRetry = viewModel::retry)
                TopicPhase.Empty -> MessageState(
                    icon = Icons.Rounded.SearchOff,
                    title = "burada bir şey yok",
                    message = if (filter is TopicFilter.All) "böyle bir başlık yok ya da gösterilecek entry kalmamış."
                    else "${filter.label} için entry bulunamadı.",
                    action = if (filter is TopicFilter.All) null else ({
                        FilledTonalButton(onClick = viewModel::showOlderEntries) { Text("tüm entry'leri göster") }
                    })
                )
                // Pulling down at the top reloads the pages on screen (new entries, votes). The
                // list itself moves down with the finger and the loading animation sits in the gap
                // that opens above it, which stays open until the refresh is done
                TopicPhase.Content -> RevealPullToRefresh(
                    isRefreshing = pullRefreshing,
                    onRefresh = {
                        pullRefreshing = true
                        viewModel.retry()
                    },
                    top = topInset,
                    // Only at the top of the topic; above a page in the middle, pulling down
                    // would cover its page marker with the animation
                    enabled = firstPage == 1
                ) { pullOffset ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().then(pullOffset),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = topInset, bottom = 128.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            // Like the bar title: the whole topic from its first page
                            TopicRow.Header -> TopicHeader(displayTitle, onClick = viewModel::showOlderEntries)
                            TopicRow.Intro -> OlderEntriesCard(viewModel::showOlderEntries)
                            TopicRow.FilterBanner -> FilterBanner(filter.label, onClear = viewModel::showOlderEntries)
                            is TopicRow.PageMarker -> PageMarker(row.page, totalPages)
                            is TopicRow.EntryItem -> EntryCard(
                                entry = row.entry,
                                number = row.number,
                                onClick = { pagerEntryId = row.entry.entryId },
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
        }

        FloatingTopBar(
            // The page shows the title itself while loading and at the top of the list
            title = if (headerGone || phase == TopicPhase.Error || phase == TopicPhase.Empty ||
                (phase == TopicPhase.Loading && startPage > 1)
            ) displayTitle else null,
            // Pages are in the bottom toolbar; only say when this is a single entry
            // The title opens the whole topic from its first page
            onTitleClick = if (loaded) viewModel::showOlderEntries else null,
            onBack = { onClose?.invoke() ?: navController.popBackStack() },
            visible = barsVisible || phase != TopicPhase.Content,
            // The pull-down gap already shows a refresh; the bar shows only page jumps
            // Page jumps and an earlier page loading above show as the bar's thin line
            isLoading = (isLoading && loaded && !pullRefreshing) || isLoadingPrevious,
            modifier = Modifier.align(Alignment.TopCenter),
            // Nothing to save or share for a topic that doesn't exist
            actions = if (phase != TopicPhase.Content) null else ({
                if (onClose == null) {
                    val tabCount = TopicTabs.items.size
                    IconButton(
                        onClick = { showTabsOverview = true },
                        modifier = Modifier.semantics {
                            contentDescription = "açık sekmeler, $tabCount"
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (tabCount > 99) "99+" else tabCount.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = if (tabCount > 99) 7.sp else 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
                // "takip et": new entries then show up in the olay list
                if (isLoggedIn && topic?.trackUrl != null) {
                    IconToggleButton(checked = topic?.isTracked == true, onCheckedChange = { viewModel.toggleTrack() }) {
                        Icon(
                            imageVector = if (topic?.isTracked == true) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                            contentDescription = if (topic?.isTracked == true) "takibi bırak" else "başlığı takip et",
                            tint = if (topic?.isTracked == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconToggleButton(
                    checked = topic?.isSaved == true,
                    onCheckedChange = { saved -> if (saved) viewModel.saveTopic(visiblePage) else viewModel.unsaveTopic() }
                ) {
                    Icon(
                        imageVector = if (topic?.isSaved == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (topic?.isSaved == true) "kaydedildi" else "kaydet",
                        tint = if (topic?.isSaved == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TopicMenu(current = filter, isLoggedIn = isLoggedIn, onAction = onMenu, offlineState = offlineState)
            })
        )

        if (showTabsOverview && onClose == null) {
            TopicTabsOverview(
                tabs = TopicTabs.items.toList(),
                onDismiss = { showTabsOverview = false },
                onSelect = { tab ->
                    showTabsOverview = false
                    if (tab.id != currentTabId) {
                        navController.navigate(Screen.TopicDetail.createRoute(tab.title, tab.url, tab.page)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                },
                onClose = { tab ->
                    TopicTabs.close(tab.id)
                }
            )
        }

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

        EntryPager(
            openEntryId = pagerEntryId,
            entries = topic?.entries.orEmpty().distinctBy { it.entryId },
            numbers = rows.mapNotNull { (it as? TopicRow.EntryItem)?.let { row -> row.entry.entryId to row.number } }.toMap(),
            title = displayTitle,
            actions = actions,
            isLoadingNext = isLoadingNext,
            onNearEnd = viewModel::loadNext,
            onClose = { lastId ->
                pagerEntryId = null
                // Back in the list at the entry the reader swiped to
                val index = rows.indexOfFirst { it is TopicRow.EntryItem && it.entry.entryId == lastId }
                if (index >= 0) scope.launch { listState.scrollToItem(index) }
            }
        )

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp))
    }
    }
}

/**
 * One entry at a time, full screen and in full; swiping sideways goes to the next or previous
 * entry of the topic, and the next page loads when the end comes close.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EntryPager(
    openEntryId: String?,
    entries: List<Entry>,
    numbers: Map<String, Int?>,
    title: String,
    actions: EntryActions,
    isLoadingNext: Boolean,
    onNearEnd: () -> Unit,
    onClose: (lastEntryId: String?) -> Unit
) {
    AnimatedVisibility(
        visible = openEntryId != null,
        enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.96f),
        exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.96f)
    ) {
        val startIndex = remember { entries.indexOfFirst { it.entryId == openEntryId }.coerceAtLeast(0) }
        val pager = rememberPagerState(initialPage = startIndex) { entries.size }
        val currentId = entries.getOrNull(pager.currentPage)?.entryId
        androidx.activity.compose.BackHandler { onClose(currentId) }
        LaunchedEffect(pager.currentPage, entries.size) {
            if (pager.currentPage >= entries.size - 3) onNearEnd()
        }
        val topInset = floatingTopBarInset()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { entries[it].entryId }) { page ->
                    val entry = entries[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, top = topInset, bottom = 32.dp)
                    ) {
                        EntryCard(
                            entry = entry,
                            isExpanded = true,
                            onToggleExpand = {},
                            actions = actions,
                            number = numbers[entry.entryId],
                            showExpandToggle = false,
                            textSelectable = true
                        )
                        if (page == entries.lastIndex && isLoadingNext) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                        }
                    }
                }
                FloatingTopBar(
                    title = title,
                    subtitle = numbers[currentId]?.let { "#$it" } ?: "${pager.currentPage + 1} / ${entries.size}",
                    onBack = { onClose(currentId) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

/** The full topic title above the entries, however long. */
@Composable
private fun TopicHeader(title: String, onClick: (() -> Unit)?) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/** Which filter is on, with a way back to the whole topic. */
@Composable
private fun FilterBanner(label: String, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 4.dp)) {
            Icon(Icons.Rounded.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            TextButton(onClick = onClear) { Text("tümü") }
        }
    }
}

@Composable
private fun OlderEntriesCard(onShow: () -> Unit) {
    FilledTonalButton(
        onClick = onShow,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(Icons.Rounded.History, contentDescription = null)
        Text("başlığın tamamını oku", modifier = Modifier.padding(start = 8.dp))
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
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "önceki sayfa")
            }
            TextButton(onClick = onPickPage) {
                Text(
                    "$currentPage / $totalPages",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNext, enabled = currentPage < totalPages) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "sonraki sayfa")
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

private fun shareTopic(context: android.content.Context, topic: Topic) {
    val path = topic.topicPath.ifBlank { topic.url }
    val link = if (path.startsWith("http")) path else EksiSession.BASE_URL + path
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "${topic.title}\n$link")
    context.startActivity(Intent.createChooser(send, null))
}
