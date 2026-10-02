package com.example.eksiscraper.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import com.example.eksiscraper.network.EksiNetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Profile pictures for places whose page doesn't carry them (message lists, conversations):
 * read from the author's public profile once and kept for the session.
 */
object AvatarCache {
    // nick -> picture url; a null value means "looked up, no picture"
    private val urls = mutableStateMapOf<String, String?>()
    private val inFlight = mutableSetOf<String>()
    // A long inbox shouldn't fire dozens of profile requests at once
    private val gate = Semaphore(3)

    fun get(nick: String): String? = urls[nick.lowercase()]

    suspend fun load(nick: String) {
        val key = nick.lowercase()
        if (key.isBlank() || urls.containsKey(key) || !inFlight.add(key)) return
        try {
            val url = gate.withPermit { EksiNetworkDataSource.fetchProfile(nick).avatarUrl }
            urls[key] = url
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            urls[key] = null
        } finally {
            inFlight.remove(key)
        }
    }
}

/** The author's picture url once known; starts the lookup on first use. */
@Composable
fun rememberAvatarUrl(nick: String): String? {
    LaunchedEffect(nick) { AvatarCache.load(nick) }
    return AvatarCache.get(nick)
}
