package com.example.eksiscraper.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Room the content makes for the refresh animation when pulled down */
private val RefreshGap = 72.dp

/**
 * Pull to refresh where the content itself moves down with the finger and the loading
 * animation sits in the gap that opens above it; the gap stays open until the refresh is done.
 * [top] is where the content starts (below floating bars). [content] gets the modifier that
 * moves it and should apply it to the scrolling list.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RevealPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    top: Dp,
    modifier: Modifier = Modifier,
    /** Off: the content shows as is, without pull to refresh */
    enabled: Boolean = true,
    content: @Composable BoxScope.(contentModifier: Modifier) -> Unit
) {
    if (!enabled) {
        Box(modifier) { content(Modifier) }
        return
    }
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = top)
                    .fillMaxWidth()
                    .height(RefreshGap)
                    .graphicsLayer {
                        val fraction = state.distanceFraction.coerceIn(0f, 1f)
                        alpha = fraction
                        scaleX = 0.6f + 0.4f * fraction
                        scaleY = 0.6f + 0.4f * fraction
                    }
            ) {
                LoadingIndicator()
            }
        }
    ) {
        content(Modifier.graphicsLayer { translationY = RefreshGap.toPx() * state.distanceFraction.coerceIn(0f, 1.3f) })
    }
}
