package com.example.eksiscraper.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Settings : Screen("settings")
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
    object Author : Screen("author") {
        const val pattern = "author?nick={nick}"
        fun createRoute(nick: String): String = "author?nick=${Uri.encode(nick)}"
    }
    object TopicDetail : Screen("topic_detail") {
        const val pattern = "topic_detail?title={title}&url={url}"

        // Uri.encode keeps '/', '?' and spaces intact through Navigation's argument decoding
        fun createRoute(title: String, url: String): String =
            "topic_detail?title=${Uri.encode(title)}&url=${Uri.encode(url)}"
    }
}
