package com.example.eksiscraper.ui.components

import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.rememberModalBottomSheetState
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
    /** (entry id, çaylaklar) -> nicks */
    load: suspend (String, Boolean) -> List<String>,
    onAuthor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // The authors' favorites, or the çaylaklar's, which the site counts apart
    var rookies by remember(entry.entryId) { mutableStateOf(false) }
    var nicks by remember(entry.entryId, rookies) { mutableStateOf<List<String>?>(null) }
    var error by remember(entry.entryId, rookies) { mutableStateOf<String?>(null) }
    LaunchedEffect(entry.entryId, rookies) {
        try {
            nicks = load(entry.entryId, rookies)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "liste yüklenemedi"
        }
    }

    // Fully open from the start and one fixed height for loading and list alike: the sheet used
    // to settle at a half state and shrink once the list replaced the loading indicator
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)
        ) {
            ToggleButton(
                checked = !rookies,
                onCheckedChange = { rookies = false },
                modifier = Modifier.weight(1f),
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
            ) { Text("yazarlar") }
            ToggleButton(
                checked = rookies,
                onCheckedChange = { rookies = true },
                modifier = Modifier.weight(1f),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) { Text("çaylaklar") }
        }
        Box(modifier = Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
            val list = nicks
            when {
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                list == null -> LoadingIndicator()
                list.isEmpty() -> Text(if (rookies) "çaylak favorisi yok" else "henüz favorileyen yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
