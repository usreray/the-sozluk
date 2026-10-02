package com.example.eksiscraper.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object TopicDetail : Screen("topic_detail/{topicIndex}") {
        fun createRoute(topicIndex: Int): String {
            return "topic_detail/$topicIndex"
        }
    }
} 