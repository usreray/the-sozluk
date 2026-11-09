package com.thesozluk.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

/**
 * "imla": the text as the AI chosen in the settings would spell and punctuate it, the changed
 * words marked, to take with "uygula" or leave. Nothing changes without the user's say.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProofreadSheet(text: String, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf("hazırlanıyor") }
    var corrected by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            corrected = com.thesozluk.app.ai.TopicSummary.proofread(text) { step = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "denetlenemedi"
        }
    }
    val highlight = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetBlur.BlurBehind()
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("imla denetimi", style = MaterialTheme.typography.titleLargeEmphasized)
            val result = corrected
            when {
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                result == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    LoadingIndicator(modifier = Modifier.size(32.dp))
                    Text("$step…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                result.trim() == text.trim() -> Text("yazım ya da noktalama hatası bulunmadı.", style = MaterialTheme.typography.bodyLarge)
                else -> {
                    val marked = remember(result) { markChanges(text, result, highlight) }
                    Text(
                        if (marked.second > 0) "${marked.second} yerde düzeltme var; değişenler renkli." else "düzeltilmiş hali:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(marked.first, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = onDismiss) { Text("vazgeç") }
                        Button(onClick = { onApply(result) }) { Text("uygula") }
                    }
                }
            }
            Text(
                "yapay zeka sadece yazım ve noktalamayı düzeltir; uygulamadan önce kontrol et, yapay zeka hata yapabilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * [corrected] with the words that differ from [original] in [style], and how many runs of
 * changed words there are. A word-level longest common subsequence; very long texts are shown
 * without marks rather than spend the memory.
 */
private fun markChanges(original: String, corrected: String, style: SpanStyle): Pair<AnnotatedString, Int> {
    val token = Regex("\\s+|[^\\s]+")
    val a = token.findAll(original).map { it.value }.toList()
    val b = token.findAll(corrected).map { it.value }.toList()
    if (a.size.toLong() * b.size > 4_000_000L) return AnnotatedString(corrected) to 0
    // lcs[i][j]: common length of a[i..] and b[j..]
    val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
        lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
    }
    var changes = 0
    val text = buildAnnotatedString {
        var i = 0
        var j = 0
        var inChange = false
        while (j < b.size) {
            if (i < a.size && a[i] == b[j]) {
                append(b[j])
                if (b[j].isNotBlank()) inChange = false
                i++
                j++
            } else if (i < a.size && lcs[i + 1][j] >= lcs[i][j + 1]) {
                i++ // dropped from the original
            } else {
                // New in the corrected text
                if (b[j].isBlank()) append(b[j]) else {
                    if (!inChange) changes++
                    inChange = true
                    withStyle(style) { append(b[j]) }
                }
                j++
            }
        }
    }
    return text to changes
}
