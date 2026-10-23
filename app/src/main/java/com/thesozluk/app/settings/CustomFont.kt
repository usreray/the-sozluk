package com.thesozluk.app.settings

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A font file the user picked (.ttf / .otf), copied into the app's storage so it stays after the
 * original is moved or deleted. Used as the "kendi" reading font.
 */
object CustomFont {
    private const val FILE = "fonts/custom-font"

    private val _family = mutableStateOf<FontFamily?>(null)
    val family: State<FontFamily?> = _family

    fun init(context: Context) {
        val file = File(context.filesDir, FILE)
        if (file.exists()) _family.value = load(file)
    }

    /** Copies the picked file in and loads it; false when it isn't a font Android can read. */
    suspend fun set(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE)
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "custom-font.tmp")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        val family = if (copied) load(temp) else null
        if (family == null) {
            temp.delete()
            return@withContext false
        }
        temp.renameTo(file.also { it.delete() })
        _family.value = load(file) ?: family
        true
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
        _family.value = null
    }

    private fun load(file: File): FontFamily? = runCatching {
        val typeface = Typeface.createFromFile(file)
        if (typeface == Typeface.DEFAULT) null else FontFamily(ComposeTypeface(typeface))
    }.getOrNull()
}
