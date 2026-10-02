package com.example.eksiscraper.ui.navigation

import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavController
import com.example.eksiscraper.ui.components.EksiLink

/** Opens a link tapped inside an entry: topics, entries and authors in-app, the rest outside. */
fun NavController.openEksiLink(link: EksiLink, uriHandler: UriHandler) {
    when (link) {
        is EksiLink.Search -> navigate(Screen.TopicDetail.createRoute(link.query, ""))
        is EksiLink.TopicPath -> navigate(Screen.TopicDetail.createRoute(link.title, link.path))
        is EksiLink.EntryLink -> navigate(Screen.TopicDetail.createRoute("#${link.id}", "/entry/${link.id}"))
        is EksiLink.Author -> navigate(Screen.Author.createRoute(link.nick))
        is EksiLink.External -> runCatching { uriHandler.openUri(link.url) }
    }
}
