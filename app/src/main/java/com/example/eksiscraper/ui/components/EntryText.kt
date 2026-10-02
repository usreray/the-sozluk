package com.example.eksiscraper.ui.components

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Where a link inside an entry leads. */
sealed interface EksiLink {
    /** (bkz: x) and hidden bkz: ekşi resolves the query to a topic */
    data class Search(val query: String) : EksiLink
    data class Author(val nick: String) : EksiLink
    data class EntryLink(val id: String) : EksiLink
    data class TopicPath(val path: String, val title: String) : EksiLink
    data class External(val url: String) : EksiLink
}

/** Maps an href from entry HTML to an in-app destination. */
fun eksiLinkFor(href: String, text: String): EksiLink? {
    val url = when {
        href.startsWith("//") -> "https:$href"
        href.startsWith("/") -> "https://eksisozluk.com$href"
        else -> href
    }
    val uri = Uri.parse(url)
    if (uri.host?.endsWith("eksisozluk.com") != true) {
        return if (uri.scheme == "http" || uri.scheme == "https") EksiLink.External(url) else null
    }
    val path = uri.path.orEmpty()
    return when {
        path == "/" && uri.getQueryParameter("q") != null -> {
            val query = uri.getQueryParameter("q").orEmpty()
            if (query.startsWith("@")) EksiLink.Author(query.removePrefix("@")) else EksiLink.Search(query)
        }
        path.startsWith("/entry/") -> EksiLink.EntryLink(path.removePrefix("/entry/").substringBefore("/"))
        path.startsWith("/biri/") -> EksiLink.Author(path.removePrefix("/biri/").substringBefore("/"))
        path.contains("--") -> EksiLink.TopicPath(path, text)
        else -> EksiLink.External(url)
    }
}

/** Builds the entry body with tappable links; falls back to plain text without HTML. */
@Composable
fun rememberEntryText(html: String, plain: String, onLink: (EksiLink) -> Unit): AnnotatedString {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    )
    return remember(html, plain, linkStyle) {
        if (html.isBlank()) AnnotatedString(plain)
        else buildAnnotatedString {
            // The site indents lines after <br>; drop that so paragraphs start flush
            var lineStart = true
            fun appendNodes(nodes: List<Node>) {
                for (node in nodes) when {
                    node is TextNode -> {
                        val text = if (lineStart) node.text().trimStart() else node.text()
                        if (text.isNotEmpty()) {
                            append(text)
                            lineStart = false
                        }
                    }
                    node is Element && node.tagName() == "br" -> {
                        append('\n')
                        lineStart = true
                    }
                    node is Element && node.tagName() == "a" -> {
                        // Hidden bkz is a "*" whose target sits in data-query
                        val query = node.attr("data-query")
                        val link = if (query.isNotBlank()) EksiLink.Search(query)
                        else eksiLinkFor(node.attr("href"), node.text())
                        val label = node.text().ifBlank { "*" }
                        if (link == null) append(label)
                        else withLink(LinkAnnotation.Clickable(link.toString(), linkStyle) { onLink(link) }) {
                            append(label)
                        }
                        lineStart = false
                    }
                    node is Element -> appendNodes(node.childNodes())
                }
            }
            appendNodes(Jsoup.parseBodyFragment(html).body().childNodes())
        }.trimmed()
    }
}

private fun AnnotatedString.trimmed(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return if (end <= start) this else subSequence(start, end)
}
