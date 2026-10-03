package com.example.eksiscraper.ui.components

import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.BookmarkAdd
import com.example.eksiscraper.settings.EntryBookmarks
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.offset
import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.eksiscraper.model.Comment
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.settings.AppSettings
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Link
import kotlinx.coroutines.launch

private const val COLLAPSED_LINES = 8

/** Entry body text with the reading settings applied (typeface, line spacing). */
@Composable
fun entryBodyStyle(): TextStyle {
    val base = MaterialTheme.typography.bodyLarge
    return base.copy(
        fontFamily = AppSettings.readingFont.value.family,
        lineHeight = base.lineHeight * AppSettings.lineSpacing.value
    )
}

/** What an entry card can do; screens wire these to their ViewModel and navigation. */
data class EntryActions(
    val onToggleFavorite: (Entry) -> Unit,
    /** +1 şükela, -1 çok kötü; tapping the active one again sends 0 (take it back) */
    val onVote: (Entry, Int) -> Unit,
    val onAuthor: (String) -> Unit,
    val onLink: (EksiLink) -> Unit,
    val onDelete: ((Entry) -> Unit)? = null,
    val onOpenTopic: ((Entry) -> Unit)? = null,
    // Comments ("yorum"); null hides the comment button (e.g. profile lists)
    val comments: ((Entry) -> CommentsUi)? = null,
    val onToggleComments: (Entry) -> Unit = {},
    val onVoteComment: (Entry, Comment, Int) -> Unit = { _, _, _ -> },
    val onWriteComment: ((Entry) -> Unit)? = null,
    /** Tapping the favorite count shows who favorited */
    val onShowFavoriters: ((Entry) -> Unit)? = null,
    /** "Bu başlıktaki entry'leri": the author's entries in the current topic */
    val onAuthorInTopic: ((Entry) -> Unit)? = null,
    /** "düzelt" on the user's own entries */
    val onEdit: ((Entry) -> Unit)? = null,
    /** "entry'yi kaydet": keep the entry (with a note) on the device */
    val onBookmark: ((Entry) -> Unit)? = null,
    /** Share the entry as a picture */
    val onShareImage: ((Entry) -> Unit)? = null
)

