package com.thesozluk.app.ui.screens

import androidx.compose.foundation.layout.isImeVisible

import androidx.compose.foundation.layout.WindowInsets


import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable

import com.thesozluk.app.ui.components.AppBottomSheet
import com.thesozluk.app.ui.components.cardEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import com.thesozluk.app.ui.components.FloatingPromptHost
import com.thesozluk.app.ui.components.PromptState
import com.thesozluk.app.ui.components.SheetBlur
import com.thesozluk.app.ui.components.glassSource
import com.thesozluk.app.settings.AppSettings
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import com.thesozluk.app.ui.components.RevealPullToRefresh
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import com.thesozluk.app.ui.components.FloatingSurface
import com.thesozluk.app.ui.components.rememberAvatarUrl
import com.thesozluk.app.ui.components.TopicListSkeleton
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
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
import com.thesozluk.app.model.MessageThread
import com.thesozluk.app.network.EksiSession
import com.thesozluk.app.ui.components.FloatingTopBar
import com.thesozluk.app.ui.components.AuthorAvatar
import com.thesozluk.app.ui.components.ErrorState
import com.thesozluk.app.ui.components.LoadingState
import com.thesozluk.app.ui.components.MessageState
import com.thesozluk.app.ui.components.segmentedShape
import com.thesozluk.app.ui.navigation.Screen
import com.thesozluk.app.viewmodel.EksiViewModelFactory
import com.thesozluk.app.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessagesScreen(
    navController: NavController,
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
    val query by viewModel.query
    val prompt = remember { PromptState() }
    // "yeni mesaj": pick the author, then their conversation opens
    var picking by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(isLoggedIn) { if (isLoggedIn && box == null) viewModel.loadBox() }
    LaunchedEffect(message) {
        message?.let {
            prompt.show(it)
            viewModel.consumeMessage()
        }
    }

    if (picking) {
        AuthorPickerSheet(
            suggest = viewModel::suggestNicks,
            onPick = { nick ->
                picking = false
                navController.navigate(Screen.MessageThread.withNick(nick))
            },
            onDismiss = { picking = false }
        )
    }

    val threadListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(0) }
    var headerOffset by remember { mutableFloatStateOf(0f) }
    val headerConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Turned off in settings: the header always stays
                headerOffset = if (!AppSettings.hideBarsOnScroll.value) 0f
                else (headerOffset + available.y).coerceIn(-headerHeight.toFloat(), 0f)
                return Offset.Zero
            }
        }
    }
    // Switching between inbox and arşiv shows a list from its top: bring the header back too
    LaunchedEffect(archive) {
        animate(headerOffset, 0f) { value, _ -> headerOffset = value }
    }
    Scaffold(
        snackbarHost = { FloatingPromptHost(prompt, Modifier.navigationBarsPadding()) },
        floatingActionButton = {
            if (isLoggedIn) {
                ExtendedFloatingActionButton(
                    onClick = { picking = true },
                    icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    text = { Text("yeni mesaj") }
                )
            }
        }
    ) { padding ->
        val top = with(density) { headerHeight.toDp() }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).nestedScroll(headerConnection)) {
            when {
                !isLoggedIn -> MessageState(
                    modifier = Modifier.padding(top = top),
                    icon = Icons.Rounded.Mail,
                    title = "mesajlar için giriş yap",
                    message = "mesajlaşmak ekşi sözlük hesabıyla mümkün."
                ) {
                    Button(onClick = { navController.navigate(Screen.Login.route) }) { Text("giriş yap") }
                }
                error != null && box == null -> ErrorState(
                    message = error.orEmpty(),
                    onRetry = { viewModel.loadBox() },
                    modifier = Modifier.padding(top = top)
                )
                box == null -> TopicListSkeleton(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 4.dp))
                else -> {
                    // Pull to refresh with the same expressive indicator as the home lists
                    RevealPullToRefresh(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.loadBox() },
                        top = top
                    ) { pullOffset ->
                    val threads = box!!.threads
                    if (threads.isEmpty() && !isLoading) {
                        MessageState(
                            icon = Icons.Rounded.Inbox,
                            title = when {
                                query != null -> "\"$query\" geçen mesaj yok"
                                archive -> "arşiv boş"
                                else -> "mesaj yok"
                            },
                            modifier = Modifier.padding(top = top)
                        )
                    }
                    LazyColumn(
                        state = threadListState,
                        modifier = Modifier.fillMaxSize().then(pullOffset).glassSource(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(threads, key = { _, t -> t.id }) { index, thread ->
                            ThreadRow(
                                thread = thread,
                                isNew = thread.isUnread,
                                index = index,
                                count = threads.size,
                                onClick = {
                                    viewModel.markRead(thread.id)
                                    navController.navigate(Screen.MessageThread.createRoute(thread.id, thread.nick))
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    }
                }
            }
            // Like the home screen: the bar and the tabs sit on one solid header that slides
            // away while scrolling down and comes back on scrolling up
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeight = it.height }
                    .graphicsLayer { translationY = headerOffset }
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                FloatingTopBar(
                    title = "mesajlar",
                    onBack = { navController.popBackStack() },
                    onTitleClick = { scope.launch { threadListState.animateScrollToItem(0) } }
                )
                if (isLoggedIn) {
                    // No 48dp touch-target padding around the buttons (as in the settings): with it
                    // the glass edge was drawn on the padding, making the button look bigger
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        ToggleButton(
                            checked = !archive && query == null,
                            onCheckedChange = {
                                searchText = ""
                                viewModel.loadBox(archive = false)
                            },
                            // Liquid glass on the one not chosen; the chosen one stays solid, as elsewhere
                            modifier = Modifier.weight(1f).then(
                                if (!archive && query == null) Modifier
                                else Modifier.cardEdge(ButtonGroupDefaults.connectedLeadingButtonShapes().shape)
                            ),
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ) { Text("gelen kutusu") }
                        ToggleButton(
                            checked = archive && query == null,
                            onCheckedChange = {
                                searchText = ""
                                viewModel.loadBox(archive = true)
                            },
                            modifier = Modifier.weight(1f).then(
                                if (archive && query == null) Modifier
                                else Modifier.cardEdge(ButtonGroupDefaults.connectedTrailingButtonShapes().shape)
                            ),
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ) { Text("arşiv") }
                    }
                    }
                    // "mesajlarda ara": the site's search through the messages
                    val focus = androidx.compose.ui.platform.LocalFocusManager.current
                    androidx.compose.material3.OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("mesajlarda ara") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (searchText.isNotEmpty() || query != null) ({
                            IconButton(onClick = {
                                searchText = ""
                                focus.clearFocus()
                                viewModel.search("")
                            }) { Icon(Icons.Rounded.Close, contentDescription = "aramayı temizle") }
                        }) else null,
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                            focus.clearFocus()
                            viewModel.search(searchText)
                        }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    )
                }
                        }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThreadRow(thread: MessageThread, isNew: Boolean, index: Int, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = segmentedShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().cardEdge(segmentedShape(index, count))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AuthorAvatar(thread.nick, size = 44, avatarUrl = rememberAvatarUrl(thread.nick))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    thread.nick,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    fontWeight = if (isNew) FontWeight.Bold else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    thread.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isNew) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Time on top, and below it a dot while there are new messages, like a messaging app
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    thread.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isNew) {
                    Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }
    }
}

