package com.thesozluk.app.network

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.thesozluk.app.model.AuthorImage
import com.thesozluk.app.model.AuthorProfile
import com.thesozluk.app.model.Channel
import com.thesozluk.app.model.Comment
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.FollowUser
import com.thesozluk.app.model.MessageBox
import com.thesozluk.app.model.ThreadDetail
import com.thesozluk.app.model.Topic

/**
 * Made-up content for taking screenshots: invented topics, entries, authors and messages, so no
 * real person's writing or account shows. Only the debug build carries a [DemoSource] (see
 * src/debug); the release build has none, so [active] is always false there and nothing here
 * changes how the app works.
 */
interface DemoSource {
    /** The demo account's nick, shown as signed in */
    val ownNick: String
    fun topics(category: String, page: Int): List<Topic>
    fun topic(query: String, page: Int, path: String): Topic
    fun suggestions(query: String): Pair<List<String>, List<String>>
    fun profile(nick: String): AuthorProfile
    fun userEntries(nick: String, tab: String, page: Int): List<Entry>
    fun userImages(nick: String): List<AuthorImage>
    fun comments(entryId: String): List<Comment>
    fun favoriters(entryId: String): List<String>
    fun followList(nick: String, following: Boolean): List<FollowUser>
    fun messageBox(): MessageBox
    fun thread(id: String): ThreadDetail
    fun channels(): List<Channel>
    fun trash(page: Int): TrashPage
    fun relations(kind: RelationKind, page: Int): RelationPage
}

object Demo {
    private const val PREFS = "demo"

    /** Set by the debug build at start; null in release */
    var source: DemoSource? = null
        private set

    private val _enabled = mutableStateOf(false)
    val enabled: State<Boolean> = _enabled

    private val _showBadge = mutableStateOf(true)
    /** The small "demo" mark on screen, hidden for clean screenshots */
    val showBadge: State<Boolean> = _showBadge

    private var prefs: android.content.SharedPreferences? = null

    /** The demo content is there and switched on */
    val active: Boolean get() = _enabled.value && source != null

    /** The demo content when it is in use, else null: `Demo.on?.topics(...) ?: real request` */
    val on: DemoSource? get() = if (_enabled.value) source else null

    /**
     * Looks for the debug build's demo content and reads the switch. In release the class is
     * not there and the demo stays off.
     */
    fun init(context: Context) {
        if (source == null) {
            source = runCatching {
                Class.forName("com.thesozluk.app.demo.DemoContent").getField("INSTANCE").get(null) as DemoSource
            }.getOrNull()
        }
        if (source == null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = p.getBoolean("enabled", false)
        _showBadge.value = p.getBoolean("badge", true)
    }

    fun setEnabled(on: Boolean) {
        if (source == null) return
        _enabled.value = on
        // Written at once: the app starts over right after
        prefs?.edit()?.putBoolean("enabled", on)?.commit()
    }

    fun setShowBadge(show: Boolean) {
        _showBadge.value = show
        prefs?.edit()?.putBoolean("badge", show)?.apply()
    }
}
