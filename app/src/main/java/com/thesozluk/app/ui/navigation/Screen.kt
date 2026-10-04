package com.thesozluk.app.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Settings : Screen("settings") {
        const val pattern = "settings?section={section}"
        fun createRoute(section: String? = null): String = if (section == null) "settings" else "settings?section=$section"
    }
    /** History, saved entries and offline topics */
    object Library : Screen("library") {
        const val pattern = "library?kind={kind}"
        fun createRoute(kind: com.thesozluk.app.ui.screens.LibraryKind): String = "library?kind=${kind.name}"
    }
    object Channels : Screen("channels") {
        const val pattern = "channels?path={path}&name={name}"
        fun createRoute(path: String, name: String): String =
            "channels?path=${Uri.encode(path)}&name=${Uri.encode(name)}"
    }
    object Messages : Screen("messages") {
        const val pattern = "messages?to={to}"
        fun createRoute(to: String = ""): String = "messages?to=${Uri.encode(to)}"
    }
    object MessageThread : Screen("message_thread") {
        const val pattern = "message_thread?id={id}&nick={nick}"
        fun createRoute(id: String, nick: String): String =
            "message_thread?id=${Uri.encode(id)}&nick=${Uri.encode(nick)}"
    }
    object Image : Screen("image") {
        const val pattern = "image?ref={ref}"
        fun createRoute(ref: String): String = "image?ref=${Uri.encode(ref)}"
    }
    object Author : Screen("author") {
        const val pattern = "author?nick={nick}"
        fun createRoute(nick: String): String = "author?nick=${Uri.encode(nick.trim().removePrefix("@"))}"
    }
    object TopicDetail : Screen("topic_detail") {
        const val pattern = "topic_detail?title={title}&url={url}&page={page}&compose={compose}"

        // Uri.encode keeps '/', '?' and spaces intact through Navigation's argument decoding
        fun createRoute(title: String, url: String, page: Int = 1, compose: Boolean = false): String =
            "topic_detail?title=${Uri.encode(title)}&url=${Uri.encode(url)}&page=$page&compose=$compose"
    }
}
