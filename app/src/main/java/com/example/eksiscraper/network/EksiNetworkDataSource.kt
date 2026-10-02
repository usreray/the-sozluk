package com.example.eksiscraper.network

import com.example.eksiscraper.BuildConfig
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

object EksiNetworkDataSource {
    private const val BASE_URL = "https://eksisozluk.com"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0"
    
    // Set via eksi.cookie in local.properties; only needed for logged-in actions like favoriting
    private val COOKIE_STRING = BuildConfig.EKSI_COOKIE

    private fun applyCommonConnectionSettings(connection: org.jsoup.Connection): org.jsoup.Connection {
        // Accept-Encoding is left to Jsoup: it only decodes gzip/deflate, not br/zstd
        val configured = connection
            .userAgent(USER_AGENT)
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
        return if (COOKIE_STRING.isNotEmpty()) configured.header("Cookie", COOKIE_STRING) else configured
    }

    suspend fun fetchTopics(page: Int, category: String): List<Topic> = withContext(Dispatchers.IO) {
        val urlPath = when (category) {
            "today" -> "basliklar/bugun"
            "stream" -> "basliklar/sorunsal"
            else -> "basliklar/gundem"
        }

        val randomDelayMs = (100L..500L).random()
        if (randomDelayMs > 0) {
            delay(randomDelayMs)
        }

        val url = if (urlPath == "basliklar/bugun" && page > 1) {
            "$BASE_URL/$urlPath/$page"
        } else {
            val pageParam = if (page > 1) "?p=$page" else ""
            "$BASE_URL/$urlPath$pageParam"
        }

        val connection = applyCommonConnectionSettings(Jsoup.connect(url))
        val response = connection.execute()

        if (response.statusCode() != 200) {
            // Fallback to mobile or handle error
            // For now, returning empty list or throwing exception could be options
            // But keeping it simple as per original logic, maybe return error topic
             return@withContext listOf(
                Topic(
                    title = "Error fetching data: HTTP ${response.statusCode()}",
                    url = "",
                    commentCount = 0,
                    entries = listOf(Entry("Please try again later.")),
                    entriesLoaded = true
                )
            )
        }

        val document = response.parse()
        val topics = HtmlParser.parseTopics(document)
        
        if (topics.isEmpty()) {
             return@withContext listOf(
                Topic(
                    title = "Error fetching data: No topics found",
                    url = "",
                    commentCount = 0,
                    entries = listOf(Entry("Please try again later.")),
                    entriesLoaded = true
                )
            )
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

        val connection = applyCommonConnectionSettings(Jsoup.connect("$BASE_URL$searchUrl"))
        val response = connection.execute()

        if (response.statusCode() != 200) {
             return@withContext Topic(
                title = query,
                url = "$BASE_URL$searchUrl",
                commentCount = 0,
                entries = listOf(Entry("HTTP error: ${response.statusCode()}")),
                entriesLoaded = true,
                totalPages = 1
            )
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

    suspend fun favoriteEntry(entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val favlaUrl = "$BASE_URL/entry/favla"
            val connection = applyCommonConnectionSettings(Jsoup.connect(favlaUrl))
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
            val connection = applyCommonConnectionSettings(Jsoup.connect(favlamaUrl))
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
}
