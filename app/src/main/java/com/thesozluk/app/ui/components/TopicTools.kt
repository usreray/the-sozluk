package com.thesozluk.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.thesozluk.app.viewmodel.TopicFilter
import kotlinx.coroutines.CancellationException

/** What the topic's "⋮" menu can ask for. */
sealed interface TopicMenuAction {
    data class Filter(val filter: TopicFilter) : TopicMenuAction
    /** Opens the filter sheet */
    data object Filters : TopicMenuAction
    data object SearchInTopic : TopicMenuAction
    data object SearchAuthor : TopicMenuAction
    data object Creator : TopicMenuAction
    data object Share : TopicMenuAction
    data object SaveOffline : TopicMenuAction
    data object DeleteOffline : TopicMenuAction
}

/** Topic filters (as on the site's topic menu) plus "başlığı açan". */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicMenu(
    current: TopicFilter,
    isLoggedIn: Boolean,
    onAction: (TopicMenuAction) -> Unit,
    /** null: not saved; "indiriliyor 3 / 20"...; "kayıtlı" when a copy is on the device */
    offlineState: String? = null
) {
    var open by remember { mutableStateOf(false) }
    fun pick(action: TopicMenuAction) {
        open = false
        onAction(action)
    }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "başlık menüsü", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                // All filters live in their own sheet (as chips) so the menu stays short; the
                // row shows the one that is on
                FilterItem(
                    if (current == TopicFilter.All) "filtrele" else "filtrele: ${current.label}",
                    Icons.Rounded.FilterList,
                    current != TopicFilter.All && current !is TopicFilter.Find && current !is TopicFilter.Author
                ) { pick(TopicMenuAction.Filters) }
                FilterItem("başlıkta ara", Icons.Rounded.Search, current is TopicFilter.Find) { pick(TopicMenuAction.SearchInTopic) }
                FilterItem("yazara göre", Icons.Rounded.AlternateEmail, current is TopicFilter.Author) { pick(TopicMenuAction.SearchAuthor) }
                FilterItem("başlığı açan", Icons.Rounded.Info, false) { pick(TopicMenuAction.Creator) }
                FilterItem("paylaş", Icons.Rounded.Share, false) { pick(TopicMenuAction.Share) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                when (offlineState) {
                    null -> FilterItem("çevrimdışı kaydet", Icons.Rounded.CloudDownload, false) { pick(TopicMenuAction.SaveOffline) }
                    "kayıtlı" -> {
                        FilterItem("çevrimdışı kopyayı güncelle", Icons.Rounded.CloudSync, false) { pick(TopicMenuAction.SaveOffline) }
                        FilterItem("çevrimdışı kopyayı sil", Icons.Rounded.CloudOff, false) { pick(TopicMenuAction.DeleteOffline) }
                    }
                    else -> FilterItem(offlineState, Icons.Rounded.Downloading, true) { pick(TopicMenuAction.DeleteOffline) }
                }
            }
        }
    }
}

@Composable
private fun FilterItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = onClick
    )
}

/** One-line input in a dialog (search in topic, author nick, blocked word). */
@Composable
fun TextPromptDialog(
    title: String,
    placeholder: String,
    confirmLabel: String = "ara",
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val submit = { if (text.isNotBlank()) onConfirm(text.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        },
        confirmButton = { TextButton(onClick = submit, enabled = text.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("vazgeç") } }
    )
}

/** "Başlığı açan": loads the site's creator box and shows it as text. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicCreatorDialog(
    load: suspend () -> com.thesozluk.app.model.TopicCreator,
    onAuthor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var creator by remember { mutableStateOf<com.thesozluk.app.model.TopicCreator?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            creator = load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "bilgi alınamadı"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text("başlığı açan") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), contentAlignment = Alignment.Center) {
                val current = creator
                when {
                    error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    current == null -> LoadingIndicator()
                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // The nick opens the author's profile
                        current.nick?.let { nick ->
                            TextButton(onClick = {
                                onDismiss()
                                onAuthor(nick)
                            }) {
                                Text(nick, style = MaterialTheme.typography.titleLargeEmphasized)
                            }
                        }
                        current.details.forEach { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("tamam") } }
    )
}

/**
 * The site's "filtrele" options as chips: şükela, today's şükela, images, links, ekşi şeyler and,
 * when logged in, the user's own entries, followed authors' and rookies' entries.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicFilterSheet(current: TopicFilter, isLoggedIn: Boolean, nick: String?, onPick: (TopicFilter) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filters = buildList {
        add(TopicFilter.All to Icons.Rounded.ViewAgenda)
        add(TopicFilter.Nice to Icons.Rounded.Star)
        add(TopicFilter.DailyNice to Icons.Rounded.Today)
        add(TopicFilter.Images to Icons.Rounded.Image)
        add(TopicFilter.Seyler to FavoriteIcons.DropFilled)
        if (isLoggedIn) {
            // The site answers the link search only for members
            add(TopicFilter.Links to Icons.Rounded.Link)
            nick?.let { add(TopicFilter.Mine(it) to Icons.Rounded.Person) }
            add(TopicFilter.Buddies to Icons.Rounded.Group)
            add(TopicFilter.Rookies to Icons.Rounded.Spa)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("filtrele", style = MaterialTheme.typography.titleLargeEmphasized)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filters.forEach { (filter, icon) ->
                    val selected = current == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onPick(filter) },
                        label = { Text(filter.label) },
                        leadingIcon = { Icon(if (selected) Icons.Rounded.Check else icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
            if (!isLoggedIn) {
                Text(
                    "linkler, benimkiler, takip ettiklerim ve çaylaklar giriş yapınca görünür",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
