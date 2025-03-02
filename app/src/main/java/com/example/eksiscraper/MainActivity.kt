// MainActivity.kt
// MainActivity.kt
package com.example.eksiscraper

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign

// ======== DATA CLASSES ========
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
    val totalPages: Int = 1
)

data class Entry(
    val content: String,
    val author: String = "",
    val date: String = ""
)

// ======== LOCAL DATA SOURCE ========
object LocalDataSource {
    fun getLocalData() = HomeResponse(
        home = listOf(
            Topic(
                title = "kahve içmeden güne başlayamayanlar",
                url = "/kahve-icmeden-gune-baslayamayanlar",
                commentCount = 120,
                entries = listOf(
                    Entry("kahve olmadan sabahları beynim çalışmıyor. resmen yaşam destek ünitem."),
                    Entry("kahve bağımlılığı diye bir şey varsa kesinlikle bende var. gün içinde 4-5 bardak içmeden olmuyor.")
                ),
                entriesLoaded = true
            ),
            Topic(
                title = "yeni nesil müziklerin eski müziklerden kötü olması",
                url = "/yeni-nesil-muziklerin-eski-muziklerden-kotu-olmasi",
                commentCount = 85,
                entries = listOf(
                    Entry("eski müziklerin ruhu vardı, şimdi çıkan şarkılar hep copy-paste gibi geliyor."),
                    Entry("tiktok sayesinde şarkılar 15 saniyelik viral kısımlar için üretiliyor gibi, gerisi boş.")
                ),
                entriesLoaded = true
            ),
            Topic(
                title = "metroda yüksek sesle konuşan insanlar",
                url = "/metroda-yuksek-sesle-konusan-insanlar",
                commentCount = 65,
                entries = listOf(
                    Entry("kardeşim, tüm vagonun sizin konuşmanızı dinlemek zorunda olması ne kadar mantıklı geliyor?"),
                    Entry("metroda bangır bangır konuşanlara neden uyarı yapılmıyor anlamıyorum. keşke herkes biraz daha saygılı olsa.")
                ),
                entriesLoaded = true
            )
        )
    )
}

// ======== NETWORK SERVICE ========
object EksiService {
    private const val BASE_URL = "https://eksisozluk.com"
    private const val FALLBACK_URL = "https://eksisozluk1923.com"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    
    // Add a flag to enable detailed logging to a file
    private var debugLogEnabled = true
    private var debugLog = StringBuilder()
    
    // Store the last fetched topics for JSON export
    private var lastFetchedTopics = listOf<Topic>()
    
    private fun logDebug(message: String) {
        println(message)
        if (debugLogEnabled) {
            debugLog.append(message).append("\n")
        }
    }
    
    suspend fun getPopularTopics(): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Clear previous debug log
            debugLog.clear()
            
            logDebug("EksiService: Starting to fetch topics")
            // Try with the main URL first
            val result = tryFetchTopicsWithoutEntries(BASE_URL)
            
            // If that fails, try with the fallback URL
            if (result.size == 1 && result[0].title.startsWith("Error fetching data")) {
                logDebug("EksiService: Main URL failed, trying fallback URL")
                val fallbackResult = tryFetchTopicsWithoutEntries(FALLBACK_URL)
                if (fallbackResult.size > 1 || !fallbackResult[0].title.startsWith("Error fetching data")) {
                    lastFetchedTopics = fallbackResult
                    return@withContext fallbackResult
                }
            }
            
