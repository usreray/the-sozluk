package com.example.eksiscraper.network

import com.example.eksiscraper.model.AuthorProfile
import com.example.eksiscraper.model.Channel
import com.example.eksiscraper.model.Comment
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.FormSpec
import com.example.eksiscraper.model.MessageBox
import com.example.eksiscraper.model.ThreadDetail
import com.example.eksiscraper.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder

/** ekşi answered 404: no such topic (e.g. a search with no match). */
class TopicNotFoundException : IOException("böyle bir başlık yok")

object EksiNetworkDataSource {
    private const val BASE_URL = EksiSession.BASE_URL
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0"
    
    // Keeps cookies the site sets (iq, ASP.NET_SessionId, ...) across requests, like a browser
    private var session = Jsoup.newSession()

    /** Drops cookies the site set during a login, e.g. after logging out. */
    fun resetSession() {
        session = Jsoup.newSession()
    }

    private fun applyCommonConnectionSettings(connection: org.jsoup.Connection): org.jsoup.Connection {
        // Accept-Encoding is left to Jsoup: it only decodes gzip/deflate, not br/zstd
        val configured = connection
            .userAgent(EksiSession.userAgent ?: USER_AGENT)
            .timeout(10000)
            .followRedirects(true)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("DNT", "1")
            .header("Sec-GPC", "1")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("Priority", "u=0, i")
            .header("TE", "trailers")
            .ignoreHttpErrors(true)
            // XHR endpoints (follow, votes) answer with JSON or plain text, which Jsoup
            // otherwise rejects with "Unhandled content type"
            .ignoreContentType(true)
        // Logged-in users: send the cookies captured from the WebView login
        EksiSession.cookies.split(";").forEach { pair ->
            val name = pair.substringBefore("=").trim()
            if (name.isNotEmpty()) configured.cookie(name, pair.substringAfter("=", "").trim())
        }
        return configured
    }

    suspend fun fetchTopics(page: Int, category: String): List<Topic> = withContext(Dispatchers.IO) {
        val urlPath = when (category) {
            "today" -> "basliklar/bugun"
            "debe" -> "debe"
            // Channels pass their own path, e.g. "basliklar/kanal/spor"
            else -> if (category.startsWith("basliklar/")) category else "basliklar/gundem"
        }

        val randomDelayMs = (100L..500L).random()
        if (randomDelayMs > 0) {
            delay(randomDelayMs)
        }

        val url = if (urlPath == "basliklar/bugun") {
            // /basliklar/bugun without a page number is 404 for logged-out users
            "$BASE_URL/$urlPath/$page"
        } else {
            val pageParam = if (page > 1) (if (urlPath.contains("?")) "&p=$page" else "?p=$page") else ""
            "$BASE_URL/$urlPath$pageParam"
        }

        // Ask for the topic index the way the site's own script does: logged-in pages leave the
        // list out of the HTML and load it with this XHR, which also works when logged out
        // Some lists (bugün when logged in) can take the site a while on the first request of a
        // session and are quick right after; allow more time and retry a timeout once
        fun request() = applyCommonConnectionSettings(session.newRequest(url))
            .header("X-Requested-With", "XMLHttpRequest")
            .timeout(20000)
            .execute()
        val response = try {
            request()
        } catch (e: java.net.SocketTimeoutException) {
            request()
        }

        // Follow lists (olay, takip, son, kenar, çaylaklar) are for logged-in users only
        if (response.statusCode() == 403 || response.statusCode() == 401) {
            throw IOException("bu listeyi görmek için giriş yapmalısın")
        }
        if (response.statusCode() != 200) {
            throw IOException("ekşi sözlük şu an yanıt vermiyor (HTTP ${response.statusCode()})")
        }

        val document = response.parse()
        val topics = HtmlParser.parseTopics(document)
        // An empty follow list is fine ("hiç başlık yok"); a page without any list is not
        if (topics.isEmpty() && document.selectFirst(".topic-list, #content-body, #partial-index") == null) {
            throw IOException("başlık listesi okunamadı")
        }

        return@withContext topics
    }

