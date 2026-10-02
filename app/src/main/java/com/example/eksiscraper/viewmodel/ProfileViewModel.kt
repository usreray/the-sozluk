package com.example.eksiscraper.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.repository.EksiRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: EksiRepository
) : ViewModel() {

    private val _savedTopics = mutableStateOf<List<Topic>>(emptyList())
    val savedTopics: State<List<Topic>> = _savedTopics

    val scrollState = LazyListState()

    init {
        viewModelScope.launch {
            repository.allSavedTopics.collectLatest { topics ->
                _savedTopics.value = topics
            }
        }
    }

    fun refreshNick() {
        viewModelScope.launch {
            repository.getOwnNick()?.let(EksiSession::saveNick)
        }
    }

    fun unsaveTopic(topic: Topic) {
        viewModelScope.launch {
            repository.unsaveTopic(topic)
        }
    }
}
