package com.thesozluk.app.data.room

import android.content.Context
import com.thesozluk.app.model.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The saved topics for places outside a topic's own screen (a list row's long press): which
 * titles are saved, kept up to date from the database once for all rows, and saving or removing.
 */
object SavedTopics {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repository: SavedTopicRepository? = null
    private var titles: StateFlow<Set<String>>? = null

    private fun repository(context: Context): SavedTopicRepository =
        repository ?: SavedTopicRepository(EksiDatabase.getDatabase(context.applicationContext).savedTopicDao()).also { repository = it }

    /** Lower-cased titles of the saved topics. */
    fun titles(context: Context): StateFlow<Set<String>> = titles ?: repository(context).allSavedTopics
        .map { list -> list.map { it.title.trim().lowercase() }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
        .also { titles = it }

    fun save(context: Context, topic: Topic) {
        scope.launch { repository(context).saveTopic(topic) }
    }

    fun remove(context: Context, topic: Topic) {
        scope.launch { repository(context).unsaveTopic(topic) }
    }
}
