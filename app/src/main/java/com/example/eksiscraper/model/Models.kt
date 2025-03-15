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
    val isSaved: Boolean = false
)

data class Entry(
    val content: String,
    val author: String = "",
    val date: String = "",
    val favoriteCount: Int = 0,
    val entryId: String = "",
    val isFavorited: Boolean = false
) 