package com.example.eksiscraper.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.eksiscraper.model.Entry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An entry turned into a picture to share (Instagram, WhatsApp...): a preview of the card and a
 * share button that renders it to a PNG.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareEntryImageSheet(entry: Entry, topicTitle: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var busy by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            Text("görsel olarak paylaş", style = MaterialTheme.typography.titleLargeEmphasized)
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())
            ) {
                // Recorded as a whole, also the part scrolled out of the preview
                Box(
                    modifier = Modifier.drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                ) { ShareCard(entry, topicTitle) }
            }
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val bitmap = layer.toImageBitmap().asAndroidBitmap()
                        shareBitmap(context, bitmap, "entry-${entry.entryId}")
                        busy = false
                        onDismiss()
                    }
                },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                if (busy) LoadingIndicator(modifier = Modifier.width(20.dp))
                else Icon(Icons.Rounded.Share, contentDescription = null)
                Text("paylaş", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** The card as it appears in the picture: topic, text, author and date, the site's name. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShareCard(entry: Entry, topicTitle: String) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.primaryContainer, shape = RoundedCornerShape(32.dp), modifier = Modifier.width(360.dp)) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                topicTitle.ifBlank { entry.topicTitle },
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = colors.onPrimaryContainer
            )
            Surface(color = colors.surface, shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        entry.content.let { if (it.length > 1800) it.take(1800).trimEnd() + "…" else it },
                        style = entryBodyStyle(),
                        color = colors.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AuthorAvatar(entry.author, size = 32, avatarUrl = entry.avatarUrl)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(entry.author, style = MaterialTheme.typography.labelLargeEmphasized, color = colors.primary)
                            Text(entry.date, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            Text("ekşi sözlük", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
        }
    }
}

/** Writes the picture to the app's cache and opens the share sheet for it. */
private suspend fun shareBitmap(context: Context, bitmap: Bitmap, name: String) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "$name.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, null))
}
