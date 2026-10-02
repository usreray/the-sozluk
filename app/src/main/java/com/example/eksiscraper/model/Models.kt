package com.example.eksiscraper.model

data class Topic(
    val title: String,
    val url: String = "",
    val commentCount: Int = 0,
    val entries: List<Entry> = emptyList(),
    val entriesLoaded: Boolean = false,
    val redirectedUrl: String = "",
    val totalPages: Int = 1,
    val isSaved: Boolean = false,
    // Page the site actually returned (differs from the requested one for ?focusto= links)
    val currentPage: Int = 1,
    // "N entry daha" on gündem (?a=popular) pages: entries written before today
    val olderEntriesCount: Int = 0,
    // Forms the site renders for logged-in users; null when logged out or not allowed
    val entryForm: FormSpec? = null,
    val deleteForm: FormSpec? = null
)

data class Entry(
    val content: String,
    val author: String = "",
    val date: String = "",
    val favoriteCount: Int = 0,
    val entryId: String = "",
    val isFavorited: Boolean = false,
    val authorId: String = "",
    /** Entry body as the site renders it, so bkz / links stay clickable */
    val contentHtml: String = "",
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    /** What the site allows on this entry: "vote", "deleteself", "edit", "share", ... */
    val flags: Set<String> = emptySet(),
    val commentCount: Int = 0,
    // Set for entries listed outside their topic (profile tabs)
    val topicTitle: String = "",
    val topicUrl: String = ""
) {
    val canVote: Boolean get() = "vote" in flags
    val canDelete: Boolean get() = "deleteself" in flags
}

/** An HTML form found on a page: where it posts, its hidden fields and its text field. */
data class FormSpec(
    val action: String,
    val fields: Map<String, String>,
    val textFieldName: String? = null
)

data class Badge(val name: String, val description: String, val imageUrl: String)

data class AuthorProfile(
    val nick: String,
    val avatarUrl: String?,
    val isVerified: Boolean,
    /** e.g. "bıçkın (493)" */
    val karma: String,
    val biography: String,
    val entryCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val joinedDate: String,
    val badges: List<Badge>
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

/** Applies a şükela (+1) / çok kötü (-1) / no vote (0) state to one entry. */
fun Entry.withVote(rate: Int): Entry = copy(isLiked = rate > 0, isDisliked = rate < 0)

fun Topic.updateEntry(entryId: String, block: (Entry) -> Entry): Topic =
    copy(entries = entries.map { if (it.entryId == entryId) block(it) else it })
