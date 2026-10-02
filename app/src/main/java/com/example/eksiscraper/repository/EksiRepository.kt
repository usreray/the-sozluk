package com.example.eksiscraper.repository

import com.example.eksiscraper.data.room.SavedTopicRepository
import com.example.eksiscraper.model.AuthorProfile
import com.example.eksiscraper.model.Channel
import com.example.eksiscraper.model.Comment
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.FormSpec
import com.example.eksiscraper.model.MessageBox
import com.example.eksiscraper.model.ThreadDetail
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

    suspend fun setFavorite(entryId: String, favorited: Boolean): Boolean {
        return if (favorited) EksiNetworkDataSource.favoriteEntry(entryId)
        else EksiNetworkDataSource.unfavoriteEntry(entryId)
    }

    suspend fun vote(entryId: String, authorId: String, rate: Int, previous: Int): Boolean =
        EksiNetworkDataSource.vote(entryId, authorId, rate, previous)

    /** Returns an error message, or null on success. */
    suspend fun submitForm(form: FormSpec, values: Map<String, String>, ajax: Boolean = false): String? =
        EksiNetworkDataSource.submitForm(form, values, ajax)

    suspend fun getProfile(nick: String): AuthorProfile = EksiNetworkDataSource.fetchProfile(nick)

    suspend fun getUserEntries(nick: String, tab: String, page: Int): List<Entry> =
        EksiNetworkDataSource.fetchUserEntries(nick, tab, page)

    suspend fun getOwnNick(): String? = EksiNetworkDataSource.fetchOwnNick()

    suspend fun getComments(entryId: String): List<Comment> = EksiNetworkDataSource.fetchComments(entryId)

    suspend fun voteComment(commentId: String, authorId: String, rate: Int, previous: Int): Boolean =
        EksiNetworkDataSource.voteComment(commentId, authorId, rate, previous)

    suspend fun getChannels(): List<Channel> = EksiNetworkDataSource.fetchChannels()

    suspend fun getFavoriters(entryId: String): List<String> = EksiNetworkDataSource.fetchFavoriters(entryId)

    suspend fun getMessageBox(archive: Boolean, page: Int): MessageBox =
        EksiNetworkDataSource.fetchMessageBox(archive, page)

    suspend fun getThread(id: String): ThreadDetail = EksiNetworkDataSource.fetchThread(id)

    /** Returns the site's reason when it rejects the message, or null once it is sent. */
    suspend fun sendMessage(form: FormSpec, to: String, text: String): String? =
        EksiNetworkDataSource.sendMessage(form, to, text)

    /** Null on success, otherwise the reason. */
    suspend fun setFollowing(url: String): String? = EksiNetworkDataSource.postRelation(url)

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
