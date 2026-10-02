package com.example.eksiscraper.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's top bar, floating over the content instead of an edge-to-edge app bar.
 *
 * Without a title (e.g. the top of a profile, where the page itself shows the big heading) it is
 * just round floating buttons, as in current Google apps; once a [title] is given it morphs into
 * a card holding it. It slides away while scrolling down and comes back on the way up
 * ([visible]).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingTopBar(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    visible: Boolean = true,
    isLoading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val compact = title == null && subtitle == null && leading == null && !isLoading
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        AnimatedContent(
            targetState = compact,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)))
                    .using(SizeTransform(clip = false))
            },
            label = "topBarShape"
        ) { isCompact ->
            if (isCompact) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (onBack != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 6.dp
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                                }
                            } else {
                                Spacer(Modifier.width(14.dp))
                            }
                            leading?.invoke()
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 6.dp)) {
                                if (title != null) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (subtitle != null) {
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            actions()
                        }
                        if (isLoading) {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top padding that lets content start below the floating bar; [compact] for screens whose bar
 * starts as round buttons only.
 */
@Composable
fun floatingTopBarInset(compact: Boolean = false): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (compact) 72.dp else 108.dp

/** True while the user scrolls toward the top (or sits at the top): show the bars then. */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            val up = if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousOffset >= firstVisibleItemScrollOffset
            }
            previousIndex = firstVisibleItemIndex
            previousOffset = firstVisibleItemScrollOffset
            up || !canScrollForward
        }
    }.value
}