            logDebug("EksiService: Returning ${result.size} topics")
            lastFetchedTopics = result
            return@withContext result
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in getPopularTopics: ${e.message}")
            // Return some fallback data in case of error
            val errorResult = listOf(
                Topic(
                    title = "Error fetching data: ${e.message}",
                    url = "",
                    commentCount = 0,
                    entries = listOf(Entry("Please check your internet connection and try again.")),
                    entriesLoaded = true
                )
            )
            lastFetchedTopics = errorResult
            return@withContext errorResult
        }
    }
    
    private suspend fun tryFetchTopicsWithoutEntries(baseUrl: String): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Add shorter random delay to mimic human behavior but not cause too much waiting
            logDebug("EksiService: Adding short delay before request")
            kotlinx.coroutines.delay((500..1000).random().toLong())
            
            val cookies = HashMap<String, String>()
            cookies["__cf_bm"] = "" // CloudFlare bypass attempt
            cookies["_ga"] = ""
            cookies["_gid"] = ""
            
            logDebug("EksiService: Setting up connection to $baseUrl")
            val connection = Jsoup.connect("$baseUrl/")
                .userAgent(USER_AGENT)
                .timeout(10000) // Reduced timeout to 10 seconds
                .referrer("https://www.google.com/search?q=eksisozluk")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "cross-site")
                .header("Sec-Fetch-User", "?1")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("DNT", "1")
                .cookies(cookies)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
            
            logDebug("EksiService: Executing connection")
            val response = connection.execute()
            
            logDebug("EksiService: Response status code: ${response.statusCode()}")
            if (response.statusCode() != 200) {
                // Try a different approach - direct mobile URL
                logDebug("EksiService: Status code not 200, trying mobile version")
                return@withContext tryMobileVersionWithoutEntries(baseUrl)
            }
            
            logDebug("EksiService: Parsing document")
            val document = response.parse()
            
            // Save the HTML for debugging
            logDebug("EksiService: HTML Content (first 1000 chars): ${document.html().take(1000)}...")
            
            // Try different selectors that might work for eksisozluk.com
            var topicElements = document.select("#content-body .topic-list li a")
            logDebug("EksiService: First selector found ${topicElements.size} elements")
            
            // If the first selector doesn't work, try alternatives
            if (topicElements.isEmpty()) {
                topicElements = document.select(".topic-list li a")
                logDebug("EksiService: Second selector found ${topicElements.size} elements")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("#popular-topics li a")
                logDebug("EksiService: Third selector found ${topicElements.size} elements")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("#partial-index li a")
                logDebug("EksiService: Fourth selector found ${topicElements.size} elements")
            }
            
            if (topicElements.isEmpty()) {
                // Use a different approach to get links that start with '/'
                logDebug("EksiService: All selectors failed, trying generic approach")
                val allLinks = document.select("a[href^='/']")
                logDebug("EksiService: Found ${allLinks.size} links starting with '/'")
                // Create a new Elements collection for non-empty text links
                val filteredLinks = Elements()
                for (link in allLinks) {
                    if (link.text().isNotEmpty()) {
                        filteredLinks.add(link)
                    }
                }
                topicElements = filteredLinks
                logDebug("EksiService: After filtering, found ${topicElements.size} links with non-empty text")
                
                // Log the first 10 links for debugging
                topicElements.take(10).forEachIndexed { index, element ->
                    logDebug("Link $index: Text='${element.text()}', Href='${element.attr("href")}'")
                }
            }
            
            val topics = mutableListOf<Topic>()
            
            // Fetch 10 topics as requested
            val limitedTopics = if (topicElements.size > 10) topicElements.subList(0, 10) else topicElements
            logDebug("EksiService: Processing ${limitedTopics.size} topics")
            
            for (topicElement in limitedTopics) {
                val title = topicElement.text()
                val href = topicElement.attr("href")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
                    logDebug("EksiService: Processing topic: $title, href: $href")
                    // Don't fetch entries yet, just store the topic info
                    val commentCountText = topicElement.parent()?.select("small")?.text()?.trim() ?: ""
                    val commentCount = commentCountText.toIntOrNull() ?: 0
                    topics.add(Topic(
                        title = title,
                        url = "$baseUrl$href",
                        commentCount = commentCount,
                        entries = emptyList(),
                        entriesLoaded = false
                    ))
                }
            }
            
            if (topics.isEmpty()) {
                logDebug("EksiService: No topics found on the page")
                throw Exception("No topics found on the page")
            }
            
            logDebug("EksiService: Successfully fetched ${topics.size} topics")
            topics
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in tryFetchTopics: ${e.message}")
            // Return error data
            listOf(
                Topic(
                    title = "Error fetching data: ${e.message}",
                    url = "",
                    commentCount = 0,
                    entries = listOf(Entry("Please check your internet connection and try again.")),
                    entriesLoaded = true
                )
            )
        }
    }
    
    private suspend fun tryMobileVersionWithoutEntries(baseUrl: String): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Try the mobile version of the site
            val mobileUrl = "$baseUrl/mobil"
            logDebug("EksiService: Trying mobile version at $mobileUrl")
            
            val connection = Jsoup.connect(mobileUrl)
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                .timeout(10000) // Reduced timeout to 10 seconds
                .referrer("https://www.google.com")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
            
            logDebug("EksiService: Executing mobile connection")
            val response = connection.execute()
            
            logDebug("EksiService: Mobile response status code: ${response.statusCode()}")
            if (response.statusCode() != 200) {
                throw Exception("HTTP error: ${response.statusCode()} - ${response.statusMessage()}")
            }
            
            logDebug("EksiService: Parsing mobile document")
            val document = response.parse()
            
            // Try mobile-specific selectors
            var topicElements = document.select(".topic-list li a")
            logDebug("EksiService: Mobile first selector found ${topicElements.size} elements")
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("ul.topic-list li a")
                logDebug("EksiService: Mobile second selector found ${topicElements.size} elements")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("a.index-link")
                logDebug("EksiService: Mobile third selector found ${topicElements.size} elements")
            }
            
            val topics = mutableListOf<Topic>()
            
            // Fetch 10 topics as requested
            val limitedTopics = if (topicElements.size > 10) topicElements.subList(0, 10) else topicElements
            logDebug("EksiService: Processing ${limitedTopics.size} mobile topics")
            
            for (topicElement in limitedTopics) {
                val title = topicElement.text()
                val href = topicElement.attr("href")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
                    logDebug("EksiService: Processing mobile topic: $title")
                    // Don't fetch entries yet, just store the topic info
                    val commentCountText = topicElement.parent()?.select("small")?.text()?.trim() ?: ""
                    val commentCount = commentCountText.toIntOrNull() ?: 0
                    topics.add(Topic(
                        title = title,
                        url = "$baseUrl$href",
                        commentCount = commentCount,
                        entries = emptyList(),
                        entriesLoaded = false
                    ))
                }
            }
            
            if (topics.isEmpty()) {
                logDebug("EksiService: No topics found on the mobile page")
                throw Exception("No topics found on the mobile page")
            }
            
            logDebug("EksiService: Successfully fetched ${topics.size} mobile topics")
            topics
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in tryMobileVersion: ${e.message}")
            listOf(
                Topic(
                    title = "Error fetching data: ${e.message}",
                    url = "",
                    commentCount = 0,
                    entries = listOf(Entry("Please check your internet connection and try again.")),
                    entriesLoaded = true
                )
            )
        }
    }
    
    // Make this method public so it can be called directly for a single topic
    suspend fun getEntriesForTopic(baseUrl: String, topicPath: String): List<Entry> = withContext(Dispatchers.IO) {
        try {
            // Add shorter random delay to mimic human behavior but not cause too much waiting
            logDebug("EksiService: Adding short delay before fetching entries")
            kotlinx.coroutines.delay((300..600).random().toLong())
            
            val cookies = HashMap<String, String>()
            cookies["__cf_bm"] = "" // CloudFlare bypass attempt
            cookies["_ga"] = ""
            cookies["_gid"] = ""
            
            logDebug("EksiService: Setting up connection for entries at $baseUrl$topicPath")
            val connection = Jsoup.connect("$baseUrl$topicPath")
                .userAgent(USER_AGENT)
                .timeout(10000) // Reduced timeout to 10 seconds
                .referrer(baseUrl)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-User", "?1")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("DNT", "1")
                .cookies(cookies)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
            
            logDebug("EksiService: Executing entries connection")
            val response = connection.execute()
            
            logDebug("EksiService: Entries response status code: ${response.statusCode()}")
            if (response.statusCode() != 200) {
                // Try mobile version
                logDebug("EksiService: Entries status code not 200, trying mobile version")
                return@withContext getMobileEntries(baseUrl, topicPath)
            }
            
            logDebug("EksiService: Parsing entries document")
            val document = response.parse()
            
            // Try different selectors for entries
            var entryElements = document.select("div.content")
            logDebug("EksiService: First entries selector found ${entryElements.size} elements")
            
            if (entryElements.isEmpty()) {
                entryElements = document.select(".entry-content")
                logDebug("EksiService: Second entries selector found ${entryElements.size} elements")
            }
            
            if (entryElements.isEmpty()) {
                entryElements = document.select("div.entry")
                logDebug("EksiService: Third entries selector found ${entryElements.size} elements")
            }
            
            val entries = mutableListOf<Entry>()
            
            // Fetch 10 entries per topic as requested
            val limitedEntries = if (entryElements.size > 10) entryElements.subList(0, 10) else entryElements
            logDebug("EksiService: Processing ${limitedEntries.size} entries")
            
            for (entryElement in limitedEntries) {
                val content = entryElement.text()
                if (content.isNotEmpty()) {
                    entries.add(Entry(content))
                }
            }
            
            if (entries.isEmpty()) {
                logDebug("EksiService: No entries found for this topic")
                entries.add(Entry("No entries found for this topic"))
            }
            
            logDebug("EksiService: Successfully fetched ${entries.size} entries")
            entries
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in getEntriesForTopic: ${e.message}")
            listOf(Entry("Error loading entries: ${e.message}"))
        }
    }
    
    private suspend fun getMobileEntries(baseUrl: String, topicPath: String): List<Entry> = withContext(Dispatchers.IO) {
        try {
            // Try the mobile version for entries
            val mobileUrl = "$baseUrl/mobil$topicPath"
            logDebug("EksiService: Trying mobile version for entries at $mobileUrl")
            
            val connection = Jsoup.connect(mobileUrl)
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                .timeout(10000) // Reduced timeout to 10 seconds
                .referrer("$baseUrl/mobil")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
            
            logDebug("EksiService: Executing mobile entries connection")
            val response = connection.execute()
            
            logDebug("EksiService: Mobile entries response status code: ${response.statusCode()}")
            if (response.statusCode() != 200) {
                return@withContext listOf(Entry("HTTP error: ${response.statusCode()} - ${response.statusMessage()}"))
            }
            
            logDebug("EksiService: Parsing mobile entries document")
            val document = response.parse()
            
            // Try mobile-specific selectors
            var entryElements = document.select(".content")
            logDebug("EksiService: Mobile first entries selector found ${entryElements.size} elements")
            
            if (entryElements.isEmpty()) {
                entryElements = document.select(".entry")
                logDebug("EksiService: Mobile second entries selector found ${entryElements.size} elements")
            }
            
            val entries = mutableListOf<Entry>()
            
            // Fetch 10 entries per topic as requested
            val limitedEntries = if (entryElements.size > 10) entryElements.subList(0, 10) else entryElements
            logDebug("EksiService: Processing ${limitedEntries.size} mobile entries")
            
            for (entryElement in limitedEntries) {
                val content = entryElement.text()
                if (content.isNotEmpty()) {
                    entries.add(Entry(content))
                }
            }
            
            if (entries.isEmpty()) {
                logDebug("EksiService: No entries found for this mobile topic")
                entries.add(Entry("No entries found for this topic"))
            }
            
            logDebug("EksiService: Successfully fetched ${entries.size} mobile entries")
            entries
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in getMobileEntries: ${e.message}")
            listOf(Entry("Error loading entries: ${e.message}"))
        }
    }
    
    // Add a method to get the debug log
    fun getDebugLog(): String {
        return debugLog.toString()
    }
    
    // Add a method to convert topics to JSON
    fun getTopicsAsJson(): String {
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"topics\": [\n")
        
        lastFetchedTopics.forEachIndexed { topicIndex, topic ->
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"title\": \"${escapeJson(topic.title)}\",\n")
            jsonBuilder.append("      \"entries\": [\n")
            
            topic.entries.forEachIndexed { entryIndex, entry ->
                jsonBuilder.append("        {\n")
                jsonBuilder.append("          \"content\": \"${escapeJson(entry.content)}\"\n")
                jsonBuilder.append("        }${if (entryIndex < topic.entries.size - 1) "," else ""}\n")
            }
            
            jsonBuilder.append("      ]\n")
            jsonBuilder.append("    }${if (topicIndex < lastFetchedTopics.size - 1) "," else ""}\n")
        }
        
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}\n")
        
        return jsonBuilder.toString()
    }
    
    // Helper function to escape JSON strings
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t")
    }
    
    // Add a new method to search for a topic
    suspend fun searchTopic(query: String, page: Int = 1, redirectedUrl: String = ""): Topic = withContext(Dispatchers.IO) {
        try {
            logDebug("EksiService: Searching for topic: $query, page: $page, redirectedUrl: $redirectedUrl")
            
            // Determine the URL to use
            val searchUrl = if (redirectedUrl.isNotEmpty()) {
                // If we have a redirected URL, use it with the page parameter if needed
                if (page > 1) {
                    // Check if the URL already has query parameters
                    if (redirectedUrl.contains("?")) {
                        // Check if it already has a page parameter
                        if (redirectedUrl.contains("p=")) {
                            // Replace existing page parameter
                            redirectedUrl.replaceFirst(Regex("p=\\d+"), "p=$page")
                        } else {
                            // Add page parameter to existing query string
                            "$redirectedUrl&p=$page"
                        }
                    } else {
                        // Add page parameter as first query parameter
                        "$redirectedUrl?p=$page"
                    }
                } else {
                    // For page 1, keep the original URL with all its parameters
                    redirectedUrl
                }
            } else {
                // If we don't have a redirected URL, use the formatted query
                "/${query.trim().replace(" ", "-").lowercase()}"
            }
            
            logDebug("EksiService: Using search URL: $searchUrl")
            
            // Connect to the URL and follow redirections
            val connection = Jsoup.connect("$BASE_URL$searchUrl")
                .userAgent(USER_AGENT)
                .timeout(10000)
                .followRedirects(true)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-User", "?1")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("DNT", "1")
                .ignoreHttpErrors(true) // Important: ignore HTTP errors to handle them gracefully
            
            logDebug("EksiService: Executing connection")
            val response = connection.execute()
            
            // Check for HTTP errors
            if (response.statusCode() != 200) {
                logDebug("EksiService: HTTP error: ${response.statusCode()} - ${response.statusMessage()}")
                
                // Try fallback URL if main URL fails
                if (BASE_URL == "https://eksisozluk.com") {
                    logDebug("EksiService: Trying fallback URL")
                    val fallbackUrl = "$FALLBACK_URL$searchUrl"
                    logDebug("EksiService: Using fallback URL: $fallbackUrl")
                    
                    val fallbackConnection = Jsoup.connect(fallbackUrl)
                        .userAgent(USER_AGENT)
                        .timeout(10000)
                        .followRedirects(true)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .ignoreHttpErrors(true)
                    
                    val fallbackResponse = fallbackConnection.execute()
                    
                    if (fallbackResponse.statusCode() == 200) {
                        logDebug("EksiService: Fallback URL successful")
                        // Use the fallback response
                        return@withContext processTopicResponse(fallbackResponse, query, page, searchUrl, FALLBACK_URL)
                    } else {
                        logDebug("EksiService: Fallback URL also failed: ${fallbackResponse.statusCode()}")
                    }
                }
                
                // If we get here, both main and fallback URLs failed
                return@withContext Topic(
                    title = query,
                    url = "$BASE_URL$searchUrl",
                    commentCount = 0,
                    entries = listOf(Entry("HTTP error: ${response.statusCode()} - ${response.statusMessage()}")),
                    entriesLoaded = true,
                    redirectedUrl = searchUrl,
                    totalPages = 1
                )
            }
            
            // Process the successful response
            return@withContext processTopicResponse(response, query, page, searchUrl, BASE_URL)
            
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in searchTopic: ${e.message}")
            
            // Return an error topic
            return@withContext Topic(
                title = "Error searching for: $query",
                url = "",
                commentCount = 0,
                entries = listOf(Entry("Error: ${e.message ?: "Unknown error"}")),
                entriesLoaded = true,
                totalPages = 1
            )
        }
    }
    
    // Helper method to process a topic response
    private suspend fun processTopicResponse(
        response: org.jsoup.Connection.Response,
        query: String,
        page: Int,
        searchUrl: String,
        baseUrl: String
    ): Topic = withContext(Dispatchers.IO) {
        try {
            // Get the final URL after redirection
            val finalUrl = response.url().toString()
            logDebug("EksiService: Final URL after redirection: $finalUrl")
            
            // Extract the redirected path from the final URL (including query parameters)
            val redirectedPath = if (finalUrl.startsWith(baseUrl)) {
                val fullPath = finalUrl.substring(baseUrl.length)
                // Keep the full path including query parameters
                fullPath
            } else {
                searchUrl // Fallback to the original search URL
            }
            
            // Parse the document to get entries
            val document = response.parse()
            
            // Try to get the actual topic title from the page
            val pageTitle = document.select("h1.topic-title, h1.başlık, h1, title").firstOrNull()?.text() ?: query
            val cleanTitle = pageTitle.replace(" - ekşi sözlük", "").trim()
            
            // Extract entries from the document
            val entryElements = document.select("div.content, div.entry-content, div.entry")
            logDebug("EksiService: Found ${entryElements.size} entries on page $page")
            
            // Try to determine the total number of pages
            var totalPages = 1
            
            // Method 1: Look for pager element with page numbers
            val pagerElements = document.select("div.pager, div.paginator, div.sub-title-container")
            if (pagerElements.isNotEmpty()) {
                logDebug("EksiService: Found pager element")
                
                // Try to find the last page number from page links
                val pageLinks = pagerElements.select("a")
                for (link in pageLinks) {
                    val pageText = link.text().trim()
                    if (pageText.matches(Regex("\\d+"))) {
                        val pageNum = pageText.toIntOrNull() ?: 1
                        if (pageNum > totalPages) {
                            totalPages = pageNum
                        }
                    }
                }
                
                // Also check for "son" (last) link which might point to the last page
                val lastPageLink = pagerElements.select("a:contains(son)").firstOrNull()
                if (lastPageLink != null) {
                    val href = lastPageLink.attr("href")
                    val lastPageMatch = Regex("p=(\\d+)").find(href)
                    if (lastPageMatch != null) {
                        val lastPage = lastPageMatch.groupValues[1].toIntOrNull() ?: 1
                        if (lastPage > totalPages) {
                            totalPages = lastPage
                        }
                    }
                }
                
                logDebug("EksiService: Found $totalPages pages from pager links")
            }
            
            // Method 2: Look for entry count info
            if (totalPages == 1) {
                // Try to find entry count from topic info
                val infoElements = document.select("div.topic-list-description small, span.entry-count, div.sub-title-container span")
                for (infoElement in infoElements) {
                    val infoText = infoElement.text()
                    // Look for patterns like "123 entry" or "123 entries"
                    val entryCountMatch = Regex("(\\d+)[\\s]*(?:entry|entries|entry'ler)").find(infoText)
                    if (entryCountMatch != null) {
                        val entryCount = entryCountMatch.groupValues[1].toIntOrNull() ?: 0
                        // Calculate pages (10 entries per page)
                        val calculatedPages = (entryCount + 9) / 10 // Ceiling division
                        if (calculatedPages > totalPages) {
                            totalPages = calculatedPages
                        }
                        logDebug("EksiService: Found entry count: $entryCount, calculated pages: $totalPages")
                        break
                    }
                }
            }
            
            // Method 3: Check if there's a "next page" link
            if (totalPages == 1 && page == 1) {
                val nextPageLink = document.select("a.next, a:contains(sonraki)").firstOrNull()
                if (nextPageLink != null) {
                    // If there's a next page link on page 1, there are at least 2 pages
                    totalPages = 2
                    logDebug("EksiService: Found next page link, setting minimum of 2 pages")
                }
            }
            
            // Ensure we have at least one page
            if (totalPages < 1) totalPages = 1
            
            // If we're on a page higher than 1 and we got entries, ensure totalPages is at least the current page
            if (page > 1 && entryElements.isNotEmpty() && totalPages < page) {
                totalPages = page
                logDebug("EksiService: Adjusting totalPages to at least current page: $page")
            }
            
            // If we couldn't detect more than 1 page but we have entries,
            // set a reasonable default to allow navigation (Ekşi Sözlük topics can have many pages)
            if (totalPages == 1 && entryElements.isNotEmpty() && entryElements.size >= 10) {
                totalPages = 100 // Default to 100 pages when we can't detect the actual count
                logDebug("EksiService: Setting default of 100 pages since detection failed")
            }
            
            logDebug("EksiService: Final estimated total pages: $totalPages")
            
            // Process entries
            val entries = mutableListOf<Entry>()
            for (entryElement in entryElements.take(10)) {
                val content = entryElement.text()
                if (content.isNotEmpty()) {
                    // Try to extract author and date if available
                    val authorElement = entryElement.parent()?.select("a.entry-author, a.author")?.firstOrNull()
                    val author = authorElement?.text() ?: ""
                    
                    val dateElement = entryElement.parent()?.select("a.entry-date, span.date")?.firstOrNull()
                    val date = dateElement?.text() ?: ""
                    
                    entries.add(Entry(content, author, date))
                }
            }
            
            if (entries.isEmpty()) {
                entries.add(Entry("No entries found for this topic on page $page"))
                // If no entries found and we're beyond page 1, adjust totalPages
                if (page > 1) {
                    totalPages = page - 1
                    logDebug("EksiService: No entries found on page $page, adjusting totalPages to $totalPages")
                }
            }
            
            // Create a topic with the search query and fetched entries
            val topic = Topic(
                title = cleanTitle,
                url = "$baseUrl$searchUrl",
                commentCount = entries.size,
                entries = entries,
                entriesLoaded = true,
                redirectedUrl = redirectedPath,
                totalPages = totalPages
            )
            
            logDebug("EksiService: Search completed for '$query', page $page, found ${entries.size} entries, totalPages: $totalPages")
            return@withContext topic
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in processTopicResponse: ${e.message}")
            
            // Return an error topic
            return@withContext Topic(
                title = query,
                url = "$baseUrl$searchUrl",
                commentCount = 0,
                entries = listOf(Entry("Error processing response: ${e.message ?: "Unknown error"}")),
                entriesLoaded = true,
                redirectedUrl = searchUrl,
                totalPages = 1
            )
        }
    }
}

