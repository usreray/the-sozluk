package com.example.eksiscraper.model

data class HomeResponse(
    val home: List<Topic>
)

data class Topic(
    val title: String,
    val url: String = "",
    val commentCount: Int = 0,
    val entries: List<Entry> = emptyList(),
    val entriesLoaded: Boolean = false,
    val loadedPages: Set<Int> = emptySet(),
    val redirectedUrl: String = "",
    val totalPages: Int = 1,
    val isSaved: Boolean = false,
    // Page the site actually returned (differs from the requested one for ?focusto= links)
    val currentPage: Int = 1,
    // "N entry daha" on gündem (?a=popular) pages: entries written before today
    val olderEntriesCount: Int = 0,
    val olderEntriesUrl: String = ""
)

data class Entry(
    val content: String,
    val author: String = "",
    val date: String = "",
    val favoriteCount: Int = 0,
    val entryId: String = "",
    val isFavorited: Boolean = false
) 
/** Returns a copy of the topic with one entry's favorite state (and count) changed. */
fun Topic.withFavorite(entryId: String, favorited: Boolean): Topic = copy(
    entries = entries.map { entry ->
        if (entry.entryId != entryId || entry.isFavorited == favorited) entry
        else entry.copy(
            isFavorited = favorited,
            favoriteCount = (entry.favoriteCount + if (favorited) 1 else -1).coerceAtLeast(0)
        )
    }
)
