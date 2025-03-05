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
    val redirectedUrl: String = "",
    val totalPages: Int = 1,
    val isSaved: Boolean = false
)

data class Entry(
    val content: String,
    val author: String = "",
    val date: String = ""
) 