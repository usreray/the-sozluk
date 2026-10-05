package com.thesozluk.app.ui.navigation

import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavController
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.ui.components.BkzPreview
import com.thesozluk.app.ui.components.EksiLink

/**
 * Opens a link tapped inside an entry: topics, entries and authors in-app, the rest outside.
 * A (bkz) first shows a preview sheet when that is on in settings, unless [preview] is false.
 */
fun NavController.openEksiLink(link: EksiLink, uriHandler: UriHandler, preview: Boolean = true) {
    if (preview && AppSettings.bkzPreview.value && (link is EksiLink.Search || link is EksiLink.TopicPath)) {
        BkzPreview.link.value = link
        return
    }
    when (link) {
        is EksiLink.Search -> navigate(Screen.TopicDetail.createRoute(link.query, ""))
        is EksiLink.TopicPath -> navigate(Screen.TopicDetail.createRoute(link.title, link.path))
        is EksiLink.EntryLink -> navigate(Screen.TopicDetail.createRoute("#${link.id}", "/entry/${link.id}"))
        is EksiLink.Author -> navigate(Screen.Author.createRoute(link.nick))
        is EksiLink.External -> runCatching { uriHandler.openUri(link.url) }
        is EksiLink.Image -> {
            if (link.ref !in com.thesozluk.app.ui.screens.ImageGallery.refs) {
                com.thesozluk.app.ui.screens.ImageGallery.refs = emptyList()
            }
            navigate(Screen.Image.createRoute(link.ref))
        }
    }
}
