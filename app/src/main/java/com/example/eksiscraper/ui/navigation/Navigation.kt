package com.example.eksiscraper.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eksiscraper.ui.screens.AuthorScreen
import com.example.eksiscraper.ui.screens.HomeScreen
import com.example.eksiscraper.ui.screens.LoginScreen
import com.example.eksiscraper.ui.screens.ProfileScreen
import com.example.eksiscraper.ui.screens.SearchScreen
import com.example.eksiscraper.ui.screens.SettingsScreen
import com.example.eksiscraper.ui.screens.TopicDetailScreen

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        // Between tabs: a quick "fade through" with a slight zoom
        enterTransition = { fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(280), initialScale = 0.94f) },
        exitTransition = { fadeOut(tween(90)) },
        popEnterTransition = { fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(280), initialScale = 0.94f) },
        popExitTransition = { fadeOut(tween(90)) }
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Search.route) { SearchScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }

        composable(
            route = Screen.Author.pattern,
            arguments = listOf(navArgument("nick") { type = NavType.StringType; defaultValue = "" }),
            enterTransition = { slideIntoContainer(SlideDirection.Start, tween(320)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(280)) + fadeOut(tween(200)) }
        ) { backStackEntry ->
            AuthorScreen(nick = backStackEntry.arguments?.getString("nick").orEmpty(), navController = navController)
        }

        composable(
            Screen.Settings.route,
            enterTransition = { slideIntoContainer(SlideDirection.Start, tween(320)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(280)) + fadeOut(tween(200)) }
        ) { SettingsScreen(navController) }

        // Login rises from the bottom like a sheet
        composable(
            Screen.Login.route,
            enterTransition = { slideInVertically { it } + fadeIn() },
            popExitTransition = { slideOutVertically { it } + fadeOut() }
        ) { LoginScreen(navController) }

        // A topic slides in over the list and back out the same way
        composable(
            route = Screen.TopicDetail.pattern,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideIntoContainer(SlideDirection.Start, tween(320)) + fadeIn(tween(200)) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(280)) + fadeOut(tween(200)) }
        ) { backStackEntry ->
            TopicDetailScreen(
                title = backStackEntry.arguments?.getString("title").orEmpty(),
                url = backStackEntry.arguments?.getString("url").orEmpty(),
                navController = navController
            )
        }
    }
}
