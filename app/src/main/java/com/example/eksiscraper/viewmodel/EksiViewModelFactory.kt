package com.example.eksiscraper.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.eksiscraper.data.room.EksiDatabase
import com.example.eksiscraper.data.room.SavedTopicRepository
import com.example.eksiscraper.repository.EksiRepository

class EksiViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    
    private val repository: EksiRepository by lazy {
        val database = EksiDatabase.getDatabase(application)
        val savedTopicRepository = SavedTopicRepository(database.savedTopicDao())
        EksiRepository(savedTopicRepository)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(repository) as T
            }
            modelClass.isAssignableFrom(TopicDetailViewModel::class.java) -> {
                TopicDetailViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AuthorViewModel::class.java) -> {
                AuthorViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}