// ======== VIEW MODEL ========
class EksiViewModel : ViewModel() {
    private val _topics = mutableStateOf<List<Topic>>(emptyList())
    val topics: State<List<Topic>> = _topics
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> get() = _error
    
    private val _isUsingLocalData = mutableStateOf(false)
    val isUsingLocalData: State<Boolean> get() = _isUsingLocalData
    
    // Add a new state for the currently selected topic
    private val _selectedTopic = mutableStateOf<Topic?>(null)
    val selectedTopic: State<Topic?> = _selectedTopic
    
    // Add a loading state specifically for the selected topic
    private val _isLoadingTopic = mutableStateOf(false)
    val isLoadingTopic: State<Boolean> = _isLoadingTopic

    // Add state for search results
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    private val _searchResult = mutableStateOf<Topic?>(null)
    val searchResult: State<Topic?> = _searchResult
    
    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage
    
    private val _isPageDialogVisible = mutableStateOf(false)
    val isPageDialogVisible: State<Boolean> = _isPageDialogVisible
    
    private val _selectedPage = mutableStateOf(1)
    val selectedPage: State<Int> = _selectedPage

    private val _redirectedUrl = mutableStateOf("")
    val redirectedUrl: State<String> = _redirectedUrl

    private val _totalPages = mutableStateOf(1)
    val totalPages: State<Int> = _totalPages

