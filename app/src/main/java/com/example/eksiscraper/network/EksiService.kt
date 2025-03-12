package com.example.eksiscraper.network

import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.HashMap

object EksiService {
    private const val BASE_URL = "https://eksisozluk.com"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    
    // Store the last fetched topics for JSON export
    private var lastFetchedTopics = listOf<Topic>()
    
    suspend fun getPopularTopics(page: Int = 1, category: String = "popular"): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Map category to the corresponding URL path
            val urlPath = when (category) {
                "today" -> "basliklar/bugun"
                "stream" -> "basliklar/sorunsal"
                else -> "basliklar/gundem" // "popular" is the default
            }
            
            // Try with the main URL first
            val result = tryFetchTopicsWithoutEntries(BASE_URL, page, urlPath)
            
            if (page == 1) {
                lastFetchedTopics = result
            }
            
            return@withContext result
        } catch (e: Exception) {
            e.printStackTrace()
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
            
            return@withContext errorResult
        }
    }
    
    private suspend fun tryFetchTopicsWithoutEntries(baseUrl: String, page: Int = 1, urlPath: String = "basliklar/gundem"): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Add shorter random delay to mimic human behavior but not cause too much waiting
            val randomDelayMs = (100L..500L).random()
            if (randomDelayMs > 0) {
                delay(randomDelayMs)
            }
            
            // Construct the URL
            val pageParam = if (page > 1) "?p=$page" else ""
            val url = "$baseUrl/$urlPath$pageParam"
            
            // Connect to the URL
            val connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(10000)  // Use shorter timeout for lightweight requests
                .followRedirects(true)
            
            // Execute the request
            val response = connection.execute()
            
            if (response.statusCode() != 200) {
                // Try a different approach - direct mobile URL
                return@withContext tryMobileVersionWithoutEntries(baseUrl, page, urlPath)
            }
            
            val document = response.parse()
            
            // Try different selectors that might work for eksisozluk.com
            var topicElements = document.select("#content-body .topic-list li a")
            
            // If the first selector doesn't work, try alternatives
            if (topicElements.isEmpty()) {
                topicElements = document.select(".topic-list li a")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("#popular-topics li a")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("#partial-index li a")
            }
            
            if (topicElements.isEmpty()) {
                // Use a different approach to get links that start with '/'
                val allLinks = document.select("a[href^='/']")
                // Create a new Elements collection for non-empty text links
                val filteredLinks = Elements()
                for (link in allLinks) {
                    if (link.text().isNotEmpty()) {
                        filteredLinks.add(link)
                    }
                }
                topicElements = filteredLinks
            }
            
            // Check if we found any topics at all
            if (topicElements.isEmpty()) {
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
            
            // Process all topics on the page (up to 50)
            // No need to calculate startIndex and endIndex as we'll use all topics from the page
            val topics = mutableListOf<Topic>()
            
            for (topicElement in topicElements) {
                val title = topicElement.text()
                val href = topicElement.attr("href")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
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
        } catch (e: Exception) {
            e.printStackTrace()
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
    
    private suspend fun tryMobileVersionWithoutEntries(baseUrl: String, page: Int = 1, urlPath: String = "basliklar/gundem"): List<Topic> = withContext(Dispatchers.IO) {
        try {
            // Try the mobile version
            val mobileUrl = if (page > 1) {
                "$baseUrl/mobil/$urlPath?p=$page"
            } else {
                "$baseUrl/mobil/$urlPath"
            }
            
            val connection = Jsoup.connect(mobileUrl)
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                .timeout(10000) // Reduced timeout to 10 seconds
                .referrer("https://www.google.com/search?q=eksisozluk+mobil")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .maxBodySize(0)
            
            val response = connection.execute()
            
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
            
            val document = response.parse()
            
            // Try different selectors for mobile version
            var topicElements = document.select(".topic-list li a")
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("ul.topic-list li a")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("a[href^='/']")
            }
            
            // Check if we found any topics at all
            if (topicElements.isEmpty()) {
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
            
            // Process all topics on the page (up to 50)
            // No need to calculate startIndex and endIndex as we'll use all topics from the page
            val topics = mutableListOf<Topic>()
            
            for (topicElement in topicElements) {
                val title = topicElement.text()
                val href = topicElement.attr("href")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
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
        } catch (e: Exception) {
            e.printStackTrace()
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
                // If we don't have a redirected URL, use the query parameter format instead of path
                "/?q=${query.trim().replace(" ", "+")}"
            }
            
            // Store the corrected URL for logging
            val correctedRedirectedUrl = searchUrl
            
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
            
            val response = connection.execute()
            
            // Check for HTTP errors
            if (response.statusCode() != 200) {
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
            
            // Try to get the actual topic title from the page
            val pageTitle = document.select("h1.topic-title, h1.başlık, h1, title").firstOrNull()?.text() ?: query
            // Remove both "- ekşi sözlük" and page information like "- sayfa 5"
            val cleanTitle = pageTitle
                .replace(" - ekşi sözlük", "")
                .replace(Regex(" - sayfa \\d+"), "")  // Remove "- sayfa X" pattern
                .trim()
            
            // Extract entries from the document
            val entryElements = document.select("div.content, div.entry-content, div.entry")
            
            // Use a simple approach: if we're on page X and got entries, there are at least X pages
            var totalPages = page
            if (entryElements.isEmpty() && page > 1) {
                // If no entries found and we're beyond page 1, adjust totalPages
                totalPages = page - 1
            }
            
            // Process entries
            val entries = mutableListOf<Entry>()
            for (entryElement in entryElements.take(10)) {
                // Instead of just text(), preserve paragraph structure by replacing <br> and <p> tags with newlines
                val htmlContent = entryElement.html()
                
                // First, normalize all possible paragraph/line break tags to consistent markers
                val contentWithMarkers = htmlContent
                    .replace("<br>", "[SINGLE_BREAK]")
                    .replace("<br/>", "[SINGLE_BREAK]")
                    .replace("<br />", "[SINGLE_BREAK]")
                    .replace("</p><p>", "[DOUBLE_BREAK]") // Paragraphs should have double breaks
                    .replace("<p>", "")
                    .replace("</p>", "[SINGLE_BREAK]")
                
                // Convert to plain text - this will strip all remaining HTML tags
                val plainText = Jsoup.parse(contentWithMarkers).text()
                
                // Replace our markers with actual newlines
                val contentWithProperBreaks = plainText
                    .replace("[SINGLE_BREAK]", "\n")
                    .replace("[DOUBLE_BREAK]", "\n\n") // Double newlines for paragraph breaks
                
                // Clean up any excessive consecutive newlines but preserve doubles
                val contentWithNormalizedBreaks = contentWithProperBreaks
                    .replace(Regex("\n{3,}"), "\n\n") // Replace 3+ newlines with double newlines
                
                // Trim each line to remove leading/trailing spaces
                val lines = contentWithNormalizedBreaks.lines()
                val trimmedLines = lines.map { it.trim() }
                val content = trimmedLines.joinToString("\n").trim()
                
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
            
            return@withContext topic
        } catch (e: Exception) {
            e.printStackTrace()
            
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
    
    // Method to get topics as JSON
    fun getTopicsAsJson(): String {
        return try {
            // Try to use Gson library if available
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            gson.toJson(lastFetchedTopics)
        } catch (e: Exception) {
            // Fall back to manual JSON generation if Gson fails
            val jsonBuilder = StringBuilder()
            jsonBuilder.append("{\n")
            jsonBuilder.append("  \"topics\": [\n")
            
            lastFetchedTopics.forEachIndexed { topicIndex, topic ->
                jsonBuilder.append("    {\n")
                jsonBuilder.append("      \"title\": \"${escapeJsonString(topic.title)}\",\n")
                jsonBuilder.append("      \"commentCount\": ${topic.commentCount},\n")
                jsonBuilder.append("      \"url\": \"${escapeJsonString(topic.url)}\"\n")
                jsonBuilder.append("    }${if (topicIndex < lastFetchedTopics.size - 1) "," else ""}\n")
            }
            
            jsonBuilder.append("  ]\n")
            jsonBuilder.append("}\n")
            
            jsonBuilder.toString()
        }
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
            
            val connection = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
            
            val response = connection.execute()
            val document = response.parse()
            
            // Extract entries from the document
            val entryElements = document.select("div.content")
            
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
            
            // Create a new Topic with the entries
            return@withContext topic.copy(
                entries = entries,
                entriesLoaded = true,
                totalPages = totalPages
            )
        } catch (e: Exception) {
            e.printStackTrace()
            
            // Return an error topic
            return@withContext topic.copy(
                entries = listOf(Entry("Error fetching entries: ${e.message ?: "Unknown error"}")),
                entriesLoaded = true,
                totalPages = 1
            )
        }
    }

    // Wrapper function that provides consistent API for fetching topics
    suspend fun getTopics(category: String = "popular", page: Int = 1): List<Topic> = withContext(Dispatchers.IO) {
        try {
            return@withContext getPopularTopics(page, category)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
} 