package com.thesozluk.app.ui.navigation

import com.thesozluk.app.ui.screens.LibraryScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavBackStackEntry
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
import com.thesozluk.app.ui.screens.AuthorScreen
import com.thesozluk.app.ui.screens.ChannelsScreen
import com.thesozluk.app.ui.screens.MessageThreadScreen
import com.thesozluk.app.ui.screens.MessagesScreen
import com.thesozluk.app.ui.screens.HomeScreen
import com.thesozluk.app.ui.screens.LoginScreen
import com.thesozluk.app.ui.screens.ProfileScreen
import com.thesozluk.app.ui.screens.SearchScreen
import com.thesozluk.app.ui.screens.SettingsScreen
import com.thesozluk.app.ui.screens.ImageViewerScreen
import com.thesozluk.app.ui.screens.TopicDetailScreen

private val tabRoutes = setOf(Screen.Home.route, Screen.Search.route, Screen.Profile.route)

/** Switching bottom-bar tabs, as opposed to opening or leaving a screen. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs(): Boolean =
    initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

// Between tabs: a quick "fade through" with a slight zoom
private fun tabEnter(): EnterTransition =
    fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(280), initialScale = 0.94f)

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { if (betweenTabs()) tabEnter() else slideIntoContainer(SlideDirection.Start, tween(280)) },
        exitTransition = {
            if (betweenTabs()) fadeOut(tween(90))
            else slideOutOfContainer(SlideDirection.Start, tween(280)) + fadeOut(tween(280), targetAlpha = 0.6f)
        },
        popEnterTransition = {
            if (betweenTabs()) tabEnter()
            else slideIntoContainer(SlideDirection.End, tween(280))
        },
        popExitTransition = {
            if (betweenTabs()) fadeOut(tween(90))
            else slideOutOfContainer(SlideDirection.End, tween(280)) + fadeOut(tween(280), targetAlpha = 0.6f)
        }
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Search.route) { SearchScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(
            route = Screen.Channels.pattern,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            ),
        ) { backStackEntry ->
            ChannelsScreen(
                path = backStackEntry.arguments?.getString("path").orEmpty(),
                name = backStackEntry.arguments?.getString("name").orEmpty(),
                navController = navController
            )
        }

        composable(
            Screen.Messages.pattern,
            arguments = listOf(navArgument("to") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            MessagesScreen(navController, composeTo = backStackEntry.arguments?.getString("to").orEmpty())
        }

        composable(
            Screen.MessageThread.pattern,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType; defaultValue = "" },
                navArgument("nick") { type = NavType.StringType; defaultValue = "" }
            ),
        ) { backStackEntry ->
            MessageThreadScreen(
                threadId = backStackEntry.arguments?.getString("id").orEmpty(),
                nick = backStackEntry.arguments?.getString("nick").orEmpty(),
                navController = navController
            )
        }

        composable(
            route = Screen.Author.pattern,
            arguments = listOf(navArgument("nick") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            AuthorScreen(nick = backStackEntry.arguments?.getString("nick").orEmpty(), navController = navController)
        }

        // Images fade in over the current screen
        composable(
            route = Screen.Image.pattern,
            arguments = listOf(navArgument("ref") { type = NavType.StringType; defaultValue = "" }),
            enterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) { backStackEntry ->
            ImageViewerScreen(ref = backStackEntry.arguments?.getString("ref").orEmpty(), navController = navController)
        }

        composable(
            Screen.Settings.pattern,
            arguments = listOf(navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            SettingsScreen(navController, backStackEntry.arguments?.getString("section"))
        }

        composable(
            Screen.Library.pattern,
            arguments = listOf(navArgument("kind") { type = NavType.StringType; defaultValue = "History" })
        ) { backStackEntry ->
            LibraryScreen(
                kind = com.thesozluk.app.ui.screens.LibraryKind.entries
                    .firstOrNull { it.name == backStackEntry.arguments?.getString("kind") }
                    ?: com.thesozluk.app.ui.screens.LibraryKind.History,
                navController = navController
            )
        }

        // Login rises from the bottom like a sheet
        composable(
            Screen.Login.route,
            enterTransition = { slideInVertically { it } + fadeIn() },
            popExitTransition = { slideOutVertically { it } + fadeOut() }
        ) { LoginScreen(navController) }

        composable(
            route = Screen.TopicDetail.pattern,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("page") { type = NavType.IntType; defaultValue = 1 },
                navArgument("compose") { type = NavType.BoolType; defaultValue = false }
            ),
        ) { backStackEntry ->
            TopicDetailScreen(
                title = backStackEntry.arguments?.getString("title").orEmpty(),
                url = backStackEntry.arguments?.getString("url").orEmpty(),
                startPage = backStackEntry.arguments?.getInt("page") ?: 1,
                openComposer = backStackEntry.arguments?.getBoolean("compose") ?: false,
                navController = navController
            )
        }
    }
}
