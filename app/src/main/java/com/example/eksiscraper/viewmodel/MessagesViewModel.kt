package com.example.eksiscraper.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.FormSpec
import com.example.eksiscraper.model.Message
import com.example.eksiscraper.model.MessageBox
import com.example.eksiscraper.model.ThreadDetail
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Message box, one open conversation, and sending through the site's own forms. */
class MessagesViewModel(private val repository: EksiRepository) : ViewModel() {

    private val _archive = mutableStateOf(false)
    val archive: State<Boolean> = _archive

    private val _box = mutableStateOf<MessageBox?>(null)
    val box: State<MessageBox?> = _box

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _thread = mutableStateOf<ThreadDetail?>(null)
    val thread: State<ThreadDetail?> = _thread

    private val _threadError = mutableStateOf<String?>(null)
    val threadError: State<String?> = _threadError

    private val _isSending = mutableStateOf(false)
    val isSending: State<Boolean> = _isSending

    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message

    // The send form (with its CSRF token) from the last page that had one
    private var sendForm: FormSpec? = null
    private var threadId: String? = null

    fun loadBox(archive: Boolean = _archive.value) {
        _archive.value = archive
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val box = repository.getMessageBox(archive, 1)
                _box.value = box
                box.sendForm?.let { sendForm = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Mesajlar yüklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openThread(id: String) {
        if (threadId == id && _thread.value != null) return
        threadId = id
        _thread.value = null
        _threadError.value = null
        refreshThread()
    }

    fun refreshThread() {
        val id = threadId ?: return
        viewModelScope.launch {
            try {
                val detail = repository.getThread(id)
                _thread.value = detail
                detail.sendForm?.let { sendForm = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _threadError.value = e.message ?: "Konuşma yüklenemedi"
            }
        }
    }

    /** Sends to [to]; replies use the same "yeni mesaj" form with the other person's nick. */
    fun send(to: String, text: String, onSent: () -> Unit) {
        _isSending.value = true
        viewModelScope.launch {
            try {
                // Some conversation pages carry no form; the message box always does
                val form = sendForm ?: runCatching { repository.getMessageBox(false, 1).sendForm }.getOrNull()
                if (form == null) {
                    _message.value = "Mesaj formu bulunamadı"
                    return@launch
                }
                sendForm = form
                val error = repository.submitForm(form, mapOf("To" to to, (form.textFieldName ?: "Message") to text))
                if (error != null) {
                    _message.value = error
                    return@launch
                }
                onSent()
                // Show the message at once; the reload brings the server's copy
                _thread.value = _thread.value?.let { it.copy(messages = it.messages + Message(text, "", "şimdi", true)) }
                if (threadId != null) refreshThread() else _message.value = "mesaj gönderildi"
                loadBox()
            } finally {
                _isSending.value = false
            }
        }
    }

    /** "archive" or "delete" for the open conversation. */
    fun threadAction(action: String, onDone: () -> Unit) {
        val form = _thread.value?.threadForm ?: return
        viewModelScope.launch {
            val error = repository.submitForm(form, mapOf("action" to action))
            if (error == null) {
                _message.value = if (action == "delete") "konuşma silindi" else "konuşma arşivlendi"
                threadId = null
                loadBox()
                onDone()
            } else {
                _message.value = error
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
