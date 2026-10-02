package com.example.eksiscraper.network

import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.Topic
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

object HtmlParser {

        fun parseTopics(document: Document): List<Topic> {
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
                        return emptyList()
                }

                val topics = mutableListOf<Topic>()

                for (topicElement in topicElements) {
                        // Get the HTML content of the element instead of just text
                        val htmlContent = topicElement.html()

                        // Remove <small> tags and their content
                        val titleWithoutSmall =
                                htmlContent.replace(Regex("<small>.*?</small>"), "").trim()

                        // Convert the cleaned HTML back to plain text
                        val title = Jsoup.parse(titleWithoutSmall).text()

                        val href = topicElement.attr("href")

                        if (title.isNotEmpty() && href.isNotEmpty()) {
                                // Don't fetch entries yet, just store the topic info
                                val commentCountText =
                                        topicElement.parent()?.select("small")?.text()?.trim() ?: ""
                                val commentCount = commentCountText.toIntOrNull() ?: 0
                                topics.add(
                                        Topic(
                                                title = title,
                                                url = href,
                                                commentCount = commentCount,
                                                entries = emptyList(),
                                                entriesLoaded = false
                                        )
                                )
                        }
                }

                return topics
        }

        fun parseTopicDetail(
                document: Document,
                query: String,
                page: Int,
                searchUrl: String,
                baseUrl: String
        ): Topic {
                // The topic heading; <title> ("x - gündem - sayfa 2 - ekşi sözlük") is a fallback.
                // A plain union selector would return <title> first, as it comes first in the page.
                val heading = document.selectFirst("h1#title, h1[data-title]")
                val cleanTitle =
                        heading?.attr("data-title")?.takeIf { it.isNotBlank() }
                                ?: heading?.text()?.takeIf { it.isNotBlank() }
                                ?: document.title()
                                        .replace(Regex(" - sayfa \\d+"), "")
                                        .replace(" - gündem", "")
                                        .replace(" - ekşi sözlük", "")
                                        .trim()
                                        .ifBlank { query }

                // Extract entry list items directly instead of just content divs
                val entryListItems = document.select("li[data-id]")

                // Extract the total number of pages
                var totalPages = page

                // Strategy 1: Look for any element with data-pagecount attribute (most reliable)
                val pageCountElement = document.select("[data-pagecount]").firstOrNull()
                if (pageCountElement != null) {
                        val attr = pageCountElement.attr("data-pagecount")
                        val parsed = attr.toIntOrNull()
                        if (parsed != null && parsed > totalPages) {
                                totalPages = parsed
                        }
                }

                // Strategy 2: Look for .pager or .pagination links as fallback
                val pagerLinks = document.select(".pager a, .pagination a, div.pager a")
                if (pagerLinks.isNotEmpty()) {
                        val maxLink = pagerLinks.mapNotNull { it.text().toIntOrNull() }.maxOrNull()
                        if (maxLink != null && maxLink > totalPages) {
                                totalPages = maxLink
                        }
                }

                // Process entries
                val entries = mutableListOf<Entry>()
                for (entryLi in entryListItems.take(10)) {
                        // Get the content div inside the li
                        val entryElement =
                                entryLi.select("div.content, div.entry-content, div.entry")
                                        .firstOrNull()
                        if (entryElement != null) {
                                // Instead of just text(), preserve paragraph structure by replacing
                                // <br> and <p>
                                // tags with newlines
                                val htmlContent = entryElement.html()

                                // First, normalize all possible paragraph/line break tags to
                                // consistent markers
                                val contentWithMarkers =
                                        htmlContent
                                                .replace("<br>", "[SINGLE_BREAK]")
                                                .replace("<br/>", "[SINGLE_BREAK]")
                                                .replace("<br />", "[SINGLE_BREAK]")
                                                .replace(
                                                        "</p><p>",
                                                        "[DOUBLE_BREAK]"
                                                ) // Paragraphs should have double breaks
                                                .replace("<p>", "")
                                                .replace("</p>", "[SINGLE_BREAK]")

                                // Convert to plain text - this will strip all remaining HTML tags
                                val plainText = Jsoup.parse(contentWithMarkers).text()

                                // Replace our markers with actual newlines
                                val contentWithProperBreaks =
                                        plainText
                                                .replace("[SINGLE_BREAK]", "\n")
                                                .replace(
                                                        "[DOUBLE_BREAK]",
                                                        "\n\n"
                                                ) // Double newlines for paragraph breaks

                                // Clean up any excessive consecutive newlines but preserve doubles
                                val contentWithNormalizedBreaks =
                                        contentWithProperBreaks.replace(
                                                Regex("\n{3,}"),
                                                "\n\n"
                                        ) // Replace 3+ newlines with double newlines

                                // Trim each line to remove leading/trailing spaces
                                val lines = contentWithNormalizedBreaks.lines()
                                val trimmedLines = lines.map { it.trim() }
                                val content = trimmedLines.joinToString("\n").trim()

                                if (content.isNotEmpty()) {
                                        // Extract author, date, and favorite count
                                        val author =
                                                entryLi.select("a.entry-author, a.author")
                                                        .firstOrNull()
                                                        ?.text()
                                                        ?: ""
                                        val date =
                                                entryLi.select("a.entry-date, span.date")
                                                        .firstOrNull()
                                                        ?.text()
                                                        ?: ""

                                        // Extract favorite count from data attribute of the li
                                        // element
                                        val favoriteCountStr =
                                                entryLi.attr("data-favorite-count") ?: "0"
                                        val favoriteCount = favoriteCountStr.toIntOrNull() ?: 0

                                        // Extract entry ID from data-id attribute
                                        val entryId = entryLi.attr("data-id") ?: ""

                                        // Extract if the entry is already favorited
                                        val isFavorited =
                                                entryLi.attr("data-isfavorite")
                                                        .equals("true", ignoreCase = true)

                                        entries.add(
                                                Entry(
                                                        content = content,
                                                        author = author,
                                                        date = date,
                                                        favoriteCount = favoriteCount,
                                                        entryId = entryId,
                                                        isFavorited = isFavorited
                                                )
                                        )
                                }
                        }
                }

                if (entries.isEmpty()) {
                        entries.add(
                                Entry(
                                        content = "No entries found for this topic on page $page",
                                        entryId = "error_no_entries_${System.currentTimeMillis()}"
                                )
                        )
                }

                // Determine redirected URL
                val finalUrl = document.location()
                val redirectedPath =
                        if (finalUrl.startsWith(baseUrl)) {
                                finalUrl.substring(baseUrl.length)
                        } else {
                                searchUrl
                        }

                return Topic(
                        title = cleanTitle,
                        // The topic's own address (after any search redirect), without the page
                        url = baseUrl + redirectedPath.substringBefore("?"),
                        commentCount = entries.size,
                        entries = entries,
                        entriesLoaded = true,
                        redirectedUrl = redirectedPath,
                        totalPages = totalPages
                )
        }
}
