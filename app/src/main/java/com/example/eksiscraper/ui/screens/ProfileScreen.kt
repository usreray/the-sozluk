package com.example.eksiscraper.ui.screens

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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.components.TopicRow
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.ProfileViewModel

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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("profil", style = MaterialTheme.typography.headlineMediumEmphasized) })
        }
    ) { padding ->
        LazyColumn(
            state = viewModel.scrollState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "account") {
                AccountCard(
                    isLoggedIn = isLoggedIn,
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onLogout = { EksiSession.logout() }
                )
            }
            item(key = "savedHeader") {
                Text(
                    text = "kaydedilen başlıklar",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 10.dp)
                )
            }
            if (savedTopics.isEmpty()) {
                item(key = "empty") {
                    MessageState(
                        icon = Icons.Rounded.Bookmarks,
                        title = "Henüz kayıt yok",
                        message = "Bir başlıktaki yer imi simgesine dokun, buraya eklensin.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            itemsIndexed(savedTopics, key = { _, topic -> "saved:${topic.title}" }) { index, topic ->
                TopicRow(
                    topic = topic,
                    shape = segmentedShape(index, savedTopics.size),
                    onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.title, topic.url)) },
                    modifier = Modifier.animateItem(),
                    trailing = {
                        IconButton(onClick = { viewModel.unsaveTopic(topic) }) {
                            Icon(
                                Icons.Rounded.BookmarkRemove,
                                contentDescription = "Kayıttan çıkar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountCard(isLoggedIn: Boolean, onLogin: () -> Unit, onLogout: () -> Unit) {
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
                            Icon(
                                imageVector = if (loggedIn) Icons.Rounded.WaterDrop else Icons.Rounded.Person,
                                contentDescription = null,
                                tint = if (loggedIn) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = if (loggedIn) "Giriş yapıldı" else "Misafir",
                            style = MaterialTheme.typography.titleLargeEmphasized
                        )
                        Text(
                            text = if (loggedIn) "Favorilerin ekşi hesabına işleniyor"
                            else "Favorilemek için ekşi hesabınla giriş yap",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (loggedIn) {
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Text("Çıkış yap", modifier = Modifier.padding(start = 8.dp))
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
