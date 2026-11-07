package com.thesozluk.app.ui.screens

import com.thesozluk.app.ui.components.AppAlertDialog
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thesozluk.app.network.AccountLists
import com.thesozluk.app.network.RelationKind
import com.thesozluk.app.network.RelationUser
import com.thesozluk.app.network.TrashEntry
import com.thesozluk.app.ui.components.AuthorAvatar
import com.thesozluk.app.ui.components.ErrorState
import com.thesozluk.app.ui.components.FloatingPromptHost
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.LargeTitle
import com.thesozluk.app.ui.components.LoadingState
import com.thesozluk.app.ui.components.MessageState
import com.thesozluk.app.ui.components.PromptState
import com.thesozluk.app.ui.components.cardEdge
import com.thesozluk.app.ui.components.glassSource
import com.thesozluk.app.ui.components.rememberEntryText
import com.thesozluk.app.ui.components.segmentedShape
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.openEksiLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// çöp kutusu

/**
 * The trash, as on the site's /cop: one's deleted entries, each with "canlandır" (back into its
 * topic) and "sil" (gone for good), and "çöpü boşalt" for all of them.
 */
@Composable
fun TrashScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val notes = remember { PromptState() }
    val uriHandler = LocalUriHandler.current
    val entries = remember { mutableStateListOf<TrashEntry>() }
    var page by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var total by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<TrashEntry?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    suspend fun loadPage(next: Int) {
        loading = true
        try {
            val result = AccountLists.fetchTrash(next)
            if (next == 1) entries.clear()
            entries.addAll(result.entries.filter { new -> entries.none { it.id == new.id } })
            page = next
            pageCount = result.pageCount
            total = result.total
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "çöp kutusu açılamadı"
        } finally {
            loading = false
        }
    }
    LaunchedEffect(reloadKey) { loadPage(1) }
    // The next page once the list nears its end
    val nearEnd by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
    LaunchedEffect(nearEnd, page) {
        if (!loading && page in 1 until pageCount && nearEnd >= entries.size - 2) loadPage(page + 1)
    }

    fun act(entry: TrashEntry, done: String, action: suspend (String) -> String?) {
        busyId = entry.id
        scope.launch {
            val failure = action(entry.id)
            busyId = null
            if (failure == null) entries.removeAll { it.id == entry.id }
            notes.show(failure ?: done)
        }
    }

    confirmDelete?.let { entry ->
        AppAlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text("tamamen silinsin mi?") },
            text = { Text("\"${entry.topicTitle}\" başlığındaki entry kalıcı olarak silinir, geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    act(entry, "entry tamamen silindi", AccountLists::deleteForever)
                }) { Text("sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("vazgeç") } }
        )
    }
    if (confirmEmpty) {
        AppAlertDialog(
            onDismissRequest = { confirmEmpty = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text("çöp boşaltılsın mı?") },
            text = { Text("çöpteki bütün entry'ler kalıcı olarak silinir, geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmpty = false
                    scope.launch {
                        val failure = AccountLists.emptyTrash()
                        notes.show(failure ?: "çöp boşaltıldı")
                        if (failure == null) reloadKey++
                    }
                }) { Text("boşalt", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("vazgeç") } }
        )
    }

    Scaffold(
        topBar = {
            FloatingTopBar(
                title = if (scrolled) "çöp kutusu" else null,
                onBack = { navController.popBackStack() },
                onTitleClick = { scope.launch { listState.animateScrollToItem(0) } },
                isLoading = loading && entries.isNotEmpty(),
                actions = if (entries.isNotEmpty()) ({
                    IconButton(onClick = { confirmEmpty = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "çöpü boşalt")
                    }
                }) else null
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when {
                entries.isEmpty() && loading -> LoadingState(messages = listOf("çöp karıştırılıyor"))
                entries.isEmpty() && error != null -> ErrorState(error.orEmpty(), onRetry = { reloadKey++ })
                entries.isEmpty() -> MessageState(
                    icon = Icons.Rounded.RestoreFromTrash,
                    title = "çöp boş",
                    message = "sildiğin entry'ler burada durur, istersen geri getirirsin."
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().glassSource(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "title") {
                        Column {
                            LargeTitle("çöp kutusu")
                            if (total.isNotBlank()) {
                                Text(
                                    total,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    itemsIndexed(entries, key = { _, it -> it.id }) { _, entry ->
                        TrashCard(
                            entry = entry,
                            busy = busyId == entry.id,
                            onOpenTopic = {
                                navController.navigate(Screen.TopicDetail.createRoute(entry.topicTitle, ""))
                            },
                            onLink = { navController.openEksiLink(it, uriHandler) },
                            onRestore = { act(entry, "entry canlandırıldı", AccountLists::restore) },
                            onDelete = { confirmDelete = entry },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            FloatingPromptHost(notes, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrashCard(
    entry: TrashEntry,
    busy: Boolean,
    onOpenTopic: () -> Unit,
    onLink: (com.thesozluk.app.ui.components.EksiLink) -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier.fillMaxWidth().cardEdge(shape)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // The title opens the topic
            Text(
                entry.topicTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenTopic)
                    .padding(vertical = 2.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (entry.date.isNotBlank()) {
                    Text(entry.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // The entry itself, set off like a quote
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    rememberEntryText(entry.contentHtml, entry.content, onLink),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Restore and delete as one connected pair, the expressive way
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { onRestore() },
                        enabled = !busy,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                        Text(if (busy) "bekle" else "canlandır")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { onDelete() },
                        enabled = !busy,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Icon(
                            Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp).size(18.dp)
                        )
                        Text("tamamen sil", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// takip / engellenmiş

/**
 * The site's /takip-engellenmis: who one follows, has blocked, whose topics are hidden, and who
 * is muted. "kaldır" takes the relation back.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RelationsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val notes = remember { PromptState() }
    var kind by rememberSaveable { mutableStateOf(RelationKind.Blocked) }
    val users = remember { mutableStateListOf<RelationUser>() }
    var page by remember { mutableIntStateOf(0) }
    var isLast by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<RelationUser?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    suspend fun loadPage(next: Int, of: RelationKind) {
        loading = true
        try {
            val result = AccountLists.fetchRelations(of, next)
            if (of != kind) return
            if (next == 1) users.clear()
            users.addAll(result.users.filter { new -> users.none { it.id == new.id } })
            page = next
            isLast = result.isLast
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "liste alınamadı"
        } finally {
            loading = false
        }
    }
    LaunchedEffect(kind, reloadKey) {
        users.clear()
        page = 0
        isLast = false
        loadPage(1, kind)
    }
    val lastVisible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
    LaunchedEffect(lastVisible, page) {
        if (!loading && !isLast && page >= 1 && lastVisible >= users.size - 2) loadPage(page + 1, kind)
    }

    confirm?.let { user ->
        AppAlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(user.nick) },
            text = {
                Text(
                    when (kind) {
                        RelationKind.Followed -> "takibi bırakılsın mı?"
                        RelationKind.Blocked -> "engeli kaldırılsın mı?"
                        RelationKind.TopicsBlocked -> "başlıkları yeniden görünsün mü?"
                        RelationKind.Muted -> "sessizden çıkarılsın mı?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    busyId = user.id
                    val from = kind
                    scope.launch {
                        val failure = AccountLists.removeRelation(from, user.id)
                        busyId = null
                        if (failure == null && from == kind) users.removeAll { it.id == user.id }
                        notes.show(failure ?: "kaldırıldı")
                    }
                }) { Text("kaldır") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("vazgeç") } }
        )
    }

    Scaffold(
        topBar = {
            FloatingTopBar(
                title = if (scrolled) "takip / engellenmiş" else null,
                onBack = { navController.popBackStack() },
                onTitleClick = { scope.launch { listState.animateScrollToItem(0) } },
                isLoading = loading && users.isNotEmpty()
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().glassSource(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item(key = "title") { LargeTitle("takip / engellenmiş") }
                item(key = "kinds") {
                    // The site's four tabs, as the home lists' connected buttons
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)
                        ) {
                            RelationKind.entries.forEachIndexed { index, option ->
                                val shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    RelationKind.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                                ToggleButton(
                                    checked = option == kind,
                                    onCheckedChange = { kind = option },
                                    shapes = shapes,
                                    modifier = Modifier.cardEdge(if (option == kind) shapes.checkedShape else shapes.shape)
                                ) { Text(option.label) }
                            }
                        }
                    }
                }
                when {
                    users.isEmpty() && loading -> item(key = "loading") { LoadingState(Modifier.padding(top = 48.dp)) }
                    users.isEmpty() && error != null -> item(key = "error") {
                        ErrorState(error.orEmpty(), onRetry = { reloadKey++ })
                    }
                    users.isEmpty() -> item(key = "empty") {
                        MessageState(icon = Icons.Rounded.PersonOff, title = kind.empty)
                    }
                    else -> itemsIndexed(users, key = { _, it -> "${kind.code}:${it.id}" }) { index, user ->
                        Surface(
                            onClick = { navController.navigate(Screen.Author.createRoute(user.nick)) },
                            shape = segmentedShape(index, users.size),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.animateItem().cardEdge(segmentedShape(index, users.size))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
                            ) {
                                AuthorAvatar(user.nick, size = 40, avatarUrl = user.avatarUrl)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        user.nick,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (user.isVerified) {
                                        Icon(
                                            Icons.Rounded.Verified,
                                            contentDescription = "onaylanmış hesap",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 6.dp).size(16.dp)
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { confirm = user },
                                    enabled = busyId != user.id,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(if (busyId == user.id) "bekle" else "kaldır", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
            FloatingPromptHost(notes, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
        }
    }
}
