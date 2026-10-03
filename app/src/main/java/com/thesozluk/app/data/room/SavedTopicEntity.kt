package com.thesozluk.app.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thesozluk.app.model.Topic

@Entity(tableName = "saved_topics")
data class SavedTopicEntity(
    @PrimaryKey val title: String,
    val url: String,
    val commentCount: Int,
    val redirectedUrl: String,
    /** Page the reader was last on, to continue from there */
    @ColumnInfo(defaultValue = "1") val lastPage: Int = 1,
    @ColumnInfo(defaultValue = "0") val savedAt: Long = 0
) {
    companion object {
        fun fromTopic(topic: Topic): SavedTopicEntity {
            return SavedTopicEntity(
                title = topic.title,
                // The whole topic, not the single entry or "bugün" link it was opened from
                url = topic.topicPath.ifBlank { topic.url },
                commentCount = 0,
                redirectedUrl = topic.redirectedUrl,
                lastPage = topic.currentPage.coerceAtLeast(1),
                savedAt = System.currentTimeMillis()
            )
        }
        
        fun toTopic(entity: SavedTopicEntity): Topic {
            return Topic(
                title = entity.title,
                url = entity.url,
                redirectedUrl = entity.redirectedUrl,
                isSaved = true,
                currentPage = entity.lastPage
            )
        }
    }
} 