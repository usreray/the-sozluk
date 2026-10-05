package com.thesozluk.app.ui.components

import java.nio.ByteBuffer
import java.io.IOException
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
    Markup("spoiler", "--- `spoiler` ---\n", "\n--- `spoiler` ---")
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
    onTextChange: ((String) -> Unit)? = null,
    onSaveToSite: ((String) -> Unit)? = null,
    isSavingDraft: Boolean = false,
    /** Uploads a picture (bytes, file name, type) and returns its link; null hides "görsel" */
    onUploadImage: (suspend (ByteArray, String, String) -> String)? = null
) {
    var value by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    var previewing by remember { mutableStateOf(false) }
    var addingLink by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val upload = onUploadImage ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        uploadError = null
        scope.launch {
            try {
                val (bytes, name, type) = readImageForUpload(context, uri)
                val link = upload(bytes, name, type)
                // The same markup the site's uploader puts in the entry box, on its own line
                val at = value.selection.min
                val insert = (if (at > 0 && value.text[at - 1] != '\n') "\n" else "") + "[$link görsel]\n"
                value = value.replaceSelection(insert)
                onTextChange?.invoke(value.text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uploadError = e.message ?: "görsel yüklenemedi"
            } finally {
                uploading = false
            }
        }
    }
    if (addingLink) {
        LinkDialog(
            initialText = value.text.substring(value.selection.min, value.selection.max),
            onConfirm = { url, label ->
                addingLink = false
                value = value.replaceSelection(if (label.isBlank()) "[$url]" else "[$url $label]")
                onTextChange?.invoke(value.text)
            },
            onDismiss = { addingLink = false }
        )
    }
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("yaz", "önizle").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = previewing == (index == 1),
                        onClick = { previewing = index == 1 },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(label) }
                    )
                }
            }
            if (previewing) {
                EntryPreview(value.text)
            } else {
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
                    SuggestionChip(onClick = { addingLink = true }, label = { Text("link") }, enabled = !isSubmitting)
                    if (onUploadImage != null) {
                        SuggestionChip(
                            onClick = {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            label = { Text(if (uploading) "yükleniyor" else "görsel") },
                            icon = {
                                if (uploading) LoadingIndicator(modifier = Modifier.size(18.dp))
                                else Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isSubmitting && !uploading
                        )
                    }
                }
                uploadError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onSaveToSite != null) {
                    OutlinedButton(
                        onClick = { onSaveToSite(value.text.trim()) },
                        enabled = value.text.isNotBlank() && !isSubmitting && !uploading,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        if (isSavingDraft) LoadingIndicator(modifier = Modifier.size(20.dp))
                        else Text("kenara kaydet")
                    }
                }
                Button(
                    onClick = { onSubmit(value.text.trim()) },
                    enabled = value.text.isNotBlank() && !isSubmitting && !uploading,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    if (isSubmitting && !isSavingDraft) LoadingIndicator(modifier = Modifier.size(20.dp))
                    else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("gönder", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** The entry as it will look once sent: bkz, hede and links in color, spoilers marked. */
@Composable
private fun EntryPreview(raw: String) {
    val html = remember(raw) { entryMarkupToHtml(raw) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 360.dp)
    ) {
        Box(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (raw.isBlank()) {
                Text("önizlenecek bir şey yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    rememberEntryText(html, raw, onLink = {}, spoilersHidden = false, onToggleSpoilers = {}),
                    inlineContent = rememberEntryInlineContent(),
                    style = entryBodyStyle(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Asks for the address and the text shown for it, as the site's link button does. */
@Composable
private fun LinkDialog(initialText: String, onConfirm: (url: String, label: String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf(initialText) }
    // The site only links http(s) addresses; a bare "site.com" gets https:// in front
    val address = url.trim().let { if (it.isEmpty() || it.contains("://")) it else "https://$it" }
    val valid = address.startsWith("http://") || address.startsWith("https://")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("link ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text("hangi adrese gidilecek?") },
                    placeholder = { Text("https://") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.replace("\n", " ").replace("]", "") },
                    label = { Text("verilecek ad (isteğe bağlı)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(address, label.trim()) }, enabled = valid) { Text("ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("vazgeç") } }
    )
}

/** Puts [insert] in place of the selection, the cursor right after it. */
private fun TextFieldValue.replaceSelection(insert: String): TextFieldValue {
    val start = selection.min
    val newText = text.substring(0, start) + insert + text.substring(selection.max)
    return copy(text = newText, selection = TextRange(start + insert.length))
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

/** The site turns down pictures over 4 MB */
private const val MAX_UPLOAD_BYTES = 4 * 1024 * 1024

/**
 * The picked picture, ready to send: re-encoded so camera metadata (location, device) stays on
 * the phone, and made smaller until it fits the limit. GIFs go as they are to keep the motion.
 */
private suspend fun readImageForUpload(context: Context, uri: Uri): Triple<ByteArray, String, String> =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val type = resolver.getType(uri).orEmpty()
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("görsel okunamadı")
        if (type == "image/gif") {
            if (bytes.size > MAX_UPLOAD_BYTES) throw IOException("gif 4 MB'tan büyük olamaz")
            return@withContext Triple(bytes, "gorsel.gif", type)
        }
        var bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > 3200) {
                val scale = 3200f / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
        // Transparent PNGs stay PNG; everything else becomes a JPEG
        if (type == "image/png" && bitmap.hasAlpha()) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (out.size() <= MAX_UPLOAD_BYTES) return@withContext Triple(out.toByteArray(), "gorsel.png", "image/png")
        }
        repeat(4) {
            for (quality in intArrayOf(90, 80, 70)) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (out.size() <= MAX_UPLOAD_BYTES) return@withContext Triple(out.toByteArray(), "gorsel.jpg", "image/jpeg")
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.75f).toInt(), (bitmap.height * 0.75f).toInt(), true)
        }
        throw IOException("görsel 4 MB'a sığdırılamadı")
    }
