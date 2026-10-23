package com.thesozluk.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thesozluk.app.network.EksiNetworkDataSource
import com.thesozluk.app.settings.AppSettings
import kotlinx.coroutines.CancellationException
import org.jsoup.Jsoup

/**
 * Picture files behind ekşi's upload links (/img/<code> pages), looked up once per session and
 * shared by every entry that shows them.
 */
object ImageUrlCache {
    private val urls = mutableStateMapOf<String, String?>()
    private val inFlight = mutableSetOf<String>()

    fun get(ref: String): String? = urls[ref]

    suspend fun load(ref: String) {
        if (urls.containsKey(ref) || !inFlight.add(ref)) return
        try {
            urls[ref] = EksiNetworkDataSource.resolveImageUrl(ref)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            urls[ref] = null
        } finally {
            inFlight.remove(ref)
        }
    }
}

/** The uploaded pictures an entry links to, in order. */
fun entryImageRefs(html: String): List<String> =
    if (html.isBlank() || !(html.contains("soz.lk/i") || html.contains("/img/") || html.contains("cdn.eksisozluk.com"))) emptyList()
    else Jsoup.parseBodyFragment(html).select("a[href]")
        .mapNotNull { (eksiLinkFor(it.attr("href"), it.text()) as? EksiLink.Image)?.ref }
        .distinct()

/**
 * The entry's pictures as previews under its text, like a social app (settings: entry'lerde
 * görselleri göster; off by default). Tapping one opens the full-screen viewer.
 */
@Composable
fun EntryImagePreviews(html: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!AppSettings.inlineImages.value) return
    val refs = remember(html) { entryImageRefs(html) }
    if (refs.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        refs.forEach { ref ->
            LaunchedEffect(ref) { ImageUrlCache.load(ref) }
            val url = ImageUrlCache.get(ref)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.height(180.dp).widthIn(min = 120.dp, max = 280.dp).clip(RoundedCornerShape(16.dp))
                    .clickable { onOpen(ref) }
            ) {
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = "entry'deki görsel",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(180.dp)
                    )
                } else {
                    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {}
                }
            }
        }
    }
}
