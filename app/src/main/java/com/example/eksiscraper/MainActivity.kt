package com.example.eksiscraper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.navigation.Navigation
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.ui.theme.EksiScraperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EksiSession.init(this)
        enableEdgeToEdge()
        setContent {
            EksiScraperTheme {
                MainScreen()
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Screen.Home.route, "Akış", Icons.Rounded.Home, Icons.Outlined.Home),
    TopLevelDestination(Screen.Search.route, "Ara", Icons.Rounded.Search, Icons.Rounded.Search),
    TopLevelDestination(Screen.Profile.route, "Profil", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Reading (topic) and login are full screen; the bar only belongs to the top-level tabs
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar, enter = expandVertically(), exit = shrinkVertically()) {
                BottomBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        // Screens draw their own top bars; only the bottom bar's space is taken here
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            Navigation(navController)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomBar(navController: NavController, currentRoute: String?) {
    ShortNavigationBar {
        topLevelDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            ShortNavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
