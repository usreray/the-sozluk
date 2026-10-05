package com.thesozluk.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.Placeholder
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.thesozluk.app.settings.AppSettings
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
    /** An image from ekşi's uploader: a /img/<code> page or a direct cdn address */
    data class Image(val ref: String) : EksiLink
}

/** Maps an href from entry HTML to an in-app destination. */
fun eksiLinkFor(href: String, text: String): EksiLink? {
    val url = when {
        href.startsWith("//") -> "https:$href"
        href.startsWith("/") -> "https://eksisozluk.com$href"
        else -> href
    }
    val uri = Uri.parse(url)
    if (uri.host == "cdn.eksisozluk.com" || uri.host == "img.ekstat.com") return EksiLink.Image(url)
    // Entries link uploads as soz.lk/i/<code>, which redirects to eksisozluk.com/img/<code>
    if (uri.host == "soz.lk" && uri.path.orEmpty().startsWith("/i/")) {
        return EksiLink.Image("/img/" + uri.path.orEmpty().removePrefix("/i/").substringBefore("/"))
    }
    if (uri.host?.endsWith("eksisozluk.com") != true) {
        return if (uri.scheme == "http" || uri.scheme == "https") EksiLink.External(url) else null
    }
    val path = uri.path.orEmpty()
    return when {
        path == "/" && uri.getQueryParameter("q") != null -> {
            val query = uri.getQueryParameter("q").orEmpty()
            if (query.startsWith("@")) EksiLink.Author(query.removePrefix("@")) else EksiLink.Search(query)
        }
        path.startsWith("/img/") -> EksiLink.Image(path)
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
    val showLinkAddresses = AppSettings.showLinkAddresses.value
    return remember(html, plain, linkStyle, showLinkAddresses) {
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
                    node is Element && node.tagName() == "sup" -> {
                        val query = node.selectFirst("a[data-query]")?.attr("data-query").orEmpty()
                        val referenceStyle = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.72.em)
                        if (query.isNotBlank()) {
                            val target = query.removePrefix(":").trim().removeSurrounding("(", ")")
                            val link = EksiLink.Search(query)
                            withLink(LinkAnnotation.Clickable(link.toString(), linkStyle) { onLink(link) }) {
                                withStyle(referenceStyle) {
                                    append('(')
                                    append(target)
                                    append(')')
                                }
                            }
                        } else {
                            withStyle(referenceStyle) {
                                append('(')
                                appendNodes(node.childNodes())
                                append(')')
                            }
                        }
                    }
                    node is Element && node.tagName() == "a" -> {
                        // Hidden bkz is a "*" whose target sits in data-query
                        val query = node.attr("data-query")
                        val link = if (query.isNotBlank()) EksiLink.Search(query)
                        else eksiLinkFor(node.attr("href"), node.text())
                        val label = node.text().ifBlank { "*" }
                        if (query.isNotBlank()) {
                            val target = query.removePrefix(":").trim().removeSurrounding("(", ")")
                            val appendReference: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit = {
                                withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.72.em)) {
                                    append(target)
                                }
                            }
                            if (link == null) appendReference()
                            else withLink(LinkAnnotation.Clickable(link.toString(), linkStyle) { onLink(link) }) {
                                appendReference()
                            }
                        } else if (link == null) append(label)
                        else withLink(LinkAnnotation.Clickable(link.toString(), linkStyle) { onLink(link) }) {
                            // bkz / hede stay plain; a link out of the sözlük or to an image shows
                            // what it is, since its text ("görsel", "şurada") looks like a bkz
                            when (link) {
                                is EksiLink.External -> appendInlineContent(LINK_ICON_EXTERNAL, "[link]")
                                is EksiLink.Image -> appendInlineContent(LINK_ICON_IMAGE, "[görsel]")
                                else -> Unit
                            }
                            append(label)
                            if (showLinkAddresses && link is EksiLink.External) append(" (${link.url})")
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

private const val LINK_ICON_EXTERNAL = "link_external"
private const val LINK_ICON_IMAGE = "link_image"

/** The icons [rememberEntryText] puts in front of links; pass to Text(inlineContent = ...). */
@Composable
fun rememberEntryInlineContent(): Map<String, InlineTextContent> {
    val tint = MaterialTheme.colorScheme.primary
    return remember(tint) {
        fun icon(vector: ImageVector) = InlineTextContent(
            Placeholder(width = 1.1.em, height = 1.em, placeholderVerticalAlign = PlaceholderVerticalAlign.Center)
        ) {
            Icon(vector, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize().padding(end = 2.dp))
        }
        mapOf(
            LINK_ICON_EXTERNAL to icon(Icons.AutoMirrored.Rounded.OpenInNew),
            LINK_ICON_IMAGE to icon(Icons.Rounded.Image)
        )
    }
}

private fun AnnotatedString.trimmed(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return if (end <= start) this else subSequence(start, end)
}
