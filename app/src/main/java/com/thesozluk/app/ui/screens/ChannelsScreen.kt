package com.thesozluk.app.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.thesozluk.app.ui.components.RevealPullToRefresh
import com.thesozluk.app.ui.components.TopicListSkeleton
import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.ErrorState
import androidx.compose.material.icons.rounded.Inbox
import com.thesozluk.app.ui.components.LoadingState
import com.thesozluk.app.ui.components.TopicRow
import com.thesozluk.app.ui.components.rememberReadTitles
import com.thesozluk.app.ui.components.segmentedShape
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.viewmodel.ChannelsViewModel
import com.thesozluk.app.viewmodel.EksiViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChannelsScreen(
    path: String,
    name: String,
    navController: NavController,
    viewModel: ChannelsViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val readTitles = rememberReadTitles()
    val isSearchResults = path.startsWith("basliklar/ara")
    val channels by viewModel.channels
    val channelsError by viewModel.channelsError
    val selected by viewModel.selected
    val state by viewModel.topics
    val listState = viewModel.listState
    val scope = rememberCoroutineScope()

    LaunchedEffect(path) { viewModel.open(path, name) }

    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            listState.layoutInfo.totalItemsCount > 0 && last >= listState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, state.topics.size) { if (nearEnd) viewModel.loadMore() }

    Scaffold(
        topBar = {
            Column {
                FloatingTopBar(
                    title = if (isSearchResults) name else "#${selected?.name ?: name}",
                    subtitle = if (isSearchResults) "arama sonuçları" else "kanal",
                    onBack = { navController.popBackStack() },
                    // The title takes the list back to its top
                    onTitleClick = { scope.launch { listState.animateScrollToItem(0) } }
                )
                if (!isSearchResults) LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(channels, key = { it.path }) { channel ->
                        FilterChip(
                            selected = channel == selected,
                            onClick = { viewModel.select(channel) },
                            label = { Text("#${channel.name}") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(
                targetState = when {
                    // The channel list only matters when no list was opened directly
                    channelsError != null && selected == null -> 0
                    state.error != null -> 1
                    state.isLoaded && state.topics.isEmpty() && !state.isLoading -> 4
                    state.topics.isEmpty() -> 2
                    else -> 3
                },
                label = "channelPhase"
            ) { phase ->
                when (phase) {
                    0 -> ErrorState(message = channelsError.orEmpty(), onRetry = viewModel::loadChannels)
                    1 -> ErrorState(message = state.error.orEmpty(), onRetry = viewModel::retry)
                    2 -> TopicListSkeleton(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp))
                    4 -> com.thesozluk.app.ui.components.MessageState(
                        icon = androidx.compose.material.icons.Icons.Rounded.Inbox,
                        title = "hiç başlık yok",
                        message = "burada şu an gösterilecek bir şey yok."
                    )
                    else -> RevealPullToRefresh(isRefreshing = state.isLoading, onRefresh = viewModel::retry, top = 0.dp) { pullOffset ->
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().then(pullOffset),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            selected?.description?.takeIf { !isSearchResults && it.isNotBlank() }?.let { description ->
                                item(key = "description") {
                                    Text(
                                        description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                                    )
                                }
                            }
                            itemsIndexed(state.topics, key = { _, topic -> topic.url }) { index, topic ->
                                TopicRow(
                                    topic = topic,
                                    shape = segmentedShape(index, state.topics.size),
                                    isRead = topic.title.trim().lowercase() in readTitles,
                                    onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.title, topic.url)) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                            if (state.isLoadingMore) {
                                item(key = "more") {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