    suspend fun searchTopic(query: String, page: Int, redirectedUrl: String = ""): Topic = withContext(Dispatchers.IO) {
        val searchUrl = if (redirectedUrl.isNotEmpty()) {
            if (page > 1) {
                if (redirectedUrl.contains("?")) {
                    if (redirectedUrl.contains("p=")) {
                        redirectedUrl.replaceFirst(Regex("p=\\d+"), "p=$page")
                    } else {
                        "$redirectedUrl&p=$page"
                    }
                } else {
                    "$redirectedUrl?p=$page"
                }
            } else {
                redirectedUrl
            }
        } else {
            "/?q=${URLEncoder.encode(query.trim(), "UTF-8")}"
        }

        val connection = applyCommonConnectionSettings(session.newRequest("$BASE_URL$searchUrl"))
        val response = connection.execute()

        if (response.statusCode() == 404) throw TopicNotFoundException()
        if (response.statusCode() != 200) {
            throw IOException("başlık yüklenemedi (HTTP ${response.statusCode()})")
        }

        val document = response.parse()
        return@withContext HtmlParser.parseTopicDetail(document, query, page, searchUrl, BASE_URL)
    }
    
    suspend fun fetchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/autocomplete/query?q=${URLEncoder.encode(query.trim(), "UTF-8")}"
        val response = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(5000)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .execute()
        if (response.statusCode() != 200) return@withContext emptyList()

