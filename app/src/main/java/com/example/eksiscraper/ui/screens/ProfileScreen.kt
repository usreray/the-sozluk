package com.example.eksiscraper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.R
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.SavedTopicsListSkeleton
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.ProfileViewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
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
        // No top app bar to match Home and Search screens
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AccountCard(
                isLoggedIn = isLoggedIn,
                onLogin = { navController.navigate(Screen.Login.route) },
                onLogout = { EksiSession.logout() }
            )

            Box(modifier = Modifier.weight(1f)) {
                if (savedTopics.isEmpty()) {
                    // Room reads are fast, so empty here means there are no saved topics
                    EmptySavedTopicsMessage(PaddingValues(0.dp))
                } else {
                    SavedTopicsList(
                        savedTopics = savedTopics,
                        onTopicSelected = { topic ->
                            val encodedTitle = URLEncoder.encode(topic.title, StandardCharsets.UTF_8.toString())
                            val encodedUrl = URLEncoder.encode(topic.url, StandardCharsets.UTF_8.toString())
                            navController.navigate("${Screen.TopicDetail.route}/$encodedTitle/$encodedUrl")
                        },
                        viewModel = viewModel,
                        paddingValues = PaddingValues(0.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(isLoggedIn: Boolean, onLogin: () -> Unit, onLogout: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isLoggedIn) "Logged in to ekşi sözlük" else "Not logged in",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isLoggedIn) {
                OutlinedButton(onClick = onLogout) { Text("Log out") }
            } else {
                Button(onClick = onLogin) { Text("Log in") }
            }
        }
    }
}

@Composable
private fun EmptySavedTopicsMessage(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bookmark_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Saved Topics",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Topics you save will appear here for easy access",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedTopicsList(
    savedTopics: List<Topic>,
    onTopicSelected: (Topic) -> Unit,
    viewModel: ProfileViewModel,
    paddingValues: PaddingValues
) {
    LazyColumn(
        state = viewModel.scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved Topics",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        
        items(savedTopics) { topic ->
            ElevatedCard(
                onClick = { onTopicSelected(topic) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = { viewModel.unsaveTopic(topic) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.bookmark_24px_filled),
                                contentDescription = "Unsave topic",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}