package com.thesozluk.app.ui.screens

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesozluk.app.data.offline.OfflineStore
import com.thesozluk.app.settings.Drafts
import com.thesozluk.app.settings.EntryBookmarks
import com.thesozluk.app.settings.ReadingHistory
import com.thesozluk.app.settings.SavedEntry
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.LargeTitle
import com.thesozluk.app.ui.components.MessageState
import com.thesozluk.app.ui.components.TextPromptDialog
import com.thesozluk.app.ui.components.entryBodyStyle
import com.thesozluk.app.ui.components.rememberEntryInlineContent
import com.thesozluk.app.ui.components.rememberEntryText
import com.thesozluk.app.ui.components.segmentedShape
import com.thesozluk.app.ui.components.TopicRow
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.openEksiLink
import com.thesozluk.app.viewmodel.EksiViewModelFactory
import com.thesozluk.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

/** The device-side collections reachable from the profile and settings. */
enum class LibraryKind(val title: String) {
    History("okuma geçmişi"),
    Bookmarks("kaydedilen entry'ler"),
    Offline("çevrimdışı başlıklar"),
    Drafts("taslaklar"),
    SavedTopics("kaydedilen başlıklar")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    kind: LibraryKind,
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var confirmClear by remember { mutableStateOf(false) }
    val history by ReadingHistory.items
    val drafts by Drafts.all
    val savedTopics by profileViewModel.savedTopics

