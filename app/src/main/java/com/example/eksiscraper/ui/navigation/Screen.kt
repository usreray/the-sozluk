package com.example.eksiscraper.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Settings : Screen("settings")
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
