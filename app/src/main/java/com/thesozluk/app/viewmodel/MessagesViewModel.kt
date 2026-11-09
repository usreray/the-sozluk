package com.thesozluk.app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesozluk.app.model.FormSpec
import com.thesozluk.app.model.MessageBox
import com.thesozluk.app.model.Message
import com.thesozluk.app.model.ThreadDetail
import com.thesozluk.app.repository.EksiRepository
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

    private var threadId: String? = null

    /** A conversation opened by nick that has no messages yet: the first message starts it. */
    private var newConversationWith: String? = null

    /** The words searched for in the messages, or null for the plain box */
    private val _query = mutableStateOf<String?>(null)
    val query: State<String?> = _query

    // The "yeni mesaj" form (with its CSRF token) from the last page that had one
    private var sendForm: FormSpec? = null

    fun loadBox(archive: Boolean = _archive.value) {
        // Another tab: drop the old list so its emptiness isn't shown while loading
        if (archive != _archive.value || _query.value != null) _box.value = null
        _archive.value = archive
        _query.value = null
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
                _error.value = e.message ?: "mesajlar yüklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** The conversations whose messages contain [keywords]; blank goes back to the box. */
    fun search(keywords: String) {
        if (keywords.isBlank()) {
            loadBox(false)
            return
        }
        val words = keywords.trim()
        _query.value = words
        _box.value = null
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val result = repository.searchMessages(words, 1)
                if (_query.value == words) _box.value = result
                result.sendForm?.let { sendForm = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "mesajlarda aranamadı"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Nicks for the "yeni mesaj" author picker. */
    suspend fun suggestNicks(query: String): List<String> =
        if (query.isBlank()) emptyList() else runCatching { repository.suggestNicks(query) }.getOrDefault(emptyList())

    /**
     * The conversation with [nick]: found in the box (and the archive), or, when there is none
     * yet, an empty one that the first message starts. The site has no address for "the
     * conversation with someone", so the box is looked through.
     */
    fun openConversationWith(nick: String) {
        if (nick.isBlank()) return
        if (newConversationWith.equals(nick, ignoreCase = true) && _thread.value != null) return
        threadId = null
        newConversationWith = null
        _thread.value = null
        _threadError.value = null
        viewModelScope.launch {
            try {
                val found = findThreadId(nick)
                if (found != null) {
                    threadId = found
                    _thread.value = repository.getThread(found)
                } else {
                    newConversationWith = nick
                    _thread.value = ThreadDetail(nick, emptyList(), null, null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _threadError.value = e.message ?: "konuşma yüklenemedi"
            }
        }
    }

    private suspend fun findThreadId(nick: String): String? {
        fun MessageBox.match() = threads.firstOrNull { it.nick.equals(nick, ignoreCase = true) }?.id
        for (page in 1..3) {
            val box = repository.getMessageBox(false, page)
            box.sendForm?.let { sendForm = it }
            box.match()?.let { return it }
            if (box.threads.isEmpty()) break
        }
        return repository.getMessageBox(true, 1).match()
    }

    /** Opening a conversation reads it on the site too, so its new-message dot goes right away. */
    fun markRead(id: String) {
        val box = _box.value ?: return
        if (box.threads.none { it.id == id && it.isUnread }) return
        _box.value = box.copy(threads = box.threads.map { if (it.id == id) it.copy(isUnread = false) else it })
    }

    fun openThread(id: String) {
        if (threadId == id && _thread.value != null) return
        threadId = id
        newConversationWith = null
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _threadError.value = e.message ?: "konuşma yüklenemedi"
            }
        }
    }

    /**
     * Sends to [to] with the site's "yeni mesaj" form; replies use it too, with the other
     * person's nick. In a conversation the reload then checks the message really arrived.
     */
    fun send(to: String, text: String, onSent: () -> Unit) {
        _isSending.value = true
        viewModelScope.launch {
            val sentBefore = _thread.value?.messages?.count { it.isOutgoing && !it.isPending && !it.isFailed }
            // In a conversation the message appears at once as "sending"; the input clears now
            val pending = Message(text, "", "gönderiliyor…", isOutgoing = true, isPending = true)
            val starting = newConversationWith
            if (threadId != null || starting != null) {
                _thread.value = _thread.value?.let { it.copy(messages = it.messages + pending) }
                onSent()
            }
            fun markFailed() {
                _thread.value = _thread.value?.let { t ->
                    t.copy(messages = t.messages.map { if (it === pending) it.copy(isPending = false, isFailed = true) else it })
                }
            }
            try {
                // Some conversation pages carry no form; the message box always does
                val form = sendForm ?: repository.getMessageBox(false, 1).sendForm
                    ?: throw IllegalStateException("mesaj formu bulunamadı")
                sendForm = form
                val error = repository.sendMessage(form, to, text)
                if (error != null) {
                    markFailed()
                    _message.value = error
                    return@launch
                }
                val id = threadId
                if (id != null && sentBefore != null) {
                    val detail = repository.getThread(id)
                    if (detail.messages.count { it.isOutgoing } <= sentBefore) {
                        markFailed()
                        _message.value = "mesaj ekşi'de görünmüyor; gönderilememiş olabilir"
                        return@launch
                    }
                    _thread.value = detail
                } else {
                    // A new message: its conversation should now be on top of the box
                    val top = repository.getMessageBox(false, 1).threads.firstOrNull()
                    if (top == null || !top.nick.equals(to, ignoreCase = true)) {
                        markFailed()
                        _message.value = "mesaj ekşi'de görünmüyor; gönderilememiş olabilir"
                        return@launch
                    }
                    if (starting != null) {
                        // The first message started the conversation: show it as the site has it
                        newConversationWith = null
                        threadId = top.id
                        _thread.value = repository.getThread(top.id)
                    } else {
                        _message.value = "mesaj gönderildi"
                        onSent()
                    }
                }
                loadBox()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                markFailed()
                _message.value = e.message ?: "mesaj gönderilemedi"
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
