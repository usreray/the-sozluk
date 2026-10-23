package com.thesozluk.app.ui.navigation

/**
 * The list a topic was opened from (a home list), so the topic screen can step to the next or
 * previous one with a sideways swipe (settings: başlıklar arasında kaydır; off by default).
 */
object TopicSequence {
    /** (title, url) in list order */
    var items: List<Pair<String, String>> = emptyList()
        private set

    fun set(list: List<Pair<String, String>>) {
        items = list
    }

    /** The topic [step] places away from the one with [url] in the list, if any. */
    fun neighbour(url: String, step: Int): Pair<String, String>? {
        val key = url.substringBefore('?')
        val index = items.indexOfFirst { it.second.substringBefore('?') == key }
        if (index < 0) return null
        return items.getOrNull(index + step)
    }
}
