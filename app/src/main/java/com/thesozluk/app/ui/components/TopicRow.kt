package com.thesozluk.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.thesozluk.app.model.Topic
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudDownload

/**
 * Expressive "segmented" list shape: rows sit 2dp apart, the group's outer corners are large
 * and the inner ones small, so a list reads as one rounded block.
 */
fun segmentedShape(index: Int, count: Int): Shape {
    val outer = 24.dp
    val inner = 6.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner
    )
}

@Composable
fun TopicRow(
    topic: Topic,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Already opened before: the title is shown faded */
    isRead: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    /** Long press lifts the row with its actions, like an entry (off in the preview copy) */
    peekable: Boolean = true
) {
    val menu = rememberTopicMenuItems(topic)
    val bounds = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
    val id = "topic:" + topic.url.ifBlank { topic.title }
    val lifted = peekable && EntryPeek.isLifted(id)
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .then(if (peekable) Modifier.onGloballyPositioned { bounds[0] = it.boundsInRoot() } else Modifier)
            // Lifted into the preview: the copy up there stands in for it
            .graphicsLayer { alpha = if (lifted) 0f else 1f }
            .then(if (peekable) Modifier.pressBounce(press, pressedScale = 0.97f) else Modifier)
            .cardEdge(shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                hapticFeedbackEnabled = false,
                onClick = onClick,
                onLongClick = if (!peekable || menu.isEmpty()) null else ({
                    EntryPeek.open(
                        EntryPeek.Peek(id = id, bounds = bounds[0], menu = menu) {
                            TopicRow(
                                topic = topic,
                                shape = RoundedCornerShape(24.dp),
                                onClick = {},
                                subtitle = subtitle,
                                isRead = isRead,
                                peekable = false
                            )
                        }
                    )
                })
            )
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (topic.commentCount > 0) CountPill(topic.commentCount)
            trailing?.invoke()
        }
    }
}

/** Lower-cased titles of topics in the reading history, empty when fading is off. */
@Composable
fun rememberReadTitles(): Set<String> {
    val history by com.thesozluk.app.settings.ReadingHistory.items
    val enabled by com.thesozluk.app.settings.AppSettings.dimReadTopics
    return remember(history, enabled) {
        if (enabled) history.mapTo(HashSet()) { it.title.trim().lowercase() } else emptySet()
    }
}

/** Number of entries written today, as on the site's index. */
@Composable
fun CountPill(count: Int) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** What a topic row's long press offers: open it from the start, save, share, search, keep offline. */
@Composable
private fun rememberTopicMenuItems(topic: Topic): List<EntryMenuItem> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = com.thesozluk.app.ui.navigation.LocalNavController.current
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    if (topic.url.isBlank()) return emptyList()
    // The topic's own address, without a list's filter (?a=popular...) or a page
    val path = topic.url.substringBefore('?')
    val link = if (path.startsWith("http")) path else com.thesozluk.app.network.EksiSession.BASE_URL + path
    val offline = com.thesozluk.app.data.offline.OfflineStore.topics.value.any { it.path == path || it.path == topic.url }
    val savedTitles by com.thesozluk.app.data.room.SavedTopics.titles(context).collectAsState()
    val saved = topic.title.trim().lowercase() in savedTitles
    return buildList {
        if (navController != null) add(EntryMenuItem("baştan oku", androidx.compose.material.icons.Icons.Rounded.FirstPage) {
            navController.navigate(com.thesozluk.app.ui.navigation.Screen.TopicDetail.createRoute(topic.title, path, page = 1))
        })
        if (saved) {
            add(EntryMenuItem("kayıttan çıkar", androidx.compose.material.icons.Icons.Rounded.BookmarkRemove) {
                com.thesozluk.app.data.room.SavedTopics.remove(context, topic)
            })
        } else {
            add(EntryMenuItem("başlığı kaydet", androidx.compose.material.icons.Icons.Rounded.BookmarkAdd) {
                // The whole topic from its start, not the list's filtered link
                com.thesozluk.app.data.room.SavedTopics.save(context, Topic(title = topic.title, url = path))
            })
        }
        add(EntryMenuItem("paylaş", androidx.compose.material.icons.Icons.Rounded.Share) {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(android.content.Intent.EXTRA_TEXT, "${topic.title}\n$link")
            context.startActivity(android.content.Intent.createChooser(send, null))
        })
        add(EntryMenuItem("google'da ara", androidx.compose.material.icons.Icons.Rounded.TravelExplore) {
            val query = java.net.URLEncoder.encode(topic.title, "UTF-8")
            com.thesozluk.app.ui.navigation.openInAppTab(context, "https://www.google.com/search?q=$query", toolbarColor)
        })
        if (offline) {
            add(EntryMenuItem("çevrimdışı kopyayı sil", androidx.compose.material.icons.Icons.Rounded.CloudOff, destructive = true) {
                com.thesozluk.app.data.offline.OfflineStore.delete(path)
            })
        } else {
            add(EntryMenuItem("çevrimdışı kaydet", androidx.compose.material.icons.Icons.Rounded.CloudDownload) {
                com.thesozluk.app.data.offline.OfflineStore.download(topic.title, path)
            })
        }
    }
}
