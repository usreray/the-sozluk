package com.example.eksiscraper.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.AuthorProfile
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.withVote
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Profile tabs; [path] is the site's partial page for that list. */
enum class AuthorTab(val path: String, val label: String) {
    Latest("son-entryleri", "son entry'ler"),
    MostLiked("en-begenilenleri", "en beğenilenler"),
    MostFavorited("en-cok-favorilenen-entryleri", "en çok favorilenenler"),
    Favorites("favori-entryleri", "favorileri")
}

data class AuthorTabState(
    val entries: List<Entry> = emptyList(),
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
                _profileError.value = e.message ?: "Profil yüklenemedi"
            }
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
                update(tab) { it.copy(isLoading = false, error = e.message ?: "Yüklenemedi") }
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
                _message.value = "Favori kaydedilemedi"
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
                _message.value = "Oy kaydedilemedi"
            }
        }
    }

    /** Follows or unfollows with the profile's own endpoints; reverts on failure. */
    fun toggleFollow() {
        val current = _profile.value ?: return
        val url = (if (current.isFollowing) current.followRemoveUrl else current.followAddUrl)
        if (url == null) {
            // Loaded before logging in: the page had no follow button; fetch it again
            _message.value = "Takip bilgisi alınamadı, profil yenileniyor"
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
