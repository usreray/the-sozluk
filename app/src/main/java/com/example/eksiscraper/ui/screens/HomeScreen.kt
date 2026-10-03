package com.example.eksiscraper.ui.screens

import com.example.eksiscraper.ui.components.RevealPullToRefresh
import androidx.compose.animation.core.animate
import androidx.compose.foundation.shape.CircleShape
import com.example.eksiscraper.ui.components.FloatingSurface
import com.example.eksiscraper.viewmodel.HomeTabRequest
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import com.example.eksiscraper.ui.components.TopicListSkeleton
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.eksiscraper.notify.SiteStatusStore
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.settings.AppSettings
import com.example.eksiscraper.ui.components.MessageState
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
    val isLoggedIn by EksiSession.isLoggedIn
    val hiddenTabs by AppSettings.hiddenTabs
    // Personal lists only when logged in; the rest as chosen in settings
    val categories = remember(isLoggedIn, hiddenTabs) {
        HomeCategory.entries.filter { (isLoggedIn || !it.needsLogin) && it.name !in hiddenTabs }
            .ifEmpty { listOf(HomeCategory.Gundem) }
    }
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val requestedTab by HomeTabRequest.tab
    LaunchedEffect(requestedTab, categories) {
        val tab = requestedTab ?: return@LaunchedEffect
        HomeTabRequest.tab.value = null
        val index = categories.indexOf(tab)
        if (index >= 0) {
            pagerState.scrollToPage(index)
            viewModel.refresh(tab)
        } else {
            navController.navigate(Screen.Channels.createRoute(tab.key, tab.label))
        }
    }
    val siteStatus by SiteStatusStore.status
    // Checked whenever home is shown again (e.g. back from reading a message)
    LaunchedEffect(isLoggedIn) { SiteStatusStore.refresh() }
    val scope = rememberCoroutineScope()
    // Title and tabs float over the lists and slide away together while scrolling down (back on
    // scrolling up), so the lists run edge to edge under the status bar
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(0) }
    var headerOffset by remember { mutableFloatStateOf(0f) }
    val headerConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                headerOffset = (headerOffset + available.y).coerceIn(-headerHeight.toFloat(), 0f)
                return Offset.Zero
            }
        }
    }
    val headerPadding = with(density) { headerHeight.toDp() }
    // Another tab's list starts at its own position, so bring the header back with it;
    // otherwise its space stays empty above that list
    LaunchedEffect(pagerState.currentPage) {
        animate(headerOffset, 0f) { value, _ -> headerOffset = value }
    }

    // Load a tab the first time it is shown
    LaunchedEffect(pagerState.currentPage, categories) {
        categories.getOrNull(pagerState.currentPage)?.let(viewModel::ensureLoaded)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(headerConnection)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            CategoryPage(
                category = categories[page],
                viewModel = viewModel,
                topPadding = headerPadding,
                onTopicClick = { title, url ->
                    navController.navigate(Screen.TopicDetail.createRoute(title, url))
                }
            )
        }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeight = it.height }
                    .graphicsLayer { translationY = headerOffset }
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "ekşi",
                            style = MaterialTheme.typography.headlineMediumEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        if (isLoggedIn) FloatingSurface(shape = CircleShape, modifier = Modifier.padding(end = 8.dp)) {
                          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                            // The site's header lights: new entries in followed topics, unread messages
                            IconButton(onClick = {
                                val olay = categories.indexOf(HomeCategory.Olay)
                                if (olay >= 0) scope.launch { pagerState.animateScrollToPage(olay) }
                                else navController.navigate(Screen.Channels.createRoute(HomeCategory.Olay.key, "olay"))
                            }) {
                                BadgedBox(badge = { if (siteStatus.hasEvents) Badge() }) {
                                    Icon(Icons.Rounded.NotificationsNone, contentDescription = "olay")
                                }
                            }
                            IconButton(onClick = { navController.navigate(Screen.Messages.createRoute()) }) {
                                BadgedBox(badge = { if (siteStatus.hasMessages) Badge() }) {
                                    Icon(Icons.Rounded.MailOutline, contentDescription = "mesajlar")
                                }
                            }
                          }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                CategoryButtons(
                    categories = categories,
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
}

/**
 * Connected toggle buttons (the M3 Expressive segmented control) when three tabs fit;
 * a scrolling row of pill toggles once there are more lists.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryButtons(categories: List<HomeCategory>, selected: Int, onSelect: (Int) -> Unit) {
    if (categories.size <= 3) {
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
        return
    }
    val rowState = rememberLazyListState()
    // Keep the selected tab in view while swiping between lists
    LaunchedEffect(selected) { rowState.animateScrollToItem(maxOf(0, selected - 1)) }
    LazyRow(
        state = rowState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(categories, key = { _, category -> category.name }) { index, category ->
            ToggleButton(checked = index == selected, onCheckedChange = { onSelect(index) }) {
                Text(category.label)
            }
        }
    }
}

private enum class HomePhase { Loading, Error, Empty, Content }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryPage(
    category: HomeCategory,
    viewModel: HomeViewModel,
    topPadding: androidx.compose.ui.unit.Dp,
    onTopicClick: (title: String, url: String) -> Unit
) {
    val state = viewModel.state(category)
    val listState = viewModel.listStates.getValue(category)
    // Topics with a blocked word are left out of every list
    val blockedWords by AppSettings.blockedWords
    val topics = remember(state.topics, blockedWords) {
        state.topics.filterNot { AppSettings.isBlocked(it.title) }
    }
    val phase = when {
        state.error != null -> HomePhase.Error
        state.isLoaded && topics.isEmpty() && !state.isLoading -> HomePhase.Empty
        topics.isEmpty() -> HomePhase.Loading
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
    LaunchedEffect(nearEnd, topics.size) {
        if (nearEnd) viewModel.loadMore(category)
    }

    Crossfade(targetState = phase, label = "homePhase") { current ->
        when (current) {
            HomePhase.Loading -> TopicListSkeleton(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 4.dp))
            HomePhase.Error -> ErrorState(
                message = state.error.orEmpty(),
                modifier = Modifier.padding(top = topPadding),
                onRetry = { viewModel.retry(category) }
            )
            HomePhase.Empty -> MessageState(
                icon = Icons.Rounded.Inbox,
                modifier = Modifier.padding(top = topPadding),
                title = "hiç başlık yok",
                message = "bu listede şu an gösterilecek bir şey yok."
            )
            HomePhase.Content -> {
                RevealPullToRefresh(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh(category) },
                    top = topPadding
                ) { pullOffset ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().then(pullOffset),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 4.dp, bottom = 24.dp + LocalBottomBarInset.current),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(topics, key = { _, topic -> topic.url }) { index, topic ->
                            TopicRow(
                                topic = topic,
                                shape = segmentedShape(index, topics.size),
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
