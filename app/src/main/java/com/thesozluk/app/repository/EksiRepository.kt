package com.thesozluk.app.repository

import com.thesozluk.app.data.room.SavedTopicRepository
import com.thesozluk.app.model.AuthorProfile
import com.thesozluk.app.model.Channel
import com.thesozluk.app.model.Comment
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.FormSpec
import com.thesozluk.app.model.MessageBox
import com.thesozluk.app.model.ThreadDetail
import com.thesozluk.app.model.Topic
import com.thesozluk.app.network.EksiNetworkDataSource
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

    suspend fun saveSiteDraft(topicPath: String, form: FormSpec, text: String): String? =
        EksiNetworkDataSource.saveSiteDraft(topicPath, form, text)

    suspend fun getProfile(nick: String): AuthorProfile = EksiNetworkDataSource.fetchProfile(nick)

    suspend fun getEditForm(entryId: String): FormSpec = EksiNetworkDataSource.fetchEditForm(entryId)

    /** Saves an entry's new text with its "düzelt" form; null on success, otherwise the reason. */
    suspend fun editEntry(form: FormSpec, text: String): String? {
        val field = form.textFieldName ?: return "düzeltme formu bulunamadı"
        return submitForm(form, mapOf(field to text))
    }

    /**
     * Deletes one of the user's own entries from anywhere (e.g. a profile list): the delete form
     * comes from the entry's own page. Null on success, otherwise the reason.
     */
    suspend fun deleteEntry(entryId: String): String? {
        val form = searchTopic("", 1, "/entry/$entryId").deleteForm ?: return "silme formu bulunamadı"
        val idField = form.fields.keys.firstOrNull { it.equals("id", ignoreCase = true) } ?: "id"
        return submitForm(form, mapOf(idField to entryId), ajax = true)
    }

    suspend fun getUserEntries(nick: String, tab: String, page: Int): List<Entry> =
        EksiNetworkDataSource.fetchUserEntries(nick, tab, page)

    suspend fun getOwnNick(): String? = EksiNetworkDataSource.fetchOwnNick()

    suspend fun getComments(entryId: String): List<Comment> = EksiNetworkDataSource.fetchComments(entryId)

    suspend fun voteComment(commentId: String, authorId: String, rate: Int, previous: Int): Boolean =
        EksiNetworkDataSource.voteComment(commentId, authorId, rate, previous)

    suspend fun getChannels(): List<Channel> = EksiNetworkDataSource.fetchChannels()

    /** Posts a relation / track url; null on success, otherwise the reason. */
    suspend fun postAction(url: String): String? = EksiNetworkDataSource.postRelation(url)

    suspend fun getFollowList(nick: String, following: Boolean) = EksiNetworkDataSource.fetchFollowList(nick, following)

    suspend fun getUserImages(nick: String) = EksiNetworkDataSource.fetchUserImages(nick)

    suspend fun getTopicCreator(topicId: String) = EksiNetworkDataSource.fetchTopicCreator(topicId)

    suspend fun getFavoriters(entryId: String, rookies: Boolean = false): List<String> =
        EksiNetworkDataSource.fetchFavoriters(entryId, rookies)

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

    suspend fun updateLastPage(title: String, page: Int) = savedTopicRepository.updateLastPage(title, page)

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
