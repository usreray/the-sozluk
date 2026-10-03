package com.thesozluk.app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesozluk.app.model.AuthorProfile
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.withVote
import com.thesozluk.app.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Profile tabs; [path] is the site's partial page for that list. */
enum class AuthorTab(val path: String, val label: String) {
    Latest("son-entryleri", "son entry'ler"),
    MostLiked("en-begenilenleri", "en beğenilenler"),
    MostFavorited("en-cok-favorilenen-entryleri", "en çok favorilenenler"),
    ThisWeek("bu-hafta-dikkat-cekenleri", "bu hafta dikkat çekenler"),
    Handmade("el-emegi-goz-nuru", "el emeği göz nuru"),
    RecentVotes("son-oylananlari", "son oylananlar"),
    Favorites("favori-entryleri", "favorileri"),
    Images("gorselleri", "görselleri")
}

data class AuthorTabState(
    val entries: List<Entry> = emptyList(),
    /** Only for [AuthorTab.Images] */
    val images: List<com.thesozluk.app.model.AuthorImage> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null
)

class AuthorViewModel(private val repository: EksiRepository) : ViewModel() {

    private var nick: String = ""

    private val _profile = mutableStateOf<AuthorProfile?>(null)
    val profile: State<AuthorProfile?> = _profile

    private val _profileError = mutableStateOf<String?>(null)
    val profileError: State<String?> = _profileError

    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message

    private val tabs = mutableStateMapOf<AuthorTab, AuthorTabState>()
    private val tabJobs = mutableMapOf<AuthorTab, Job>()

    fun tab(tab: AuthorTab): AuthorTabState = tabs[tab] ?: AuthorTabState()

