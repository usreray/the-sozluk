package com.example.eksiscraper.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/** ekşi markup the toolbar can insert: [before] + selection + [after]. */
private data class Markup(val label: String, val before: String, val after: String)

private val markups = listOf(
    Markup("(bkz: )", "(bkz: ", ")"),
    Markup("hede", "`", "`"),
    Markup("* gizli bkz", "`:", "`"),
    Markup("spoiler", "--- `spoiler` ---\n", "\n--- `spoiler` ---"),
    Markup("link", "[http:// ", "]")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EntryComposerSheet(
    topicTitle: String,
    heading: String = "entry gir",
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    /** A saved draft (or the site's "kenar" text) to continue from */
    initialText: String = "",
    /** Called on every edit so the text survives closing the sheet */
    onTextChange: ((String) -> Unit)? = null
) {
    var value by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(heading, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(topicTitle, style = MaterialTheme.typography.titleLargeEmphasized)
            if (initialText.isNotEmpty()) {
                Text(
                    "taslaktan devam ediyorsun",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    if (it.text != value.text) onTextChange?.invoke(it.text)
                    value = it
                },
                placeholder = { Text("ne düşünüyorsun?") },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                minLines = 6
            )
            // Wraps the selection (or inserts at the cursor) in ekşi's markup
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                markups.forEach { markup ->
                    SuggestionChip(
                        onClick = {
                            value = value.wrapSelection(markup)
                            onTextChange?.invoke(value.text)
                        },
                        label = { Text(markup.label) },
                        enabled = !isSubmitting
                    )
                }
            }
            Button(
                onClick = { onSubmit(value.text.trim()) },
                enabled = value.text.isNotBlank() && !isSubmitting,
                modifier = Modifier.align(Alignment.End),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                if (isSubmitting) LoadingIndicator(modifier = Modifier.size(20.dp))
                else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("gönder", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun TextFieldValue.wrapSelection(markup: Markup): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + markup.before + selected + markup.after + text.substring(end)
    // Cursor lands inside the markup, ready to type the target
    val cursor = start + markup.before.length + selected.length
    return copy(text = newText, selection = TextRange(cursor))
}
