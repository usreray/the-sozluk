package com.example.eksiscraper.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.components.TopicRow
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val query by viewModel.query
    val suggestions by viewModel.suggestions
    val isLoading by viewModel.isLoadingSuggestions
    val focusManager = LocalFocusManager.current

    // ekşi redirects a query to its topic (or entry for "#123"), so a search opens the topic screen
    fun open(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        focusManager.clearFocus()
        navController.navigate(Screen.TopicDetail.createRoute(trimmed, ""))
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = viewModel::updateQuery,
                    onSearch = ::open,
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("başlık, #entry ya da @yazar") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = viewModel::clear) {
                                Icon(Icons.Rounded.Close, contentDescription = "Temizle")
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {}

        AnimatedVisibility(visible = isLoading) {
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }

        Crossfade(targetState = query.isBlank(), label = "searchBody") { blank ->
            if (blank) {
                MessageState(
                    icon = Icons.Rounded.TravelExplore,
                    title = "Ne arıyorsun?",
                    message = "Bir başlık yaz, #numara ile bir entry'ye git ya da @ ile bir yazar ara."
                )
            } else {
                SuggestionList(query = query, suggestions = suggestions, onSelect = ::open)
            }
        }
    }
}

@Composable
private fun SuggestionList(query: String, suggestions: List<String>, onSelect: (String) -> Unit) {
    // The typed text itself always comes first, so a search is one tap away
    val rows = listOf(query.trim()) + suggestions.filterNot { it.equals(query.trim(), ignoreCase = true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
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
