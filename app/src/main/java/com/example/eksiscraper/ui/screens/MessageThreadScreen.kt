package com.example.eksiscraper.ui.screens

import com.example.eksiscraper.ui.components.rememberEntryInlineContent
import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eksiscraper.model.Message
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.AuthorAvatar
import com.example.eksiscraper.ui.components.ErrorState
import com.example.eksiscraper.ui.components.LoadingState
import com.example.eksiscraper.ui.components.rememberEntryText
import com.example.eksiscraper.ui.navigation.Screen
import com.example.eksiscraper.ui.navigation.openEksiLink
import com.example.eksiscraper.viewmodel.EksiViewModelFactory
import com.example.eksiscraper.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageThreadScreen(
    threadId: String,
    nick: String,
    navController: NavController,
    viewModel: MessagesViewModel = viewModel(
        factory = EksiViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val thread by viewModel.thread
    val threadError by viewModel.threadError
    val isSending by viewModel.isSending
    val message by viewModel.message
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    var reply by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val otherNick = thread?.nick?.ifBlank { null } ?: nick

    LaunchedEffect(threadId) { viewModel.openThread(threadId) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    // Conversations read bottom-up: start at the newest message
    LaunchedEffect(thread?.messages?.size) {
        val size = thread?.messages?.size ?: 0
        if (size > 0) listState.scrollToItem(size - 1)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("konuşma silinsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.threadAction("delete") { navController.popBackStack() }
                }) { Text("sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("vazgeç") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            FloatingTopBar(
                title = otherNick,
                subtitle = "profili aç",
                onBack = { navController.popBackStack() },
                onTitleClick = { navController.navigate(Screen.Author.createRoute(otherNick)) },
                leading = {
                    AuthorAvatar(
                        otherNick,
                        size = 36,
                        modifier = Modifier.clickable { navController.navigate(Screen.Author.createRoute(otherNick)) }
                    )
                },
                actions = if (thread?.threadForm == null) null else ({
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "diğer") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("arşivle") },
                                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.threadAction("archive") { navController.popBackStack() }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("sil", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuOpen = false
                                        confirmDelete = true
                                    }
                                )
                            }
                        }
                })
            )
        },
        bottomBar = {
            // Reply box: replies go through the "yeni mesaj" form to the same nick
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        placeholder = { Text("yanıt yaz") },
                        shape = RoundedCornerShape(28.dp),
                        maxLines = 5,
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        onClick = { viewModel.send(otherNick, reply.trim()) { reply = "" } },
                        enabled = reply.isNotBlank(),
                        modifier = Modifier.padding(start = 8.dp).size(52.dp)
                    ) {
                        if (isSending) LoadingIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "gönder")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                threadError != null && thread == null -> ErrorState(message = threadError.orEmpty(), onRetry = viewModel::refreshThread)
                thread == null -> LoadingState(messages = listOf("konuşma açılıyor"))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(thread!!.messages) { _, item ->
                        Bubble(item) { link -> navController.openEksiLink(link, uriHandler) }
                    }
                }
            }
        }
    }
}

/** Incoming on the left in a neutral tone, ours on the right in the accent color. */
@Composable
private fun Bubble(message: Message, onLink: (com.example.eksiscraper.ui.components.EksiLink) -> Unit) {
    val outgoing = message.isOutgoing
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 24.dp, topEnd = 24.dp,
                bottomStart = if (outgoing) 24.dp else 6.dp,
                bottomEnd = if (outgoing) 6.dp else 24.dp
            ),
            color = if (outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 320.dp).alpha(if (message.isPending) 0.6f else 1f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                // Long-press to select and copy, as in any messaging app
                SelectionContainer {
                    Text(
                        rememberEntryText(message.html, message.text, onLink),
                        inlineContent = rememberEntryInlineContent(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    if (message.isFailed) "gönderilemedi" else message.time,
                    style = MaterialTheme.typography.labelSmall,
                    // A muted tone of the bubble's own text color, so it fits both bubble colors
                    color = if (message.isFailed) MaterialTheme.colorScheme.error else LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}