/** What the card needs to show an entry's comments. */
data class CommentsUi(
    val isOpen: Boolean,
    val isLoading: Boolean,
    val comments: List<Comment>,
    val error: String?
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EntryCard(
    entry: Entry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    actions: EntryActions,
    modifier: Modifier = Modifier,
    /** Position in the topic, shown when enabled in settings */
    number: Int? = null,
    /** Tapping the card (outside its buttons and links) */
    onClick: (() -> Unit)? = null,
    /** False where the entry is always shown in full */
    showExpandToggle: Boolean = true,
    /** The full-screen reader allows selecting text; list cards do not. */
    textSelectable: Boolean = false
) {
    var overflows by remember(entry.entryId) { mutableStateOf(false) }
    val showAvatars by AppSettings.showAvatars
    val showNumbers by AppSettings.showEntryNumbers
    var confirmDelete by remember { mutableStateOf(false) }
    val text = rememberEntryText(entry.contentHtml, entry.content, actions.onLink)

    if (confirmDelete && actions.onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("entry silinsin mi?") },
            text = { Text("bu entry ekşi sözlük'ten silinecek. bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete(entry)
                }) { Text("sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("vazgeç") } }
        )
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(start = AppSettings.entryPadding.value.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)) {
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
            if (textSelectable) SelectionContainer {
                Text(
                    text = text,
                    inlineContent = rememberEntryInlineContent(),
                    style = entryBodyStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!isExpanded) overflows = it.hasVisualOverflow },
                    modifier = Modifier.padding(end = 12.dp).animateContentSize()
                )
            } else {
                Text(
                    text = text,
                    inlineContent = rememberEntryInlineContent(),
                    style = entryBodyStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!isExpanded) overflows = it.hasVisualOverflow },
                    modifier = Modifier.padding(end = 12.dp).animateContentSize()
                )
            }
            if (showExpandToggle && (overflows || isExpanded)) {
                TextButton(onClick = onToggleExpand) {
                    Text(if (isExpanded) "daha az göster" else "devamını oku")
                }
            }

            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.canVote) VoteButtons(entry, actions.onVote)
                FavoriteButton(
                    isFavorited = entry.isFavorited,
                    count = entry.favoriteCount,
                    onToggle = { actions.onToggleFavorite(entry) },
                    onCountClick = actions.onShowFavoriters?.let { show -> { show(entry) } }
                )
                val comments = actions.comments?.invoke(entry)
                if (comments != null && (entry.commentCount > 0 || actions.onWriteComment != null)) {
                    IconToggleButton(checked = comments.isOpen, onCheckedChange = { actions.onToggleComments(entry) }) {
                        BadgedBox(badge = {
                            if (entry.commentCount > 0) Badge { Text(entry.commentCount.toString()) }
                        }) {
                            Icon(
                                if (comments.isOpen) Icons.Rounded.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "yorumlar",
                                tint = if (comments.isOpen) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // The user's own entries: the site's flags say so, or the nick matches
                val own = entry.author.isNotBlank() && entry.author.equals(EksiSession.nick.value, ignoreCase = true)
                EntryMenu(
                    entry = entry,
                    onAuthorInTopic = actions.onAuthorInTopic?.let { show -> { show(entry) } },
                    onEdit = if (("edit" in entry.flags || own) && actions.onEdit != null) ({ actions.onEdit(entry) }) else null,
                    onBookmark = actions.onBookmark?.let { { it(entry) } },
                    onShareImage = actions.onShareImage?.let { { it(entry) } },
                    onDelete = if ((entry.canDelete || own) && actions.onDelete != null) ({ confirmDelete = true }) else null
                )
            }
            // Author on a line of its own below the actions, so a long nick and the full date
            // (with an edit time) have the card's whole width
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, end = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f, fill = false)
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
                        text = if (showNumbers && number != null) "#$number · ${entry.date}" else entry.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2
                    )
                }
                if (showAvatars) {
                    Spacer(Modifier.width(6.dp))
                    AuthorAvatar(
                        entry.author,
                        avatarUrl = entry.avatarUrl,
                        modifier = Modifier.clip(CircleShape).clickable { actions.onAuthor(entry.author) }
                    )
                }
            }
            val comments = actions.comments?.invoke(entry)
            AnimatedVisibility(visible = comments?.isOpen == true) {
                if (comments != null) {
                    CommentsSection(
                        comments = comments,
                        onAuthor = actions.onAuthor,
                        onLink = actions.onLink,
                        onVote = { comment, rate -> actions.onVoteComment(entry, comment, rate) },
                        onWrite = actions.onWriteComment?.let { write -> { write(entry) } },
                        textSelectable = textSelectable
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommentsSection(
    comments: CommentsUi,
    onAuthor: (String) -> Unit,
    onLink: (EksiLink) -> Unit,
    onVote: (Comment, Int) -> Unit,
    onWrite: (() -> Unit)?,
    textSelectable: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            comments.isLoading -> Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            comments.error != null -> Text(
                comments.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            comments.comments.isEmpty() -> Text(
                "henüz yorum yok",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        comments.comments.forEach { comment ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AuthorAvatar(
                            comment.author,
                            avatarUrl = comment.avatarUrl,
                            size = 24,
                            modifier = Modifier.clip(CircleShape).clickable { onAuthor(comment.author) }
                        )
                        Text(
                            comment.author,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp).clickable { onAuthor(comment.author) }
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            comment.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                    if (textSelectable) SelectionContainer {
                        Text(
                            rememberEntryText(comment.contentHtml, comment.content, onLink),
                            inlineContent = rememberEntryInlineContent(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp, end = 10.dp)
                        )
                    } else {
                        Text(
                            rememberEntryText(comment.contentHtml, comment.content, onLink),
                            inlineContent = rememberEntryInlineContent(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp, end = 10.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconToggleButton(
                            checked = comment.isLiked,
                            onCheckedChange = { onVote(comment, if (comment.isLiked) 0 else 1) },
                            modifier = Modifier.size(36.dp)
                        ) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "beğen", modifier = Modifier.size(20.dp)) }
                        Text("${comment.upVotes}", style = MaterialTheme.typography.labelMedium)
                        IconToggleButton(
                            checked = comment.isDisliked,
                            onCheckedChange = { onVote(comment, if (comment.isDisliked) 0 else -1) },
                            modifier = Modifier.size(36.dp)
                        ) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "beğenme", modifier = Modifier.size(20.dp)) }
                        Text("${comment.downVotes}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (onWrite != null) {
            TextButton(onClick = onWrite) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("yorum yaz", modifier = Modifier.padding(start = 8.dp))
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
private fun FavoriteButton(isFavorited: Boolean, count: Int, onToggle: () -> Unit, onCountClick: (() -> Unit)?) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorited) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "favScale"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(checked = isFavorited, onCheckedChange = { onToggle() }) {
            Icon(
                imageVector = if (isFavorited) Icons.Rounded.WaterDrop else Icons.Outlined.WaterDrop,
                contentDescription = if (isFavorited) "favoriden çıkar" else "favorile",
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
            label = "favCount",
            // The icon button has a wide touch area; pull the count in next to the drop
            modifier = Modifier.offset(x = (-10).dp)
        ) { value ->
            Text(
                text = if (value > 0) value.toString() else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (value > 0 && onCountClick != null) {
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCountClick).padding(horizontal = 6.dp, vertical = 4.dp)
                } else Modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EntryMenu(
    entry: Entry,
    onAuthorInTopic: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onBookmark: (() -> Unit)? = null,
    onShareImage: (() -> Unit)? = null
) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "diğer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                if (entry.entryId.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("paylaş") },
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
                    text = { Text("metni kopyala") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = {
                        open = false
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("entry", entry.content))) }
                    }
                )
                if (entry.entryId.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("linki kopyala") },
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                        onClick = {
                            open = false
                            val link = "${EksiSession.BASE_URL}/entry/${entry.entryId}"
                            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("entry", link))) }
                        }
                    )
                }
                if (onAuthorInTopic != null) {
                    DropdownMenuItem(
                        text = { Text("${entry.author} · bu başlıktakiler") },
                        leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                        onClick = {
                            open = false
                            onAuthorInTopic()
                        }
                    )
                }
                if (onBookmark != null) {
                    val saved = EntryBookmarks.isSaved(entry.entryId)
                    DropdownMenuItem(
                        text = { Text(if (saved) "kayıttan çıkar" else "entry'yi kaydet") },
                        leadingIcon = { Icon(if (saved) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd, contentDescription = null) },
                        onClick = {
                            open = false
                            onBookmark()
                        }
                    )
                }
                if (onShareImage != null) {
                    DropdownMenuItem(
                        text = { Text("görsel olarak paylaş") },
                        leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                        onClick = {
                            open = false
                            onShareImage()
                        }
                    )
                }
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text("düzelt") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            open = false
                            onEdit()
                        }
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("sil", color = MaterialTheme.colorScheme.error) },
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
fun AuthorAvatar(author: String, modifier: Modifier = Modifier, size: Int = 36, avatarUrl: String? = null) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = author,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size.dp).clip(MaterialShapes.Cookie6Sided.toShape())
        )
        return
    }
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
