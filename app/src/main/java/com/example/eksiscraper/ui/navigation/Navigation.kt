package com.example.eksiscraper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eksiscraper.ui.screens.HomeScreen
import com.example.eksiscraper.ui.screens.ProfileScreen
import com.example.eksiscraper.ui.screens.SearchScreen
import com.example.eksiscraper.ui.screens.TopicDetailScreen

@Composable
fun Navigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        composable(Screen.Search.route) {
            SearchScreen(navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        
        composable(
            route = "${Screen.TopicDetail.route}/{title}/{url}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val url = backStackEntry.arguments?.getString("url") ?: ""
            TopicDetailScreen(
                title = title,
                url = url,
                navController = navController
            )
        }
    }
}