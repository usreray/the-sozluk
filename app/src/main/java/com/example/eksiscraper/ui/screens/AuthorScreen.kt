package com.example.eksiscraper.ui.screens

import com.example.eksiscraper.ui.components.ShareEntryImageSheet
import com.example.eksiscraper.settings.EntryBookmarks
import com.example.eksiscraper.ui.components.RevealPullToRefresh
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.eksiscraper.ui.components.EntryComposerSheet
import com.example.eksiscraper.settings.AppSettings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import com.example.eksiscraper.ui.components.EntryListSkeleton
import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuDefaults
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.eksiscraper.model.AuthorProfile
import com.example.eksiscraper.model.Badge
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.AuthorAvatar
import com.example.eksiscraper.ui.components.EntryActions
import com.example.eksiscraper.ui.components.EntryCard
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.floatingTopBarInset
import com.example.eksiscraper.ui.components.isScrollingUp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.LoginRequiredDialog
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.ui.navigation.openEksiLink
import com.example.eksiscraper.viewmodel.AuthorTab
import com.example.eksiscraper.viewmodel.AuthorViewModel
import com.example.eksiscraper.viewmodel.EksiViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthorScreen(
    nick: String,
    navController: NavController,
    viewModel: AuthorViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val profile by viewModel.profile
    val profileError by viewModel.profileError
    val message by viewModel.message
    val isLoggedIn by EksiSession.isLoggedIn
    var selectedTab by rememberSaveable { mutableStateOf(AuthorTab.Latest) }
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var favoritersOf by remember { mutableStateOf<com.example.eksiscraper.model.Entry?>(null) }
    val tabState = viewModel.tab(selectedTab)
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(nick) { viewModel.load(nick) }
    LaunchedEffect(profile, selectedTab) { if (profile != null) viewModel.ensureTabLoaded(selectedTab) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, tabState.entries.size, selectedTab) {
        if (nearEnd) viewModel.loadMore(selectedTab)
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

    val requireLogin: (() -> Unit) -> Unit = { action -> if (isLoggedIn) action() else showLoginDialog = true }
    // true: who they follow, false: their followers, null: closed
    var followSheet by remember { mutableStateOf<Boolean?>(null) }
    followSheet?.let { following ->
        com.example.eksiscraper.ui.components.FollowListSheet(
            title = if (following) "takip ettikleri" else "takipçileri",
            total = if (following) profile?.followingCount ?: 0 else profile?.followerCount ?: 0,
            load = { viewModel.followList(following) },
            onAuthor = { other -> navController.navigate(Screen.Author.createRoute(other)) },
            onDismiss = { followSheet = null }
        )
    }
    var pendingRelation by remember { mutableStateOf<com.example.eksiscraper.model.RelationAction?>(null) }
    favoritersOf?.let { entry ->
        com.example.eksiscraper.ui.components.FavoritersSheet(
            entry = entry,
            load = { id, rookies -> viewModel.favoriters(id, rookies) },
            onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
            onDismiss = { favoritersOf = null }
        )
    }
    val isOwnProfile = isLoggedIn && nick.equals(EksiSession.nick.value, ignoreCase = true)
    val isSubmitting by viewModel.isSubmitting
    val isRefreshing by viewModel.isRefreshing
    var shareImageOf by remember { mutableStateOf<com.example.eksiscraper.model.Entry?>(null) }
    shareImageOf?.let { entry ->
        ShareEntryImageSheet(entry = entry, topicTitle = entry.topicTitle, onDismiss = { shareImageOf = null })
    }
    var editTarget by remember { mutableStateOf<Pair<com.example.eksiscraper.model.Entry, com.example.eksiscraper.model.FormSpec>?>(null) }
    val scope = rememberCoroutineScope()
    val actions = EntryActions(
        onShowFavoriters = { entry -> requireLogin { favoritersOf = entry } },
        onToggleFavorite = { entry -> requireLogin { viewModel.toggleFavorite(entry) } },
        onVote = { entry, rate -> requireLogin { viewModel.vote(entry, rate) } },
        onAuthor = { other -> if (other != nick) navController.navigate(Screen.Author.createRoute(other)) },
        onLink = { link -> navController.openEksiLink(link, uriHandler) },
        onOpenTopic = { entry ->
            navController.navigate(Screen.TopicDetail.createRoute(entry.topicTitle, entry.topicUrl.ifBlank { "/entry/${entry.entryId}" }))
        },
        onBookmark = { entry ->
            if (EntryBookmarks.isSaved(entry.entryId)) EntryBookmarks.remove(entry.entryId)
            else EntryBookmarks.save(entry, entry.topicTitle, entry.topicUrl)
            scope.launch { snackbar.showSnackbar(if (EntryBookmarks.isSaved(entry.entryId)) "entry kaydedildi" else "kayıttan çıkarıldı") }
        },
        onShareImage = { entry -> shareImageOf = entry },
        // Own profile: the user's entries can be edited and deleted from here too
        onDelete = if (isOwnProfile) viewModel::deleteEntry else null,
        onEdit = if (isOwnProfile) ({ entry ->
            scope.launch {
                try {
                    editTarget = entry to viewModel.editForm(entry)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snackbar.showSnackbar(e.message ?: "düzeltme sayfası açılamadı")
                }
            }
        }) else null
    )
    editTarget?.let { (entry, form) ->
        EntryComposerSheet(
            topicTitle = entry.topicTitle,
            heading = "entry'yi düzelt",
            isSubmitting = isSubmitting,
            initialText = form.textValue.ifBlank { entry.content },
            onSubmit = { text -> viewModel.submitEdit(form, text) { editTarget = null } },
            onDismiss = { if (!isSubmitting) editTarget = null }
        )
    }

    // The nick moves into the floating bar once the big header has scrolled away
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    // A surface at the root sets the background and the default text color (no Scaffold here)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = when {
                profileError != null -> 2
                profile == null -> 0
                else -> 1
            },
            label = "authorPhase"
        ) { phase ->
            when (phase) {
                0 -> LoadingState(messages = listOf("profil açılıyor", "rozetler parlatılıyor"))
                2 -> ErrorState(message = profileError.orEmpty(), onRetry = viewModel::retry)
                else -> RevealPullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh(selectedTab) },
                    top = floatingTopBarInset(compact = true)
                ) { pullOffset ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().then(pullOffset),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = floatingTopBarInset(compact = true), bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "header") {
                        ProfileHeader(
                            profile = profile!!,
                            isOwn = nick.equals(EksiSession.nick.value, ignoreCase = true),
                            onFollow = { requireLogin { viewModel.toggleFollow() } },
                            onMessage = { requireLogin { navController.navigate(Screen.Messages.createRoute(nick)) } },
                            onShowFollows = { following -> followSheet = following },
                            onAvatarClick = { url ->
                                ImageGallery.refs = emptyList()
                                navController.navigate(Screen.Image.createRoute(url))
                            }
                        )
                    }
                    item(key = "tabs") {
                        TabButtons(selected = selectedTab, onSelect = { selectedTab = it })
                    }
                    // Images as a three-column grid of square thumbnails; a tap opens the viewer
                    items(tabState.images.chunked(3), key = { "img:${it.first().ref}" }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { image ->
                                AsyncImage(
                                    model = image.thumbnailUrl,
                                    contentDescription = "görsel",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable {
                                            // The viewer pages through all of the author's images
                                            ImageGallery.refs = tabState.images.map { it.ref }
                                            navController.navigate(Screen.Image.createRoute(image.ref))
                                        }
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    items(tabState.entries, key = { "${selectedTab.name}:${it.entryId}" }) { entry ->
                        EntryCard(
                            entry = entry,
                            isExpanded = false,
                            onToggleExpand = {
                                navController.navigate(Screen.TopicDetail.createRoute(entry.topicTitle, "/entry/${entry.entryId}"))
                            },
                            actions = actions,
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "footer:${selectedTab.name}") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            when {
                                // First page of a tab: entry-shaped placeholders; later pages: a small indicator
                                tabState.isLoading && selectedTab == AuthorTab.Images -> LoadingIndicator()
                                tabState.isLoading && tabState.entries.isEmpty() -> EntryListSkeleton(contentPadding = PaddingValues(0.dp))
                                tabState.isLoading -> LoadingIndicator()
                                tabState.error != null -> TextButton(onClick = { viewModel.retryTab(selectedTab) }) {
                                    Text("yüklenemedi, tekrar dene")
                                }
                                tabState.page > 0 && tabState.entries.isEmpty() && tabState.images.isEmpty() -> MessageState(
                                    icon = Icons.Rounded.Inbox,
                                    title = if (selectedTab == AuthorTab.Images) "hiç görsel paylaşmamış" else "burada entry yok"
                                )
                            }
                        }
                    }
                }
                }
            }
        }

        val context = LocalContext.current
        FloatingTopBar(
            title = if (headerGone) nick else null,
            onBack = { navController.popBackStack() },
            // The nick in the bar takes the page back to its top
            onTitleClick = { scope.launch { listState.animateScrollToItem(0) } },
            visible = !AppSettings.hideBarsOnScroll.value || listState.isScrollingUp(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            IconButton(onClick = {
                val link = "${EksiSession.BASE_URL}/biri/${nick.trim().replace(' ', '-')}"
                val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(android.content.Intent.EXTRA_TEXT, link)
                context.startActivity(android.content.Intent.createChooser(send, null))
            }) {
                Icon(Icons.Rounded.Share, contentDescription = "profili paylaş")
            }
            val relations = profile?.relations.orEmpty()
            if (relations.isNotEmpty()) RelationMenu(relations, onPick = { pendingRelation = it })
        }
        pendingRelation?.let { action ->
            val label = if (action.isAdded) action.removeLabel else action.label
            AlertDialog(
                onDismissRequest = { pendingRelation = null },
                title = { Text("$label?") },
                text = { Text("$nick için \"$label\" uygulanacak.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingRelation = null
                        viewModel.toggleRelation(action)
                    }) { Text("evet") }
                },
                dismissButton = { TextButton(onClick = { pendingRelation = null }) { Text("vazgeç") } }
            )
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileHeader(
    profile: AuthorProfile,
    isOwn: Boolean,
    onFollow: () -> Unit,
    onMessage: () -> Unit,
    onShowFollows: (following: Boolean) -> Unit,
    /** Opens the profile picture full screen */
    onAvatarClick: (String) -> Unit
) {
    var openBadge by remember { mutableStateOf<Badge?>(null) }
    openBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { openBadge = null },
            icon = {
                AsyncImage(model = badge.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp))
            },
            title = { Text(badge.name) },
            text = { Text(badge.description) },
            confirmButton = { TextButton(onClick = { openBadge = null }) { Text("tamam") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val avatarShape = MaterialShapes.Cookie12Sided.toShape()
        if (profile.avatarUrl != null) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = profile.nick,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(avatarShape)
                    .clickable { profile.avatarUrl?.let(onAvatarClick) }
            )
        } else {
            AuthorAvatar(profile.nick, size = 112)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(profile.nick, style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
            if (profile.isVerified) {
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = "onaylanmış hesap",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp).size(24.dp)
                )
            }
        }
        if (profile.karma.isNotBlank()) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    profile.karma,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        if (profile.biography.isNotBlank()) {
            // Some bios are essays; keep the header short until asked
            var bioExpanded by remember { mutableStateOf(false) }
            var bioOverflows by remember { mutableStateOf(false) }
            Text(
                profile.biography,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = if (bioExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!bioExpanded) bioOverflows = it.hasVisualOverflow },
                modifier = Modifier.animateContentSize()
            )
            if (bioOverflows || bioExpanded) {
                TextButton(onClick = { bioExpanded = !bioExpanded }) {
                    Text(if (bioExpanded) "daha az" else "devamı")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("entry", profile.entryCount, Modifier.weight(1f))
            StatTile("takipçi", profile.followerCount, Modifier.weight(1f)) { onShowFollows(false) }
            StatTile("takip", profile.followingCount, Modifier.weight(1f)) { onShowFollows(true) }
        }
        if (!isOwn) {
            // Follow / message, as on the site's profile buttons
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ToggleButton(
                    checked = profile.isFollowing,
                    onCheckedChange = { onFollow() },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Icon(
                        if (profile.isFollowing) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                    Text(if (profile.isFollowing) "takip ediliyor" else "takip et")
                }
                ToggleButton(
                    checked = false,
                    onCheckedChange = { onMessage() },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Icon(Icons.Rounded.Mail, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                    Text("mesaj")
                }
            }
        }
        if (profile.joinedDate.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "${profile.joinedDate} tarihinden beri",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        if (profile.badges.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(profile.badges, key = { it.name }) { badge ->
                    Surface(
                        onClick = { openBadge = badge },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(88.dp).padding(10.dp)
                        ) {
                            AsyncImage(model = badge.imageUrl, contentDescription = badge.name, modifier = Modifier.size(44.dp))
                            Text(
                                badge.name,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                // Always two lines tall, so one-line and two-line names make equal cards
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                "%,d".format(value).replace(',', '.'),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.primary
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Profile lists as a scrollable row of connected toggle buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabButtons(selected: AuthorTab, onSelect: (AuthorTab) -> Unit) {
    val tabs = AuthorTab.entries
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        tabs.forEachIndexed { index, tab ->
            ToggleButton(
                checked = tab == selected,
                onCheckedChange = { onSelect(tab) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) { Text(tab.label) }
        }
    }
}

/** The profile's other relation buttons (block, block topics, mute) in a menu. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RelationMenu(
    relations: List<com.example.eksiscraper.model.RelationAction>,
    onPick: (com.example.eksiscraper.model.RelationAction) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "diğer")
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                relations.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(if (action.isAdded) action.removeLabel else action.label) },
                        leadingIcon = {
                            Icon(
                                if (action.isAdded) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Block,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            open = false
                            onPick(action)
                        }
                    )
                }
            }
        }
    }
}
