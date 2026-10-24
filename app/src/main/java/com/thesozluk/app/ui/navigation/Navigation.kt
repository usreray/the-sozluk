package com.thesozluk.app.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.scaleOut
import androidx.compose.animation.ExitTransition
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

private val tabRoutes = setOf(Screen.Home.route, Screen.Search.route, Screen.Random.route, Screen.Profile.route)

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
        // Going back (the back gesture plays this with the finger): the page below is already in
        // place, and the leaving page moves itself off in BackSwipe; the host only keeps it drawn
        // Between tabs (back from profil to akış, by gesture or tab): the leaving tab sinks back and
        // fades evenly over the whole move, so the gesture can follow it, while the tab below
        // rises into place (BackSwipe's reveal). No delays: a seeked delay reads as a stall.
        popEnterTransition = { EnterTransition.None },
        popExitTransition = {
            if (betweenTabs()) fadeOut(tween(300, easing = LinearEasing)) + scaleOut(tween(300, easing = LinearEasing), targetScale = 0.9f)
            else ExitTransition.KeepUntilTransitionsFinished
        }
    ) {
        screen(navController, Screen.Home.route, backSwipe = false) { HomeScreen(navController) }
        screen(navController, Screen.Search.route, backSwipe = false) { SearchScreen(navController) }
        screen(navController, Screen.Random.route, backSwipe = false) { com.thesozluk.app.ui.screens.RandomScreen(navController) }
        screen(navController, Screen.Profile.route, backSwipe = false) { ProfileScreen(navController) }
        screen(
            navController,
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

        screen(
            navController,
            Screen.Messages.pattern,
            arguments = listOf(navArgument("to") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            MessagesScreen(navController, composeTo = backStackEntry.arguments?.getString("to").orEmpty())
        }

        screen(
            navController,
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

        screen(
            navController,
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

        screen(
            navController,
            Screen.Settings.pattern,
            arguments = listOf(navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            SettingsScreen(navController, backStackEntry.arguments?.getString("section"))
        }

        screen(
            navController,
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
        screen(
            navController,
            Screen.Login.route,
            enterTransition = { slideInVertically { it } + fadeIn() },
            popExitTransition = { slideOutVertically { it } + fadeOut() },
            backSwipe = false
        ) { LoginScreen(navController) }

        screen(
            navController,
            route = Screen.TopicDetail.pattern,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("page") { type = NavType.IntType; defaultValue = 1 },
                navArgument("compose") { type = NavType.BoolType; defaultValue = false },
                navArgument("focus") { type = NavType.StringType; defaultValue = "" }
            ),
        ) { backStackEntry ->
            TopicDetailScreen(
                title = backStackEntry.arguments?.getString("title").orEmpty(),
                url = backStackEntry.arguments?.getString("url").orEmpty(),
                startPage = backStackEntry.arguments?.getInt("page") ?: 1,
                openComposer = backStackEntry.arguments?.getBoolean("compose") ?: false,
                focusEntry = backStackEntry.arguments?.getString("focus").orEmpty(),
                navController = navController
            )
        }
    }
}
