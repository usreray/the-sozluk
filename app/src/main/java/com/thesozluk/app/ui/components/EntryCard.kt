package com.thesozluk.app.ui.components

import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.material3.ripple
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.BookmarkAdd
import com.thesozluk.app.settings.EntryBookmarks
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thesozluk.app.model.Comment
import com.thesozluk.app.model.Entry
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.settings.AppSettings
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
    val onShareImage: ((Entry) -> Unit)? = null,
    /** The topic the entries are in, when shown on its page (entries elsewhere carry their own) */
    val topicTitle: String = ""
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
    textSelectable: Boolean = false,
    /** Long-press lifts the card into [EntryPeek]; off for the lifted copy itself */
    peekable: Boolean = true
) {
    var overflows by remember(entry.entryId) { mutableStateOf(false) }
    val showAvatars by AppSettings.showAvatars
    val showNumbers by AppSettings.showEntryNumbers
    // X-like layout: the author leads, the text sits beside the avatar
    val feed = AppSettings.entryStyle.value == com.thesozluk.app.settings.EntryStyle.Feed
    val feedIndent = if (feed && showAvatars) 52.dp else 0.dp
    var confirmDelete by remember { mutableStateOf(false) }
    var spoilersRevealed by rememberSaveable(entry.entryId) { mutableStateOf(false) }
    val text = rememberEntryText(
        entry.contentHtml,
        entry.content,
        actions.onLink,
        spoilersHidden = !spoilersRevealed,
        onToggleSpoilers = if (AppSettings.hideSpoilers.value) ({ spoilersRevealed = !spoilersRevealed }) else null
    )

    if (confirmDelete && actions.onDelete != null) {
        AppAlertDialog(
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

    // The user's own entries: the site's flags say so, or the nick matches
    val own = entry.author.isNotBlank() && entry.author.equals(EksiSession.nick.value, ignoreCase = true)
    // "entry'yi özetle", when an AI is chosen in the settings
    var summarizing by remember { mutableStateOf(false) }
    if (summarizing) {
        // The topic gives the entry its meaning, so the AI gets it too
        val topicTitle = entry.topicTitle.ifBlank { actions.topicTitle }
        SummarySheet(
            heading = "entry'nin özeti",
            title = topicTitle,
            footer = "bu entry yapay zeka ile özetlenir; yapay zeka hata yapabilir.",
            run = { progress -> com.thesozluk.app.ai.TopicSummary.summarizeEntry(topicTitle, entry, progress) },
            onDismiss = { summarizing = false }
        )
    }
    val canSummarize = com.thesozluk.app.ai.TopicSummary.engine.value != com.thesozluk.app.ai.AiEngine.Off &&
        entry.content.isNotBlank()
    val menu = rememberEntryMenuItems(
        entry = entry,
        onSummarize = if (canSummarize) ({ summarizing = true }) else null,
        onAuthorInTopic = actions.onAuthorInTopic?.let { show -> { show(entry) } },
        onEdit = if (("edit" in entry.flags || own) && actions.onEdit != null) ({ actions.onEdit(entry) }) else null,
        onBookmark = actions.onBookmark?.let { { it(entry) } },
        onShareImage = actions.onShareImage?.let { { it(entry) } },
        onDelete = if ((entry.canDelete || own) && actions.onDelete != null) ({ confirmDelete = true }) else null
    )
    // Where the card sits on screen, for the preview to rise from
    val bounds = remember { arrayOf(Rect.Zero) }
    val canPeek = peekable && !textSelectable && entry.entryId.isNotEmpty()
    val press = remember { MutableInteractionSource() }
    val lifted = canPeek && EntryPeek.isLifted(entry.entryId)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .then(if (canPeek) Modifier.onGloballyPositioned { bounds[0] = it.boundsInRoot() } else Modifier)
            // Lifted into the preview: the copy up there stands in for it
            .graphicsLayer { alpha = if (lifted) 0f else 1f }
            .then(if (canPeek) Modifier.pressBounce(press, pressedScale = 0.97f) else Modifier)
            .clip(RoundedCornerShape(28.dp))
            // Liquid glass: the bars' fine light line along the card's top edge
            .then(if (liquidGlass()) Modifier.glassEdge(RoundedCornerShape(28.dp)) else Modifier)
            .then(
                when {
                    canPeek -> Modifier.combinedClickable(
                        interactionSource = press,
                        indication = if (onClick != null) ripple() else null,
                        hapticFeedbackEnabled = false,
                        onClick = onClick ?: {},
                        onLongClick = {
                            EntryPeek.open(
                                EntryPeek.Peek(id = entry.entryId, bounds = bounds[0], menu = menu) {
                                    EntryCard(
                                        entry = entry,
                                        isExpanded = false,
                                        onToggleExpand = {},
                                        actions = actions,
                                        number = number,
                                        showExpandToggle = false,
                                        peekable = false
                                    )
                                }
                            )
                        }
                    )
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                }
            )
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
            if (feed) FeedHeader(entry, number, showAvatars, showNumbers, actions.onAuthor, menu)
            Column(modifier = Modifier.padding(start = feedIndent)) {
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
            EntryImagePreviews(
                html = entry.contentHtml,
                onOpen = { ref -> actions.onLink(EksiLink.Image(ref)) },
                modifier = Modifier.padding(top = 8.dp, end = 12.dp)
            )
            }

            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = (feedIndent - 12.dp).coerceAtLeast(0.dp))) {
                if (entry.canVote) VoteButtons(entry, actions.onVote)
                FavoriteButton(
                    isFavorited = entry.isFavorited,
                    count = entry.favoriteCount,
                    onToggle = { actions.onToggleFavorite(entry) },
                    onCountClick = actions.onShowFavoriters?.let { show -> { show(entry) } }
                )
                entry.eksiSeylerUrl?.let { url ->
                    IconButton(
                        onClick = { actions.onLink(EksiLink.External(url)) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = FavoriteIcons.DropFilled,
                            contentDescription = "ekşi şeyler'de aç",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // In the feed layout the menu sits up by the author
                if (!feed) EntryMenu(menu)
            }
            if (!feed) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Text(
                            text = entry.author,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.authorIsVerified) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "onaylanmış hesap",
                                tint = Color(0xFF34A853),
                                modifier = Modifier.padding(start = 3.dp).size(16.dp)
                            )
                        }
                        if (entry.authorIsAdFree) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "reklamsız kullanıcı",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.padding(start = 3.dp).size(16.dp)
                            )
                        }
                    }
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.cardEdge(RoundedCornerShape(20.dp))
            ) {
                // Laid out like an entry: the text, then the votes on the left and the author on the
                // right (nick and date, then the avatar)
                Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 8.dp)) {
                    if (textSelectable) SelectionContainer {
                        Text(
                            rememberEntryText(comment.contentHtml, comment.content, onLink),
                            inlineContent = rememberEntryInlineContent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            rememberEntryText(comment.contentHtml, comment.content, onLink),
                            inlineContent = rememberEntryInlineContent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
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
                        Spacer(Modifier.weight(1f))
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAuthor(comment.author) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                comment.author,
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                comment.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }
                        if (com.thesozluk.app.settings.AppSettings.showAvatars.value) {
                            Spacer(Modifier.width(6.dp))
                            AuthorAvatar(
                                comment.author,
                                avatarUrl = comment.avatarUrl,
                                size = 32,
                                modifier = Modifier.clip(CircleShape).clickable { onAuthor(comment.author) }
                            )
                        }
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
    val up = remember { MutableInteractionSource() }
    val down = remember { MutableInteractionSource() }
    IconToggleButton(
        checked = entry.isLiked,
        onCheckedChange = { onVote(entry, if (entry.isLiked) 0 else 1) },
        interactionSource = up,
        modifier = Modifier.pressBounce(up, pressedScale = 0.75f),
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
        interactionSource = down,
        modifier = Modifier.pressBounce(down, pressedScale = 0.75f),
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
    val favInteraction = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(
            checked = isFavorited,
            onCheckedChange = { onToggle() },
            interactionSource = favInteraction,
            modifier = Modifier.pressBounce(favInteraction, pressedScale = 0.75f)
        ) {
            Icon(
                imageVector = if (isFavorited) FavoriteIcons.DropFilled else FavoriteIcons.Drop,
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

/** The entry's actions, in menu order: sharing and copying, then the reader's own, then deleting. */
@Composable
private fun rememberEntryMenuItems(
    entry: Entry,
    onSummarize: (() -> Unit)?,
    onAuthorInTopic: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onBookmark: (() -> Unit)?,
    onShareImage: (() -> Unit)?
): List<EntryMenuItem> {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val link = "${EksiSession.BASE_URL}/entry/${entry.entryId}"
    val saved = EntryBookmarks.isSaved(entry.entryId)
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    val navController = com.thesozluk.app.ui.navigation.LocalNavController.current
    val isLoggedIn by EksiSession.isLoggedIn
    return buildList {
        if (entry.entryId.isNotEmpty()) add(EntryMenuItem("paylaş", Icons.Rounded.Share) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link)
            context.startActivity(Intent.createChooser(send, null))
        })
        add(EntryMenuItem("metni kopyala", Icons.Rounded.ContentCopy) {
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("entry", entry.content))) }
        })
        if (onSummarize != null) add(EntryMenuItem("entry'yi özetle", Icons.Rounded.AutoAwesome, onClick = onSummarize))
        if (onAuthorInTopic != null) add(EntryMenuItem("yazarın bu başlıktakileri", Icons.Rounded.FilterList, onClick = onAuthorInTopic))
        if (onBookmark != null) add(EntryMenuItem(
            if (saved) "kayıttan çıkar" else "entry'yi kaydet",
            if (saved) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd,
            onClick = onBookmark
        ))
        if (onShareImage != null) add(EntryMenuItem("görsel olarak paylaş", Icons.Rounded.Image, onClick = onShareImage))
        if (onEdit != null) add(EntryMenuItem("düzelt", Icons.Rounded.Edit, onClick = onEdit))
        if (onDelete != null) add(EntryMenuItem("sil", Icons.Rounded.Delete, destructive = true, onClick = onDelete))
        // Someone else's entry: a message to its author, starting with the entry's number
        if (onDelete == null && onEdit == null && isLoggedIn && navController != null &&
            entry.entryId.isNotEmpty() && entry.author.isNotBlank()
        ) add(EntryMenuItem("mesaj gönder", Icons.Rounded.MailOutline) {
            navController.navigate(
                com.thesozluk.app.ui.navigation.Screen.MessageThread.withNick(entry.author, "(#${entry.entryId})\n")
            )
        })
        // Someone else's entry: the site's own complaint form, on the entry's page in a browser tab
        if (onDelete == null && onEdit == null && entry.entryId.isNotEmpty()) add(EntryMenuItem("şikayet et", Icons.Rounded.Flag) {
            com.thesozluk.app.ui.navigation.openInAppTab(context, link, toolbarColor)
        })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EntryMenu(items: List<EntryMenuItem>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "diğer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                AppMenuItem(
                    text = item.label,
                    icon = item.icon,
                    color = if (item.destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                    iconTint = if (item.destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                    onClick = {
                        open = false
                        item.onClick()
                    }
                )
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

/**
 * The feed layout's top line, as in a social feed: the avatar, the nick (with its marks) over
 * the date, and the ⋮ menu at the end. The avatar and the nick open the author.
 */
@Composable
private fun FeedHeader(
    entry: Entry,
    number: Int?,
    showAvatars: Boolean,
    showNumbers: Boolean,
    onAuthor: (String) -> Unit,
    menu: List<EntryMenuItem>
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        if (showAvatars) {
            AuthorAvatar(
                entry.author,
                avatarUrl = entry.avatarUrl,
                size = 40,
                modifier = Modifier.clip(CircleShape).clickable { onAuthor(entry.author) }
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAuthor(entry.author) }
                .padding(vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.author,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (entry.authorIsVerified) {
                    Icon(Icons.Rounded.Verified, contentDescription = "onaylanmış hesap", tint = Color(0xFF34A853), modifier = Modifier.padding(start = 3.dp).size(16.dp))
                }
                if (entry.authorIsAdFree) {
                    Icon(Icons.Rounded.Verified, contentDescription = "reklamsız kullanıcı", tint = Color(0xFFFFC107), modifier = Modifier.padding(start = 3.dp).size(16.dp))
                }
            }
            Text(
                if (showNumbers && number != null) "#$number · ${entry.date}" else entry.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        EntryMenu(menu)
    }
}
