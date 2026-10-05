package com.thesozluk.app.model

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
    val deleteForm: FormSpec? = null,
    val commentForm: FormSpec? = null,
    /** The "görsel yükle" drop zone under the entry box */
    val imageUploader: ImageUploader? = null,
    /** The topic's own path (/slug--id) as linked from its heading; differs on /entry/<id> pages */
    val topicPath: String = "",
    val topicId: String = "",
    // "takip et" on the topic (logged in only): current state and the urls it posts to
    val isTracked: Boolean = false,
    val trackUrl: String? = null,
    val untrackUrl: String? = null
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
    val topicUrl: String = "",
    /** Profile picture; null for the default placeholder */
    val avatarUrl: String? = null,
    /** Topic page the entry was loaded from (the list spans many pages) */
    val page: Int = 1,
    val eksiSeylerUrl: String? = null,
    val authorIsVerified: Boolean = false,
    val authorIsAdFree: Boolean = false
) {
    val canVote: Boolean get() = "vote" in flags
    val canDelete: Boolean get() = "deleteself" in flags
}

/** An HTML form found on a page: where it posts, its hidden fields and its text field. */
data class FormSpec(
    val action: String,
    val fields: Map<String, String>,
    val textFieldName: String? = null,
    /** Text already in the field, e.g. an entry left "kenarda" (saved as a draft on the site) */
    val textValue: String = "",
    /** The page's picture uploader (the "düzelt" page has one too) */
    val imageUploader: ImageUploader? = null
)

/** The site's picture uploader: the form pictures are posted to, and where a picture's key deletes it. */
data class ImageUploader(val form: FormSpec, val deleteUrl: String?)

/** A picture uploaded for an entry: its link (soz.lk/i/...) and the key that deletes it. */
data class UploadedImage(val link: String, val key: String)

/** A relation button on a profile (engelle, başlıklarını engelle, sessize al, ...). */
data class RelationAction(
    val label: String,
    val removeLabel: String,
    val addUrl: String,
    val removeUrl: String,
    val isAdded: Boolean
)

data class Badge(
    val name: String,
    val description: String,
    val imageUrl: String,
    /** False for catalog badges the profile owner has not earned. */
    val owned: Boolean = true
)

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
    val badges: List<Badge>,
    // Follow endpoints from the profile's "takip et" button; null when logged out
    val followAddUrl: String? = null,
    val followRemoveUrl: String? = null,
    val isFollowing: Boolean = false,
    /** Other relation buttons besides "takip et"; empty when logged out */
    val relations: List<RelationAction> = emptyList(),
    val isRookie: Boolean = false,
    /** The user's badge collection and ownership state from /rozetler/{nick}. */
    val allBadges: List<Badge> = badges,
    /** Original biography markup, kept so its links remain tappable. */
    val biographyHtml: String = "",
    /** Ekşi Sözlük's reklamsız subscription status badge. */
    val isAdFree: Boolean = false
)

/** "Başlığı açan": the author's nick plus the plain facts (date, counts) from the site's box. */
data class TopicCreator(val nick: String?, val details: List<String>)

/** An image an author uploaded: its /img/<code> page and the thumbnail file. */
data class AuthorImage(val ref: String, val thumbnailUrl: String)

/** Someone in a follower / following list. */
data class FollowUser(val nick: String, val avatarUrl: String?, val isVerified: Boolean)

/** A comment ("yorum") under an entry. */
data class Comment(
    val id: String,
    val author: String,
    val authorId: String,
    val content: String,
    val contentHtml: String,
    val date: String,
    val avatarUrl: String?,
    val upVotes: Int,
    val downVotes: Int,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false
) {
    /** New vote [rate] replacing [previous], keeping the counts in step. */
    fun withVote(rate: Int, previous: Int): Comment {
        val up = upVotes - (if (previous > 0) 1 else 0) + (if (rate > 0) 1 else 0)
        val down = downVotes - (if (previous < 0) 1 else 0) + (if (rate < 0) 1 else 0)
        return copy(isLiked = rate > 0, isDisliked = rate < 0, upVotes = up.coerceAtLeast(0), downVotes = down.coerceAtLeast(0))
    }
}

/** A conversation in the message box. */
data class MessageThread(
    val id: String,
    val nick: String,
    /** Messages in the conversation (the number next to the nick) */
    val messageCount: Int,
    val preview: String,
    val time: String
)

data class Message(
    val text: String,
    val html: String,
    val time: String,
    val isOutgoing: Boolean,
    /** Shown at once while sending; replaced by the server's copy */
    val isPending: Boolean = false,
    val isFailed: Boolean = false
)

data class MessageBox(
    val threads: List<MessageThread>,
    /** "Yeni mesaj" form (To + Message + token) */
    val sendForm: FormSpec?,
    /** Bulk delete / archive form */
    val threadForm: FormSpec?
)

data class ThreadDetail(
    val nick: String,
    val messages: List<Message>,
    val sendForm: FormSpec?,
    /** Archive / delete for this conversation */
    val threadForm: FormSpec?
)

/** A channel (#spor, #ilişkiler, ...) and its topic list path. */
data class Channel(val name: String, val description: String, val path: String)

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
