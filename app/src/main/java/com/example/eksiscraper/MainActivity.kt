package com.example.eksiscraper

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.settings.AppSettings
import com.example.eksiscraper.ui.components.FloatingNavBar
import com.example.eksiscraper.ui.components.FloatingNavBarInset
import com.example.eksiscraper.ui.components.LocalBottomBarInset
import com.example.eksiscraper.ui.components.NavDestination
import com.example.eksiscraper.ui.navigation.Navigation
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.ui.theme.EksiScraperTheme
import com.example.eksiscraper.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EksiSession.init(this)
        AppSettings.init(this)
        enableEdgeToEdge()
        setContent {
            // Status / navigation bar icons follow the app theme, not only the system one
            val dark = isAppInDarkTheme()
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
                onDispose {}
            }
            EksiScraperTheme {
                MainScreen()
            }
        }
    }
}

private val topLevelDestinations = listOf(
    NavDestination(Screen.Home.route, "Akış", Icons.Rounded.Home, Icons.Outlined.Home),
    NavDestination(Screen.Search.route, "Ara", Icons.Rounded.Search, Icons.Rounded.Search),
    NavDestination(Screen.Profile.route, "Profil", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Reading, profiles, settings and login are full screen; the bar belongs to the tabs
    val showBar = topLevelDestinations.any { it.route == currentRoute }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CompositionLocalProvider(LocalBottomBarInset provides if (showBar) FloatingNavBarInset else 0.dp) {
            Navigation(navController)
        }
        AnimatedVisibility(
            visible = showBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
        ) {
            FloatingNavBar(
                destinations = topLevelDestinations,
                selectedRoute = currentRoute,
                onSelect = { destination ->
                    if (destination.route != currentRoute) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
