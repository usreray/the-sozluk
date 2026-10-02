package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Channel
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The channel list and the topics of the selected channel (endless, like the home tabs). */
class ChannelsViewModel(private val repository: EksiRepository) : ViewModel() {

    private val _channels = mutableStateOf<List<Channel>>(emptyList())
    val channels: State<List<Channel>> = _channels

    private val _channelsError = mutableStateOf<String?>(null)
    val channelsError: State<String?> = _channelsError

    private val _selected = mutableStateOf<Channel?>(null)
    val selected: State<Channel?> = _selected

    private val _topics = mutableStateOf(CategoryState())
    val topics: State<CategoryState> = _topics

    val listState = LazyListState()
    private var job: Job? = null

    init {
        loadChannels()
    }

    /** Opens the channel picked on the search screen. */
    fun open(path: String, name: String) {
        if (path.isEmpty() || _selected.value?.path == path) return
        select(_channels.value.firstOrNull { it.path == path } ?: Channel(name, "", path))
    }

    fun loadChannels() {
        _channelsError.value = null
        viewModelScope.launch {
            try {
                val list = repository.getChannels()
                _channels.value = list
                // The channel opened before the list arrived gets its description now
                _selected.value?.let { current -> list.firstOrNull { it.path == current.path }?.let { _selected.value = it } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _channelsError.value = e.message ?: "kanallar yüklenemedi"
            }
        }
    }

    fun select(channel: Channel) {
        if (_selected.value == channel && _topics.value.page > 0) return
        _selected.value = channel
        _topics.value = CategoryState()
        viewModelScope.launch { listState.scrollToItem(0) }
        load(1)
    }

    fun loadMore() {
        val s = _topics.value
        if (s.page > 0 && s.canLoadMore && !s.isLoading && !s.isLoadingMore) load(s.page + 1)
    }

    fun retry() = load(1)

    private fun load(page: Int) {
        val channel = _selected.value ?: return
        job?.cancel()
        _topics.value = if (page == 1) _topics.value.copy(isLoading = true, error = null)
        else _topics.value.copy(isLoadingMore = true)
        job = viewModelScope.launch {
            try {
                val result = repository.getPopularTopics(page, channel.path.removePrefix("/"))
                _topics.value = _topics.value.copy(
                    topics = (if (page == 1) result else _topics.value.topics + result).distinctBy { it.url },
                    page = page,
                    canLoadMore = result.size >= 20,
                    isLoading = false,
                    isLoadingMore = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _topics.value = if (page == 1) _topics.value.copy(isLoading = false, error = e.message ?: "yüklenemedi")
                else _topics.value.copy(isLoadingMore = false, canLoadMore = false)
            }
        }
    }
}
