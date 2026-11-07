package com.thesozluk.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * A small live picture of the app's bars for the appearance settings: colored shapes drift
 * behind a top bar and a bottom bar, which are drawn the way the current settings draw them.
 * Plain with blur off, frosted with blur on, and with the light-catching rim and top line
 * once "sıvı cam" is on, so flipping the switches shows the difference right away.
 */
@Composable
fun GlassPreview(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val hazeState = rememberHazeState()
    val still = motionReduced()
    val drift = rememberInfiniteTransition(label = "glassPreview")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val phase = if (still) 0.5f else t
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .cardEdge(shape)
            .background(colors.surfaceContainerLow)
    ) {
        // What passes behind the bars: colored shapes moving across, and a few "lines of text"
        Box(Modifier.matchParentSize().hazeSource(hazeState)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(colors.primary, radius = h * 0.32f, center = Offset(w * (0.1f + 0.7f * phase), h * 0.22f))
                drawCircle(colors.tertiary, radius = h * 0.26f, center = Offset(w * (0.85f - 0.6f * phase), h * 0.82f))
                drawCircle(colors.secondary, radius = h * 0.2f, center = Offset(w * (0.5f + 0.3f * (phase - 0.5f)), h * 0.5f))
                val line = colors.onSurface.copy(alpha = 0.18f)
                for (i in 0 until 4) {
                    val y = h * (0.42f + i * 0.09f)
                    drawRoundRect(
                        line,
                        topLeft = Offset(w * 0.08f, y),
                        size = androidx.compose.ui.geometry.Size(w * (if (i == 3) 0.5f else 0.84f), 8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
        // Top bar: back button and a title card
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        ) {
            PreviewBar(CircleShape, hazeState, Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            PreviewBar(RoundedCornerShape(50), hazeState, Modifier.height(44.dp)) {
                Text("önizleme", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 18.dp))
            }
        }
        // Bottom bar: the tabs
        PreviewBar(
            RoundedCornerShape(50),
            hazeState,
            Modifier.align(Alignment.BottomCenter).padding(12.dp).height(52.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                listOf(Icons.Rounded.Home, Icons.Rounded.Search, Icons.Rounded.Casino, Icons.Rounded.Person).forEachIndexed { i, icon ->
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (i == 0) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/** One bar of the preview, drawn as the bars are under the current settings. */
@Composable
private fun PreviewBar(shape: Shape, state: HazeState, modifier: Modifier, content: @Composable () -> Unit) {
    val blur = AppSettings.blurEffects.value
    val liquid = liquidGlass()
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            liquid -> GlassBackdrop(shape, Modifier.matchParentSize(), state = state)
            blur -> Box(Modifier.matchParentSize().glass(shape, state).background(surface.copy(alpha = 0.62f), shape))
            else -> Box(Modifier.matchParentSize().background(surface, shape))
        }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) { content() }
    }
}
