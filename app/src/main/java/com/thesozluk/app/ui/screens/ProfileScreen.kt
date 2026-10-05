package com.thesozluk.app.ui.screens

import androidx.compose.foundation.Image
import com.thesozluk.app.ui.components.BrandIcons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.AutoStories
import com.thesozluk.app.data.offline.OfflineStore
import com.thesozluk.app.settings.ReadingHistory
import com.thesozluk.app.settings.EntryBookmarks
import com.thesozluk.app.ui.navigation.TabReselect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.DeleteOutline
import com.thesozluk.app.settings.Drafts
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.LargeTitle
import com.thesozluk.app.ui.components.CreatorLinks
import androidx.compose.runtime.derivedStateOf
import com.thesozluk.app.ui.components.LocalBottomBarInset
import com.thesozluk.app.ui.components.MessageState
import com.thesozluk.app.ui.components.TopicRow
import com.thesozluk.app.ui.components.segmentedShape
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.viewmodel.EksiViewModelFactory
import com.thesozluk.app.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val savedTopics by viewModel.savedTopics
    val isLoggedIn by EksiSession.isLoggedIn
    val nick by EksiSession.nick
    val drafts by Drafts.all

    // The nick isn't part of the login itself; look it up once from the site header
    LaunchedEffect(isLoggedIn, nick) {
        if (isLoggedIn && nick == null) viewModel.refreshNick()
    }

    val scrolled by remember { derivedStateOf { viewModel.scrollState.firstVisibleItemIndex > 0 } }
    // Tapping "profil" again in the bottom bar: back to the top
    LaunchedEffect(Unit) {
        TabReselect.events.collect { route ->
            if (route == Screen.Profile.route) viewModel.scrollState.animateScrollToItem(0)
        }
    }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            // The page shows a big "profil"; the bar takes the title only once it scrolled away
            FloatingTopBar(
                title = if (scrolled) "profil" else null,
                titleOffset = 32.dp,
                onTitleClick = { scope.launch { viewModel.scrollState.animateScrollToItem(0) } }
            ) {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "ayarlar")
                    }
            }
        }
    ) { padding ->
        // Edge to edge: the list starts below the bar but scrolls under it and the status bar
        LazyColumn(
            state = viewModel.scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = 24.dp + LocalBottomBarInset.current
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "title") { LargeTitle("profil") }
            item(key = "account") {
                AccountCard(
                    isLoggedIn = isLoggedIn,
                    nick = nick,
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onLogout = { EksiSession.logout() },
                    onOpenProfile = { nick?.let { navController.navigate(Screen.Author.createRoute(it)) } }
                )
            }
            // Device-side library sections
            item(key = "library") {
                val rows = listOf(
                    Triple(LibraryKind.Bookmarks, Icons.Rounded.Bookmarks, "${EntryBookmarks.items.value.size} entry"),
                    Triple(LibraryKind.History, Icons.Rounded.AutoStories, "${ReadingHistory.items.value.size} başlık"),
                    Triple(LibraryKind.Offline, Icons.Rounded.CloudDone, OfflineStore.topics.value.size.let { n ->
                        val running = OfflineStore.progress.values.count { !it.finished }
                        "$n başlık" + if (running > 0) " · $running indiriliyor" else ""
                    }),
                    Triple(LibraryKind.Drafts, Icons.Rounded.Description, "${drafts.size} taslak"),
                    Triple(LibraryKind.SavedTopics, Icons.Rounded.BookmarkAdd, "${savedTopics.size} başlık")
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 20.dp)) {
                    rows.forEachIndexed { index, (kind, icon, count) ->
                        Surface(
                            onClick = { navController.navigate(Screen.Library.createRoute(kind)) },
                            shape = segmentedShape(index, rows.size),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(kind.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 16.dp))
                                Text(count, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item(key = "creator-links") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("geliştirici", style = MaterialTheme.typography.titleMedium)
                        CreatorLinks()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountCard(
    isLoggedIn: Boolean,
    nick: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val container = if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(shape = RoundedCornerShape(32.dp), color = container, contentColor = content) {
        AnimatedContent(targetState = isLoggedIn, label = "account") { loggedIn ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = (if (loggedIn) MaterialShapes.Sunny else MaterialShapes.Cookie9Sided).toShape(),
                        color = if (loggedIn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (loggedIn) {
                                // The logo in one color from the theme, so it fits any palette
                                val color = MaterialTheme.colorScheme.onPrimary
                                val logo = remember(color) { BrandIcons.logo(color, color) }
                                Image(logo, contentDescription = null, modifier = Modifier.size(34.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = if (loggedIn) nick ?: "giriş yapıldı" else "misafir",
                            style = MaterialTheme.typography.titleLargeEmphasized
                        )
                        Text(
                            text = if (loggedIn) "favorilerin ekşi hesabına işleniyor"
                            else "favorilemek için ekşi hesabınla giriş yap",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (loggedIn) {
                    var confirmLogout by remember { mutableStateOf(false) }
                    if (confirmLogout) {
                        AlertDialog(
                            onDismissRequest = { confirmLogout = false },
                            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                            title = { Text("çıkış yapılsın mı?") },
                            text = { Text("favorileme, oylama ve mesajlar için tekrar giriş yapman gerekecek.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirmLogout = false
                                    onLogout()
                                }) { Text("çıkış yap", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("vazgeç") } }
                        )
                    }
                    // Connected pair on the card: the profile is the main action, logout the quiet one
                    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        if (nick != null) {
                            Button(
                                onClick = onOpenProfile,
                                modifier = Modifier.weight(1f),
                                shapes = ButtonDefaults.shapes(),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null)
                                Text("profilim", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        FilledTonalButton(
                            onClick = { confirmLogout = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                            Text("çıkış yap", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null)
                        Text("ekşi sözlük ile giriş yap", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
