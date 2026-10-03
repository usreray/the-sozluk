package com.thesozluk.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The site's sort orders for the detailed search. */
private enum class SearchOrder(val value: String, val label: String) {
    Date("Date", "yeniden eskiye"),
    Count("Count", "dolu dolu"),
    Topic("Topic", "alfabetik")
}

/**
 * The site's detailed search ("mükemmel ara"): words, author, a date range, şükela only and the
 * sort order. [onSearch] gets the list path (basliklar/ara?...) and a short name for it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdvancedSearchSheet(initialKeywords: String, onSearch: (path: String, name: String) -> Unit, onDismiss: () -> Unit) {
    var keywords by remember { mutableStateOf(initialKeywords) }
    var author by remember { mutableStateOf("") }
    var from by remember { mutableStateOf<Long?>(null) }
    var to by remember { mutableStateOf<Long?>(null) }
    var niceOnly by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(SearchOrder.Date) }
    var picking by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("gelişmiş arama", style = MaterialTheme.typography.titleLargeEmphasized)
            OutlinedTextField(
                value = keywords,
                onValueChange = { keywords = it },
                label = { Text("kelimeler") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it.removePrefix("@") },
                label = { Text("yazar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("ne zaman", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DateButton(label = from?.let(::displayDate) ?: "başlangıç", modifier = Modifier.weight(1f)) { picking = "from" }
                DateButton(label = to?.let(::displayDate) ?: "bitiş", modifier = Modifier.weight(1f)) { picking = "to" }
                if (from != null || to != null) {
                    IconButton(onClick = { from = null; to = null }) { Icon(Icons.Rounded.Close, contentDescription = "tarihleri temizle") }
                }
            }
            ListItem(
                headlineContent = { Text("güzelinden olsun") },
                supportingContent = { Text("sadece şükela entry'ler") },
                trailingContent = { Switch(checked = niceOnly, onCheckedChange = { niceOnly = it }) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text("sıralama", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                SearchOrder.entries.forEachIndexed { index, choice ->
                    ToggleButton(
                        checked = order == choice,
                        onCheckedChange = { order = choice },
                        modifier = Modifier.weight(1f),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            SearchOrder.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) { Text(choice.label, maxLines = 1) }
                }
            }
            Button(
                onClick = {
                    fun enc(v: String) = URLEncoder.encode(v, "UTF-8")
                    val params = buildList {
                        add("SearchForm.Keywords=" + enc(keywords.trim()))
                        if (author.isNotBlank()) add("SearchForm.Author=" + enc(author.trim()))
                        from?.let { add("SearchForm.When.From=" + queryDate(it)) }
                        to?.let { add("SearchForm.When.To=" + queryDate(it)) }
                        add("SearchForm.NiceOnly=$niceOnly")
                        add("SearchForm.SortOrder=${order.value}")
                    }
                    val name = keywords.trim().ifBlank { author.trim().let { if (it.isNotBlank()) "@$it" else "arama" } }
                    onSearch("basliklar/ara?" + params.joinToString("&"), "“$name”")
                },
                enabled = keywords.isNotBlank() || author.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text("ara", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    picking?.let { which ->
        val state = rememberDatePickerState(initialSelectedDateMillis = if (which == "from") from else to)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    if (which == "from") from = state.selectedDateMillis else to = state.selectedDateMillis
                    picking = null
                }) { Text("tamam") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("vazgeç") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DateButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Text(label, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
    }
}

// The date picker works in UTC milliseconds
private fun utcFormat(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
private fun displayDate(millis: Long): String = utcFormat("dd.MM.yyyy").format(Date(millis))
private fun queryDate(millis: Long): String = utcFormat("yyyy-MM-dd").format(Date(millis))
