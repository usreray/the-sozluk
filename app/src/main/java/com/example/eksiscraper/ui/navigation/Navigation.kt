package com.example.eksiscraper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eksiscraper.ui.screens.HomeScreen
import com.example.eksiscraper.ui.screens.ProfileScreen
import com.example.eksiscraper.ui.screens.SearchScreen
import com.example.eksiscraper.ui.screens.TopicDetailScreen
import com.example.eksiscraper.viewmodel.EksiViewModel

@Composable
fun Navigation(
    navController: NavHostController,
    viewModel: EksiViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController, viewModel)
        }
        
        composable(Screen.Search.route) {
            SearchScreen(viewModel)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
        
        composable(
            route = Screen.TopicDetail.route,
            arguments = listOf(
                navArgument("topicIndex") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val topicIndex = backStackEntry.arguments?.getInt("topicIndex") ?: 0
            TopicDetailScreen(
                topicIndex = topicIndex,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
} 