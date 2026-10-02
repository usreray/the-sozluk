package com.example.eksiscraper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A sweep of light across a placeholder block, the usual "loading" shimmer. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceBright
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x, 0f),
            end = Offset(x + 500f, 200f)
        ),
        shape = shape
    )
}

@Composable
private fun Line(width: Float, height: Dp = 14.dp) {
    Box(Modifier.fillMaxWidth(width).height(height).shimmer())
}

/** Placeholder rows shaped like a topic list. */
@Composable
fun TopicListSkeleton(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(16.dp)) {
    val widths = listOf(0.82f, 0.6f, 0.9f, 0.7f, 0.55f, 0.85f, 0.65f, 0.75f, 0.5f, 0.8f, 0.6f, 0.7f)
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        widths.forEachIndexed { index, width ->
            Surface(shape = segmentedShape(index, widths.size), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)
                ) {
                    Box(Modifier.weight(1f)) { Line(width) }
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.size(width = 36.dp, height = 24.dp).shimmer(RoundedCornerShape(50)))
                }
            }
        }
    }
}

/** Placeholder cards shaped like entries. */
@Composable
fun EntryListSkeleton(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(12.dp)) {
    val shapes = listOf(listOf(1f, 0.95f, 0.7f), listOf(1f, 0.5f), listOf(0.9f, 1f, 0.95f, 0.4f), listOf(1f, 0.8f))
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        shapes.forEach { lines ->
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    lines.forEach { Line(it, 16.dp) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Box(Modifier.size(width = 96.dp, height = 20.dp).shimmer(RoundedCornerShape(50)))
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(width = 84.dp, height = 12.dp).shimmer())
                            Box(Modifier.size(width = 110.dp, height = 10.dp).shimmer())
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.size(36.dp).shimmer(CircleShape))
                    }
                }
            }
        }
    }
}
