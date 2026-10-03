package com.thesozluk.app.ui.components

import com.thesozluk.app.settings.AppSettings
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
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
 * The app's top bar, floating over the content as separate surfaces in the M3 Expressive way:
 * a round back button, a card with the title, and a pill with the actions, each on its own.
 *
 * Without a title (e.g. the top of a profile, where the page itself shows the big heading) only
 * the round buttons float; the title card appears once a [title] is given. The bar slides away
 * while scrolling down and comes back on the way up ([visible]). [actions] null draws no pill.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FloatingTopBar(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    visible: Boolean = true,
    isLoading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    /** Makes the title card tappable, e.g. to open the profile of a conversation */
    onTitleClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val showTitle = title != null || subtitle != null || leading != null
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                FloatingSurface(shape = CircleShape) {
                    IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "geri")
                    }
                }
            }
            // The title card sits centered in the space between back and actions, as wide as
            // its text: short titles stay balanced instead of hugging the back button
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f)
                ) {
                    // Long press shows the whole title in a tooltip
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                        tooltip = { if (title != null) PlainTooltip { Text(title) } },
                        state = rememberTooltipState(),
                        enableUserInput = title != null
                    ) {
                    FloatingSurface(
                        shape = RoundedCornerShape(26.dp),
                        onClick = onTitleClick
                    ) {
                        // Same height as the back button and the actions, so the bar reads as one row;
                        // pages show their full title themselves, so one line is enough here
                        Box(modifier = Modifier.height(52.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp)
                            ) {
                                leading?.let {
                                    it()
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(
                                    modifier = Modifier.weight(1f, fill = false),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (title != null) {
                                        // A title too long for the bar scrolls right to left (unless
                                        // turned off in settings, then it ends with "…")
                                        val marquee = AppSettings.marqueeTitles.value
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleMediumEmphasized,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = if (marquee) TextOverflow.Clip else TextOverflow.Ellipsis,
                                            modifier = if (marquee) {
                                                Modifier.basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    initialDelayMillis = 1500,
                                                    repeatDelayMillis = 2500
                                                )
                                            } else Modifier
                                        )
                                    }
                                    if (subtitle != null) {
                                        Text(
                                            subtitle,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (isLoading) {
                                // Along the bottom edge inside the card, not below it
                                LinearWavyProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    }
                }
            }
            if (actions != null) {
                FloatingSurface(shape = CircleShape) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) { actions() }
                }
            } else if (onBack != null) {
                // Mirror the back button so the title is centered on the screen
                Spacer(Modifier.width(52.dp))
            }
        }
    }
}

/** One floating piece of the top bar. */
@Composable
fun FloatingSurface(
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, shadowElevation = 6.dp, modifier = modifier, content = content)
    } else {
        Surface(shape = shape, color = color, shadowElevation = 6.dp, modifier = modifier, content = content)
    }
}

/**
 * Top padding that lets content start below the floating bar; [compact] for screens whose bar
 * starts as round buttons only.
 */
@Composable
fun floatingTopBarInset(compact: Boolean = false): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (compact) 72.dp else 96.dp

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

/**
 * The big page heading for screens whose bar starts as bare round buttons (settings, profile,
 * messages); the bar shows the title only after this has scrolled away.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp)
    )
}
