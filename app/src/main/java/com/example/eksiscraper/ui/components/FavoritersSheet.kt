package com.example.eksiscraper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.eksiscraper.model.Entry
import kotlinx.coroutines.CancellationException

/** Who favorited an entry, as a bottom sheet; tapping a nick opens the profile. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FavoritersSheet(
    entry: Entry,
    load: suspend (String) -> List<String>,
    onAuthor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nicks by remember(entry.entryId) { mutableStateOf<List<String>?>(null) }
    var error by remember(entry.entryId) { mutableStateOf<String?>(null) }
    LaunchedEffect(entry.entryId) {
        try {
            nicks = load(entry.entryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Liste yüklenemedi"
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)
        ) {
            Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "${entry.favoriteCount} favori",
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
            val list = nicks
            when {
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                list == null -> LoadingIndicator()
                list.isEmpty() -> Text("henüz favorileyen yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    itemsIndexed(list, key = { _, nick -> nick }) { _, nick ->
                        Text(
                            nick,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismiss()
                                    onAuthor(nick)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
