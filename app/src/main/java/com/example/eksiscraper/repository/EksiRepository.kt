package com.example.eksiscraper.repository

import com.example.eksiscraper.data.room.SavedTopicRepository
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EksiRepository(
    private val savedTopicRepository: SavedTopicRepository
) {
    // Network operations
    suspend fun getPopularTopics(page: Int, category: String): List<Topic> {
        val topics = EksiNetworkDataSource.fetchTopics(page, category)
        return updateSavedStatus(topics)
    }

    suspend fun searchTopic(query: String, page: Int, redirectedUrl: String = ""): Topic {
        val topic = EksiNetworkDataSource.searchTopic(query, page, redirectedUrl)
        return updateSavedStatus(topic)
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        return EksiNetworkDataSource.fetchSuggestions(query)
    }

    suspend fun favoriteEntry(entryId: String): Boolean {
        return EksiNetworkDataSource.favoriteEntry(entryId)
    }

    suspend fun unfavoriteEntry(entryId: String): Boolean {
        return EksiNetworkDataSource.unfavoriteEntry(entryId)
    }

    // Local operations (delegated to SavedTopicRepository)
    val allSavedTopics: Flow<List<Topic>> = savedTopicRepository.allSavedTopics

    suspend fun saveTopic(topic: Topic) {
        savedTopicRepository.saveTopic(topic)
    }

    suspend fun unsaveTopic(topic: Topic) {
        savedTopicRepository.unsaveTopic(topic)
    }

    suspend fun isTopicSaved(title: String): Boolean {
        return savedTopicRepository.isTopicSaved(title)
    }

    // Helper to update saved status
    private suspend fun updateSavedStatus(topics: List<Topic>): List<Topic> {
        val savedTopics = savedTopicRepository.allSavedTopics.first()
        val savedTitles = savedTopics.map { it.title }.toSet()
        
        return topics.map { topic ->
            topic.copy(isSaved = savedTitles.contains(topic.title))
        }
    }

    private suspend fun updateSavedStatus(topic: Topic): Topic {
        val isSaved = savedTopicRepository.isTopicSaved(topic.title)
        return topic.copy(isSaved = isSaved)
    }
}
