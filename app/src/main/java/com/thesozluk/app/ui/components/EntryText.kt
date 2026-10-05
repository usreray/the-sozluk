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

/** "--- spoiler ---" as the site renders it: the word is a bkz between dashes. */
private val SPOILER_MARKER = Regex(
    """-{3}(?:\s|&nbsp;)*<a\b[^>]*>(?:\s|&nbsp;)*spoiler(?:\s|&nbsp;)*</a>(?:\s|&nbsp;)*-{3}""",
    RegexOption.IGNORE_CASE
)
private const val SPOILER_TAG = "spoiler-mark"

/**
 * Builds the entry body with tappable links; falls back to plain text without HTML.
 * With [onToggleSpoilers], text between spoiler markers is covered while [spoilersHidden]
 * and tapping the markers or the covered text calls it.
 */
@Composable
fun rememberEntryText(
    html: String,
    plain: String,
    onLink: (EksiLink) -> Unit,
    spoilersHidden: Boolean = false,
    onToggleSpoilers: (() -> Unit)? = null
): AnnotatedString {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    )
    val coverColor = MaterialTheme.colorScheme.onSurfaceVariant
    val showLinkAddresses = AppSettings.showLinkAddresses.value
    return remember(html, plain, linkStyle, showLinkAddresses, spoilersHidden, onToggleSpoilers != null, coverColor) {
        if (html.isBlank()) AnnotatedString(plain)
        else buildAnnotatedString {
            // The site indents lines after <br>; drop that so paragraphs start flush
            var lineStart = true
            var inSpoiler = false
            // Covered text has no links of its own: one tap anywhere on it reveals the spoiler
            fun covered() = inSpoiler && spoilersHidden
            // Only the words get the cover; spaces and line breaks around them stay open,
            // so the bar doesn't start right after the marker's dashes
            fun appendCovered(text: String) {
                val start = text.indexOfFirst { !it.isWhitespace() }
                if (start < 0) {
                    append(text)
                    return
                }
                val end = text.indexOfLast { !it.isWhitespace() } + 1
                append(text.substring(0, start))
                withLink(LinkAnnotation.Clickable("spoiler-reveal") { onToggleSpoilers?.invoke() }) {
                    withStyle(SpanStyle(color = coverColor, background = coverColor)) {
                        append(text.substring(start, end))
                    }
                }
                append(text.substring(end))
            }
            fun appendNodes(nodes: List<Node>) {
                for (node in nodes) when {
                    node is Element && node.tagName() == SPOILER_TAG -> {
                        withLink(LinkAnnotation.Clickable("spoiler", linkStyle) { onToggleSpoilers?.invoke() }) {
                            append("--- spoiler ---")
                        }
                        inSpoiler = !inSpoiler
                        lineStart = false
                    }
                    covered() && node is Element && node.tagName() != "br" && node.select("br").isEmpty() -> {
                        appendCovered(node.text().ifBlank { "*" })
                        lineStart = false
                    }
                    node is TextNode -> {
                        val text = if (lineStart) node.text().trimStart() else node.text()
                        if (text.isNotEmpty()) {
                            if (covered()) appendCovered(text) else append(text)
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
            val source = if (onToggleSpoilers != null) html.replace(SPOILER_MARKER, "<$SPOILER_TAG></$SPOILER_TAG>") else html
            appendNodes(Jsoup.parseBodyFragment(source).body().childNodes())
        }.trimmed()
    }
}

/**
 * Turns what the user typed in ekşi's markup into the HTML the site would show, so the
 * composer can preview it with [rememberEntryText]: (bkz: x), `hede`, `:gizli bkz`,
 * [http://adres metin] and line breaks.
 */
fun entryMarkupToHtml(raw: String): String {
    fun esc(text: String) = org.jsoup.nodes.Entities.escape(text)
    fun href(target: String) = when {
        target.matches(Regex("#\\d+")) -> "/entry/${target.drop(1)}"
        target.startsWith("@") -> "/biri/${Uri.encode(target.drop(1))}"
        else -> "/?q=${Uri.encode(target)}"
    }
    fun bkz(target: String) = "<a class=\"b\" href=\"${esc(href(target.trim()))}\">${esc(target.trim())}</a>"
    val markup = Regex("""\(bkz: ?([^)]+)\)|`:([^`]+)`|`([^`]+)`|\[(https?://[^\s\]]+)(?: ([^\]]+))?]""")
    val html = StringBuilder()
    var last = 0
    for (match in markup.findAll(raw)) {
        html.append(esc(raw.substring(last, match.range.first)))
        val (bkzTarget, hidden, hede, url, label) = match.destructured
        html.append(
            when {
                bkzTarget.isNotEmpty() -> "(bkz: ${bkz(bkzTarget)})"
                hidden.isNotEmpty() -> "<sup class=\"ab\"><a data-query=\"${esc(hidden.trim())}\">*</a></sup>"
                hede.isNotEmpty() -> bkz(hede)
                else -> "<a href=\"${esc(url)}\">${esc(label.ifBlank { url })}</a>"
            }
        )
        last = match.range.last + 1
    }
    html.append(esc(raw.substring(last)))
    return html.toString().replace("\n", "<br>")
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
