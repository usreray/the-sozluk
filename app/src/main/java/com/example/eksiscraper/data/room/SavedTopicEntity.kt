package com.example.eksiscraper.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.eksiscraper.model.Topic

@Entity(tableName = "saved_topics")
data class SavedTopicEntity(
    @PrimaryKey val title: String,
    val url: String,
    val commentCount: Int,
    val redirectedUrl: String,
    val totalPages: Int
) {
    companion object {
        fun fromTopic(topic: Topic): SavedTopicEntity {
            return SavedTopicEntity(
                title = topic.title,
                url = topic.url,
                commentCount = topic.commentCount,
                redirectedUrl = topic.redirectedUrl,
                totalPages = topic.totalPages
            )
        }
        
        fun toTopic(entity: SavedTopicEntity): Topic {
            return Topic(
                title = entity.title,
                url = entity.url,
                commentCount = entity.commentCount,
                redirectedUrl = entity.redirectedUrl,
                totalPages = entity.totalPages,
                isSaved = true
            )
        }
    }
} 