/**
 * "yeni mesaj": type a few letters of a nick and pick the author from the site's suggestions (or
 * take the nick as typed); their conversation opens next.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthorPickerSheet(suggest: suspend (String) -> List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var nicks by remember { mutableStateOf<List<String>>(emptyList()) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Ask once typing pauses
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(250)
        nicks = suggest(query.trim().removePrefix("@"))
    }
    val typed = query.trim().removePrefix("@")
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetBlur.BlurBehind()
        // Back puts the keyboard away first; once it is down the sheet's own back (with its
        // predictive gesture) closes it. The handler is added afresh each time the keyboard comes
        // up, so it is the newest one and goes before the sheet's
        // Some phones keep reporting the keyboard as up after it went down; the handler steps
        // aside after one use either way (typing or the keyboard coming up again re-arms it), so a
        // second back always reaches the sheet
        // The sheet's own window: the keyboard and focus belong to it, not to the screen behind
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val keyboardUp = imeVisible()
        androidx.compose.runtime.key(keyboardUp, query) {
            var used by remember { mutableStateOf(false) }
            if (keyboardUp && !used) {
                androidx.activity.compose.BackHandler {
                    used = true
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("yeni mesaj", style = MaterialTheme.typography.titleLargeEmphasized)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("yazar ara") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { if (typed.isNotBlank()) onPick(typed) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            // The suggestions, and the nick as typed when it isn't among them
            val options = (if (typed.isNotBlank() && nicks.none { it.equals(typed, ignoreCase = true) }) listOf(typed) else emptyList()) + nicks
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                options.forEach { nick ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(nick) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        AuthorAvatar(nick, size = 36, avatarUrl = rememberAvatarUrl(nick))
                        Text(nick, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}

/** Whether the keyboard is up in this window. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun imeVisible(): Boolean = WindowInsets.isImeVisible
