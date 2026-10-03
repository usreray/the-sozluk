package com.thesozluk.app.data.room

import com.thesozluk.app.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedTopicRepository(private val savedTopicDao: SavedTopicDao) {
    
    val allSavedTopics: Flow<List<Topic>> = savedTopicDao.getAllSavedTopics().map { entities ->
        entities.map { SavedTopicEntity.toTopic(it) }
    }
    
    suspend fun saveTopic(topic: Topic) {
        val entity = SavedTopicEntity.fromTopic(topic)
        savedTopicDao.insertSavedTopic(entity)
    }
    
    suspend fun unsaveTopic(topic: Topic) {
        savedTopicDao.deleteSavedTopicByTitle(topic.title)
    }
    
    suspend fun updateLastPage(title: String, page: Int) = savedTopicDao.updateLastPage(title, page)

    suspend fun isTopicSaved(title: String): Boolean {
        return savedTopicDao.isTopicSaved(title) > 0
    }
    
    suspend fun getSavedTopicByTitle(title: String): Topic? {
        val entity = savedTopicDao.getSavedTopicByTitle(title)
        return entity?.let { SavedTopicEntity.toTopic(it) }
    }
} 