package com.example.eksiscraper.ui.screens

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
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
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
    val actions = EntryActions(
        onToggleFavorite = { entry -> requireLogin { viewModel.toggleFavorite(entry) } },
        onVote = { entry, rate -> requireLogin { viewModel.vote(entry, rate) } },
        onAuthor = { other -> if (other != nick) navController.navigate(Screen.Author.createRoute(other)) },
        onLink = { link -> navController.openEksiLink(link, uriHandler) },
        onOpenTopic = { entry -> navController.navigate(Screen.TopicDetail.createRoute(entry.topicTitle, "/entry/${entry.entryId}")) }
    )

    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = headerGone, enter = fadeIn(), exit = fadeOut()) { Text(nick) }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Crossfade(
            targetState = when {
                profileError != null -> 2
                profile == null -> 0
                else -> 1
            },
            modifier = Modifier.padding(padding),
            label = "authorPhase"
        ) { phase ->
            when (phase) {
                0 -> LoadingState(messages = listOf("profil açılıyor", "rozetler parlatılıyor"))
                2 -> ErrorState(message = profileError.orEmpty(), onRetry = viewModel::retry)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "header") { ProfileHeader(profile!!) }
                    item(key = "tabs") {
                        TabButtons(selected = selectedTab, onSelect = { selectedTab = it })
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
                                tabState.isLoading -> LoadingIndicator()
                                tabState.error != null -> TextButton(onClick = { viewModel.retryTab(selectedTab) }) {
                                    Text("Yüklenemedi, tekrar dene")
                                }
                                tabState.page > 0 && tabState.entries.isEmpty() -> MessageState(
                                    icon = Icons.Rounded.Inbox,
                                    title = "Burada entry yok"
                                )
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
private fun ProfileHeader(profile: AuthorProfile) {
    var openBadge by remember { mutableStateOf<Badge?>(null) }
    openBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { openBadge = null },
            icon = {
                AsyncImage(model = badge.imageUrl, contentDescription = null, modifier = Modifier.size(56.dp))
            },
            title = { Text(badge.name) },
            text = { Text(badge.description) },
            confirmButton = { TextButton(onClick = { openBadge = null }) { Text("Tamam") } }
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
                modifier = Modifier.size(112.dp).clip(avatarShape)
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
            StatTile("takipçi", profile.followerCount, Modifier.weight(1f))
            StatTile("takip", profile.followingCount, Modifier.weight(1f))
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
                                maxLines = 2,
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
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
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