        // Response: {"Titles":[...],"Query":"...","Nicks":[...]}
        val titles = JSONObject(response.body()).optJSONArray("Titles") ?: return@withContext emptyList()
        List(titles.length()) { titles.getString(it) }
    }

    /** True if these cookies belong to a logged-in user (the login-only page doesn't redirect). */
    suspend fun isLoggedIn(cookies: String, userAgent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = Jsoup.connect("$BASE_URL${EksiSession.LOGGED_IN_PATH}")
                .userAgent(userAgent)
                .timeout(10000)
                .followRedirects(false)
                .ignoreHttpErrors(true)
                .header("Cookie", cookies)
            connection.execute().statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun favoriteEntry(entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val favlaUrl = "$BASE_URL/entry/favla"
            val connection = applyCommonConnectionSettings(session.newRequest(favlaUrl))
                .method(org.jsoup.Connection.Method.POST)
                .referrer("$BASE_URL/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Requested-With", "XMLHttpRequest")
            
            connection.data("entryId", entryId)
            val response = connection.execute()
            return@withContext response.statusCode() == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun unfavoriteEntry(entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val favlamaUrl = "$BASE_URL/entry/favlama"
            val connection = applyCommonConnectionSettings(session.newRequest(favlamaUrl))
                .method(org.jsoup.Connection.Method.POST)
                .referrer("$BASE_URL/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Requested-With", "XMLHttpRequest")
            
            connection.data("entryId", entryId)
            val response = connection.execute()
            return@withContext response.statusCode() == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * şükela (rate 1) or çok kötü (rate -1), as the site's own script sends it; [owner] is the
     * author's id. [rate] 0 takes a vote back.
     */
    suspend fun vote(entryId: String, authorId: String, rate: Int, previous: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Switching sides or taking a vote back first removes the old one
                if (previous != 0) {
                    val removed = ajaxPost("/entry/removevote", mapOf("id" to entryId, "rate" to "$previous", "owner" to authorId))
                    if (!removed || rate == 0) return@withContext removed
                }
                ajaxPost("/entry/vote", mapOf("id" to entryId, "rate" to "$rate", "owner" to authorId))
            } catch (e: Exception) {
                false
            }
        }

    /** Comment votes use the same payload as entries, on /yorum/vote. */
    suspend fun voteComment(commentId: String, authorId: String, rate: Int, previous: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (previous != 0) {
                    val removed = ajaxPost("/yorum/removevote", mapOf("id" to commentId, "rate" to "$previous", "owner" to authorId))
                    if (!removed || rate == 0) return@withContext removed
                }
                ajaxPost("/yorum/vote", mapOf("id" to commentId, "rate" to "$rate", "owner" to authorId))
            } catch (e: Exception) {
                false
            }
        }

    suspend fun fetchComments(entryId: String): List<Comment> = withContext(Dispatchers.IO) {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/yorum/liste/$entryId"))
            .header("X-Requested-With", "XMLHttpRequest")
            .execute()
        if (response.statusCode() != 200) throw IOException("yorumlar yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseComments(response.parse())
    }

    /** The message box (or its archive), with the forms to send and to delete / archive. */
    suspend fun fetchMessageBox(archive: Boolean, page: Int): MessageBox = withContext(Dispatchers.IO) {
        val path = if (archive) "/mesaj/arsiv" else "/mesaj"
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL$path?p=$page"))
            .followRedirects(false)
            .execute()
        // A redirect to /giris means the session expired
        if (response.statusCode() in 300..399) throw IOException("oturum süresi dolmuş; tekrar giriş yap")
        if (response.statusCode() != 200) throw IOException("mesajlar yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseMessageBox(response.parse())
    }

    /**
     * Posts the "yeni mesaj" form. Where ekşi redirects afterwards varies (the conversation or
     * /mesaj), so only a notice on the landing page means a rejection; callers confirm delivery
     * by reloading.
     */
    suspend fun sendMessage(form: FormSpec, to: String, text: String): String? = withContext(Dispatchers.IO) {
        try {
            val action = if (form.action.startsWith("http")) form.action else BASE_URL + form.action
            val response = applyCommonConnectionSettings(session.newRequest(action))
                .method(org.jsoup.Connection.Method.POST)
                .referrer("$BASE_URL/mesaj")
                .header("Origin", BASE_URL)
                .data(form.fields + mapOf("To" to to, (form.textFieldName ?: "Message") to text))
                .execute()
            val landed = response.url().path
            val notice = Jsoup.parse(response.body())
                .select(".field-validation-error, .validation-summary-errors, #message-validation-result, #notice, .notice, .toast, .alert, .error")
                .filterNot { it.attr("style").replace(" ", "").contains("display:none") }
                .map { it.ownText().ifBlank { it.text() }.trim().trimEnd('×').trim() }
                .firstOrNull { it.isNotEmpty() && it.length < 300 }
            android.util.Log.d("EksiForm", "POST ${form.action} -> ${response.statusCode()} $landed notice=${notice ?: "-"}")
            if (response.statusCode() !in 200..399) "mesaj gönderilemedi (HTTP ${response.statusCode()})" else notice
        } catch (e: Exception) {
            e.message ?: "mesaj gönderilemedi"
        }
    }

    suspend fun fetchThread(id: String): ThreadDetail = withContext(Dispatchers.IO) {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/mesaj/$id")).execute()
        if (response.statusCode() != 200) throw IOException("konuşma yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseThread(response.parse())
    }

    /** Nicks who favorited an entry (logged-in only; anonymous gets 403). */
    /** Who favorited an entry; [rookies]: the çaylak (rookie) favorites the site lists apart. */
    suspend fun fetchFavoriters(entryId: String, rookies: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val path = if (rookies) "entry/caylakfavorites" else "entry/favorileyenler"
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/$path?entryId=$entryId"))
            .header("X-Requested-With", "XMLHttpRequest")
            .timeout(30000)
            .execute()
        if (response.statusCode() == 403) throw IOException("favorileyenleri görmek için giriş yap")
        if (response.statusCode() != 200) throw IOException("liste yüklenemedi (HTTP ${response.statusCode()})")
        response.parse().select("li a[href^=/biri/]").map { it.text().trim() }.filter { it.isNotEmpty() }
    }

    suspend fun fetchChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/kanallar")).execute()
        if (response.statusCode() != 200) throw IOException("kanallar yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseChannels(response.parse())
    }

    /** Follow / unfollow with the URL from the profile's "takip et" button. */
    /**
     * Returns null on success or the reason. Like the site's script, judges by the reply text:
     * "LimitReached", "InvalidRelation" and "SystemUser" are refusals even with HTTP 200.
     */
    suspend fun postRelation(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val path = if (url.startsWith("http")) url.removePrefix(BASE_URL) else url
            val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL$path"))
                .method(org.jsoup.Connection.Method.POST)
                .referrer("$BASE_URL/")
                .header("Origin", BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "*/*")
                .requestBody("")
                .execute()
            val reply = response.body().trim().trim('"')
            android.util.Log.d("EksiForm", "POST ${path.substringBefore("?")} -> ${response.statusCode()} reply=${reply.take(40)}")
            when {
                response.statusCode() !in 200..299 -> "işlem başarısız (HTTP ${response.statusCode()})"
                reply == "LimitReached" -> "bu işlem için sınıra ulaştın"
                reply == "InvalidRelation" -> "bu kullanıcıyla bu işlem yapılamıyor"
                reply == "SystemUser" -> "sistem kullanıcısına bu yapılamaz"
                else -> null
            }
        } catch (e: Exception) {
            e.message ?: "işlem başarısız"
        }
    }

    private fun ajaxPost(path: String, data: Map<String, String>): Boolean {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL$path"))
            .method(org.jsoup.Connection.Method.POST)
            .referrer("$BASE_URL/")
            .header("X-Requested-With", "XMLHttpRequest")
            .data(data)
            .execute()
        return response.statusCode() == 200
    }

    /**
     * Posts a form read from a page (entry form, delete form) with its hidden fields, including
     * the CSRF token, plus [values]. Returns an error message, or null on success.
     */
    suspend fun submitForm(form: FormSpec, values: Map<String, String>, ajax: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            try {
                val action = if (form.action.startsWith("http")) form.action else BASE_URL + form.action
                val request = applyCommonConnectionSettings(session.newRequest(action))
                    .method(org.jsoup.Connection.Method.POST)
                    .referrer("$BASE_URL/")
                    .header("Origin", BASE_URL)
                    .data(form.fields + values)
                // The site's own script posts some forms as XHR; then the status code is the verdict
                if (ajax) request.header("X-Requested-With", "XMLHttpRequest").header("Accept", "*/*")
                val response = request.execute()
                val body = response.body()
                val document = runCatching { Jsoup.parse(body) }.getOrNull()
                val error = document?.select(
                    ".field-validation-error, .validation-summary-errors, #message-validation-result, .error-message"
                )?.text()?.trim()?.ifEmpty { null }
                // Diagnostics without content: status, where the post landed, and any error text
                android.util.Log.d(
                    "EksiForm",
                    "POST ${form.action} -> ${response.statusCode()} ${response.url().path} " +
                        "type=${response.contentType()} len=${body.length} error=${error ?: "-"}"
                )
                when {
                    response.statusCode() !in 200..399 -> "işlem başarısız (HTTP ${response.statusCode()})"
                    error != null -> error
                    // XHR endpoints may answer {"Success":false,"Message":"..."}
                    body.trimStart().startsWith("{") && body.contains("\"Success\":false") ->
                        runCatching { JSONObject(body).optString("Message") }.getOrNull()?.ifEmpty { null } ?: "İşlem başarısız"
                    else -> null
                }
            } catch (e: Exception) {
                e.message ?: "işlem başarısız"
            }
        }

    /**
     * The file behind an uploaded image. Entries link to a /img/<code> page that only wraps the
     * picture (img#image, also in og:image); cdn addresses are already the file.
     */
    suspend fun resolveImageUrl(ref: String): String = withContext(Dispatchers.IO) {
        if (ref.startsWith("http") && !ref.contains("eksisozluk.com/img/")) return@withContext ref
        val path = if (ref.startsWith("http")) ref.substringAfter("eksisozluk.com") else ref
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL$path"))
            .timeout(20000)
            .execute()
        if (response.statusCode() != 200) throw IOException("görsel bulunamadı (HTTP ${response.statusCode()})")
        val document = response.parse()
        val src = document.selectFirst("img#image")?.attr("src")?.ifBlank { null }
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")?.ifBlank { null }
            ?: throw IOException("görsel bulunamadı")
        if (src.startsWith("//")) "https:$src" else src
    }

    /** Followers (or, with [following], who they follow): the site's JSON list, up to 100 people. */
    suspend fun fetchFollowList(nick: String, following: Boolean): List<com.example.eksiscraper.model.FollowUser> =
        withContext(Dispatchers.IO) {
            val kind = if (following) "following" else "follower"
            val response = applyCommonConnectionSettings(
                session.newRequest("$BASE_URL/$kind?nick=${URLEncoder.encode(nick, "UTF-8")}")
            )
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json")
                .execute()
            if (response.statusCode() != 200) throw IOException("liste yüklenemedi (HTTP ${response.statusCode()})")
            val array = org.json.JSONArray(response.body())
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                val avatar = item.optString("AvatarUrl").ifBlank { item.optString("Picture") }
                com.example.eksiscraper.model.FollowUser(
                    nick = item.optJSONObject("Nick")?.optString("Value").orEmpty(),
                    avatarUrl = avatar.takeIf { it.startsWith("http") && !it.contains("default-profile") },
                    isVerified = item.optBoolean("IsVerified")
                )
            }.filter { it.nick.isNotBlank() }
        }

    /** The form of an entry's "düzelt" page (/entry/duzelt/<id>), holding its current text. */
    suspend fun fetchEditForm(entryId: String): FormSpec = withContext(Dispatchers.IO) {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/entry/duzelt/$entryId"))
            .timeout(20000)
            .execute()
        if (response.statusCode() == 403 || response.statusCode() == 401) throw IOException("bu entry'yi düzeltemezsin")
        if (response.statusCode() != 200) throw IOException("düzeltme sayfası açılamadı (HTTP ${response.statusCode()})")
        HtmlParser.parseEditForm(response.parse()) ?: throw IOException("düzeltme formu bulunamadı")
    }

    /** The header lights the site polls (GET /top/led); null when it can't be read. */
    suspend fun fetchSiteStatus(): com.example.eksiscraper.notify.SiteStatus? = withContext(Dispatchers.IO) {
        try {
            val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/top/led"))
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json")
                .followRedirects(false)
                .timeout(15000)
                .execute()
            if (response.statusCode() != 200) return@withContext null
            val json = JSONObject(response.body())
            com.example.eksiscraper.notify.SiteStatus(
                hasMessages = json.optBoolean("HasMessages"),
                hasEvents = json.optBoolean("HasEvents")
            )
        } catch (e: Exception) {
            null
        }
    }

    /** "Başlığı açan" box: who opened the topic and when, as plain text lines. */
    suspend fun fetchTopicCreator(topicId: String): com.example.eksiscraper.model.TopicCreator = withContext(Dispatchers.IO) {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/topic/gettopiccreatorinfo?topicId=$topicId"))
            .header("X-Requested-With", "XMLHttpRequest")
            .execute()
        if (response.statusCode() == 403) throw IOException("bunu görmek için giriş yapmalısın")
        if (response.statusCode() != 200) throw IOException("bilgi alınamadı (HTTP ${response.statusCode()})")
        val body = response.parse().body()
        val nick = body.selectFirst("a[href^=/biri/]")?.text()?.trim()?.removePrefix("@")?.ifBlank { null }
        // The box carries the site's own buttons (takip et, başlıklarını engelle, ...); drop them
        body.select(
            "a[data-add-url], a[data-remove-url], .relation-link, .mute-icon-link, button, form, .dropdown-menu, " +
                "#remove-relation-dialog, [id*=dialog], .modal, [style*=display:none], [style*=display: none]"
        ).remove()
        body.select("a[href^=/biri/]").remove()
        val actionWords = setOf(
            "takip et", "takibi bırak", "takip etme", "engelle", "engeli kaldır",
            "başlıklarını engelle", "sessize al", "sessizden çıkar", "mesaj gönder", "mesaj at"
        )
        // Leaf blocks only, so nested markup doesn't repeat the same text
        val lines = body.select("p, li, div, span")
            .filter { block -> block.children().none { it.tagName() in setOf("p", "li", "div", "span") } }
            .map { it.text().trim().trim('·', '-', '|').trim() }
            // Questions are leftovers of a confirmation dialog ("... emin misiniz?")
            .filter { it.isNotEmpty() && it.lowercase() !in actionWords && it != nick && !it.contains("emin misiniz") }
            .distinct()
        com.example.eksiscraper.model.TopicCreator(nick, lines)
    }

    suspend fun fetchProfile(nick: String): AuthorProfile = withContext(Dispatchers.IO) {
        val response = applyCommonConnectionSettings(session.newRequest("$BASE_URL/biri/${encodePath(nick)}"))
            .execute()
        if (response.statusCode() == 404) throw IOException("böyle bir yazar yok")
        if (response.statusCode() != 200) throw IOException("profil yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseProfile(response.parse(), nick)
    }

    /** The author's uploaded images ("görselleri"); the site sends them all at once. */
    suspend fun fetchUserImages(nick: String): List<com.example.eksiscraper.model.AuthorImage> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/gorselleri?nick=${URLEncoder.encode(nick, "UTF-8")}"
        val response = applyCommonConnectionSettings(session.newRequest(url))
            .header("X-Requested-With", "XMLHttpRequest")
            .execute()
        if (response.statusCode() != 200) throw IOException("görseller yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseUserImages(response.parse())
    }

    /** One page of a profile tab such as "son-entryleri" or "en-begenilenleri". */
    suspend fun fetchUserEntries(nick: String, tab: String, page: Int): List<Entry> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$tab?nick=${URLEncoder.encode(nick, "UTF-8")}&p=$page"
        val response = applyCommonConnectionSettings(session.newRequest(url))
            .header("X-Requested-With", "XMLHttpRequest")
            .execute()
        if (response.statusCode() != 200) throw IOException("entry'ler yüklenemedi (HTTP ${response.statusCode()})")
        HtmlParser.parseUserEntries(response.parse())
    }

    /** The logged-in user's nick, read from the "ben" link in the site header. */
    suspend fun fetchOwnNick(): String? = withContext(Dispatchers.IO) {
        try {
            val document = applyCommonConnectionSettings(session.newRequest("$BASE_URL/")).execute().parse()
            document.select("#top-navigation a[href^=/biri/], header a[href^=/biri/], nav a[href^=/biri/]")
                .firstOrNull()?.attr("href")?.removePrefix("/biri/")?.substringBefore("/")?.substringBefore("?")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                ?.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    // Nicks use dashes for spaces in profile URLs ("il leone" -> /biri/il-leone)
    private fun encodePath(nick: String) = URLEncoder.encode(nick.trim().replace(' ', '-'), "UTF-8")
}