    Scaffold(
        topBar = {
            FloatingTopBar(
                title = if (scrolled) kind.title else null,
                onBack = { navController.popBackStack() },
                onTitleClick = { scope.launch { listState.animateScrollToItem(0) } },
                actions = if (kind == LibraryKind.History && history.isNotEmpty()) ({
                    IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "geçmişi temizle") }
                }) else null
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { LargeTitle(kind.title) }
            when (kind) {
                LibraryKind.History -> {
                    if (history.isEmpty()) item { Empty(Icons.Rounded.History, "geçmiş boş", "açtığın başlıklar burada listelenir.") }
                    itemsIndexed(history, key = { _, it -> "h:${it.title}" }) { index, item ->
                        Row2(
                            title = item.title,
                            subtitle = DateUtils.getRelativeTimeSpanString(item.openedAt).toString(),
                            shape = segmentedShape(index, history.size),
                            onClick = { navController.navigate(Screen.TopicDetail.createRoute(item.title, item.url)) },
                            trailing = {
                                IconButton(onClick = { ReadingHistory.remove(item.title) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "geçmişten kaldır", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }
                }
                LibraryKind.Bookmarks -> {
                    val saved = EntryBookmarks.items.value
                    if (saved.isEmpty()) item { Empty(Icons.Rounded.Bookmarks, "kaydedilen entry yok", "bir entry'nin ⋮ menüsünden \"entry'yi kaydet\"i seç.") }
                    itemsIndexed(saved, key = { _, it -> "b:${it.entry.entryId}" }) { _, item ->
                        SavedEntryCard(item, navController, Modifier.padding(bottom = 10.dp))
                    }
                }
                LibraryKind.Offline -> {
                    val topics = OfflineStore.topics.value
                    // Downloads still running and not saved before
                    val running = OfflineStore.progress.filter { (path, p) -> !p.finished && topics.none { it.path == path } }
                    val count = topics.size + running.size
                    if (count == 0) item { Empty(Icons.Rounded.CloudDownload, "çevrimdışı başlık yok", "bir başlığı indirmek için başlıktaki ⋮ menüsünden \"çevrimdışı kaydet\"i seç.") }
                    running.entries.forEachIndexed { index, (path, p) ->
                        item(key = "r:$path") {
                            Row2(
                                title = path.removePrefix("/").substringBefore("--").replace('-', ' '),
                                subtitle = if (p.total > 0) "indiriliyor · ${p.done} / ${p.total} sayfa" else "indiriliyor…",
                                shape = segmentedShape(index, count),
                                progress = if (p.total > 0) p.done.toFloat() / p.total else null,
                                onClick = {},
                                trailing = {
                                    IconButton(onClick = { OfflineStore.cancel(path) }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "durdur", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                    }
                    topics.forEachIndexed { i, topic ->
                        item(key = "o:${topic.path}") {
                            val p = OfflineStore.progress[topic.path]
                            val updating = p != null && !p.finished
                            Row2(
                                title = topic.title,
                                subtitle = if (updating) "güncelleniyor · ${p!!.done} / ${p.total} sayfa"
                                else "${topic.pages} sayfa · ${topic.entries} entry · ${formatBytes(topic.bytes)} · " +
                                    DateUtils.getRelativeTimeSpanString(topic.savedAt),
                                shape = segmentedShape(running.size + i, count),
                                progress = if (updating && p!!.total > 0) p.done.toFloat() / p.total else null,
                                onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.title, topic.path)) },
                                trailing = {
                                    IconButton(onClick = { OfflineStore.delete(topic.path) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                    }
                }
                LibraryKind.Drafts -> {
                    if (drafts.isEmpty()) item { Empty(Icons.Rounded.Description, "taslak yok", "göndermediğin entry'ler burada listelenir.") }
                    itemsIndexed(drafts, key = { _, draft -> "d:${draft.title}" }) { index, draft ->
                        Surface(
                            onClick = { navController.navigate(Screen.TopicDetail.createRoute(draft.title, draft.url)) },
                            shape = segmentedShape(index, drafts.size),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(draft.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(draft.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { Drafts.delete(draft.title) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "taslağı sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                LibraryKind.SavedTopics -> {
                    if (savedTopics.isEmpty()) item {
                        Empty(Icons.Rounded.Bookmarks, "henüz kayıt yok", "bir başlıktaki yer imi simgesine dokun, buraya eklensin.")
                    }
                    itemsIndexed(savedTopics, key = { _, topic -> "s:${topic.title}" }) { index, topic ->
                        TopicRow(
                            topic = topic,
                            shape = segmentedShape(index, savedTopics.size),
                            onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.title, topic.url, topic.currentPage)) },
                            modifier = Modifier,
                            subtitle = listOfNotNull(
                                "çevrimdışı".takeIf { OfflineStore.get(topic.url) != null },
                                "${topic.currentPage}. sayfada kaldın".takeIf { topic.currentPage > 1 }
                            ).joinToString(" · ").ifBlank { null },
                            trailing = {
                                IconButton(onClick = { profileViewModel.unsaveTopic(topic) }) {
                                    Icon(Icons.Rounded.BookmarkRemove, contentDescription = "kayıttan çıkar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("geçmiş silinsin mi?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    ReadingHistory.clear()
                    confirmClear = false
                }) { Text("sil") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmClear = false }) { Text("vazgeç") } }
        )
    }
}

@Composable
private fun Empty(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    MessageState(icon = icon, title = title, message = message, modifier = Modifier.padding(top = 24.dp))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Row2(
    title: String,
    subtitle: String,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    progress: Float? = null,
    trailing: @Composable () -> Unit
) {
    Surface(onClick = onClick, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing()
            }
            if (progress != null) {
                LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp))
            }
        }
    }
}

/** A saved entry: its topic, the text, who wrote it, the note; open, edit the note or remove. */
@Composable
private fun SavedEntryCard(item: SavedEntry, navController: NavController, modifier: Modifier = Modifier) {
    val entry = item.entry
    val uriHandler = LocalUriHandler.current
    var editingNote by remember { mutableStateOf(false) }
    val text = rememberEntryText(entry.contentHtml, entry.content) { link -> navController.openEksiLink(link, uriHandler) }
    Surface(
        onClick = { navController.navigate(Screen.TopicDetail.createRoute(entry.topicTitle.ifBlank { "#${entry.entryId}" }, "/entry/${entry.entryId}")) },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.topicTitle.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Text(entry.topicTitle, style = MaterialTheme.typography.titleMediumEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
                }
            }
            Text(
                text,
                inlineContent = rememberEntryInlineContent(),
                style = entryBodyStyle(),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp)
            )
            if (item.note.isNotBlank()) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(end = 12.dp)) {
                    Text(item.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.author, style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.primary)
                    Text(entry.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { editingNote = true }) {
                    Icon(Icons.AutoMirrored.Rounded.StickyNote2, contentDescription = "not", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { EntryBookmarks.remove(entry.entryId) }) {
                    Icon(Icons.Rounded.BookmarkRemove, contentDescription = "kayıttan çıkar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (editingNote) {
        TextPromptDialog(
            title = "not",
            placeholder = "bu entry hakkında bir not",
            confirmLabel = "kaydet",
            initial = item.note,
            onConfirm = {
                EntryBookmarks.save(entry, entry.topicTitle, entry.topicUrl, note = it)
                editingNote = false
            },
            onDismiss = { editingNote = false }
        )
    }
}