    fun load(nick: String) {
        if (this.nick == nick) return
        this.nick = nick
        _profileError.value = null
        viewModelScope.launch {
            try {
                _profile.value = repository.getProfile(nick)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _profileError.value = e.message ?: "profil yüklenemedi"
            }
        }
    }

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    /** Pull to refresh: the profile and the open tab from its first page. */
    fun refresh(tab: AuthorTab) {
        if (nick.isEmpty() || _isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                _profile.value = repository.getProfile(nick)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = e.message ?: "profil yenilenemedi"
            }
            loadTab(tab, 1)
            tabJobs[tab]?.join()
            _isRefreshing.value = false
        }
    }

    fun retry() {
        val current = nick
        nick = ""
        load(current)
    }

    fun ensureTabLoaded(tab: AuthorTab) {
        val s = tab(tab)
        if (s.page == 0 && !s.isLoading && s.error == null) loadTab(tab, 1)
    }

    fun loadMore(tab: AuthorTab) {
        val s = tab(tab)
        if (s.page > 0 && s.canLoadMore && !s.isLoading) loadTab(tab, s.page + 1)
    }

    fun retryTab(tab: AuthorTab) = loadTab(tab, if (tab(tab).entries.isEmpty()) 1 else tab(tab).page + 1)

    private fun loadTab(tab: AuthorTab, page: Int) {
        if (nick.isEmpty()) return
        tabJobs[tab]?.cancel()
        update(tab) { it.copy(isLoading = true, error = null) }
        tabJobs[tab] = viewModelScope.launch {
            try {
                if (tab == AuthorTab.Images) {
                    // One list, no pages
                    val images = repository.getUserImages(nick)
                    update(tab) { it.copy(images = images, page = 1, isLoading = false, canLoadMore = false) }
                    return@launch
                }
                val result = repository.getUserEntries(nick, tab.path, page)
                update(tab) {
                    it.copy(
                        entries = (if (page == 1) result else it.entries + result).distinctBy { e -> e.entryId },
                        page = page,
                        isLoading = false,
                        canLoadMore = result.isNotEmpty()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                update(tab) { it.copy(isLoading = false, error = e.message ?: "yüklenemedi") }
            }
        }
    }

    fun toggleFavorite(entry: Entry) {
        val target = !entry.isFavorited
        updateEntry(entry.entryId) {
            it.copy(isFavorited = target, favoriteCount = (it.favoriteCount + if (target) 1 else -1).coerceAtLeast(0))
        }
        viewModelScope.launch {
            if (!repository.setFavorite(entry.entryId, target)) {
                updateEntry(entry.entryId) { it.copy(isFavorited = !target, favoriteCount = entry.favoriteCount) }
                _message.value = "favori kaydedilemedi"
            }
        }
    }

    fun vote(entry: Entry, rate: Int) {
        val previous = if (entry.isLiked) 1 else if (entry.isDisliked) -1 else 0
        if (previous == rate) return
        updateEntry(entry.entryId) { it.withVote(rate) }
        viewModelScope.launch {
            if (!repository.vote(entry.entryId, entry.authorId, rate, previous)) {
                updateEntry(entry.entryId) { it.withVote(previous) }
                _message.value = "oy kaydedilemedi"
            }
        }
    }

    /** Follows or unfollows with the profile's own endpoints; reverts on failure. */
    fun toggleFollow() {
        val current = _profile.value ?: return
        val url = (if (current.isFollowing) current.followRemoveUrl else current.followAddUrl)
        if (url == null) {
            // Loaded before logging in: the page had no follow button; fetch it again
            _message.value = "takip bilgisi alınamadı, profil yenileniyor"
            retry()
            return
        }
        val target = !current.isFollowing
        _profile.value = current.copy(
            isFollowing = target,
            followerCount = (current.followerCount + if (target) 1 else -1).coerceAtLeast(0)
        )
        viewModelScope.launch {
            val error = repository.setFollowing(url)
            if (error == null) {
                _message.value = if (target) "${current.nick} takip ediliyor" else "takip bırakıldı"
            } else {
                _profile.value = current
                _message.value = error
            }
        }
    }

    /** Engelle / başlıklarını engelle / sessize al (or undo); the profile is reloaded afterwards like on the site. */
    fun toggleRelation(action: com.thesozluk.app.model.RelationAction) {
        val url = if (action.isAdded) action.removeUrl else action.addUrl
        if (url.isBlank()) return
        viewModelScope.launch {
            val error = repository.postAction(url)
            if (error == null) {
                _message.value = if (action.isAdded) "${action.removeLabel}: tamam" else "${action.label}: tamam"
                _profile.value = try {
                    repository.getProfile(nick)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _profile.value
                }
            } else {
                _message.value = error
            }
        }
    }

    suspend fun favoriters(entryId: String, rookies: Boolean = false): List<String> = repository.getFavoriters(entryId, rookies)

    private val _isSubmitting = mutableStateOf(false)
    val isSubmitting: State<Boolean> = _isSubmitting

    suspend fun editForm(entry: Entry): com.thesozluk.app.model.FormSpec = repository.getEditForm(entry.entryId)

    /** Saves an edited entry and reloads the open lists so they show it. */
    fun submitEdit(form: com.thesozluk.app.model.FormSpec, text: String, onSuccess: () -> Unit) {
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val error = repository.editEntry(form, text)
                if (error != null) {
                    _message.value = error
                } else {
                    onSuccess()
                    _message.value = "entry düzeltildi"
                    tabs.keys.toList().forEach { loadTab(it, 1) }
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /** Deletes one of the user's own entries and takes it out of every list. */
    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            val error = try {
                repository.deleteEntry(entry.entryId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "entry silinemedi"
            }
            if (error == null) {
                tabs.keys.toList().forEach { tab -> update(tab) { s -> s.copy(entries = s.entries.filterNot { it.entryId == entry.entryId }) } }
                _message.value = "entry silindi"
            } else {
                _message.value = error
            }
        }
    }

    suspend fun followList(following: Boolean) = repository.getFollowList(nick, following)

    fun consumeMessage() {
        _message.value = null
    }

    // An entry can appear in several tabs; keep them all in sync
    private fun updateEntry(entryId: String, block: (Entry) -> Entry) {
        for (tab in tabs.keys.toList()) {
            update(tab) { s -> s.copy(entries = s.entries.map { if (it.entryId == entryId) block(it) else it }) }
        }
    }

    private inline fun update(tab: AuthorTab, block: (AuthorTabState) -> AuthorTabState) {
        tabs[tab] = block(tab(tab))
    }
}
