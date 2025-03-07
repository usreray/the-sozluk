package com.example.eksiscraper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eksiscraper.ui.navigation.Navigation
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.ui.theme.EksiScraperTheme
import com.example.eksiscraper.viewmodel.EksiViewModel
import androidx.compose.material.icons.filled.KeyboardArrowRight
import kotlin.math.ceil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EksiScraperTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: EksiViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(
                        modifier = Modifier
                            .fillMaxSize()
                .padding(innerPadding)
        ) {
            Navigation(navController, viewModel)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem(
            title = "Home",
            icon = Icons.Default.Home,
            route = Screen.Home.route
        ),
        NavigationItem(
            title = "Search",
            icon = Icons.Default.Search,
            route = Screen.Search.route
        ),
        NavigationItem(
            title = "You",
            icon = Icons.Default.Person,
            route = Screen.Profile.route
        )
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Get ViewModel reference
    val viewModel: EksiViewModel = viewModel()
    
    NavigationBar(
        modifier = Modifier.clip(RectangleShape),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route || 
                (currentRoute?.startsWith(Screen.TopicDetail.route.substringBefore("{")) == true && 
                 item.route == Screen.Home.route)
            
            NavigationBarItem(
                icon = {
                        Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = selected,
                onClick = {
                    // Check if we're on a topic detail screen
                    val isOnTopicDetail = currentRoute?.startsWith(Screen.TopicDetail.route.substringBefore("{")) == true
                    
                    // Special handling for Home tab
                    if (item.route == Screen.Home.route) {
                        // If we're already on the Home tab (which includes topic detail)
                        if (selected) {
                            println("MainActivity: Already on Home tab, resetting to home page")
                            
                            // If we're on a topic detail screen, navigate back to home
                            if (isOnTopicDetail) {
                                viewModel.resetHomeScreen()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                // If we're already on the home screen, just scroll to top
                                // without reloading data
                                println("MainActivity: Already on home screen, scrolling to top")
                                viewModel.scrollHomeToTop()
                            }
                            return@NavigationBarItem
                        }
                        
                        // If we're not on the Home tab but have a selected topic
                        if (viewModel.selectedTopic.value != null) {
                            println("MainActivity: Topic is selected, navigating to topic detail instead of home")
                            
                            // Find the index of the selected topic
                            val topicIndex = viewModel.topics.value.indexOfFirst { 
                                it.title == viewModel.selectedTopic.value?.title 
                            }
                            
                            if (topicIndex != -1) {
                                // Navigate to topic detail
                                val topicDetailRoute = "${Screen.TopicDetail.route.substringBefore("{")}" +
                                        "$topicIndex"
                                
                                // Set the active screen to topic_detail
                                viewModel.setActiveScreen("topic_detail")
                                
                                navController.navigate(topicDetailRoute) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                                return@NavigationBarItem
                            }
                        }
                        
                        // If no topic is selected or we couldn't find the topic index,
                        // navigate to home as usual
                        viewModel.setActiveScreen("home")
                        
                        // Navigate to home
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                        return@NavigationBarItem
                    }
                    
                    // For other tabs, check if we're already on that tab
                    if (currentRoute != item.route) {
                        println("MainActivity: Navigating to ${item.route}")
                        
                        // If navigating to Search or Profile screens
                        if (item.route == Screen.Search.route || item.route == Screen.Profile.route) {
                            // Don't clear the selected topic when navigating to Search or Profile
                            // Just set the active screen
                            viewModel.setActiveScreen(item.route.substringBefore("/"))
                        }
                        
                        // Navigate to the destination
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    } else {
                        // If already on the screen and clicked again, reset to initial state
                        when (item.route) {
                            Screen.Search.route -> viewModel.clearSearch()
                            Screen.Profile.route -> viewModel.resetProfileScreen()
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)
