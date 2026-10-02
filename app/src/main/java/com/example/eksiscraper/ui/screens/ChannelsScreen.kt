package com.example.eksiscraper.ui.screens

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.TopicRow
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.ChannelsViewModel
import com.example.eksiscraper.viewmodel.EksiViewModelFactory

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
    val channels by viewModel.channels
    val channelsError by viewModel.channelsError
    val selected by viewModel.selected
    val state by viewModel.topics
    val listState = viewModel.listState

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
                TopAppBar(
                    title = { Text("#${selected?.name ?: name}", style = MaterialTheme.typography.headlineMediumEmphasized) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                        }
                    }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
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
                    channelsError != null -> 0
                    state.error != null -> 1
                    state.topics.isEmpty() -> 2
                    else -> 3
                },
                label = "channelPhase"
            ) { phase ->
                when (phase) {
                    0 -> ErrorState(message = channelsError.orEmpty(), onRetry = viewModel::loadChannels)
                    1 -> ErrorState(message = state.error.orEmpty(), onRetry = viewModel::retry)
                    2 -> LoadingState(messages = listOf("kanal açılıyor", "başlıklar diziliyor"))
                    else -> PullToRefreshBox(isRefreshing = state.isLoading, onRefresh = viewModel::retry) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            selected?.description?.takeIf { it.isNotBlank() }?.let { description ->
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
