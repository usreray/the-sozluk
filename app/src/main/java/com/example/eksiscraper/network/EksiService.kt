package com.example.eksiscraper.network

import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.HashMap

object EksiService {
    private const val BASE_URL = "https://eksisozluk.com"
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
    
    // Add a function to log the HTML content
    private fun logHtmlContent(html: String) {
        val truncatedHtml = if (html.length > 5000) {
            html.substring(0, 5000) + "... [truncated]"
        } else {
            html
        }
        logDebug("EksiService: HTML Content:\n$truncatedHtml")
    }
    
    // Add a function to log the parsed topics
    private fun logTopics(topics: List<Topic>, page: Int) {
        logDebug("EksiService: ===== FETCHED TOPICS FOR PAGE $page =====")
        topics.forEachIndexed { index, topic ->
            logDebug("EksiService: Topic ${index + 1}:")
            logDebug("EksiService:   Title: ${topic.title}")
            logDebug("EksiService:   URL: ${topic.url}")
            logDebug("EksiService:   Comment Count: ${topic.commentCount}")
            logDebug("EksiService:   Entries Loaded: ${topic.entriesLoaded}")
            if (topic.entriesLoaded && topic.entries.isNotEmpty()) {
                logDebug("EksiService:   First Entry: ${topic.entries.first().content.take(100)}${if (topic.entries.first().content.length > 100) "..." else ""}")
            }
        }
        logDebug("EksiService: ===== END OF TOPICS FOR PAGE $page =====")
    }
    
    // Add a function to log the entries
    private fun logEntries(topic: Topic, entries: List<Entry>) {
        logDebug("EksiService: ===== FETCHED ENTRIES FOR TOPIC: ${topic.title} =====")
        logDebug("EksiService: Total entries: ${entries.size}")
        entries.take(5).forEachIndexed { index, entry ->
            logDebug("EksiService: Entry ${index + 1}:")
            logDebug("EksiService:   Content: ${entry.content.take(100)}${if (entry.content.length > 100) "..." else ""}")
            logDebug("EksiService:   Author: ${entry.author}")
            logDebug("EksiService:   Date: ${entry.date}")
        }
        if (entries.size > 5) {
            logDebug("EksiService: ... and ${entries.size - 5} more entries")
        }
        logDebug("EksiService: ===== END OF ENTRIES FOR TOPIC: ${topic.title} =====")
    }
    
    suspend fun getPopularTopics(page: Int = 1): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Clear previous debug log if it's the first page
            if (page == 1) {
                debugLog.clear()
            }
            
            logDebug("EksiService: Starting to fetch topics for page $page")
            // Try with the main URL first
            val result = tryFetchTopicsWithoutEntries(BASE_URL, page)
            
            logDebug("EksiService: Result for page $page has ${result.size} topics")
            if (result.isNotEmpty()) {
                logDebug("EksiService: First topic title for page $page: ${result.first().title}")
                if (result.size > 1) {
                    logDebug("EksiService: Last topic title for page $page: ${result.last().title}")
                }
            }
            
            logDebug("EksiService: Returning ${result.size} topics for page $page")
            if (page == 1) {
                lastFetchedTopics = result
            }
            
            // Log the parsed topics
            logTopics(result, page)
            
            return@withContext result
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in getPopularTopics for page $page: ${e.message}")
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
            if (page == 1) {
                lastFetchedTopics = errorResult
            }
            
            // Log the parsed topics
            logTopics(errorResult, page)
            
