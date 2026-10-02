package com.example.eksiscraper.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.network.EksiSession

private const val COLLAPSED_LINES = 8

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EntryCard(
    entry: Entry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overflows by remember(entry.entryId) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 12.dp)) {
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!isExpanded) overflows = it.hasVisualOverflow },
                modifier = Modifier.padding(end = 8.dp).animateContentSize()
            )
            if (overflows || isExpanded) {
                TextButton(onClick = onToggleExpand) {
                    Text(if (isExpanded) "daha az göster" else "devamını oku")
                }
            }

            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FavoriteButton(entry.isFavorited, entry.favoriteCount, onToggleFavorite)
                ShareButton(entry.entryId)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.author,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.width(10.dp))
                AuthorAvatar(entry.author)
            }
        }
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

@Composable
private fun ShareButton(entryId: String) {
    if (entryId.isEmpty()) return
    val context = LocalContext.current
    IconButton(onClick = {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "${EksiSession.BASE_URL}/entry/$entryId")
        context.startActivity(Intent.createChooser(send, null))
    }) {
        Icon(
            Icons.Rounded.Share,
            contentDescription = "Paylaş",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** First letter of the nick in a cookie shape; the color is stable per author. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthorAvatar(author: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (Math.floorMod(author.hashCode(), 3)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(shape = MaterialShapes.Cookie6Sided.toShape(), color = container, modifier = modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = author.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = content
            )
        }
    }
}