    // Add state for topic pagination
    private val _topicCurrentPage = mutableStateOf(1)
    val topicCurrentPage: State<Int> = _topicCurrentPage
    
    private val _topicTotalPages = mutableStateOf(1)
    val topicTotalPages: State<Int> = _topicTotalPages

    init {
        fetchTopics()
    }

    fun loadLocalData() {
        _topics.value = LocalDataSource.getLocalData().home
        _isUsingLocalData.value = true
        _isLoading.value = false
        _error.value = null
    }
    
    fun fetchTopics() {
        _isLoading.value = true
        _error.value = null
        _isUsingLocalData.value = false
        
        viewModelScope.launch {
            try {
                // Add a timeout mechanism
                val timeoutJob = viewModelScope.launch {
                    delay(30000) // 30 seconds timeout
                    if (_isLoading.value) {
                        _isLoading.value = false
                        _error.value = "Request timed out. Loading local data instead."
                        loadLocalData()
                    }
                }
                
                println("EksiViewModel: Starting to fetch topics")
                val result = EksiService.getPopularTopics()
                
                // Cancel the timeout job since we got a response
                timeoutJob.cancel()
                
                if (result.isEmpty()) {
                    println("EksiViewModel: No topics found, loading local data")
                    _error.value = "No topics found. Please try again later."
                    loadLocalData()
                } else if (result.size == 1 && result[0].title.startsWith("Error fetching data")) {
                    println("EksiViewModel: Error in fetched data, loading local data")
                    _error.value = result[0].title
                    loadLocalData()
                } else {
                    println("EksiViewModel: Successfully fetched ${result.size} topics")
                    _topics.value = result
                    _isUsingLocalData.value = false
                }
                _isLoading.value = false
            } catch (e: Exception) {
                println("EksiViewModel: Error fetching topics: ${e.message}")
                _error.value = "Failed to load data: ${e.message ?: "Unknown error"}"
                _isLoading.value = false
                // Fallback to local data if network request fails
                loadLocalData()
            }
        }
    }
    
