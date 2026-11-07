package com.thesozluk.app.ui.screens

import com.thesozluk.app.ui.components.AppAlertDialog
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import com.thesozluk.app.ui.components.cardEdge
import com.thesozluk.app.ui.components.AppMenuItem
import com.thesozluk.app.ui.components.AppMenu
import com.thesozluk.app.ui.components.FloatingPromptHost
import com.thesozluk.app.ui.components.PromptState
import com.thesozluk.app.ui.components.glassSource
import com.thesozluk.app.ui.components.ShareEntryImageSheet
import com.thesozluk.app.settings.EntryBookmarks
import com.thesozluk.app.ui.components.RevealPullToRefresh
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.thesozluk.app.ui.components.EntryComposerSheet
import com.thesozluk.app.settings.AppSettings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import com.thesozluk.app.ui.components.EntryListSkeleton
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
import com.thesozluk.app.model.AuthorProfile
import com.thesozluk.app.model.Badge
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.ui.components.AuthorAvatar
import com.thesozluk.app.ui.components.EksiLink
import com.thesozluk.app.ui.components.EntryActions
import com.thesozluk.app.ui.components.EntryCard
import com.thesozluk.app.ui.components.rememberEntryInlineContent
import com.thesozluk.app.ui.components.rememberEntryText
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.floatingTopBarInset
import com.thesozluk.app.ui.components.isScrollingUp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.thesozluk.app.ui.components.ErrorState
import com.thesozluk.app.ui.components.LoadingState
import com.thesozluk.app.ui.components.LoginRequiredDialog
import com.thesozluk.app.ui.components.MessageState
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.openEksiLink
import com.thesozluk.app.viewmodel.AuthorTab
import com.thesozluk.app.viewmodel.AuthorViewModel
import com.thesozluk.app.viewmodel.EksiViewModelFactory

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
    var expandedEntries by remember(nick) { mutableStateOf(emptySet<String>()) }
    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var favoritersOf by remember { mutableStateOf<com.thesozluk.app.model.Entry?>(null) }
    val tabState = viewModel.tab(selectedTab)
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val prompt = remember { PromptState() }

    LaunchedEffect(nick) { viewModel.load(nick) }
    LaunchedEffect(profile, selectedTab) { if (profile != null) viewModel.ensureTabLoaded(selectedTab) }
    LaunchedEffect(message) {
        message?.let {
            prompt.show(it)
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
        com.thesozluk.app.ui.components.FollowListSheet(
            title = if (following) "takip ettikleri" else "takipçileri",
            total = if (following) profile?.followingCount ?: 0 else profile?.followerCount ?: 0,
            load = { viewModel.followList(following) },
            onAuthor = { other -> navController.navigate(Screen.Author.createRoute(other)) },
            onDismiss = { followSheet = null },
            // One's own following list: unfollow from here, as on the site's list
            onUnfollow = if (following && nick.equals(EksiSession.nick.value, ignoreCase = true)) ({ user ->
                com.thesozluk.app.network.AccountLists.removeRelation(com.thesozluk.app.network.RelationKind.Followed, user.id)
            }) else null
        )
    }
    var pendingRelation by remember { mutableStateOf<com.thesozluk.app.model.RelationAction?>(null) }
    favoritersOf?.let { entry ->
        com.thesozluk.app.ui.components.FavoritersSheet(
            entry = entry,
            load = { id, rookies -> viewModel.favoriters(id, rookies) },
            onAuthor = { nick -> navController.navigate(Screen.Author.createRoute(nick)) },
            onDismiss = { favoritersOf = null }
        )
    }
    val isOwnProfile = isLoggedIn && nick.equals(EksiSession.nick.value, ignoreCase = true)
    val isSubmitting by viewModel.isSubmitting
    val isRefreshing by viewModel.isRefreshing
    var shareImageOf by remember { mutableStateOf<com.thesozluk.app.model.Entry?>(null) }
    shareImageOf?.let { entry ->
        ShareEntryImageSheet(entry = entry, topicTitle = entry.topicTitle, onDismiss = { shareImageOf = null })
    }
    var editTarget by remember { mutableStateOf<Pair<com.thesozluk.app.model.Entry, com.thesozluk.app.model.FormSpec>?>(null) }
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
            scope.launch { prompt.show(if (EntryBookmarks.isSaved(entry.entryId)) "entry kaydedildi" else "kayıttan çıkarıldı") }
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
                    prompt.show(e.message ?: "düzeltme sayfası açılamadı")
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
            onUploadImage = form.imageUploader?.let { u ->
                { bytes: ByteArray, name: String, type: String ->
                    viewModel.uploadImage(u, bytes, name, type, "/entry/duzelt/${entry.entryId}")
                }
            },
            onDeleteImage = form.imageUploader?.takeIf { it.deleteUrl != null }?.let { u ->
                { key: String -> viewModel.deleteImage(u, key) }
            },
            onSubmit = { text -> viewModel.submitEdit(form, text) { editTarget = null } },
            onDismiss = { if (!isSubmitting) editTarget = null }
        )
    }

    // One's own profile: biography, picture and featured badges can be changed here
    val pickContext = LocalContext.current
    var cropping by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val pickPicture = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching { pickContext.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        // The picked picture goes to the cropper first
        cropping = bytes?.let { com.thesozluk.app.ui.components.decodeForCrop(it) }
    }
    cropping?.let { picture ->
        com.thesozluk.app.ui.components.ProfilePhotoCropDialog(
            bitmap = picture,
            onCrop = { square ->
                cropping = null
                viewModel.setProfilePicture(square)
            },
            onDismiss = { cropping = null }
        )
    }
    val ownProfileEditor = ProfileEditor(
        isSaving = viewModel.isSavingProfile.value,
        onSaveBiography = viewModel::saveBiography,
        onPickPicture = {
            pickPicture.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        },
        onRemovePicture = viewModel::removeProfilePicture,
        onSaveBadges = viewModel::selectBadges
    )

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
                    modifier = Modifier.fillMaxSize().then(pullOffset).glassSource(),
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
                            onShowEntries = {
                                scope.launch {
                                    listState.animateScrollToItem(1)
                                    selectedTab = AuthorTab.Latest
                                }
                            },
                            onBioLink = { link -> navController.openEksiLink(link, uriHandler) },
                            onAvatarClick = { url ->
                                ImageGallery.refs = emptyList()
                                navController.navigate(Screen.Image.createRoute(url))
                            },
                            editor = if (nick.equals(EksiSession.nick.value, ignoreCase = true)) ownProfileEditor else null
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
                            isExpanded = entry.entryId in expandedEntries,
                            onClick = {
                                navController.navigate(Screen.TopicDetail.createRoute(entry.topicTitle, "/entry/${entry.entryId}"))
                            },
                            onToggleExpand = {
                                expandedEntries = if (entry.entryId in expandedEntries) {
                                    expandedEntries - entry.entryId
                                } else {
                                    expandedEntries + entry.entryId
                                }
                            },
                            showExpandToggle = true,
                            actions = actions,
                            modifier = Modifier
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
            titleOffset = if (profile?.relations?.isNotEmpty() == true) 24.dp else 0.dp,
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
            // One's own profile: the account's lists on the site
            if (nick.equals(EksiSession.nick.value, ignoreCase = true)) {
                var ownMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { ownMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "diğer")
                    }
                    AppMenu(expanded = ownMenu, onDismissRequest = { ownMenu = false }) {
                        AppMenuItem(
                            text = "çöp kutusu",
                            icon = Icons.Rounded.RestoreFromTrash,
                            onClick = { ownMenu = false; navController.navigate(Screen.Trash.route) }
                        )
                        AppMenuItem(
                            text = "takip / engellenmiş",
                            icon = Icons.Rounded.PersonOff,
                            onClick = { ownMenu = false; navController.navigate(Screen.Relations.route) }
                        )
                    }
                }
            }
        }
        pendingRelation?.let { action ->
            val label = if (action.isAdded) action.removeLabel else action.label
            AppAlertDialog(
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
        FloatingPromptHost(prompt, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
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
    onShowEntries: () -> Unit,
    onBioLink: (EksiLink) -> Unit,
    /** Opens the profile picture full screen */
    onAvatarClick: (String) -> Unit,
    /** Set on one's own profile */
    editor: ProfileEditor? = null
) {
    var editingBio by remember { mutableStateOf(false) }
    var avatarMenu by remember { mutableStateOf(false) }
    if (editingBio && editor != null) {
        BiographyDialog(
            initial = profile.biography,
            isSaving = editor.isSaving,
            onSave = { text -> editor.onSaveBiography(text) { editingBio = false } },
            onDismiss = { editingBio = false }
        )
    }
    var openBadge by remember { mutableStateOf<Badge?>(null) }
    var showAllBadges by remember { mutableStateOf(false) }
    openBadge?.let { badge ->
        AppAlertDialog(
            onDismissRequest = { openBadge = null },
            icon = {
                AsyncImage(model = badge.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp))
            },
            title = { Text(badge.name) },
            text = { Text(badge.description) },
            confirmButton = { TextButton(onClick = { openBadge = null }) { Text("tamam") } }
        )
    }
    if (showAllBadges && editor != null) {
        FeaturedBadgesDialog(
            profile = profile,
            isSaving = editor.isSaving,
            onOpenBadge = { openBadge = it },
            onSave = { ids -> editor.onSaveBadges(ids) { showAllBadges = false } },
            onDismiss = { showAllBadges = false }
        )
    } else if (showAllBadges) {
        AppAlertDialog(
            onDismissRequest = { showAllBadges = false },
            title = { Text("bütün rozetler") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(profile.allBadges, key = { it.name }) { badge ->
                        Surface(
                            onClick = { showAllBadges = false; openBadge = badge },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.cardEdge(RoundedCornerShape(12.dp))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                AsyncImage(
                                    model = badge.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp).alpha(if (badge.owned) 1f else 0.32f)
                                )
                                Text(
                                    badge.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAllBadges = false }) { Text("kapat") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val avatarShape = MaterialShapes.Cookie12Sided.toShape()
        Box {
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
        if (editor != null) {
            // A small camera button on the picture's corner: upload a new one or remove it
            Box(Modifier.align(Alignment.BottomEnd)) {
                Surface(
                    onClick = { avatarMenu = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (editor.isSaving) {
                            androidx.compose.material3.CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = "profil fotoğrafını değiştir", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                androidx.compose.material3.DropdownMenu(expanded = avatarMenu, onDismissRequest = { avatarMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("yeni fotoğraf yükle") },
                        leadingIcon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                        enabled = !editor.isSaving,
                        onClick = { avatarMenu = false; editor.onPickPicture() }
                    )
                    if (profile.avatarUrl != null) {
                        DropdownMenuItem(
                            text = { Text("fotoğrafı sil") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            enabled = !editor.isSaving,
                            onClick = { avatarMenu = false; editor.onRemovePicture() }
                        )
                    }
                }
            }
        }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(profile.nick, style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
            if (profile.isRookie) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp).cardEdge(RoundedCornerShape(50))
                ) {
                    Text(
                        "çaylak",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            if (profile.isVerified) {
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = "onaylanmış hesap",
                    tint = Color(0xFF34A853),
                    modifier = Modifier.padding(start = 6.dp).size(24.dp)
                )
            }
            if (profile.isAdFree) {
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = "reklamsız abone",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.padding(start = 6.dp).size(24.dp)
                )
            }
        }
        if (profile.karma.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.cardEdge(RoundedCornerShape(50))
            ) {
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
                rememberEntryText(profile.biographyHtml, profile.biography, onBioLink),
                inlineContent = rememberEntryInlineContent(),
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
        if (editor != null) {
            androidx.compose.material3.FilledTonalButton(onClick = { editingBio = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                Text(if (profile.biography.isBlank()) "biyografi ekle" else "biyografi düzenle")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("entry", profile.entryCount, Modifier.weight(1f), onShowEntries)
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
        if (profile.badges.isNotEmpty() || (editor != null && profile.allBadges.isNotEmpty())) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(profile.badges, key = { it.name }) { badge ->
                    Surface(
                        onClick = { openBadge = badge },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.cardEdge(RoundedCornerShape(20.dp))
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
                val featuredBadgeNames = profile.badges.mapTo(mutableSetOf()) { it.name.trim().lowercase() }
                val featuredBadgeImages = profile.badges.map { it.imageUrl }.filter(String::isNotBlank).toSet()
                val additionalOwnedBadges = profile.allBadges.count {
                    it.owned && it.name.trim().lowercase() !in featuredBadgeNames &&
                        (it.imageUrl.isBlank() || it.imageUrl !in featuredBadgeImages)
                }
                if (profile.allBadges.size > profile.badges.size || editor != null) {
                    item(key = "all-badges") {
                        Surface(
                            onClick = { showAllBadges = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.cardEdge(RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.width(88.dp).padding(10.dp).height(74.dp)
                            ) {
                                if (editor != null) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Text("düzenle", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    // "+N" only when there are more earned ones than shown
                                    if (additionalOwnedBadges > 0) {
                                        Text("+$additionalOwnedBadges", style = MaterialTheme.typography.titleMedium)
                                        Text("tümü", style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        Text("tümü", style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                            }
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
        modifier = modifier.cardEdge(RoundedCornerShape(24.dp))
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
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        tabs.forEachIndexed { index, tab ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = tab == selected,
                onCheckedChange = { onSelect(tab) },
                shapes = shapes,
                // Liquid glass: the light line along the top, as on the home lists' buttons
                modifier = Modifier.cardEdge(if (tab == selected) shapes.checkedShape else shapes.shape)
            ) { Text(tab.label) }
        }
    }
    }
}

/** The profile's other relation buttons (block, block topics, mute) in a menu. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RelationMenu(
    relations: List<com.thesozluk.app.model.RelationAction>,
    onPick: (com.thesozluk.app.model.RelationAction) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "diğer")
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            relations.forEach { action ->
                AppMenuItem(
                    text = if (action.isAdded) action.removeLabel else action.label,
                    icon = if (action.isAdded) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Block,
                    onClick = {
                        open = false
                        onPick(action)
                    }
                )
            }
        }
    }
}

/** What one's own profile page can change; the page shows its edit controls when this is given. */
class ProfileEditor(
    val isSaving: Boolean,
    val onSaveBiography: (text: String, onSuccess: () -> Unit) -> Unit,
    val onPickPicture: () -> Unit,
    val onRemovePicture: () -> Unit,
    val onSaveBadges: (ids: List<String>, onSuccess: () -> Unit) -> Unit
)

/** Writing the biography; the site's (bkz: ) style links work in it as in entries. */
@Composable
private fun BiographyDialog(initial: String, isSaving: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AppAlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("biyografi") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("kendinden bahset. (bkz: hede) gibi bağlantılar da olur") },
                minLines = 4,
                maxLines = 10,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = !isSaving && text.isNotBlank() && text.trim() != initial.trim()) {
                Text(if (isSaving) "kaydediliyor" else "kaydet")
            }
        },
        dismissButton = {
            Row {
                if (initial.isNotBlank()) {
                    TextButton(onClick = { onSave("") }, enabled = !isSaving) {
                        Text("sil", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss, enabled = !isSaving) { Text("vazgeç") }
            }
        }
    )
}

/** The site lets four badges stand on the profile, in the order they were chosen. */
private const val MAX_FEATURED_BADGES = 4

/**
 * Like the site's badge page: the featured badges in slots at the top, all badges below. Hold a
 * badge and drag it up onto a slot to put it there (the one in the slot goes back down); slots
 * can be dragged onto each other to change the order. The site can't take a badge off the
 * profile, so there are always as many slots as earned badges, up to four.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedBadgesDialog(
    profile: AuthorProfile,
    isSaving: Boolean,
    onOpenBadge: (Badge) -> Unit,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val earned = remember(profile.allBadges) { profile.allBadges.filter { it.owned && it.id.isNotBlank() } }
    val required = earned.size.coerceAtMost(MAX_FEATURED_BADGES)
    // The featured ones by id; the profile page's list may lack ids, so match by name too
    val initial = remember(profile) {
        val featured = profile.badges.mapNotNull { badge ->
            badge.id.ifBlank { null }
                ?: earned.firstOrNull { it.name.trim().equals(badge.name.trim(), ignoreCase = true) }?.id
        }.filter { id -> earned.any { it.id == id } }.distinct()
        // Fill up to the slot count, as the site shows that many anyway
        (featured + earned.map { it.id }.filter { it !in featured }).take(required)
    }
    val slots = remember(initial) { mutableStateListOf<String>().apply { addAll(initial) } }
    val byId = remember(profile.allBadges) { profile.allBadges.associateBy { it.id } }
    // Earned ones first, so they are at hand
    val badges = remember(profile.allBadges) { profile.allBadges.sortedByDescending { it.owned } }

    // Dragging: what is held, where the finger is, and where each slot sits (window coordinates)
    var dragged by remember { mutableStateOf<Badge?>(null) }
    var fromSlot by remember { mutableIntStateOf(-1) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    val hoveredSlot = slotBounds.entries.firstOrNull { dragged != null && it.value.contains(finger) }?.key
    val haptics = LocalHapticFeedback.current

    fun drop() {
        val badge = dragged ?: return
        // Read now, not from the composition the gesture started in
        val target = slotBounds.entries.firstOrNull { it.value.contains(finger) }?.key
        if (target != null) {
            if (fromSlot >= 0) {
                // Slot onto slot: swap them
                val other = slots[target]
                slots[target] = slots[fromSlot]
                slots[fromSlot] = other
            } else {
                // From the list: takes the slot's place; if already featured elsewhere, they trade
                val existing = slots.indexOf(badge.id)
                if (existing >= 0) slots[existing] = slots[target]
                slots[target] = badge.id
            }
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        dragged = null
        fromSlot = -1
    }

    // Hold and drag; the start point is turned into window coordinates with the item's position
    @Composable
    fun Modifier.dragSource(badge: Badge, slot: Int): Modifier {
        // Kept across recompositions, so the drag reads where the item is now (the list scrolls)
        val itemOrigin = remember { arrayOf(Offset.Zero) }
        return this
            .onGloballyPositioned { itemOrigin[0] = it.positionInWindow() }
            .pointerInput(badge.id, slot, isSaving) {
                if (isSaving) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        dragged = badge
                        fromSlot = slot
                        finger = itemOrigin[0] + at
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        finger += amount
                    },
                    onDragEnd = { drop() },
                    onDragCancel = { dragged = null; fromSlot = -1 }
                )
            }
    }

    AppAlertDialog(
        onDismissRequest = { if (!isSaving && dragged == null) onDismiss() },
        title = { Text("rozetler") },
        text = {
            Box(Modifier.onGloballyPositioned { origin = it.positionInWindow() }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (required == 0) {
                        Text(
                            "henüz kazandığın bir rozet yok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("öne çıkan rozetler", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            slots.forEachIndexed { index, id ->
                                val badge = byId[id]
                                val hovered = hoveredSlot == index
                                val scale by animateFloatAsState(if (hovered) 1.08f else 1f, label = "slotScale")
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (hovered) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    border = androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        if (hovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.85f)
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .cardEdge(RoundedCornerShape(16.dp))
                                        .onGloballyPositioned { slotBounds[index] = it.boundsInWindow() }
                                        .then(if (badge != null) Modifier.dragSource(badge, index) else Modifier)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        // The slot's place in the order
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                        AsyncImage(
                                            model = badge?.imageUrl,
                                            contentDescription = badge?.name,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .alpha(if (dragged != null && fromSlot == index) 0.3f else 1f)
                                        )
                                        Text(
                                            badge?.name.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            "öne çıkarmak istediğin rozeti basılı tutup yukarı, bir yerin üstüne sürükle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Text("bütün rozetler", style = MaterialTheme.typography.titleSmall)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 80.dp),
                        modifier = Modifier.heightIn(max = 320.dp),
                        userScrollEnabled = dragged == null,
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(badges, key = { it.id.ifBlank { it.name } }) { badge ->
                            val featured = badge.id in slots
                            val canPick = badge.owned && badge.id.isNotBlank() && required > 0
                            Surface(
                                onClick = { onOpenBadge(badge) },
                                color = if (featured) MaterialTheme.colorScheme.surfaceContainerHighest
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .cardEdge(RoundedCornerShape(12.dp))
                                    .then(if (canPick) Modifier.dragSource(badge, -1) else Modifier)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                    AsyncImage(
                                        model = badge.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .alpha(
                                                when {
                                                    !badge.owned -> 0.3f
                                                    dragged?.id == badge.id && fromSlot < 0 -> 0.3f
                                                    else -> 1f
                                                }
                                            )
                                    )
                                    Text(
                                        badge.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        minLines = 2,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                // The held badge, following the finger
                dragged?.let { badge ->
                    val size = 56.dp
                    val half = with(LocalDensity.current) { (size / 2).toPx() }
                    AsyncImage(
                        model = badge.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .zIndex(10f)
                            .graphicsLayer {
                                translationX = finger.x - origin.x - half
                                translationY = finger.y - origin.y - half
                                scaleX = 1.15f
                                scaleY = 1.15f
                                shadowElevation = 12.dp.toPx()
                                shape = CircleShape
                            }
                            .size(size)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(slots.toList()) },
                enabled = !isSaving && slots.size == required && slots.toList() != initial
            ) {
                Text(if (isSaving) "kaydediliyor" else "kaydet")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("vazgeç") } }
    )
}
