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
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0"
    
    // Add the cookie string
    private const val COOKIE_STRING = ""
    
    // Store the last fetched topics for JSON export
    private var lastFetchedTopics = listOf<Topic>()
    
    // Helper function to add cookies to every connection
    private fun applyCommonConnectionSettings(connection: org.jsoup.Connection): org.jsoup.Connection {
        return connection
            .userAgent(USER_AGENT)
            .timeout(10000)
            .followRedirects(true)
            .header("Cookie", COOKIE_STRING)
            .header("Host", "eksisozluk.com")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
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
    }
    
    // Function to favorite an entry
    suspend fun favoriteEntry(entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // URL for favoriting an entry
            val favlaUrl = "$BASE_URL/entry/favla"
            
            // Set up the connection for POST request
            val connection = applyCommonConnectionSettings(Jsoup.connect(favlaUrl))
                .method(org.jsoup.Connection.Method.POST)
                .referrer("$BASE_URL/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                // Add CSRF protection headers if needed
                .header("X-Requested-With", "XMLHttpRequest")
            
            // Add the entry ID as form data
            connection.data("entryId", entryId)
            
            // Execute the request
            val response = connection.execute()
            
            // Check if the request was successful (status code 200)
            return@withContext response.statusCode() == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    // Function to unfavorite an entry
    suspend fun unfavoriteEntry(entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // URL for unfavoriting an entry
            val favlamaUrl = "$BASE_URL/entry/favlama"
            
            // Set up the connection for POST request
            val connection = applyCommonConnectionSettings(Jsoup.connect(favlamaUrl))
                .method(org.jsoup.Connection.Method.POST)
                .referrer("$BASE_URL/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                // Add CSRF protection headers if needed
                .header("X-Requested-With", "XMLHttpRequest")
            
            // Add the entry ID as form data
            connection.data("entryId", entryId)
            
            // Execute the request
            val response = connection.execute()
            
            // Check if the request was successful (status code 200)
            return@withContext response.statusCode() == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    // Function to toggle entry favorite state (for backward compatibility)
    suspend fun toggleEntryFavorite(entryId: String): Boolean = withContext(Dispatchers.IO) {
        // Default to favoriting if we don't know the state
        return@withContext favoriteEntry(entryId)
    }
    
    // Function to toggle entry favorite state based on current state
    suspend fun toggleEntryFavorite(entryId: String, isFavorited: Boolean): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (isFavorited) {
            // If already favorited, call unfavorite
            unfavoriteEntry(entryId)
        } else {
            // If not favorited, call favorite
            favoriteEntry(entryId)
        }
    }
    
    suspend fun getPopularTopics(page: Int = 1, category: String = "popular"): List<Topic> = withContext(Dispatchers.IO) {
        try {
            println("EksiService.getPopularTopics: page=$page, category=$category")
            
            // Map category to the corresponding URL path
            val urlPath = when (category) {
                "today" -> "basliklar/bugun"
                "stream" -> "basliklar/sorunsal"
                else -> "basliklar/gundem" // "popular" is the default
            }
            
            println("EksiService.getPopularTopics: mapped category to urlPath=$urlPath")
            
            // Try with the main URL first
            val result = tryFetchTopicsWithoutEntries(BASE_URL, page, urlPath)
            
            println("EksiService.getPopularTopics: fetched ${result.size} topics")
            
            if (page == 1) {
                lastFetchedTopics = result
            }
            
            return@withContext result
        } catch (e: Exception) {
            println("EksiService.getPopularTopics: error: ${e.message}")
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
            
            // Construct the URL with special handling for "bugun" category
            val url = if (urlPath == "basliklar/bugun" && page > 1) {
                "$baseUrl/$urlPath/$page"
            } else {
                val pageParam = if (page > 1) "?p=$page" else ""
                "$baseUrl/$urlPath$pageParam"
            }
            
            println("tryFetchTopicsWithoutEntries: fetching URL=$url")
            
            // Connect to the URL with cookie
            val connection = applyCommonConnectionSettings(Jsoup.connect(url))
            
            // Execute the request
            val response = connection.execute()
            
            println("tryFetchTopicsWithoutEntries: HTTP response status=${response.statusCode()}")
            
            if (response.statusCode() != 200) {
                println("tryFetchTopicsWithoutEntries: Status code ${response.statusCode()}, trying mobile version")
                // Try a different approach - direct mobile URL
                return@withContext tryMobileVersionWithoutEntries(baseUrl, page, urlPath)
            }
            
            val document = response.parse()
            
            // Try different selectors that might work for eksisozluk.com
            var topicElements = document.select("#content-body .topic-list li a")
            
            // If the first selector doesn't work, try alternatives
            if (topicElements.isEmpty()) {
                println("tryFetchTopicsWithoutEntries: first selector failed, trying alternatives")
                topicElements = document.select(".topic-list li a")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("#popular-topics li a")
            }
            
            if (topicElements.isEmpty()) {
                topicElements = document.select("#partial-index li a")
            }
            
            if (topicElements.isEmpty()) {
                println("tryFetchTopicsWithoutEntries: all standard selectors failed, trying generic links")
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
                println("tryFetchTopicsWithoutEntries: No topics found in the page")
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
            
            println("tryFetchTopicsWithoutEntries: Found ${topicElements.size} topic elements")
            
            // Process all topics on the page (up to 50)
            // No need to calculate startIndex and endIndex as we'll use all topics from the page
            val topics = mutableListOf<Topic>()
            
            for (topicElement in topicElements) {
                // Get the HTML content of the element instead of just text
                val htmlContent = topicElement.html()
                
                // Remove <small> tags and their content
                val titleWithoutSmall = htmlContent.replace(Regex("<small>.*?</small>"), "").trim()
                
                // Convert the cleaned HTML back to plain text
                val title = Jsoup.parse(titleWithoutSmall).text()
                
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
            println("tryFetchTopicsWithoutEntries: Processed ${topics.size} topics, isLastPage=$isLastPage")
            
            if (topics.isEmpty()) {
                println("tryFetchTopicsWithoutEntries: No valid topics found after processing")
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
            println("tryFetchTopicsWithoutEntries: Error: ${e.message}")
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
            println("tryMobileVersionWithoutEntries: Attempting mobile version fetch for page=$page, urlPath=$urlPath")
            // Try the mobile version
            val mobileUrl = if (page > 1) {
                "$baseUrl/mobil/$urlPath?p=$page"
            } else {
                "$baseUrl/mobil/$urlPath"
            }
            
            println("tryMobileVersionWithoutEntries: Using mobile URL=$mobileUrl")
            
            // Use the helper function to set up the connection with cookie
            val connection = applyCommonConnectionSettings(Jsoup.connect(mobileUrl))
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                .referrer("https://www.google.com/search?q=eksisozluk+mobil")
                .maxBodySize(0)
            
            val response = connection.execute()
            
            println("tryMobileVersionWithoutEntries: HTTP response status=${response.statusCode()}")
            
            if (response.statusCode() != 200) {
                println("tryMobileVersionWithoutEntries: Failed with status code ${response.statusCode()}")
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
                println("tryMobileVersionWithoutEntries: First selector failed, trying alternatives")
                topicElements = document.select("ul.topic-list li a")
            }
            
            if (topicElements.isEmpty()) {
                println("tryMobileVersionWithoutEntries: Standard selectors failed, trying generic links")
                topicElements = document.select("a[href^='/']")
            }
            
            // Check if we found any topics at all
            if (topicElements.isEmpty()) {
                println("tryMobileVersionWithoutEntries: No topics found in mobile version")
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
            
            println("tryMobileVersionWithoutEntries: Found ${topicElements.size} topic elements")
            
            // Process all topics on the page (up to 50)
            // No need to calculate startIndex and endIndex as we'll use all topics from the page
            val topics = mutableListOf<Topic>()
            
            for (topicElement in topicElements) {
                // Get the HTML content of the element instead of just text
                val htmlContent = topicElement.html()
                
                // Remove <small> tags and their content
                val titleWithoutSmall = htmlContent.replace(Regex("<small>.*?</small>"), "").trim()
                
                // Convert the cleaned HTML back to plain text
                val title = Jsoup.parse(titleWithoutSmall).text()
                
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
            println("tryMobileVersionWithoutEntries: Processed ${topics.size} topics, isLastPage=$isLastPage")
            
            if (topics.isEmpty()) {
                println("tryMobileVersionWithoutEntries: No valid topics found after processing")
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
            println("tryMobileVersionWithoutEntries: Error: ${e.message}")
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
            println("EksiService.searchTopic: query='$query', page=$page, redirectedUrl='$redirectedUrl'")
            
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
            
            println("EksiService.searchTopic: searchUrl='$searchUrl'")
            
            // Store the corrected URL for logging
            val correctedRedirectedUrl = searchUrl
            
            // Connect to the URL and follow redirections with cookie
            val connection = applyCommonConnectionSettings(Jsoup.connect("$BASE_URL$searchUrl"))
            
            val response = connection.execute()
            
            println("EksiService.searchTopic: HTTP response status: ${response.statusCode()}")
            
            // Check for HTTP errors
            if (response.statusCode() != 200) {
                return@withContext Topic(
                    title = query,
                    url = "$BASE_URL$searchUrl",
                    commentCount = 0,
                    entries = listOf(Entry(
                        content = "HTTP error: ${response.statusCode()} - ${response.statusMessage()}",
                        entryId = "error_http_${response.statusCode()}"
                    )),
                    entriesLoaded = true,
                    redirectedUrl = searchUrl,
                    totalPages = 1
                )
            }
            
            // Process the successful response
            return@withContext processTopicResponse(response, query, page, searchUrl)
            
        } catch (e: Exception) {
            e.printStackTrace()
            println("EksiService.searchTopic: Error: ${e.message}")
            
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
            println("processTopicResponse: finalUrl='$finalUrl'")
            
            // Extract the redirected path from the final URL (including query parameters)
            val redirectedPath = if (finalUrl.startsWith(BASE_URL)) {
                val fullPath = finalUrl.substring(BASE_URL.length)
                // Keep the full path including query parameters
                fullPath
            } else {
                searchUrl // Fallback to the original search URL
            }
            
            println("processTopicResponse: redirectedPath='$redirectedPath'")
            
            // Parse the document to get entries
            val document = response.parse()
            
            // Try to get the actual topic title from the page
            val pageTitle = document.select("h1.topic-title, h1.başlık, h1, title").firstOrNull()?.text() ?: query
            // Remove both "- ekşi sözlük" and page information like "- sayfa 5"
            val cleanTitle = pageTitle
                .replace(" - ekşi sözlük", "")
                .replace(Regex(" - sayfa \\d+"), "")  // Remove "- sayfa X" pattern
                .trim()
            
            println("processTopicResponse: pageTitle='$pageTitle', cleanTitle='$cleanTitle'")
            
            // Extract entry list items directly instead of just content divs
            val entryListItems = document.select("li[data-id]")
            
            // Extract the total number of pages from data-pagecount attribute
            var totalPages = page
            val pagerElement = document.select("div.pager").firstOrNull()
            
            if (pagerElement != null) {
                // Extract the data-pagecount attribute
                val pageCountStr = pagerElement.attr("data-pagecount")
                println("processTopicResponse: pageCountStr='$pageCountStr'")
                
                if (pageCountStr.isNotEmpty()) {
                    val parsedPageCount = pageCountStr.toIntOrNull() ?: page
                    
                    // Verify the page count with additional checks if possible
                    val lastPageLink = pagerElement.select("a").lastOrNull { 
                        it.text().matches(Regex("\\d+")) 
                    }?.text()?.toIntOrNull()
                    
                    if (lastPageLink != null && lastPageLink != parsedPageCount) {
                        println("processTopicResponse: WARNING - pagecount mismatch: attr=$parsedPageCount, lastLink=$lastPageLink")
                        // Use the last page link as a fallback
                        totalPages = lastPageLink
                    } else {
                        totalPages = parsedPageCount
                    }
                    
                    println("processTopicResponse: totalPages set from verification: $totalPages")
                }
            }
            
            println("processTopicResponse: finalTotalPages=$totalPages, entryItemsCount=${entryListItems.size}")
            
            // Process entries
            val entries = mutableListOf<Entry>()
            for (entryLi in entryListItems.take(10)) {
                // Get the content div inside the li
                val entryElement = entryLi.select("div.content, div.entry-content, div.entry").firstOrNull()
                if (entryElement != null) {
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
                        // Extract author, date, and favorite count
                        val author = entryLi.select("a.entry-author, a.author").firstOrNull()?.text() ?: ""
                        val date = entryLi.select("a.entry-date, span.date").firstOrNull()?.text() ?: ""
                        
                        // Extract favorite count from data attribute of the li element
                        val favoriteCountStr = entryLi.attr("data-favorite-count") ?: "0"
                        val favoriteCount = favoriteCountStr.toIntOrNull() ?: 0
                        
                        // Extract entry ID from data-id attribute
                        val entryId = entryLi.attr("data-id") ?: ""
                        
                        // Extract if the entry is already favorited
                        val isFavorited = entryLi.attr("data-isfavorite").equals("true", ignoreCase = true)
                        
                        entries.add(Entry(
                            content = content,
                            author = author,
                            date = date,
                            favoriteCount = favoriteCount,
                            entryId = entryId,
                            isFavorited = isFavorited
                        ))
                    }
                }
            }
            
            if (entries.isEmpty()) {
                entries.add(Entry(
                    content = "No entries found for this topic on page $page",
                    entryId = "error_no_entries_${System.currentTimeMillis()}"
                ))
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
            
            println("processTopicResponse: returning topic with title='${topic.title}', totalPages=${topic.totalPages}, redirectedUrl='${topic.redirectedUrl}'")
            
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
            
            // Use the helper function to apply common settings including cookie
            val connection = applyCommonConnectionSettings(Jsoup.connect(pageUrl))
            
            val response = connection.execute()
            val document = response.parse()
            
            // Extract entry list items directly instead of just content divs
            val entryListItems = document.select("li[data-id]")
            
            val entries = mutableListOf<Entry>()
            
            for (entryLi in entryListItems) {
                // Get the content div inside the li
                val entryElement = entryLi.select("div.content").firstOrNull()
                if (entryElement != null) {
                    val content = entryElement.html()
                    val author = entryLi.select("a.entry-author").text() ?: "Unknown"
                    val date = entryLi.select("a.entry-date").text() ?: ""
                    
                    // Extract favorite count from data attribute of the li element
                    val favoriteCountStr = entryLi.attr("data-favorite-count") ?: "0"
                    val favoriteCount = favoriteCountStr.toIntOrNull() ?: 0
                    
                    // Extract entry ID from data-id attribute
                    val entryId = entryLi.attr("data-id") ?: ""
                    
                    // Extract if the entry is already favorited
                    val isFavorited = entryLi.attr("data-isfavorite").equals("true", ignoreCase = true)
                    
                    entries.add(Entry(
                        content = content,
                        author = author,
                        date = date,
                        favoriteCount = favoriteCount,
                        entryId = entryId,
                        isFavorited = isFavorited
                    ))
                }
            }
            
            // Extract the total number of pages from data-pagecount attribute
            var totalPages = 1
            val pagerElement = document.select("div.pager").firstOrNull()
            
            if (pagerElement != null) {
                // Extract the data-pagecount attribute
                val pageCountStr = pagerElement.attr("data-pagecount")
                println("fetchEntriesForTopic: pageCountStr='$pageCountStr'")
                
                if (pageCountStr.isNotEmpty()) {
                    val parsedPageCount = pageCountStr.toIntOrNull() ?: 1
                    
                    // Verify the page count with additional checks if possible
                    val lastPageLink = pagerElement.select("a").lastOrNull { 
                        it.text().matches(Regex("\\d+")) 
                    }?.text()?.toIntOrNull()
                    
                    if (lastPageLink != null && lastPageLink != parsedPageCount) {
                        println("fetchEntriesForTopic: WARNING - pagecount mismatch: attr=$parsedPageCount, lastLink=$lastPageLink")
                        // Use the last page link as a fallback
                        totalPages = lastPageLink
                    } else {
                        totalPages = parsedPageCount
                    }
                    
                    println("fetchEntriesForTopic: totalPages set from verification: $totalPages")
                }
            }
            
            println("fetchEntriesForTopic: finalTotalPages=$totalPages, topic=${topic.title}, entriesCount=${entries.size}")
            
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