    // Add a method to set the selected topic directly from the list with pagination
    fun selectTopic(index: Int, page: Int = 1) {
        println("EksiViewModel: selectTopic called with index $index, page $page, topics size: ${_topics.value.size}")
        if (index >= 0 && index < _topics.value.size) {
            _selectedTopic.value = _topics.value[index]
            _topicCurrentPage.value = page
            println("EksiViewModel: Selected topic set to: ${_selectedTopic.value?.title}, page: $page")
            
            // Fetch entries if they haven't been loaded yet or if we're changing pages
            if (_selectedTopic.value != null) {
                fetchEntriesForSelectedTopic(page)
            }
        } else {
            println("EksiViewModel: Invalid index $index for topics size ${_topics.value.size}")
        }
    }
    
    // Update to support pagination
    private fun fetchEntriesForSelectedTopic(page: Int = 1) {
        val topic = _selectedTopic.value ?: return
        
        _isLoadingTopic.value = true
        _topicCurrentPage.value = page
        
        viewModelScope.launch {
            try {
                println("EksiViewModel: Fetching entries for topic: ${topic.title}, page: $page")
                
                // Use the topic's original URL for fetching entries
                if (topic.url.isNotEmpty()) {
                    // Extract base URL and path from the topic's URL
                    val baseUrl = if (topic.url.startsWith("http")) {
                        val uri = java.net.URI(topic.url)
                        "${uri.scheme}://${uri.host}"
                    } else {
                        "https://eksisozluk.com"
                    }
                    
                    // Extract the path from the full URL or use the URL directly if it's just a path
                    val path = if (topic.url.startsWith("http")) {
                        java.net.URI(topic.url).path + java.net.URI(topic.url).query?.let { "?$it" } ?: ""
                    } else {
                        topic.url
                    }
                    
                    // Preserve the original URL parameters (like ?a=popular)
                    val originalPath = path
                    
                    // Modify the path to include page parameter if needed
                    val pageParam = if (page > 1) {
                        if (originalPath.contains("?")) {
                            "&p=$page"
                        } else {
                            "?p=$page"
                        }
                    } else ""
                    
                    // Construct the final path with all parameters
                    val pathWithPage = if (originalPath.contains("p=")) {
                        // Replace existing page parameter
                        originalPath.replaceFirst(Regex("p=\\d+"), "p=$page")
                    } else {
                        originalPath + pageParam
                    }
                    
                    println("EksiViewModel: Using URL: $baseUrl$pathWithPage")
                    
                    // Use the searchTopic method with the exact original path
                    val searchResult = EksiService.searchTopic(topic.title, page, originalPath)
                    
                    // Update total pages
                    _topicTotalPages.value = searchResult.totalPages
                    
                    // Create a new topic with the loaded entries
                    val updatedTopic = topic.copy(
                        entries = searchResult.entries, 
                        entriesLoaded = true,
                        totalPages = searchResult.totalPages,
                        redirectedUrl = searchResult.redirectedUrl.ifEmpty { originalPath }
                    )
                    
                    // Update the selected topic
                    _selectedTopic.value = updatedTopic
                    
                    // Also update the topic in the list
                    val updatedTopics = _topics.value.toMutableList()
                    val index = updatedTopics.indexOfFirst { it.title == topic.title }
                    if (index != -1) {
                        updatedTopics[index] = updatedTopic
                        _topics.value = updatedTopics
                    }
                    
                    println("EksiViewModel: Successfully fetched ${searchResult.entries.size} entries for topic: ${topic.title}, page: $page, totalPages: ${searchResult.totalPages}")
                } else {
                    // Fallback to search by title if URL is empty
                    println("EksiViewModel: No URL available for topic, falling back to search by title")
                    val searchResult = EksiService.searchTopic(topic.title, page)
                    
                    // Update total pages
                    _topicTotalPages.value = searchResult.totalPages
                    
                    // Create a new topic with the loaded entries
                    val updatedTopic = topic.copy(
                        entries = searchResult.entries, 
                        entriesLoaded = true,
                        totalPages = searchResult.totalPages,
                        redirectedUrl = searchResult.redirectedUrl
                    )
                    
                    // Update the selected topic
                    _selectedTopic.value = updatedTopic
                    
                    // Also update the topic in the list
                    val updatedTopics = _topics.value.toMutableList()
                    val index = updatedTopics.indexOfFirst { it.title == topic.title }
                    if (index != -1) {
                        updatedTopics[index] = updatedTopic
                        _topics.value = updatedTopics
                    }
                    
                    println("EksiViewModel: Successfully fetched ${searchResult.entries.size} entries for topic: ${topic.title}, page: $page, totalPages: ${searchResult.totalPages}")
                }
            } catch (e: Exception) {
                println("EksiViewModel: Error fetching entries: ${e.message}")
                
                // Update the selected topic with error message
                val errorEntries = listOf(Entry("Error loading entries: ${e.message ?: "Unknown error"}"))
                val updatedTopic = topic.copy(
                    entries = errorEntries,
                    entriesLoaded = true
                )
                _selectedTopic.value = updatedTopic
            } finally {
                _isLoadingTopic.value = false
            }
        }
    }
    