            return@withContext errorResult
        }
    }
    
    private suspend fun tryFetchTopicsWithoutEntries(baseUrl: String, page: Int = 1): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Add shorter random delay to mimic human behavior but not cause too much waiting
            logDebug("EksiService: Adding short delay before request for page $page")
            kotlinx.coroutines.delay((500..1000).random().toLong())
            
            val cookies = HashMap<String, String>()
            cookies["__cf_bm"] = "" // CloudFlare bypass attempt
            cookies["_ga"] = ""
            cookies["_gid"] = ""
            
            // Construct URL with page parameter if needed
            val url = if (page > 1) {
                "$baseUrl/basliklar/gundem?p=$page"  // Use the "gundem" (agenda/popular) section with pagination
            } else {
                "$baseUrl/basliklar/gundem"  // Use the "gundem" section for first page
            }
            
            logDebug("EksiService: Setting up connection to $url for page $page")
            val connection = Jsoup.connect(url)
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
                return@withContext tryMobileVersionWithoutEntries(baseUrl, page)
            }
            
            logDebug("EksiService: Parsing document")
            val document = response.parse()
            
            // Log the HTML content for debugging
            logHtmlContent(document.html())
            
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
            
            // Check if we found any topics at all
            if (topicElements.isEmpty()) {
                logDebug("EksiService: No topics found with any selector for page $page")
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
            
            // Process all topics returned by the server
            // The server should already handle pagination based on the URL
            logDebug("EksiService: Found ${topicElements.size} topics for page $page")
            
            // Process all topics on the page (up to 50)
            // No need to calculate startIndex and endIndex as we'll use all topics from the page
            val topics = mutableListOf<Topic>()
            
            for (topicElement in topicElements) {
                val title = topicElement.text()
                val href = topicElement.attr("href")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
                    logDebug("EksiService: Processing topic: $title, href: $href")
                    // Don't fetch entries yet, just store the topic info
                    val commentCountText = topicElement.parent()?.select("small")?.text()?.trim() ?: ""
                    val commentCount = commentCountText.toIntOrNull() ?: 0
                    topics.add(Topic(
                        title = title,
                        url = href,
                        commentCount = commentCount,
                        entries = emptyList(),
                        entriesLoaded = false
                    ))
                }
            }
            
            // If we found fewer than 50 topics, we've reached the last page
            val isLastPage = topics.size < 50
            logDebug("EksiService: Found ${topics.size} topics for page $page, isLastPage: $isLastPage")
            
            if (topics.isEmpty()) {
                logDebug("EksiService: No topics found")
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
            
            logDebug("EksiService: Successfully fetched ${topics.size} topics")
            
            // Log the parsed topics
            logTopics(topics, page)
            
            return@withContext topics
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in tryFetchTopicsWithoutEntries: ${e.message}")
            return@withContext listOf(
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
    
    private suspend fun tryMobileVersionWithoutEntries(baseUrl: String, page: Int = 1): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Try the mobile version
            val mobileUrl = if (page > 1) {
                "$baseUrl/mobil/basliklar/gundem?p=$page"
            } else {
                "$baseUrl/mobil/basliklar/gundem"
            }
            logDebug("EksiService: Trying mobile version at $mobileUrl for page $page")
            
            val connection = Jsoup.connect(mobileUrl)
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                .timeout(10000) // Reduced timeout to 10 seconds
                .referrer("https://www.google.com/search?q=eksisozluk+mobil")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
            
            logDebug("EksiService: Executing mobile connection")
            val response = connection.execute()
            
            logDebug("EksiService: Mobile response status code: ${response.statusCode()}")
            if (response.statusCode() != 200) {
                return@withContext listOf(
                    Topic(
                        title = "Error fetching data: HTTP error ${response.statusCode()}",
                        url = "",
                        commentCount = 0,
                        entries = listOf(Entry("Please check your internet connection and try again.")),
                        entriesLoaded = true
                    )
                )
            }
            
            logDebug("EksiService: Parsing mobile document")
            val document = response.parse()
            
            // Log the HTML content for debugging
            logHtmlContent(document.html())
            
            // Try different selectors for mobile version
            var topicElements = document.select(".topic-list li a")
            logDebug("EksiService: Mobile first selector found ${topicElements.size} elements")
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("ul.topic-list li a")
                logDebug("EksiService: Mobile second selector found ${topicElements.size} elements")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("a[href^='/']")
                logDebug("EksiService: Mobile third selector found ${topicElements.size} elements")
            }
            
            // Check if we found any topics at all
            if (topicElements.isEmpty()) {
                logDebug("EksiService: No topics found with any mobile selector for page $page")
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
            
            // Process all topics returned by the server
            // The server should already handle pagination based on the URL
            logDebug("EksiService: Found ${topicElements.size} mobile topics for page $page")
            
            // Process all topics on the page (up to 50)
            // No need to calculate startIndex and endIndex as we'll use all topics from the page
            val topics = mutableListOf<Topic>()
            
            for (topicElement in topicElements) {
                val title = topicElement.text()
                val href = topicElement.attr("href")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
                    logDebug("EksiService: Processing mobile topic: $title, href: $href")
                    // Don't fetch entries yet, just store the topic info
                    val commentCountText = topicElement.parent()?.select("small")?.text()?.trim() ?: ""
                    val commentCount = commentCountText.toIntOrNull() ?: 0
                    topics.add(Topic(
                        title = title,
                        url = href,
                        commentCount = commentCount,
                        entries = emptyList(),
                        entriesLoaded = false
                    ))
                }
            }
            
            // If we found fewer than 50 topics, we've reached the last page
            val isLastPage = topics.size < 50
            logDebug("EksiService: Found ${topics.size} mobile topics for page $page, isLastPage: $isLastPage")
            
            if (topics.isEmpty()) {
                logDebug("EksiService: No mobile topics found")
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
            
            logDebug("EksiService: Successfully fetched ${topics.size} mobile topics")
            
            // Log the parsed topics
            logTopics(topics, page)
            
            return@withContext topics
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in tryMobileVersionWithoutEntries: ${e.message}")
            return@withContext listOf(
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
            
            // Store the corrected URL for logging
            val correctedRedirectedUrl = searchUrl
            
            // Update the log message to use the corrected URL
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
                
                // If we get here, the URL request failed
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
            return@withContext processTopicResponse(response, query, page, searchUrl)
            
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
        searchUrl: String
    ): Topic = withContext(Dispatchers.IO) {
        try {
            // Get the final URL after redirection
            val finalUrl = response.url().toString()
            logDebug("EksiService: Final URL after redirection: $finalUrl")
            
            // Extract the redirected path from the final URL (including query parameters)
            val redirectedPath = if (finalUrl.startsWith(BASE_URL)) {
                val fullPath = finalUrl.substring(BASE_URL.length)
                // Keep the full path including query parameters
                fullPath
            } else {
                searchUrl // Fallback to the original search URL
            }
            
            // Parse the document to get entries
            val document = response.parse()
            
            // Log the HTML content for debugging
            logHtmlContent(document.html())
            
            // Try to get the actual topic title from the page
            val pageTitle = document.select("h1.topic-title, h1.başlık, h1, title").firstOrNull()?.text() ?: query
            // Remove both "- ekşi sözlük" and page information like "- sayfa 5"
            val cleanTitle = pageTitle
                .replace(" - ekşi sözlük", "")
                .replace(Regex(" - sayfa \\d+"), "")  // Remove "- sayfa X" pattern
                .trim()
            
            // Extract entries from the document
            val entryElements = document.select("div.content, div.entry-content, div.entry")
            logDebug("EksiService: Found ${entryElements.size} entries on page $page")
            
            // Use a simple approach: if we're on page X and got entries, there are at least X pages
            var totalPages = page
            if (entryElements.isEmpty() && page > 1) {
                // If no entries found and we're beyond page 1, adjust totalPages
                totalPages = page - 1
                logDebug("EksiService: No entries found on page $page, setting totalPages to $totalPages")
            }
            
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
            }
            
            // Create a topic with the search query and fetched entries
            val topic = Topic(
                title = cleanTitle,
                url = "$BASE_URL$searchUrl",
                commentCount = entries.size,
                entries = entries,
                entriesLoaded = true,
                redirectedUrl = redirectedPath,
                totalPages = totalPages
            )
            
            logDebug("EksiService: Search completed for '$query', page $page, found ${entries.size} entries")
            
            // Log the fetched entries
            logEntries(topic, entries)
            
            return@withContext topic
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in processTopicResponse: ${e.message}")
            
            // Return an error topic
            return@withContext Topic(
                title = query,
                url = "$BASE_URL$searchUrl",
                commentCount = 0,
                entries = listOf(Entry("Error processing response: ${e.message ?: "Unknown error"}")),
                entriesLoaded = true,
                redirectedUrl = searchUrl,
                totalPages = 1
            )
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
            jsonBuilder.append("      \"title\": \"${escapeJsonString(topic.title)}\",\n")
            jsonBuilder.append("      \"entries\": [\n")
            
            topic.entries.forEachIndexed { entryIndex, entry ->
                jsonBuilder.append("        {\n")
                jsonBuilder.append("          \"content\": \"${escapeJsonString(entry.content)}\"\n")
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
    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t")
    }

    suspend fun fetchEntriesForTopic(topic: Topic, page: Int = 1): Topic = withContext(Dispatchers.IO) {
        try {
            logDebug("EksiService: Fetching entries for topic: ${topic.title}, page: $page")
            
            val url = if (topic.url.startsWith("http")) {
                topic.url
            } else {
                "$BASE_URL${topic.url}"
            }
            
            // Add page parameter if needed
            val pageUrl = if (page > 1) {
                if (url.contains("?")) {
                    "$url&p=$page"
                } else {
                    "$url?p=$page"
                }
            } else {
                url
            }
            
            logDebug("EksiService: Fetching entries from URL: $pageUrl")
            
            val connection = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
            
            val response = connection.execute()
            val document = response.parse()
            
            // Log the HTML content for debugging
            logHtmlContent(document.html())
            
            // Extract entries from the document
            val entryElements = document.select("div.content")
            logDebug("EksiService: Found ${entryElements.size} entries on page $page")
            
            val entries = mutableListOf<Entry>()
            
            for (entryElement in entryElements) {
                val content = entryElement.html()
                val author = entryElement.parent()?.select("a.entry-author")?.text() ?: "Unknown"
                val date = entryElement.parent()?.select("a.entry-date")?.text() ?: ""
                
                entries.add(Entry(
                    content = content,
                    author = author,
                    date = date
                ))
            }
            
            // Try to determine the total number of pages
            var totalPages = 1
            val pagerElements = document.select("div.pager")
            if (pagerElements.isNotEmpty()) {
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
            }
            
            logDebug("EksiService: Successfully fetched ${entries.size} entries for topic: ${topic.title}, total pages: $totalPages")
            
            // Log the fetched entries
            logEntries(topic, entries)
            
            // Create a new Topic with the entries
            return@withContext topic.copy(
                entries = entries,
                entriesLoaded = true,
                totalPages = totalPages
            )
        } catch (e: Exception) {
            e.printStackTrace()
            logDebug("EksiService: Exception in fetchEntriesForTopic: ${e.message}")
            
            // Return an error topic
            return@withContext topic.copy(
                entries = listOf(Entry("Error fetching entries: ${e.message ?: "Unknown error"}")),
                entriesLoaded = true,
                totalPages = 1
            )
        }
    }
} 