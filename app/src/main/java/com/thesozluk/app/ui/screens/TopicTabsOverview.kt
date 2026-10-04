package com.thesozluk.app.ui.screens

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thesozluk.app.network.EksiNetworkDataSource
import com.thesozluk.app.ui.navigation.TopicTab
import com.thesozluk.app.ui.navigation.TopicTabs
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicTabsOverview(
    tabs: List<TopicTab>,
    onDismiss: () -> Unit,
    onSelect: (TopicTab) -> Unit,
    onClose: (TopicTab) -> Unit,
    onNewTopic: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("açık başlıklar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onNewTopic) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "başlık ara",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "kapat") }
                }
                if (tabs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("açık başlık yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            var dragX by remember(tab.id) { mutableFloatStateOf(0f) }
                            var loadingPreview by remember(tab.id) { mutableStateOf(false) }
                            val density = LocalDensity.current
                            val dismissThreshold = with(density) { 88.dp.toPx() }
                            LaunchedEffect(tab.id, tab.preview) {
                                if (tab.preview.isBlank()) {
                                    loadingPreview = true
                                    val preview = runCatching {
                                        EksiNetworkDataSource.searchTopic(tab.title, tab.page, tab.url)
                                            .entries.firstOrNull()?.content.orEmpty()
                                    }.getOrDefault("")
                                    TopicTabs.updatePreview(tab.id, preview)
                                    loadingPreview = false
                                }
                            }
                            Card(
                                onClick = { onSelect(tab) },
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .heightIn(min = 230.dp)
                                    .pointerInput(tab.id, dismissThreshold) {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, amount ->
                                                dragX += amount
                                                change.consume()
                                            },
                                            onDragEnd = {
                                                if (abs(dragX) >= dismissThreshold) onClose(tab) else dragX = 0f
                                            },
                                            onDragCancel = { dragX = 0f }
                                        )
                                    }
                                    .graphicsLayer {
                                        translationX = dragX
                                        alpha = 1f - (abs(dragX) / dismissThreshold).coerceIn(0f, 0.35f)
                                    }
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            tab.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            minLines = 2,
                                            maxLines = Int.MAX_VALUE,
                                            overflow = TextOverflow.Clip,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { onClose(tab) }, modifier = Modifier.padding(start = 2.dp)) {
                                            Icon(Icons.Rounded.Close, contentDescription = "sekmeni kapat")
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            tab.preview.ifBlank { if (loadingPreview) "önizleme yükleniyor" else "önizleme alınamadı" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 6,
                                            overflow = TextOverflow.Ellipsis,
                                            minLines = 6,
                                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
