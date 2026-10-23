package com.thesozluk.app.ui.screens

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.runtime.snapshotFlow
import com.thesozluk.app.ui.components.pressBounce
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.app.Application
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.settings.EntryBookmarks
import com.thesozluk.app.ui.components.AuthorAvatar
import com.thesozluk.app.ui.components.FavoriteIcons
import com.thesozluk.app.ui.components.LocalBottomBarInset
import com.thesozluk.app.ui.components.LoginRequiredDialog
import com.thesozluk.app.ui.components.ShareEntryImageSheet
import com.thesozluk.app.ui.components.entryBodyStyle
import com.thesozluk.app.ui.components.rememberEntryInlineContent
import com.thesozluk.app.ui.components.rememberEntryText
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.TabReselect
import com.thesozluk.app.ui.navigation.openEksiLink
import com.thesozluk.app.viewmodel.EksiViewModelFactory
import com.thesozluk.app.viewmodel.RandomMode
import com.thesozluk.app.viewmodel.RandomPick
import com.thesozluk.app.viewmodel.RandomViewModel
import kotlinx.coroutines.launch

/**
 * "rastgele": random entries arrive like messages in a chat, each from its author with the topic
 * it belongs to underneath; older ones stay above.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RandomScreen(
    navController: NavController,
    viewModel: RandomViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    var mode by rememberSaveable { mutableStateOf(RandomMode.Mixed) }
    val picks = viewModel.picks
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val message by viewModel.message
    val isLoggedIn by EksiSession.isLoggedIn
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLoginDialog by remember { mutableStateOf(false) }
    var shareImageOf by remember { mutableStateOf<RandomPick?>(null) }

    // The first visit brings one right away; tapping the tab again brings another
    LaunchedEffect(Unit) { if (viewModel.picks.isEmpty()) viewModel.next(mode) }
    LaunchedEffect(Unit) {
        TabReselect.events.collect { route -> if (route == Screen.Random.route) viewModel.next(mode) }
    }
    // The chat is laid out from the bottom, so new messages push the older ones up without the
    // list jumping; after reading further up, a new one brings the view back down
    LaunchedEffect(isLoading) {
        if (isLoading && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
            listState.animateScrollToItem(0)
        }
    }
    // A new entry taller than the screen would show its end (the chat is anchored at the bottom);
    // bring its beginning into view instead, so it reads from the top
    // Once per entry: coming back to the tab keeps where the reader was
    var aligned by rememberSaveable { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    LaunchedEffect(picks.lastOrNull()?.entry?.entryId) {
        val newest = picks.lastOrNull()?.entry?.entryId ?: return@LaunchedEffect
        if (newest == aligned) return@LaunchedEffect
        aligned = newest
        // Wait until the loading row (the first visit's spinner or the typing bubble) is gone and
        // the entry sits at the bottom, so its size and position are final
        val item = snapshotFlow {
            if (viewModel.isLoading.value) null
            else listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == newest && it.index == 0 }
        }.filterNotNull().first()
        val info = listState.layoutInfo
        val room = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
        // Some room above it too, so the author's name and the bubble's top edge are in view
        val margin = with(density) { 24.dp.roundToPx() }
        if (item.size + margin > room) listState.animateScrollToItem(0, item.size - room + margin)
        else listState.animateScrollToItem(0)
    }
    // The chat is anchored at the bottom, so a bubble that grows ("devamını oku") would push its
    // start up and off the screen. Follow its size while it animates and scroll by the change, so
    // the text stays where the reader was and simply continues below.
    val keepTopOf: (String) -> Unit = { key ->
        scope.launch {
            var previous = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.size ?: return@launch
            repeat(45) {
                withFrameNanos { }
                val size = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.size ?: return@launch
                if (size != previous) {
                    listState.scrollBy((size - previous).toFloat())
                    previous = size
                }
            }
        }
    }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    if (showLoginDialog) {
        LoginRequiredDialog(
            onLogin = { showLoginDialog = false; navController.navigate(Screen.Login.route) },
            onDismiss = { showLoginDialog = false }
        )
    }
    shareImageOf?.let { pick ->
        ShareEntryImageSheet(entry = pick.entry, topicTitle = pick.topicTitle, onDismiss = { shareImageOf = null })
    }
    var opening by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    // The entry inside its own topic: on the page it is on, scrolled to it
    val openTopic: (RandomPick) -> Unit = { pick ->
        if (opening == null) scope.launch {
            opening = pick.entry.entryId
            val page = viewModel.pageOf(pick)
            opening = null
            navController.navigate(
                Screen.TopicDetail.createRoute(
                    pick.topicTitle,
                    pick.topicPath.ifBlank { "/entry/${pick.entry.entryId}" },
                    page = page,
                    focus = pick.entry.entryId
                )
            )
        }
    }
    // A small tick when a new entry arrives
    LaunchedEffect(picks.size) { if (picks.isNotEmpty()) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            // Title with the modes next to it
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("rastgele", style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        mode.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Scrolls sideways when large text makes the chips wider than the screen
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp)
            ) {
                RandomMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { if (mode != item) { mode = item; viewModel.next(item) } },
                        label = { Text(item.label, maxLines = 1, softWrap = false) },
                        leadingIcon = {
                            Icon(
                                when (item) {
                                    RandomMode.Good -> Icons.Rounded.Star
                                    RandomMode.Deep -> Icons.Rounded.Waves
                                    RandomMode.Mixed -> Icons.Rounded.Shuffle
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 96.dp + LocalBottomBarInset.current
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                // Newest at the bottom like a chat; items are declared newest first
                reverseLayout = true
            ) {
                error?.let { text ->
                    item(key = "error") {
                        Text(
                            text,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
                if (isLoading && picks.isEmpty()) {
                    item(key = "first") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 96.dp)
                        ) {
                            LoadingIndicator(Modifier.size(64.dp))
                            Text("ilk entry geliyor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (isLoading) {
                    item(key = "typing") { TypingBubble(Modifier.animateItem()) }
                }
                items(picks.asReversed(), key = { it.entry.entryId }) { pick ->
                    EntryBubble(
                        pick = pick,
                        modifier = Modifier.animateItem(fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                        onAuthor = { navController.navigate(Screen.Author.createRoute(pick.entry.author)) },
                        onLink = { link -> navController.openEksiLink(link, uriHandler) },
                        onTopic = { openTopic(pick) },
                        onResize = { keepTopOf(pick.entry.entryId) },
                        isOpening = opening == pick.entry.entryId,
                        onFavorite = { if (isLoggedIn) viewModel.toggleFavorite(pick.entry) else showLoginDialog = true },
                        onBookmark = {
                            if (EntryBookmarks.isSaved(pick.entry.entryId)) EntryBookmarks.remove(pick.entry.entryId)
                            else EntryBookmarks.save(pick.entry, pick.topicTitle, pick.topicPath)
                            scope.launch {
                                snackbar.showSnackbar(if (EntryBookmarks.isSaved(pick.entry.entryId)) "entry kaydedildi" else "kayıttan çıkarıldı")
                            }
                        },
                        onShareImage = { shareImageOf = pick }
                    )
                }
            }
        }
        val fabInteraction = remember { MutableInteractionSource() }
        ExtendedFloatingActionButton(
            onClick = { if (!isLoading) viewModel.next(mode) },
            interactionSource = fabInteraction,
            icon = {
                // While fetching, the dice keeps rolling (the same dice as on the tab)
                val roll = rememberInfiniteTransition(label = "dice")
                val turn by roll.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing)),
                    label = "diceTurn"
                )
                Icon(
                    Icons.Rounded.Casino,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = if (isLoading) turn else 0f }
                )
            },
            text = { Text("başka getir") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp + LocalBottomBarInset.current)
                .pressBounce(fabInteraction, pressedScale = 0.9f)
        )
        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp + LocalBottomBarInset.current)
        )
    }
}

/** One entry as a message: avatar and nick, the text in a bubble, the topic and favorites below. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EntryBubble(
    pick: RandomPick,
    modifier: Modifier,
    onAuthor: () -> Unit,
    onLink: (com.thesozluk.app.ui.components.EksiLink) -> Unit,
    onTopic: () -> Unit,
    /** "devamını oku" / "daha az": the bubble is about to change height */
    onResize: () -> Unit,
    isOpening: Boolean,
    onFavorite: () -> Unit,
    onBookmark: () -> Unit,
    onShareImage: () -> Unit
) {
    val entry = pick.entry
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(entry.entryId) { mutableStateOf(false) }
    var overflows by remember(entry.entryId) { mutableStateOf(false) }
    var spoilersRevealed by rememberSaveable(entry.entryId) { mutableStateOf(false) }
    val text = rememberEntryText(
        entry.contentHtml,
        entry.content,
        onLink,
        spoilersHidden = !spoilersRevealed,
        onToggleSpoilers = if (AppSettings.hideSpoilers.value) ({ spoilersRevealed = !spoilersRevealed }) else null
    )
    Row(verticalAlignment = Alignment.Top, modifier = modifier.fillMaxWidth()) {
        Box(Modifier.padding(top = 2.dp).clip(CircleShape).clickable(onClick = onAuthor)) {
            AuthorAvatar(entry.author, size = 36, avatarUrl = entry.avatarUrl.takeIf { AppSettings.showAvatars.value })
        }
        Column(Modifier.padding(start = 10.dp).widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                // A long nick gives way (ends with …) so the date always shows in full
                Text(
                    entry.author,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).clickable(onClick = onAuthor)
                )
                Text(
                    "  ·  ${entry.date.substringBefore(" ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Box {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.combinedClickable(onClick = onTopic, onLongClick = { menu = true })
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).animateContentSize()) {
                        Text(
                            text,
                            inlineContent = rememberEntryInlineContent(),
                            style = entryBodyStyle(),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (expanded) Int.MAX_VALUE else 12,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
                        )
                        if (overflows || expanded) {
                            Text(
                                if (expanded) "daha az" else "devamını oku",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp).clickable {
                                    expanded = !expanded
                                    onResize()
                                }
                            )
                        }
                    }
                }
                if (isOpening) {
                    LoadingIndicator(Modifier.align(Alignment.Center).size(40.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("başlığı aç") }, onClick = { menu = false; onTopic() })
                    DropdownMenuItem(text = { Text("metni kopyala") }, onClick = { menu = false; clipboard.setText(AnnotatedString(entry.content)) })
                    DropdownMenuItem(
                        text = { Text("linki kopyala") },
                        onClick = { menu = false; clipboard.setText(AnnotatedString("https://eksisozluk.com/entry/${entry.entryId}")) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (EntryBookmarks.isSaved(entry.entryId)) "kayıttan çıkar" else "kaydet") },
                        onClick = { menu = false; onBookmark() }
                    )
                    DropdownMenuItem(text = { Text("görsel olarak paylaş") }, onClick = { menu = false; onShareImage() })
                }
            }
            // The topic it belongs to, its favorites and where it came from
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Surface(
                    onClick = onTopic,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        "# " + pick.topicTitle,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onFavorite).padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        if (entry.isFavorited) FavoriteIcons.DropFilled else FavoriteIcons.Drop,
                        contentDescription = if (entry.isFavorited) "favoriden çıkar" else "favorile",
                        tint = if (entry.isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        entry.favoriteCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Text(
                pick.source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Spacer(Modifier.weight(1f, fill = false))
    }
}

/** The "yazıyor..." bubble while the next entry is on its way. */
@Composable
private fun TypingBubble(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(start = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                repeat(3) { index ->
                    val lift by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            keyframes {
                                durationMillis = 900
                                0f at 0 + index * 150
                                1f at 150 + index * 150
                                0f at 300 + index * 150
                            }
                        ),
                        label = "dot$index"
                    )
                    Box(
                        Modifier
                            .size(8.dp)
                            .graphicsLayer { translationY = -lift * 5.dp.toPx() }
                            .alpha(0.4f + 0.6f * lift)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}