    // Add a method to fetch a single topic by its URL (for future use if needed)
    fun fetchSingleTopic(topicTitle: String, topicUrl: String) {
        _isLoadingTopic.value = true
        
        viewModelScope.launch {
            try {
                // First check if we already have this topic in our list
                val existingTopic = _topics.value.find { it.title == topicTitle }
                if (existingTopic != null) {
                    _selectedTopic.value = existingTopic
                    _isLoadingTopic.value = false
                    return@launch
                }
                
                // If not found in our list, fetch it individually
                val baseUrl = "https://eksisozluk.com"
                val entries = EksiService.getEntriesForTopic(baseUrl, topicUrl)
                _selectedTopic.value = Topic(
                    title = topicTitle,
                    url = topicUrl,
                    commentCount = 0,
                    entries = entries,
                    entriesLoaded = true
                )
                _isLoadingTopic.value = false
            } catch (e: Exception) {
                _selectedTopic.value = Topic(
                    "Error loading topic: ${e.message}",
                    "",
                    0,
                    listOf(Entry("Please check your internet connection and try again.")),
                    true
                )
                _isLoadingTopic.value = false
            }
        }
    }
    
    // Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // Perform search
    fun search(page: Int = 1) {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            return
        }
        
        _isSearching.value = true
        _currentPage.value = page
        
        viewModelScope.launch {
            try {
                println("EksiViewModel: Searching for: $query, page: $page, redirectedUrl: ${_redirectedUrl.value}")
                
                // Always use the stored redirected URL if available, regardless of the page
                val result = if (_redirectedUrl.value.isNotEmpty()) {
                    // We already have a redirected URL, so use it with the new page number
                    println("EksiViewModel: Using existing redirectedUrl: ${_redirectedUrl.value} for page $page")
                    EksiService.searchTopic(query, page, _redirectedUrl.value)
                } else {
                    // First search, need to discover the redirected URL
                    println("EksiViewModel: First search, no redirectedUrl yet")
                    val initialResult = EksiService.searchTopic(query, 1, "")
                    
                    if (initialResult.redirectedUrl.isNotEmpty()) {
                        _redirectedUrl.value = initialResult.redirectedUrl
                        println("EksiViewModel: Discovered redirectedUrl: ${_redirectedUrl.value}")
                        
                        // If the requested page is not 1, fetch that page using the discovered redirected URL
                        if (page > 1) {
                            println("EksiViewModel: Fetching requested page $page using discovered redirectedUrl")
                            EksiService.searchTopic(query, page, _redirectedUrl.value)
                        } else {
                            initialResult
                        }
                    } else {
                        initialResult
                    }
                }
                
                _searchResult.value = result
                
                // Update the total pages
                _totalPages.value = result.totalPages
            } catch (e: Exception) {
                println("EksiViewModel: Error searching: ${e.message}")
                _searchResult.value = Topic(
                    title = "Error searching for: $query",
                    url = "",
                    commentCount = 0,
                    entries = listOf(Entry("Error: ${e.message ?: "Unknown error"}")),
                    entriesLoaded = true
                )
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    // Clear search results and reset state
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResult.value = null
        _redirectedUrl.value = "" // Clear the redirected URL when clearing the search
        _currentPage.value = 1 // Reset to page 1
    }
    
    // Show page selection dialog
    fun showPageDialog() {
        _selectedPage.value = _currentPage.value
        _isPageDialogVisible.value = true
    }
    
    // Hide page selection dialog
    fun hidePageDialog() {
        _isPageDialogVisible.value = false
    }
    
    // Update selected page without upper limit validation
    fun updateSelectedPage(page: Int) {
        if (page >= 1) { // Only validate that page is positive
            _selectedPage.value = page
        }
    }
    
    // Apply selected page and load entries
    fun applySelectedPage() {
        val page = _selectedPage.value
        if (page != _currentPage.value) {
            search(page)
        }
        hidePageDialog()
    }

    // Add method to navigate to a specific page for a topic
    fun navigateTopicToPage(page: Int) {
        if (page >= 1 && _selectedTopic.value != null) {
            val topicIndex = _topics.value.indexOfFirst { it.title == _selectedTopic.value?.title }
            if (topicIndex != -1) {
                selectTopic(topicIndex, page)
            }
        }
    }
}

// ======== MAIN ACTIVITY ========
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Storage permissions denied, cannot save debug logs", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        requestStoragePermissions()
        
        setContent {
            EksiScraperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
    
    private fun requestStoragePermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 and below
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        requestPermissionLauncher.launch(permissions)
    }
    
