package com.example.eksiscraper.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.rounded.Tune
import com.example.eksiscraper.ui.components.AdvancedSearchSheet
import com.example.eksiscraper.ui.navigation.TabReselect
import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.settings.SearchHistory
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.components.TopicRow
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.SearchViewModel
import com.example.eksiscraper.viewmodel.ChannelsViewModel
import com.example.eksiscraper.ui.components.LocalBottomBarInset
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    channelsViewModel: ChannelsViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val channels by channelsViewModel.channels
    val suggestions by viewModel.suggestions
    val isLoading by viewModel.isLoadingSuggestions
    val searchBarState = rememberSearchBarState()
    // Detailed search sheet, opened with the words typed so far
    var advancedFor by remember { mutableStateOf<String?>(null) }
    advancedFor?.let { initial ->
        AdvancedSearchSheet(
            initialKeywords = initial,
            onSearch = { path, name ->
                advancedFor = null
                SearchHistory.add(name.removeSurrounding("“", "”"))
                navController.navigate(Screen.Channels.createRoute(path, name))
            },
            onDismiss = { advancedFor = null }
        )
    }
    // Tapping "ara" again in the bottom bar opens the search field
    LaunchedEffect(Unit) {
        TabReselect.events.collect { route ->
            if (route == Screen.Search.route) searchBarState.animateToExpanded()
        }
    }
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val query = textFieldState.text.toString()

    // The field owns the text; the ViewModel only turns it into autocomplete suggestions
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collectLatest(viewModel::updateQuery)
    }

    // ekşi redirects a query to its topic (or entry for "#123"), so a search opens the topic screen
    fun open(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        SearchHistory.add(trimmed)
        scope.launch { searchBarState.animateToCollapsed() }
        if (trimmed.startsWith("@") && trimmed.length > 1) {
            navController.navigate(Screen.Author.createRoute(trimmed.removePrefix("@")))
        } else {
            navController.navigate(Screen.TopicDetail.createRoute(trimmed, ""))
        }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = ::open,
            placeholder = { Text("başlık, #entry ya da @yazar") },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "geri")
                    }
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.clearText() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "temizle")
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SearchBar(
            state = searchBarState,
            inputField = inputField,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp + LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "başlık yaz, #numara ile bir entry'ye git ya da @ ile bir yazar ara",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.OutlinedButton(onClick = { advancedFor = "" }) {
                Icon(Icons.Rounded.Tune, contentDescription = null)
                Text("gelişmiş arama", modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "kanallar",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (channels.isEmpty()) {
                LoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                channels.forEach { channel ->
                    ElevatedSuggestionChip(
                        onClick = { navController.navigate(Screen.Channels.createRoute(channel.path, channel.name)) },
                        label = { Text("#${channel.name}") }
                    )
                }
            }
        }
    }

    // Tapping the bar opens the full-screen search with live suggestions
    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
        AnimatedVisibility(visible = isLoading) {
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
        Crossfade(targetState = query.isBlank(), label = "searchBody") { blank ->
            if (blank) {
                SearchHistoryList(SearchHistory.items.value, ::open, SearchHistory::remove)
            } else {
                SuggestionList(
                    query = query,
                    suggestions = suggestions,
                    onSelect = ::open,
                    onAdvanced = { text ->
                        scope.launch { searchBarState.animateToCollapsed() }
                        advancedFor = text
                    },
                    onSearchTitles = { text ->
                        scope.launch { searchBarState.animateToCollapsed() }
                        SearchHistory.add(text)
                        // The site's detailed search: every topic whose title contains the words
                        val path = "basliklar/ara?SearchForm.Keywords=" + java.net.URLEncoder.encode(text.trim(), "UTF-8")
                        navController.navigate(Screen.Channels.createRoute(path, "“${text.trim()}”"))
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryList(history: List<String>, onSelect: (String) -> Unit, onRemove: (String) -> Unit) {
    if (history.isEmpty()) {
        MessageState(Icons.Rounded.Search, "yazmaya başla", "öneriler burada belirecek.")
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "son aramalar",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(history, key = { _, item -> item }) { index, item ->
                Surface(
                    onClick = { onSelect(item) },
                    shape = segmentedShape(index, history.size),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp)) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 16.dp))
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "arama geçmişinden kaldır", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionList(
    query: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit,
    onSearchTitles: (String) -> Unit,
    onAdvanced: (String) -> Unit
) {
    // The typed text itself always comes first, so a search is one tap away
    val rows = listOf(query.trim()) + suggestions.filterNot { it.equals(query.trim(), ignoreCase = true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (!query.trim().startsWith("@") && !query.trim().startsWith("#")) {
            item(key = "searchTitles") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { onSearchTitles(query) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                        Text("başlıklarda ara", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Words plus author, dates, şükela only, sort order
                    androidx.compose.material3.OutlinedButton(onClick = { onAdvanced(query) }) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Text("gelişmiş", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        itemsIndexed(rows, key = { index, text -> "$index:$text" }) { index, text ->
            TopicRow(
                topic = Topic(title = text),
                shape = segmentedShape(index, rows.size),
                onClick = { onSelect(text) },
                modifier = Modifier.animateItem(),
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}
