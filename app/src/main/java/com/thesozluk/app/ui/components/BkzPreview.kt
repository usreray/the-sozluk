package com.thesozluk.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.thesozluk.app.model.Topic
import com.thesozluk.app.network.EksiNetworkDataSource
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.openEksiLink
import kotlinx.coroutines.CancellationException

/** The (bkz) waiting to be previewed; set by openEksiLink, shown by [BkzPreviewHost]. */
object BkzPreview {
    val link = mutableStateOf<EksiLink?>(null)
}

/** Shows the first entry of a tapped (bkz) in a sheet, with a button to open the topic. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BkzPreviewHost(navController: NavController) {
    val link = BkzPreview.link.value ?: return
    val uriHandler = LocalUriHandler.current
    val (title, path) = when (link) {
        is EksiLink.Search -> link.query to ""
        is EksiLink.TopicPath -> link.title to link.path
        else -> return
    }
    var topic by remember(link) { mutableStateOf<Topic?>(null) }
    var failed by remember(link) { mutableStateOf(false) }
    LaunchedEffect(link) {
        try {
            topic = EksiNetworkDataSource.searchTopic(title, 1, path)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }
    val dismiss = { BkzPreview.link.value = null }
    val first = topic?.entries?.firstOrNull()

    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                topic?.title?.takeIf { it.isNotBlank() } ?: title,
                style = MaterialTheme.typography.titleLargeEmphasized
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 360.dp)
            ) {
                when {
                    first != null -> Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            // A bkz inside the preview previews that topic in turn
                            rememberEntryText(first.contentHtml, first.content, { next ->
                                if (next is EksiLink.Search || next is EksiLink.TopicPath) BkzPreview.link.value = next
                                else { dismiss(); navController.openEksiLink(next, uriHandler) }
                            }),
                            inlineContent = rememberEntryInlineContent(),
                            style = entryBodyStyle(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${first.author} · ${first.date}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                    failed || topic != null -> Box(Modifier.padding(16.dp)) {
                        Text("önizleme alınamadı", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
            }
            Button(
                onClick = {
                    dismiss()
                    val loaded = topic
                    if (loaded != null && loaded.title.isNotBlank()) {
                        navController.navigate(Screen.TopicDetail.createRoute(loaded.title, loaded.topicPath.ifBlank { path }))
                    } else {
                        navController.openEksiLink(link, uriHandler, preview = false)
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("başlığa git") }
        }
    }
}