    companion object {
        // Function to save debug log to a file
        fun saveDebugLogToFile(context: android.content.Context) {
            try {
                val debugLog = EksiService.getDebugLog()
                if (debugLog.isBlank()) {
                    Toast.makeText(context, "No debug data to save", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Create a timestamp for the filename
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "eksi_debug_$timestamp.txt"
                
                // Get the Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                
                // Write the debug log to the file
                file.writeText(debugLog)
                
                // Also save the fetched topics as JSON
                val topicsJson = EksiService.getTopicsAsJson()
                val jsonFilename = "eksi_topics_$timestamp.json"
                val jsonFile = File(downloadsDir, jsonFilename)
                jsonFile.writeText(topicsJson)
                
                Toast.makeText(
                    context, 
                    "Debug log saved to Downloads/$filename\nTopics saved to Downloads/$jsonFilename", 
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context, 
                    "Failed to save debug log: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

// ======== NAVIGATION ========
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Profile : Screen("profile", "You", Icons.Default.Person)
    object TopicDetail : Screen("topic_detail/{topicIndex}", "Topic Detail", Icons.Default.Info) {
        fun createRoute(topicIndex: Int): String = "topic_detail/$topicIndex"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: EksiViewModel = viewModel()) {
    val navController = rememberNavController()
    
    println("MainApp: Composing MainApp")
    
    // Fetch topics when the app starts
    LaunchedEffect(Unit) {
        println("MainApp: LaunchedEffect triggered, fetching topics")
        viewModel.fetchTopics()
    }

    // Track current route for highlighting the correct nav item
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.wrapContentWidth(),
                    shape = RoundedCornerShape(50.dp), // Keep the pill-like shape
                    colors = CardDefaults.cardColors(
                        // Use default Material You color instead of custom color with alpha
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home button - with selection indicator
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Selection indicator
                            if (currentRoute == Screen.Home.route) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {}
                            }
                            
                            IconButton(
                                onClick = {
                                    if (currentRoute != Screen.Home.route) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Screen.Home.icon,
                                    contentDescription = Screen.Home.title,
                                    tint = if (currentRoute == Screen.Home.route) 
                                        MaterialTheme.colorScheme.onSecondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Search button - with selection indicator
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Selection indicator
                            if (currentRoute == Screen.Search.route) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {}
                            }
                            
                            IconButton(
                                onClick = {
                                    if (currentRoute != Screen.Search.route) {
                                        navController.navigate(Screen.Search.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else {
                                        // If already on Search screen and tapped again, clear the search
                                        viewModel.clearSearch()
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Screen.Search.icon,
                                    contentDescription = Screen.Search.title,
                                    tint = if (currentRoute == Screen.Search.route) 
                                        MaterialTheme.colorScheme.onSecondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Profile button - with selection indicator
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Selection indicator
                            if (currentRoute == Screen.Profile.route) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {}
                            }
                            
                            IconButton(
                                onClick = {
                                    if (currentRoute != Screen.Profile.route) {
                                        navController.navigate(Screen.Profile.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Screen.Profile.icon,
                                    contentDescription = Screen.Profile.title,
                                    tint = if (currentRoute == Screen.Profile.route) 
                                        MaterialTheme.colorScheme.onSecondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                println("MainApp: Navigating to HomeScreen")
                HomeScreen(viewModel = viewModel, navController = navController)
            }
            
            composable(
                route = Screen.TopicDetail.route,
                arguments = listOf(
                    navArgument("topicIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val topicIndex = backStackEntry.arguments?.getInt("topicIndex") ?: 0
                println("MainApp: Navigating to TopicDetailScreen with topicIndex=$topicIndex")
                TopicDetailScreen(
                    topicIndex = topicIndex,
                    navController = navController,
                    viewModel = viewModel
                )
            }
            
            composable(Screen.Search.route) {
                println("MainApp: Navigating to SearchScreen")
                SearchScreen(viewModel = viewModel)
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

// ======== SCREENS ========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EksiViewModel = viewModel(),
    navController: NavHostController
) {
    val topics by viewModel.topics
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val isUsingLocalData by viewModel.isUsingLocalData
    
    val context = LocalContext.current
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    Box(modifier = Modifier.fillMaxSize()) {
        if (error != null && topics.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.fetchTopics() }) {
                    Text("Retry")
                }
            }
        } else {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.fetchTopics() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Status bar and action buttons - now at the very top of the app
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUsingLocalData) "Using Local Data" else "Live Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isUsingLocalData) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                
                                Row {
                                    // Save Debug Log button
                                    Button(
                                        onClick = { MainActivity.saveDebugLogToFile(context) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("Save Logs")
                                    }
                                    
                                    if (!isUsingLocalData) {
                                        Button(
                                            onClick = { viewModel.loadLocalData() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Text("Use Local Data")
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.fetchTopics() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Try Live Data")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                    
                    if (isUsingLocalData) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Using local data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Could not connect to eksisozluk.com. Showing cached data instead.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.fetchTopics() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
                        itemsIndexed(topics) { index, topic ->
                            TopicListItem(
                                topic = topic,
                                onClick = { 
                                    println("HomeScreen: Topic clicked: ${topic.title}, index: $index")
                                    // Set the selected topic before navigating
                                    viewModel.selectTopic(index)
                                    println("HomeScreen: After selectTopic, navigating to detail screen")
                                    navController.navigate(Screen.TopicDetail.createRoute(index)) 
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (isLoading && topics.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: EksiViewModel = viewModel()) {
    val searchQuery by viewModel.searchQuery
    val searchResult by viewModel.searchResult
    val isSearching by viewModel.isSearching
    val currentPage by viewModel.currentPage
    val isPageDialogVisible by viewModel.isPageDialogVisible
    val selectedPage by viewModel.selectedPage
    val context = LocalContext.current
    
    // Get total pages from the search result, with a minimum of the current page
    val totalPages = (searchResult?.totalPages ?: 1).coerceAtLeast(currentPage)
    
    // Page selection dialog
    if (isPageDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hidePageDialog() },
            title = { Text("Select Page") },
            text = {
                Column {
                    Text("Current page: $currentPage")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Page input field
                    var pageInput by remember { mutableStateOf(currentPage.toString()) }
                    
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { 
                            // Only allow numeric input
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                pageInput = it
                                // Update selected page if valid
                                it.toIntOrNull()?.let { num ->
                                    if (num >= 1) { // Allow any page number >= 1
                                        viewModel.updateSelectedPage(num)
                                    }
                                }
                            }
                        },
                        label = { Text("Page number") },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick navigation buttons - only First, Prev, Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { 
                                viewModel.updateSelectedPage(1)
                                pageInput = "1"
                            },
                            enabled = currentPage != 1
                        ) {
                            Text("First")
                        }
                        
                        Button(
                            onClick = { 
                                val prev = (selectedPage - 1).coerceAtLeast(1)
                                viewModel.updateSelectedPage(prev)
                                pageInput = prev.toString()
                            },
                            enabled = selectedPage > 1
                        ) {
                            Text("Prev")
                        }
                        
                        Button(
                            onClick = { 
                                val next = selectedPage + 1
                                viewModel.updateSelectedPage(next)
                                pageInput = next.toString()
                            }
                        ) {
                            Text("Next")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.applySelectedPage() },
                    enabled = selectedPage != currentPage && selectedPage >= 1
                ) {
                    Text("Go to Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hidePageDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Only show search container if no results are displayed or if we're searching
        if (searchResult == null || isSearching) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Enter search term") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { viewModel.search() }
                            )
                        )
                        
                        Button(
                            onClick = { viewModel.search() },
                            enabled = searchQuery.isNotEmpty() && !isSearching
                        ) {
                            Text("Search")
                        }
                    }
                }
            }
        }
        
        // Search results or loading indicator
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
        contentAlignment = Alignment.Center
    ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Searching for \"$searchQuery\"...")
                }
            }
        } else if (searchResult != null) {
            // Display search results
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Topic title with page selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = searchResult?.title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Page selector - simplified display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Previous page button - just icon
                            IconButton(
                                onClick = { 
                                    if (currentPage > 1) {
                                        viewModel.search(currentPage - 1)
                                    }
                                },
                                enabled = currentPage > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Previous Page"
                                )
                            }
                            
                            // Page info and change page button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Page $currentPage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                
                                // Page button that opens the dialog
                                OutlinedButton(
                                    onClick = { viewModel.showPageDialog() },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text("Change Page")
                                }
                            }
                            
                            // Next page button - just icon
                            IconButton(
                                onClick = { viewModel.search(currentPage + 1) }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Page"
                                )
                            }
                        }
                    }
                }
                
                // Entries - Make sure searchResult is not null before accessing its properties
                searchResult?.let { result ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        if (result.entries.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = "No entries found for \"${result.title}\" on page $currentPage",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(result.entries) { index, entry ->
                                EntryItem(
                                    entry = entry, 
                                    index = ((currentPage - 1) * 10) + index + 1
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state - no search performed yet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enter a search term and press Search",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Profile Screen (Coming Soon)")
    }
}

// ======== COMPONENTS ========
@Composable
fun TopicListItem(topic: Topic, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    topicIndex: Int,
    navController: NavHostController,
    viewModel: EksiViewModel = viewModel()
) {
    val topics by viewModel.topics
    val selectedTopic by viewModel.selectedTopic
    val isLoadingTopic by viewModel.isLoadingTopic
    val isLoading by viewModel.isLoading
    val currentPage by viewModel.topicCurrentPage
    val totalPages by viewModel.topicTotalPages
    val isPageDialogVisible by viewModel.isPageDialogVisible
    val selectedPage by viewModel.selectedPage
    
    println("TopicDetailScreen: Composed with topicIndex=$topicIndex, selectedTopic=${selectedTopic?.title}, topics.size=${topics.size}")
    
    // When this screen is first composed, ensure the selected topic is set
    LaunchedEffect(topicIndex) {
        println("TopicDetailScreen: LaunchedEffect triggered with topicIndex=$topicIndex")
        viewModel.selectTopic(topicIndex)
    }
    
    // Page selection dialog
    if (isPageDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hidePageDialog() },
            title = { Text("Select Page") },
            text = {
                Column {
                    Text("Current page: $currentPage")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Page input field
                    var pageInput by remember { mutableStateOf(currentPage.toString()) }
                    
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { 
                            // Only allow numeric input
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                pageInput = it
                                // Update selected page if valid
                                it.toIntOrNull()?.let { num ->
                                    if (num >= 1) { // Allow any page number >= 1
                                        viewModel.updateSelectedPage(num)
                                    }
                                }
                            }
                        },
                        label = { Text("Page number") },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick navigation buttons - only First, Prev, Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { 
                                viewModel.updateSelectedPage(1)
                                pageInput = "1"
                            },
                            enabled = currentPage != 1
                        ) {
                            Text("First")
                        }
                        
                        Button(
                            onClick = { 
                                val prev = (selectedPage - 1).coerceAtLeast(1)
                                viewModel.updateSelectedPage(prev)
                                pageInput = prev.toString()
                            },
                            enabled = selectedPage > 1
                        ) {
                            Text("Prev")
                        }
                        
                        Button(
                            onClick = { 
                                val next = selectedPage + 1
                                viewModel.updateSelectedPage(next)
                                pageInput = next.toString()
                            }
                        ) {
                            Text("Next")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.hidePageDialog()
                        viewModel.navigateTopicToPage(selectedPage)
                    },
                    enabled = selectedPage != currentPage && selectedPage >= 1
                ) {
                    Text("Go to Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hidePageDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Topic Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Show loading indicator if we're still loading topics or the selected topic
            if (isLoading || isLoadingTopic || (selectedTopic == null && topics.isEmpty())) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (isLoadingTopic) "Loading entries..." else "Loading topics...")
                    }
                }
                return@Column
            }
            
            // Get the topic either from selectedTopic or directly from the topics list
            val topic = selectedTopic ?: if (topicIndex >= 0 && topicIndex < topics.size) {
                topics[topicIndex]
            } else {
                // If we can't get the topic, show an error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: Topic not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                return@Column
            }
            
            println("TopicDetailScreen: Displaying topic: ${topic.title}")
            
            // Topic title with page selector - matching the SearchScreen UI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Page selector - simplified display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous page button - just icon
                        IconButton(
                            onClick = { 
                                if (currentPage > 1) {
                                    viewModel.navigateTopicToPage(currentPage - 1)
                                }
                            },
                            enabled = currentPage > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous Page"
                            )
                        }
                        
                        // Page info and change page button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Page $currentPage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            // Page button that opens the dialog
                            OutlinedButton(
                                onClick = { viewModel.showPageDialog() },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text("Change Page")
                            }
                        }
                        
                        // Next page button - just icon
                        IconButton(
                            onClick = { viewModel.navigateTopicToPage(currentPage + 1) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Page"
                            )
                        }
                    }
                }
            }
            
            // Entries
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                if (topic.entries.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "No entries found for \"${topic.title}\" on page $currentPage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(topic.entries) { index, entry ->
                        EntryItem(
                            entry = entry, 
                            index = ((currentPage - 1) * 10) + index + 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntryItem(entry: Entry, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EksiScraperTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
