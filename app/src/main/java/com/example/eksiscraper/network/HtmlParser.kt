package com.example.eksiscraper.network

import com.example.eksiscraper.model.AuthorProfile
import com.example.eksiscraper.model.Badge
import com.example.eksiscraper.model.Channel
import com.example.eksiscraper.model.Comment
import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.FormSpec
import com.example.eksiscraper.model.Message
import com.example.eksiscraper.model.MessageBox
import com.example.eksiscraper.model.MessageThread
import com.example.eksiscraper.model.ThreadDetail
import com.example.eksiscraper.model.Topic
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
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

                // No generic "every link" fallback: it turned the site menu (bugün, gündem,
                // debe, ...) into fake topics when the list was missing

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
                val entryListItems = document.select("#entry-item-list > li[data-id]")

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

                val returnedPage =
                        document.selectFirst("[data-currentpage]")
                                ?.attr("data-currentpage")
                                ?.toIntOrNull()
                                ?: page

                // On ?a=popular / ?day= pages a "N entry daha" link above the list counts the
                // entries before today's first one (its href is /slug--id?focusto=<entry id>)
                val firstEntry = entryListItems.firstOrNull()
                val olderLink =
                        document.select("a.showall").firstOrNull { link ->
                                firstEntry != null &&
                                        link.attr("href").contains("focusto=${firstEntry.attr("data-id")}")
                        }
                val olderCount =
                        olderLink?.text()?.substringBefore(" ")?.toIntOrNull() ?: 0

                val entries = entryListItems.take(10).mapNotNull(::parseEntry)

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
                        totalPages = maxOf(totalPages, returnedPage),
                        currentPage = returnedPage,
                        olderEntriesCount = olderCount,
                        entryForm = findEntryForm(document),
                        deleteForm = findDeleteForm(document),
                        commentForm = document.selectFirst("form#comment-entry-form")?.let(::formSpec)
                )
        }

        /** One entry `<li>` as rendered in topic pages and profile lists. */
        fun parseEntry(li: Element): Entry? {
                val contentElement = li.selectFirst("div.content") ?: return null
                val content = toPlainText(contentElement)
                if (content.isEmpty()) return null
                return Entry(
                        content = content,
                        contentHtml = contentElement.html(),
                        author = li.attr("data-author").ifEmpty { li.selectFirst("a.entry-author")?.text().orEmpty() },
                        authorId = li.attr("data-author-id"),
                        date = li.selectFirst("a.entry-date")?.text().orEmpty(),
                        favoriteCount = li.attr("data-favorite-count").toIntOrNull() ?: 0,
                        entryId = li.attr("data-id"),
                        isFavorited = li.attr("data-isfavorite").equals("true", ignoreCase = true),
                        isLiked = li.attr("data-isliked").equals("true", ignoreCase = true),
                        isDisliked = li.attr("data-isdisliked").equals("true", ignoreCase = true),
                        flags = li.attr("data-flags").split(' ').filter { it.isNotBlank() }.toSet(),
                        commentCount = li.attr("data-comment-count").toIntOrNull() ?: 0,
                        avatarUrl = avatarUrl(li)
                )
        }

        /** The real profile picture inside an entry/comment footer, if the author set one. */
        private fun avatarUrl(element: Element): String? {
                val src = element.selectFirst("img.avatar")?.attr("src").orEmpty()
                if (src.isBlank() || src.contains("default-profile")) return null
                return if (src.startsWith("//")) "https:$src" else src
        }

        fun parseComments(document: Document): List<Comment> =
                document.select("li[data-comment-id]").mapNotNull { li ->
                        val content = li.selectFirst(".comment-content") ?: return@mapNotNull null
                        Comment(
                                id = li.attr("data-comment-id"),
                                author = li.attr("data-author"),
                                authorId = li.attr("data-author-id"),
                                content = toPlainText(content),
                                contentHtml = content.html(),
                                date = li.selectFirst("a.entry-date")?.text().orEmpty(),
                                avatarUrl = avatarUrl(li),
                                upVotes = li.attr("data-up-vote-count").toIntOrNull() ?: 0,
                                downVotes = li.attr("data-down-vote-count").toIntOrNull() ?: 0,
                                isLiked = li.attr("data-isliked").equals("true", ignoreCase = true),
                                isDisliked = li.attr("data-isdisliked").equals("true", ignoreCase = true)
                        )
                }

        fun parseMessageBox(document: Document): MessageBox = MessageBox(
                threads = document.select("ul#threads > li article").mapNotNull { article ->
                        val link = article.selectFirst("a[href^=/mesaj/]") ?: return@mapNotNull null
                        val heading = link.selectFirst("h2")
                        val count = heading?.selectFirst("small")?.text()?.filter(Char::isDigit)?.toIntOrNull() ?: 0
                        heading?.select("small")?.remove()
                        MessageThread(
                                id = link.attr("href").removePrefix("/mesaj/"),
                                nick = heading?.text().orEmpty(),
                                messageCount = count,
                                // Our own last message carries the same "sender -> recipient:" prefix
                                preview = link.selectFirst("p")?.also(::stripSenderPrefix)?.text().orEmpty(),
                                time = article.selectFirst("time")?.text().orEmpty()
                        )
                },
                sendForm = document.selectFirst("form#message-send-form")?.let(::formSpec),
                threadForm = document.selectFirst("form#message-thread-list-form")?.let(::formSpec)
        )

        fun parseThread(document: Document): ThreadDetail = ThreadDetail(
                // The title shows "@nick"; the form and profile URLs want the bare nick
                nick = document.selectFirst("#message-thread-title a[href^=/biri/]")?.text().orEmpty().trim().removePrefix("@"),
                messages = document.select("#message-thread > article").map { article ->
                        // Received messages are marked "incoming"; the rest are ours
                        val outgoing = !article.hasClass("incoming")
                        val paragraph = article.selectFirst("p")
                        if (outgoing && paragraph != null) stripSenderPrefix(paragraph)
                        Message(
                                text = paragraph?.let(::toPlainText).orEmpty(),
                                html = paragraph?.html().orEmpty(),
                                time = article.selectFirst("footer time")?.text().orEmpty(),
                                isOutgoing = outgoing
                        )
                },
                sendForm = document.selectFirst("form#message-send-form")?.let(::formSpec),
                threadForm = document.selectFirst("form#message-thread-form")?.let(::formSpec)
        )

        /**
         * The site writes our own messages as "sender -> recipient: text" (the nicks sometimes as
         * links). The bubble already says who wrote it, so drop that prefix.
         */
        private fun stripSenderPrefix(paragraph: Element) {
                val prefix = Regex("^\\s*.{1,40}? -> .{1,40}?: ")
                val nodes = paragraph.childNodes().toList()
                val seen = StringBuilder()
                for ((index, node) in nodes.withIndex()) {
                        val text = when (node) {
                                is TextNode -> node.wholeText
                                is Element -> node.text()
                                else -> ""
                        }
                        val match = prefix.find(seen.toString() + text)
                        if (match != null) {
                                // The prefix ends inside this node: drop what came before and its head
                                val cut = match.range.last + 1 - seen.length
                                nodes.take(index).forEach { it.remove() }
                                if (node is TextNode) node.text(node.wholeText.substring(cut)) else node.remove()
                                return
                        }
                        seen.append(text)
                        if (seen.length > 100) return
                }
        }

        /** Channels linked from the site navigation (/basliklar/kanal/...). */
        fun parseChannels(document: Document): List<Channel> =
                document.select("a[href^=/basliklar/kanal/]")
                        .map { Channel(it.text().removePrefix("#").trim(), it.attr("title"), it.attr("href")) }
                        .filter { it.name.isNotBlank() }
                        .distinctBy { it.path }

        /** Text with line breaks kept: <br> and paragraphs become newlines. */
        private fun toPlainText(element: Element): String {
                val marked = element.html()
                        .replace(Regex("<br\\s*/?>"), "[BR]")
                        .replace("</p><p>", "[BR][BR]")
                        .replace("<p>", "")
                        .replace("</p>", "[BR]")
                return Jsoup.parse(marked).text()
                        .replace("[BR]", "\n")
                        .replace(Regex("\n{3,}"), "\n\n")
                        .lines()
                        .joinToString("\n") { it.trim() }
                        .trim()
        }

        /**
         * The "write an entry" form logged-in users get under a topic. Found by shape (a form
         * with a textarea that isn't the comment or message form) so small markup changes don't
         * break it.
         */
        private fun findEntryForm(document: Document): FormSpec? {
                val form = document.select("form").firstOrNull { form ->
                        form.selectFirst("textarea") != null &&
                                form.id() != "comment-entry-form" &&
                                !form.id().contains("comment") &&
                                !form.attr("action").contains("mesaj") &&
                                !form.attr("action").contains("yorum")
                } ?: return null
                return formSpec(form)
        }

        /** The form behind the "sil" item of the user's own entries. */
        private fun findDeleteForm(document: Document): FormSpec? {
                val candidates = document.select("form").filter { form ->
                        val action = form.attr("action").lowercase()
                        form.selectFirst("input[name=id], input[name=Id]") != null &&
                                (action.contains("sil") || action.contains("delete"))
                }
                // Moderators also get a "delete other" form; prefer the one for own entries
                val form = candidates.firstOrNull { !it.id().contains("other") } ?: return null
                return formSpec(form)
        }

        private fun formSpec(form: Element): FormSpec {
                val fields = form.select("input[name]")
                        .filter { it.attr("type") !in setOf("submit", "button", "checkbox") }
                        .associate { it.attr("name") to it.attr("value") }
                return FormSpec(
                        action = form.attr("action"),
                        fields = fields,
                        textFieldName = form.selectFirst("textarea[name]")?.attr("name")
                )
        }

        fun parseProfile(document: Document, nick: String): AuthorProfile {
                fun count(id: String) =
                        document.selectFirst("#$id")?.text()?.filter(Char::isDigit)?.toIntOrNull() ?: 0
                val avatar = document.selectFirst("img.avatar")?.attr("src").orEmpty()
                return AuthorProfile(
                        nick = document.selectFirst("#user-profile-title")?.attr("data-nick")
                                ?.ifBlank { null } ?: nick,
                        // The default picture is an SVG placeholder; the app shows initials instead
                        avatarUrl = avatar.takeIf { it.isNotBlank() && !it.contains("default-profile") }
                                ?.let { if (it.startsWith("//")) "https:$it" else it },
                        isVerified = document.selectFirst("#verified-badge") != null,
                        karma = document.selectFirst("#nick-container + p.muted, p.muted")?.text().orEmpty(),
                        biography = document.selectFirst("#profile-biography .content")?.text().orEmpty(),
                        entryCount = count("entry-count-total"),
                        followerCount = count("user-follower-count"),
                        followingCount = count("user-following-count"),
                        joinedDate = document.selectFirst(".recorddate")?.text().orEmpty(),
                        followAddUrl = document.selectFirst("#buddy-link")?.attr("data-add-url")?.ifBlank { null },
                        followRemoveUrl = document.selectFirst("#buddy-link")?.attr("data-remove-url")?.ifBlank { null },
                        isFollowing = document.selectFirst("#buddy-link")?.attr("data-added") == "true",
                        badges = document.select("a.user-profile-badge-item").map {
                                Badge(
                                        name = it.attr("data-name"),
                                        description = it.attr("data-title"),
                                        imageUrl = it.selectFirst("img")?.attr("src").orEmpty()
                                )
                        }
                )
        }

        /** Profile tabs (son entryleri, en beğenilenleri, ...): entries with their topic. */
        fun parseUserEntries(document: Document): List<Entry> =
                document.select(".topic-item").flatMap { item ->
                        val heading = item.selectFirst("h1")
                        val title = heading?.attr("data-title")?.ifBlank { null } ?: heading?.text().orEmpty()
                        val url = heading?.selectFirst("a")?.attr("href").orEmpty()
                        item.select("li[data-id]").mapNotNull(::parseEntry)
                                .map { it.copy(topicTitle = title, topicUrl = url) }
                }
}
