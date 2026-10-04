package com.thesozluk.app

import com.thesozluk.app.ui.navigation.openEksiLink
import com.thesozluk.app.ui.components.EksiLink
import com.thesozluk.app.ui.components.eksiLinkFor
import androidx.compose.ui.platform.LocalUriHandler
import com.thesozluk.app.ui.navigation.TabReselect
import com.thesozluk.app.viewmodel.HomeTabRequest
import com.thesozluk.app.viewmodel.HomeCategory
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
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
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.ui.components.FloatingNavBar
import com.thesozluk.app.ui.components.FloatingNavBarInset
import com.thesozluk.app.ui.components.LocalBottomBarInset
import com.thesozluk.app.ui.components.NavDestination
import com.thesozluk.app.ui.navigation.Navigation
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.ui.navigation.TopicTabs
import com.thesozluk.app.ui.theme.TheSozlukTheme
import com.thesozluk.app.ui.theme.isAppInDarkTheme
import com.thesozluk.app.notify.Notifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.graphics.Brush
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EksiSession.init(this)
        AppSettings.init(this)
        TopicTabs.init(this, AppSettings.rememberOpenTabs.value)
        com.thesozluk.app.settings.Drafts.init(this)
        com.thesozluk.app.settings.ReadingHistory.init(this)
        com.thesozluk.app.settings.EntryBookmarks.init(this)
        com.thesozluk.app.settings.SearchHistory.init(this)
        com.thesozluk.app.data.offline.OfflineStore.init(this)
        getSharedPreferences("drive_backup", MODE_PRIVATE).takeIf { it.all.isNotEmpty() }?.let { prefs ->
            androidx.work.WorkManager.getInstance(this).cancelUniqueWork("google-drive-backup")
            prefs.edit().clear().apply()
        }
        Notifier.schedule(this, AppSettings.notifications.value && EksiSession.isLoggedIn.value)
        // Logged in on Android 13+: ask once for the permission the message / olay alerts need
        if (EksiSession.isLoggedIn.value && AppSettings.notifications.value && !Notifier.canNotify(this) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AppSettings.takeNotificationPrompt()
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        pendingOpen.value = openTarget(intent)
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
            TheSozlukTheme {
                MainScreen(pendingOpen)
            }
        }
    }

    // A notification tapped while the app is open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTarget(intent)?.let { pendingOpen.value = it }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            AppSettings.setNotifications(false)
            Notifier.schedule(this, false)
        }
    }

    /** Where a tapped notification wants to go ("messages" / "olay"), until handled */
    private val pendingOpen = mutableStateOf<String?>(null)

    /** What an intent asks for: a notification / shortcut target, or "link:<url>" for an ekşi link. */
    private fun openTarget(intent: Intent?): String? {
        intent ?: return null
        intent.getStringExtra(Notifier.EXTRA_TOPIC_PATH)?.let { path ->
            val title = intent.getStringExtra("topic_title").orEmpty()
            return "topic:${android.net.Uri.encode(title)}|${android.net.Uri.encode(path)}"
        }
        intent.getStringExtra(Notifier.EXTRA_OPEN)?.let { return it }
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null && data.scheme?.startsWith("http") == true) return "link:$data"
        return null
    }
}

private val topLevelDestinations = listOf(
    NavDestination(Screen.Home.route, "akış", Icons.Rounded.Home, Icons.Outlined.Home),
    NavDestination(Screen.Search.route, "ara", Icons.Rounded.Search, Icons.Rounded.Search),
    NavDestination(Screen.Profile.route, "profil", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun MainScreen(pendingOpen: MutableState<String?>) {
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(pendingOpen.value) {
        val target = pendingOpen.value
        when {
            target?.startsWith("topic:") == true -> {
                val (title, path) = target.removePrefix("topic:").split('|', limit = 2)
                    .let { android.net.Uri.decode(it[0]) to android.net.Uri.decode(it.getOrElse(1) { "" }) }
                navController.navigate(Screen.TopicDetail.createRoute(title, path))
            }
            // A link from another app: open it like a link inside an entry
            target?.startsWith("link:") == true -> {
                val url = target.removePrefix("link:")
                val link = eksiLinkFor(url, "")
                if (link != null && link !is EksiLink.External) navController.openEksiLink(link, uriHandler)
            }
            // App icon shortcut "ara": the search tab with its field open
            target == "search" -> {
                navController.navigate(Screen.Search.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                kotlinx.coroutines.delay(350)
                TabReselect.reselect(Screen.Search.route)
            }
        }
        when (target) {
            Notifier.OPEN_MESSAGES -> navController.navigate(Screen.Messages.createRoute())
            // The olay tab on the home screen (or the olay list if that tab is turned off)
            Notifier.OPEN_EVENTS -> {
                HomeTabRequest.tab.value = HomeCategory.Olay
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        pendingOpen.value = null
    }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // Reading, profiles, settings and login are full screen; the bar belongs to the tabs
    val showBar = topLevelDestinations.any { it.route == currentRoute }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CompositionLocalProvider(LocalBottomBarInset provides if (showBar) FloatingNavBarInset else 0.dp) {
            Navigation(navController)
        }
        // Edge to edge: content scrolls under the status bar, so the bar gets a soft scrim of the
        // surface color that keeps the clock and icons readable (not over full-screen images)
        if (currentRoute?.startsWith(Screen.Image.route) != true) {
            val surface = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.85f), surface.copy(alpha = 0f))))
            )
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
                    if (destination.route == currentRoute) {
                        TabReselect.reselect(destination.route)
                    } else {
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
