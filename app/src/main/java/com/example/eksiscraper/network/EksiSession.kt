package com.example.eksiscraper.network

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

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _isLoggedIn.value = cookies.isNotEmpty()
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
        prefs.edit()
            .putString(KEY_COOKIES, webCookies)
            .putString(KEY_USER_AGENT, webViewUserAgent)
            .apply()
        _isLoggedIn.value = true
    }

    fun logout() {
        prefs.edit().remove(KEY_COOKIES).remove(KEY_USER_AGENT).apply()
        CookieManager.getInstance().removeAllCookies(null)
        _isLoggedIn.value = false
    }
}
