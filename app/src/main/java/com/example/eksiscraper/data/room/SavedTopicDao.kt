package com.example.eksiscraper.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTopicDao {
    @Query("SELECT * FROM saved_topics ORDER BY savedAt DESC")
    fun getAllSavedTopics(): Flow<List<SavedTopicEntity>>
    
    @Query("SELECT * FROM saved_topics WHERE title = :title LIMIT 1")
    suspend fun getSavedTopicByTitle(title: String): SavedTopicEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedTopic(topic: SavedTopicEntity)
    
    @Query("DELETE FROM saved_topics WHERE title = :title")
    suspend fun deleteSavedTopicByTitle(title: String)
    
    @Query("UPDATE saved_topics SET lastPage = :page WHERE title = :title")
    suspend fun updateLastPage(title: String, page: Int)

    @Query("SELECT COUNT(*) FROM saved_topics WHERE title = :title")
    suspend fun isTopicSaved(title: String): Int
} 