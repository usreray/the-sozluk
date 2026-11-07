package com.thesozluk.app.network

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Holds the cookies of a WebView login on eksisozluk.com.
 *
 * Anonymous users need no cookies: Jsoup's session collects whatever the site sets.
 * After login we store the WebView's cookies so requests are made as the user.
 */
object EksiSession {
    const val BASE_URL = "https://eksisozluk.com"
    // /mesaj is only reachable when logged in, so landing there proves the login worked
    const val LOGGED_IN_PATH = "/mesaj"
    const val LOGIN_URL = "$BASE_URL/giris?returnUrl=%2Fmesaj"
    private const val PREFS = "eksi_session"
    private const val KEY_COOKIES = "cookies"
    private const val KEY_USER_AGENT = "user_agent"
    private const val KEY_NICK = "nick"
    private const val KEY_ACCOUNTS = "accounts"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = mutableStateOf(false)
    /** Signed in; the demo (debug builds only) counts as signed in */
    val isLoggedIn: State<Boolean> = androidx.compose.runtime.derivedStateOf { Demo.active || _isLoggedIn.value }

    // The logged-in user's nick, for "my profile"; read from the site after login
    private val _nick = mutableStateOf<String?>(null)
    val nick: State<String?> = androidx.compose.runtime.derivedStateOf { if (Demo.active) Demo.source?.ownNick else _nick.value }

    /** An account signed in on this device: its own cookies, so switching needs no new login */
    data class SavedAccount(val nick: String, val cookies: String, val userAgent: String?)

    private val _accounts = mutableStateOf<List<SavedAccount>>(emptyList())
    /** Every account signed in here, the current one included */
    val accounts: State<List<SavedAccount>> = _accounts

    /** Set before opening the login page to sign in a further account, not to replace this one */
    var addingAccount: Boolean = false

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _isLoggedIn.value = cookies.isNotEmpty()
        _nick.value = prefs.getString(KEY_NICK, null)
        _accounts.value = readAccounts()
        // Signed in before accounts were kept: the current one is the first
        _nick.value?.let { if (cookies.isNotEmpty()) rememberCurrent(it) }
    }

    fun saveNick(nick: String) {
        prefs.edit().putString(KEY_NICK, nick).apply()
        _nick.value = nick
        if (cookies.isNotEmpty()) rememberCurrent(nick)
    }

    /** Keeps the current session in the account list under [nick] (updating its cookies). */
    private fun rememberCurrent(nick: String) {
        val current = SavedAccount(nick, cookies, userAgent)
        val list = _accounts.value.filterNot { it.nick.equals(nick, ignoreCase = true) } + current
        saveAccounts(list.sortedBy { it.nick.lowercase() })
    }

    private fun readAccounts(): List<SavedAccount> = runCatching {
        val array = org.json.JSONArray(prefs.getString(KEY_ACCOUNTS, "[]"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            SavedAccount(o.getString("nick"), o.getString("cookies"), o.optString("ua").ifBlank { null })
        }
    }.getOrDefault(emptyList())

    private fun saveAccounts(list: List<SavedAccount>) {
        val array = org.json.JSONArray()
        list.forEach { a ->
            array.put(org.json.JSONObject().put("nick", a.nick).put("cookies", a.cookies).put("ua", a.userAgent.orEmpty()))
        }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).commit()
        _accounts.value = list
    }

    /**
     * Makes [account] the current one: its cookies for the app's requests and for the WebView
     * (login page, complaint form), and a fresh request session. The caller restarts the app's
     * screens so nothing of the other account stays on them.
     */
    fun switchTo(account: SavedAccount) {
        prefs.edit()
            .putString(KEY_COOKIES, account.cookies)
            .putString(KEY_USER_AGENT, account.userAgent)
            .putString(KEY_NICK, account.nick)
            .commit()
        setWebViewCookies(account.cookies)
        EksiNetworkDataSource.resetSession()
        _isLoggedIn.value = true
        _nick.value = account.nick
    }

    /** The login page for another account: starts with no cookies, the current one stays saved. */
    fun prepareToAddAccount() {
        _nick.value?.let { if (cookies.isNotEmpty()) rememberCurrent(it) }
        addingAccount = true
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    /** Left the login page without adding an account: the current one's cookies back in the WebView */
    fun cancelAddAccount() {
        if (!addingAccount) return
        addingAccount = false
        setWebViewCookies(cookies)
    }

    private fun setWebViewCookies(cookieHeader: String) {
        val manager = CookieManager.getInstance()
        manager.removeAllCookies(null)
        cookieHeader.split(";").map { it.trim() }.filter { it.contains('=') }.forEach { manager.setCookie(BASE_URL, it) }
        manager.flush()
    }

    val cookies: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_COOKIES, "").orEmpty() else ""

    // Cloudflare ties its clearance cookie to the browser, so reuse the WebView's User-Agent
    val userAgent: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_USER_AGENT, null) else null

    /** The WebView's current cookies for eksisozluk.com. */
    fun webViewCookies(): String = CookieManager.getInstance().getCookie(BASE_URL).orEmpty()

    /** Stores cookies of a login that has been confirmed. */
    fun saveLogin(webCookies: String, webViewUserAgent: String) {
        // A new session: whose it is gets known from the site (saveNick)
        if (addingAccount) {
            prefs.edit().remove(KEY_NICK).commit()
            _nick.value = null
            addingAccount = false
        }
        EksiNetworkDataSource.resetSession()
        prefs.edit()
            .putString(KEY_COOKIES, webCookies)
            .putString(KEY_USER_AGENT, webViewUserAgent)
            .apply()
        _isLoggedIn.value = true
    }

    /**
     * Signs the current account out and forgets it. Returns the account switched to when others
     * are signed in here too (the caller restarts the screens), null when none is left.
     */
    fun logout(): SavedAccount? {
        val leaving = _nick.value
        val rest = _accounts.value.filterNot { leaving != null && it.nick.equals(leaving, ignoreCase = true) }
        saveAccounts(rest)
        rest.firstOrNull()?.let { next ->
            switchTo(next)
            return next
        }
        prefs.edit().remove(KEY_COOKIES).remove(KEY_USER_AGENT).remove(KEY_NICK).apply()
        CookieManager.getInstance().removeAllCookies(null)
        EksiNetworkDataSource.resetSession()
        _isLoggedIn.value = false
        _nick.value = null
        return null
    }

    /** Forgets a signed-in account other than the current one. */
    fun forget(account: SavedAccount) {
        if (account.nick.equals(_nick.value, ignoreCase = true)) return
        saveAccounts(_accounts.value.filterNot { it.nick.equals(account.nick, ignoreCase = true) })
    }
}
