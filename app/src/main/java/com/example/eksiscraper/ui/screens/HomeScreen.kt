package com.example.eksiscraper.ui.screens

import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.LocalBottomBarInset
import com.example.eksiscraper.ui.components.TopicRow
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.HomeCategory
import com.example.eksiscraper.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

private val loadingMessages = listOf("başlıklar toplanıyor", "gündem taranıyor", "entry'ler sayılıyor")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val categories = HomeCategory.entries
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Load a tab the first time it is shown
    LaunchedEffect(pagerState.currentPage) {
        viewModel.ensureLoaded(categories[pagerState.currentPage])
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "ekşi",
                            style = MaterialTheme.typography.headlineMediumEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
                CategoryButtons(
                    selected = pagerState.currentPage,
                    onSelect = { index ->
                        scope.launch {
                            if (index == pagerState.currentPage) {
                                // Tapping the open tab again jumps back to the top
                                viewModel.listStates.getValue(categories[index]).animateScrollToItem(0)
                            } else {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            beyondViewportPageCount = 1
        ) { page ->
            CategoryPage(
                category = categories[page],
                viewModel = viewModel,
                onTopicClick = { title, url ->
                    navController.navigate(Screen.TopicDetail.createRoute(title, url))
                }
            )
        }
    }
}

/** Connected toggle buttons: the M3 Expressive take on a segmented control. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryButtons(selected: Int, onSelect: (Int) -> Unit) {
    val categories = HomeCategory.entries
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        categories.forEachIndexed { index, category ->
            ToggleButton(
                checked = index == selected,
                onCheckedChange = { onSelect(index) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(category.label)
            }
        }
    }
}

private enum class HomePhase { Loading, Error, Content }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryPage(
    category: HomeCategory,
    viewModel: HomeViewModel,
    onTopicClick: (title: String, url: String) -> Unit
) {
    val state = viewModel.state(category)
    val listState = viewModel.listStates.getValue(category)
    val phase = when {
        state.error != null -> HomePhase.Error
        state.topics.isEmpty() -> HomePhase.Loading
        else -> HomePhase.Content
    }

    // Infinite scroll: fetch the next page a few rows before the end
    val nearEnd by remember(listState) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 6
        }
    }
    LaunchedEffect(nearEnd, state.topics.size) {
        if (nearEnd) viewModel.loadMore(category)
    }

    Crossfade(targetState = phase, label = "homePhase") { current ->
        when (current) {
            HomePhase.Loading -> LoadingState(messages = loadingMessages)
            HomePhase.Error -> ErrorState(
                message = state.error.orEmpty(),
                onRetry = { viewModel.retry(category) }
            )
            HomePhase.Content -> {
                val refreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh(category) },
                    state = refreshState,
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = refreshState,
                            isRefreshing = state.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + LocalBottomBarInset.current),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(state.topics, key = { _, topic -> topic.url }) { index, topic ->
                            TopicRow(
                                topic = topic,
                                shape = segmentedShape(index, state.topics.size),
                                onClick = { onTopicClick(topic.title, topic.url) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        if (state.isLoadingMore) {
                            item(key = "loadingMore") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { LoadingIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }
}
