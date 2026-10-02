package com.example.eksiscraper.ui.components

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.network.EksiSession
import kotlinx.coroutines.launch

private const val COLLAPSED_LINES = 8

/** What an entry card can do; screens wire these to their ViewModel and navigation. */
data class EntryActions(
    val onToggleFavorite: (Entry) -> Unit,
    /** +1 şükela, -1 çok kötü; tapping the active one again sends 0 (take it back) */
    val onVote: (Entry, Int) -> Unit,
    val onAuthor: (String) -> Unit,
    val onLink: (EksiLink) -> Unit,
    val onDelete: ((Entry) -> Unit)? = null,
    val onOpenTopic: ((Entry) -> Unit)? = null
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EntryCard(
    entry: Entry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    actions: EntryActions,
    modifier: Modifier = Modifier
) {
    var overflows by remember(entry.entryId) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val text = rememberEntryText(entry.contentHtml, entry.content, actions.onLink)

    if (confirmDelete && actions.onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Entry silinsin mi?") },
            text = { Text("Bu entry ekşi sözlük'ten silinecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete(entry)
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } }
        )
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)) {
            if (entry.topicTitle.isNotBlank() && actions.onOpenTopic != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 12.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onOpenTopic(entry) }
                ) {
                    Text(
                        text = entry.topicTitle,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp).size(16.dp)
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!isExpanded) overflows = it.hasVisualOverflow },
                modifier = Modifier.padding(end = 12.dp).animateContentSize()
            )
            if (overflows || isExpanded) {
                TextButton(onClick = onToggleExpand) {
                    Text(if (isExpanded) "daha az göster" else "devamını oku")
                }
            }

            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.canVote) VoteButtons(entry, actions.onVote)
                FavoriteButton(entry.isFavorited, entry.favoriteCount) { actions.onToggleFavorite(entry) }
                Spacer(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { actions.onAuthor(entry.author) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.author,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.width(4.dp))
                AuthorAvatar(entry.author, modifier = Modifier.clickable { actions.onAuthor(entry.author) })
                EntryMenu(
                    entry = entry,
                    onAuthor = { actions.onAuthor(entry.author) },
                    onDelete = if (entry.canDelete && actions.onDelete != null) ({ confirmDelete = true }) else null
                )
            }
        }
    }
}

/** şükela / çok kötü: chevrons like on the site; the active one is tinted. */
@Composable
private fun VoteButtons(entry: Entry, onVote: (Entry, Int) -> Unit) {
    IconToggleButton(
        checked = entry.isLiked,
        onCheckedChange = { onVote(entry, if (entry.isLiked) 0 else 1) },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContentColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = if (entry.isLiked) "şükela'yı geri al" else "şükela")
    }
    IconToggleButton(
        checked = entry.isDisliked,
        onCheckedChange = { onVote(entry, if (entry.isDisliked) 0 else -1) },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContentColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = if (entry.isDisliked) "oyu geri al" else "çok kötü")
    }
}

/** ekşi's favorite is a drop; it pops with a bouncy spring and the count rolls. */
@Composable
private fun FavoriteButton(isFavorited: Boolean, count: Int, onToggle: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorited) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "favScale"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(checked = isFavorited, onCheckedChange = { onToggle() }) {
            Icon(
                imageVector = if (isFavorited) Icons.Rounded.WaterDrop else Icons.Outlined.WaterDrop,
                contentDescription = if (isFavorited) "Favoriden çıkar" else "Favorile",
                tint = if (isFavorited) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scale(scale)
            )
        }
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val up = targetState > initialState
                slideInVertically { if (up) it else -it } togetherWith
                    slideOutVertically { if (up) -it else it }
            },
            label = "favCount"
        ) { value ->
            Text(
                text = if (value > 0) value.toString() else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EntryMenu(entry: Entry, onAuthor: () -> Unit, onDelete: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Diğer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                if (entry.entryId.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Paylaş") },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        onClick = {
                            open = false
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, "${EksiSession.BASE_URL}/entry/${entry.entryId}")
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Metni kopyala") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = {
                        open = false
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("entry", entry.content))) }
                    }
                )
                DropdownMenuItem(
                    text = { Text("${entry.author} profili") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    onClick = {
                        open = false
                        onAuthor()
                    }
                )
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            open = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** First letter of the nick in a cookie shape; the color is stable per author. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthorAvatar(author: String, modifier: Modifier = Modifier, size: Int = 36) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (Math.floorMod(author.hashCode(), 3)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(shape = MaterialShapes.Cookie6Sided.toShape(), color = container, modifier = modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = author.firstOrNull()?.uppercase() ?: "?",
                style = if (size > 48) MaterialTheme.typography.headlineMediumEmphasized
                else MaterialTheme.typography.titleMediumEmphasized,
                color = content
            )
        }
    }
}
