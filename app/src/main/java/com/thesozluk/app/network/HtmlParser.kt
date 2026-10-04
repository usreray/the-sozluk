package com.thesozluk.app.network

import com.thesozluk.app.model.AuthorProfile
import com.thesozluk.app.model.Badge
import com.thesozluk.app.model.Channel
import com.thesozluk.app.model.Comment
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.FormSpec
import com.thesozluk.app.model.Message
import com.thesozluk.app.model.MessageBox
import com.thesozluk.app.model.MessageThread
import com.thesozluk.app.model.ThreadDetail
import com.thesozluk.app.model.RelationAction
import com.thesozluk.app.model.Topic
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import kotlin.math.roundToInt

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
                                val commentCountText = topicElement.parent()?.select("small")?.text().orEmpty()
                                val commentCount = parseTopicCount(commentCountText)
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

        /** Parses both exact counts and the site's shortened forms such as "1,1 bin" / "1.1b". */
        private fun parseTopicCount(text: String): Int {
                val match = Regex("""^\s*([\d., ]+)\s*(bin|b|k)?\s*$""", RegexOption.IGNORE_CASE).matchEntire(text)
                        ?: return 0
                val raw = match.groupValues[1].replace(" ", "")
                val unit = match.groupValues[2]
                if (unit.isNotEmpty()) {
                        val value = raw.replace(',', '.').toDoubleOrNull() ?: return 0
                        return (value * 1_000).roundToInt()
                }
                if (Regex("""^\d{1,3}(?:[.,]\d{3})+$""").matches(raw)) {
                        return raw.filter(Char::isDigit).toIntOrNull() ?: 0
                }
                return raw.filter(Char::isDigit).toIntOrNull() ?: 0
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
                val topicContainer = document.selectFirst("#topic") ?: document

                // Ignore unrelated pagers elsewhere on the page (for example, search widgets).
                val pageCountElement = topicContainer.selectFirst(".pager[data-pagecount]")
                if (pageCountElement != null) {
                        val attr = pageCountElement.attr("data-pagecount")
                        val parsed = attr.toIntOrNull()
                        if (parsed != null && parsed > totalPages) {
                                totalPages = parsed
                        }
                }

                // Fall back only to links in the topic's own pager.
                val pagerLinks = topicContainer.select(".pager a")
                if (pagerLinks.isNotEmpty()) {
                        val maxLink = pagerLinks.mapNotNull { it.text().toIntOrNull() }.maxOrNull()
                        if (maxLink != null && maxLink > totalPages) {
                                totalPages = maxLink
                        }
                }

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

                val entries = entryListItems.take(10).mapNotNull(::parseEntry).distinctBy { it.entryId }
                val trackLink = document.selectFirst("#track-topic-link")

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
                        entries = entries,
                        entriesLoaded = true,
                        redirectedUrl = redirectedPath,
                        totalPages = totalPages,
                        currentPage = page,
                        olderEntriesCount = olderCount,
                        entryForm = findEntryForm(document),
                        deleteForm = findDeleteForm(document),
                        commentForm = document.selectFirst("form#comment-entry-form")?.let(::formSpec),
                        topicPath = heading?.selectFirst("a[href]")?.attr("href")?.substringBefore("?").orEmpty(),
                        topicId = heading?.attr("data-id").orEmpty(),
                        // The site toggles data-tracked (0/1) and posts to data-trackurl / data-untrackurl
                        isTracked = trackLink?.attr("data-tracked") == "1",
                        trackUrl = trackLink?.attr("data-trackurl")?.ifBlank { null },
                        untrackUrl = trackLink?.attr("data-untrackurl")?.ifBlank { null }
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
                        avatarUrl = avatarUrl(li),
                        eksiSeylerUrl = eksiSeylerUrl(li),
                        authorIsVerified = li.selectFirst("svg.verified-badge, svg[class*=verified], #verified-badge") != null ||
                                authorBadgePresent(li, "verified", "onaylı hesap"),
                        authorIsAdFree = hasSubscriberBadge(li) ||
                                authorStatusAreaHasAdFreeBadge(li) ||
                                authorBadgePresent(li, "subscriber-badge", "status-badge-large", "ad-free", "adfree", "no-ads", "reklamsız")
                )
        }

        private fun hasSubscriberBadge(element: Element): Boolean =
                element.select("svg").any { svg ->
                        svg.id().contains("subscriber-badge", ignoreCase = true) ||
                                svg.classNames().any { it.contains("subscriber-badge", ignoreCase = true) }
                }

        /** Entry markup may use a compact subscriber SVG next to the author instead of the profile's large variant. */
        private fun authorStatusAreaHasAdFreeBadge(entry: Element): Boolean {
                val author = entry.selectFirst(".entry-author, a[href^=/biri/]") ?: return false
                val area = author.parent() ?: author
                return hasSubscriberBadge(area)
        }

        private fun authorBadgePresent(entry: Element, vararg terms: String): Boolean {
                val author = entry.selectFirst(".entry-author, a[href^=/biri/]") ?: return false
                val area = author.parent() ?: author
                val markers = (author.parents() + entry + area + author + author.select("*") + area.select("*")).flatMap { node ->
                        buildList {
                                add(node.id())
                                addAll(node.classNames())
                                add(node.attr("title"))
                                add(node.attr("aria-label"))
                                add(node.attr("data-badge"))
                                add(node.attr("data-title"))
                                add(node.attr("data-name"))
                                add(node.attr("data-author-status"))
                                add(node.attr("data-author-badge"))
                                add(node.attr("data-verified"))
                                add(node.attr("data-ad-free"))
                                add(node.attr("src"))
                                add(node.attr("alt"))
                                add(node.attributes().toString())
                        }
                }.filter { it.isNotBlank() }
                return markers.any { marker -> terms.any { marker.contains(it, ignoreCase = true) } }
        }

        private fun eksiSeylerUrl(li: Element): String? {
                val metadata = li.clone().apply {
                        select(".content, .entry-author, .entry-date").remove()
                }
                val href = metadata.select("a[href]").firstOrNull {
                        it.attr("href").contains("seyler", ignoreCase = true)
                }?.attr("href")?.trim().orEmpty()
                return when {
                        href.startsWith("//") -> "https:$href"
                        href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true) -> href
                        else -> null
                }
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

        /** "düzelt" page of an entry: its form, with the entry's current text in the field. */
        fun parseEditForm(document: Document): FormSpec? = findEntryForm(document)

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
                        textFieldName = form.selectFirst("textarea[name]")?.attr("name"),
                        textValue = form.selectFirst("textarea[name]")?.wholeText()?.trim().orEmpty()
                )
        }

        fun parseProfile(document: Document, nick: String): AuthorProfile {
                fun count(id: String) =
                        document.selectFirst("#$id")?.text()?.filter(Char::isDigit)?.toIntOrNull() ?: 0
                val rank = document.selectFirst("#nick-container + p.muted")?.text()?.trim().orEmpty()
                val profileTitle = document.selectFirst("#user-profile-title")
                val nickContainer = document.selectFirst("#nick-container")
                val headerNodes = buildList<Element?> {
                        addAll(generateSequence(profileTitle) { it.parent() }.take(3).toList())
                        add(nickContainer)
                        add(nickContainer?.nextElementSibling())
                        add(nickContainer?.parent()?.nextElementSibling())
                }.filterNotNull()
                val isRookie = rank.equals("çaylak", ignoreCase = true) || headerNodes.any { node ->
                        node.ownText().trim().equals("çaylak", ignoreCase = true) ||
                                node.select("*").any { it.ownText().trim().equals("çaylak", ignoreCase = true) }
                }
                // The profile's own picture box; a bare img.avatar can be the logged-in user's
                // own picture in the site header
                val avatar = (document.selectFirst("#profile-logo img") ?: document.selectFirst("#user-profile-title img.avatar, img.logo.avatar"))
                        ?.attr("src").orEmpty()
                return AuthorProfile(
                        nick = document.selectFirst("#user-profile-title")?.attr("data-nick")
                                ?.ifBlank { null } ?: nick,
                        // The default picture is an SVG placeholder; the app shows initials instead
                        avatarUrl = avatar.takeIf { it.isNotBlank() && !it.contains("default-profile") }
                                ?.let { if (it.startsWith("//")) "https:$it" else it },
                        isVerified = document.selectFirst("#verified-badge") != null,
                        karma = rank.takeUnless { isRookie }
                                ?: document.select("p.muted").firstOrNull { it.text().matches(Regex(".*\\(\\d+\\).*")) }?.text().orEmpty(),
                        biography = document.selectFirst("#profile-biography .content")?.text().orEmpty(),
                        biographyHtml = document.selectFirst("#profile-biography .content")?.html().orEmpty(),
                        entryCount = count("entry-count-total"),
                        followerCount = count("user-follower-count"),
                        followingCount = count("user-following-count"),
                        joinedDate = document.selectFirst(".recorddate")?.text().orEmpty(),
                        followAddUrl = document.selectFirst("#buddy-link")?.attr("data-add-url")?.ifBlank { null },
                        followRemoveUrl = document.selectFirst("#buddy-link")?.attr("data-remove-url")?.ifBlank { null },
                        isFollowing = document.selectFirst("#buddy-link")?.attr("data-added") == "true",
                        relations = document.select("a.relation-link[data-add-url], a.mute-icon-link[data-add-url]")
                                .filter { it.id() != "buddy-link" }
                                .mapNotNull { link ->
                                        val label = link.attr("data-add-caption").ifBlank { link.text().trim() }
                                        if (label.isBlank()) return@mapNotNull null
                                        RelationAction(
                                                label = label,
                                                removeLabel = link.attr("data-remove-caption").ifBlank { "$label (geri al)" },
                                                addUrl = link.attr("data-add-url"),
                                                removeUrl = link.attr("data-remove-url"),
                                                isAdded = link.attr("data-added") == "true"
                                        )
                                }
                                .distinctBy { it.addUrl },
                        isRookie = isRookie,
                        badges = document.select("a.user-profile-badge-item").map {
                                parseBadge(it)
                        },
                        // The site includes shared SVG definitions on every page. Only inspect
                        // the profile header so those definitions cannot badge every author.
                        isAdFree = headerNodes.any(::hasSubscriberBadge)
                )
        }

        /** Each list item is one badge in this author's collection; data-owned distinguishes earned badges. */
        fun parseAllBadges(document: Document): List<Badge> = document.select("li.badge-item-otheruser[data-owned]").mapNotNull { item ->
                val image = item.selectFirst("img")
                val badgeLink = item.selectFirst("a") ?: item
                val name = badgeLink.attr("data-name")
                        .ifBlank { item.attr("data-name") }
                        .ifBlank { image?.attr("alt").orEmpty() }
                        .ifBlank { item.selectFirst(".badge-name, .badge-title")?.text().orEmpty() }
                        .ifBlank { item.text().trim() }
                if (name.isBlank()) return@mapNotNull null
                parseBadge(item).copy(
                        name = name,
                        description = badgeLink.attr("data-title").ifBlank { item.attr("data-title") },
                        owned = item.attr("data-owned").equals("true", ignoreCase = true)
                )
        }.distinctBy { it.name }

        private fun parseBadge(element: Element): Badge {
                val image = element.selectFirst("img")
                return Badge(
                        name = element.attr("data-name").ifBlank { image?.attr("alt").orEmpty() }.ifBlank { element.text().trim() },
                        description = element.attr("data-title").ifBlank { image?.attr("title").orEmpty() },
                        imageUrl = image?.attr("src").orEmpty().let { if (it.startsWith("//")) "https:$it" else it }
                )
        }

        /** "görselleri": links to /img/<code> pages, each with its picture as a CSS background. */
        fun parseUserImages(document: Document): List<com.thesozluk.app.model.AuthorImage> =
                document.select("#img-row a[href^=/img/], a[href^=/img/]").mapNotNull { link ->
                        val style = link.selectFirst("[style]")?.attr("style").orEmpty()
                        val thumb = Regex("url\\('?([^')]+)'?\\)").find(style)?.groupValues?.get(1)
                                ?: return@mapNotNull null
                        com.thesozluk.app.model.AuthorImage(link.attr("href"), thumb)
                }.distinctBy { it.ref }

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
