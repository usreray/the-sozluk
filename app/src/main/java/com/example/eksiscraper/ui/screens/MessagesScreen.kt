package com.example.eksiscraper.ui.screens

import com.example.eksiscraper.ui.components.TopicListSkeleton
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.MessageThread
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.AuthorAvatar
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.MessageState
import com.example.eksiscraper.ui.components.segmentedShape
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    composeTo: String = "",
    viewModel: MessagesViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val isLoggedIn by EksiSession.isLoggedIn
    val box by viewModel.box
    val archive by viewModel.archive
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val message by viewModel.message
    val isSending by viewModel.isSending
    val snackbar = remember { SnackbarHostState() }
    // Opened from a profile's "mesaj" button: start a message to that nick
    var composing by rememberSaveable { mutableStateOf(composeTo.isNotEmpty()) }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn && box == null) viewModel.loadBox() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (composing) {
        NewMessageSheet(
            initialTo = composeTo,
            isSending = isSending,
            onSend = { to, text -> viewModel.send(to, text) { composing = false } },
            onDismiss = { if (!isSending) composing = false }
        )
    }

    val threadListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                FloatingTopBar(
                    title = "mesajlar",
                    onBack = { navController.popBackStack() },
                    onTitleClick = { scope.launch { threadListState.animateScrollToItem(0) } }
                )
                if (isLoggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        ToggleButton(
                            checked = !archive,
                            onCheckedChange = { viewModel.loadBox(archive = false) },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ) { Text("gelen kutusu") }
                        ToggleButton(
                            checked = archive,
                            onCheckedChange = { viewModel.loadBox(archive = true) },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ) { Text("arşiv") }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isLoggedIn) {
                ExtendedFloatingActionButton(
                    onClick = { composing = true },
                    icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    text = { Text("yeni mesaj") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !isLoggedIn -> MessageState(
                    icon = Icons.Rounded.Mail,
                    title = "mesajlar için giriş yap",
                    message = "mesajlaşmak ekşi sözlük hesabıyla mümkün."
                ) {
                    Button(onClick = { navController.navigate(Screen.Login.route) }) { Text("giriş yap") }
                }
                error != null && box == null -> ErrorState(message = error.orEmpty(), onRetry = { viewModel.loadBox() })
                box == null -> TopicListSkeleton(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp))
                else -> PullToRefreshBox(isRefreshing = isLoading, onRefresh = { viewModel.loadBox() }) {
                    val threads = box!!.threads
                    if (threads.isEmpty() && !isLoading) {
                        MessageState(icon = Icons.Rounded.Inbox, title = if (archive) "arşiv boş" else "mesaj yok")
                    }
                    LazyColumn(
                        state = threadListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(threads, key = { _, t -> t.id }) { index, thread ->
                            ThreadRow(
                                thread = thread,
                                index = index,
                                count = threads.size,
                                onClick = { navController.navigate(Screen.MessageThread.createRoute(thread.id, thread.nick)) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThreadRow(thread: MessageThread, index: Int, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = segmentedShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(thread.nick, size = 44)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        thread.nick,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (thread.messageCount > 1) {
                        Text(
                            "· ${thread.messageCount} mesaj",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Text(
                    thread.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(thread.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewMessageSheet(initialTo: String, isSending: Boolean, onSend: (String, String) -> Unit, onDismiss: () -> Unit) {
    var to by rememberSaveable { mutableStateOf(initialTo) }
    var text by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("yeni mesaj", style = MaterialTheme.typography.titleLargeEmphasized)
            OutlinedTextField(
                value = to,
                onValueChange = { to = it },
                label = { Text("kime") },
                singleLine = true,
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("mesaj") },
                minLines = 5,
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSend(to.trim(), text.trim()) },
                enabled = to.isNotBlank() && text.isNotBlank() && !isSending,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isSending) LoadingIndicator(modifier = Modifier.padding(end = 8.dp).size(20.dp))
                else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("gönder")
            }
        }
    }
}
