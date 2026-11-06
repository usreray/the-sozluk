package com.thesozluk.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
import kotlin.math.sign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short notes for the reader ("kaydedildi", "kopyalandı", errors), shown by a [FloatingPromptHost]
 * as a floating glass card. A new note takes the place of the one on screen.
 */
@Stable
class PromptState {
    internal class Prompt(val message: String, val durationMs: Long) {
        val done = CompletableDeferred<Unit>()
    }

    internal var current by mutableStateOf<Prompt?>(null)
        private set

    /** Shows [message] and returns once it has gone, by itself or swiped away. */
    suspend fun show(message: String, long: Boolean = message.length > 60) {
        current?.done?.complete(Unit)
        val prompt = Prompt(message, if (long) 5_000L else 3_000L)
        current = prompt
        try {
            prompt.done.await()
        } finally {
            if (current === prompt) current = null
        }
    }

    internal fun dismiss(prompt: Prompt) {
        prompt.done.complete(Unit)
        if (current === prompt) current = null
    }
}

/** The screen's prompts, for code deep inside it (settings rows and the like). */
val LocalPrompt = staticCompositionLocalOf<PromptState?> { null }

/**
 * The note from [state] as a rounded glass card: it slides up into place, leaves on its own after
 * a few seconds, and can be swiped away to either side (a short swipe springs back).
 */
@Composable
fun FloatingPromptHost(state: PromptState, modifier: Modifier = Modifier) {
    // Kept after the note is done so it can slide out with its text
    var shown by remember { mutableStateOf<PromptState.Prompt?>(null) }
    val current = state.current
    val reduced = motionReduced()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    // Sideways, under the finger
    val drag = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val alpha = remember { Animatable(0f) }
    var heightPx by remember { mutableIntStateOf(0) }
    val travel = { heightPx + with(density) { 32.dp.toPx() } }

    LaunchedEffect(current) {
        if (current != null) {
            if (shown != null && shown !== current) {
                // A new note replaces the old one: quick fade, then the new one comes up
                alpha.animateTo(0f, tween(if (reduced) 0 else 120))
            }
            shown = current
            drag.snapTo(0f)
            offset.snapTo(if (reduced) 0f else travel())
            launch { alpha.animateTo(1f, tween(if (reduced) 0 else 220)) }
            if (reduced) offset.snapTo(0f) else offset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 420f))
            delay(current.durationMs)
            state.dismiss(current)
        } else if (shown != null) {
            if (drag.value != 0f) {
                // Swiped off the side: already gone
                alpha.snapTo(0f)
            } else {
                launch { alpha.animateTo(0f, tween(if (reduced) 0 else 260)) }
                if (!reduced) offset.animateTo(travel(), tween(260))
            }
            shown = null
        }
    }

    val prompt = shown ?: return
    val glass = LocalGlass.current != null && AppSettings.blurEffects.value
    val container = MaterialTheme.colorScheme.surfaceContainerHighest.let { if (glass) it.copy(alpha = 0.72f) else it }
    val shape = RoundedCornerShape(24.dp)
    val liquid = liquidGlass() && LocalGlass.current != null
    Box(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .widthIn(max = 480.dp)
                .onSizeChanged { heightPx = it.height; widthPx = it.width }
                .graphicsLayer {
                    translationY = offset.value
                    translationX = drag.value
                    // Fades as it goes, and tips a little the way it is thrown
                    val gone = if (widthPx > 0) (abs(drag.value) / widthPx).coerceIn(0f, 1f) else 0f
                    this.alpha = alpha.value * (1f - gone * 0.8f)
                    rotationZ = if (widthPx > 0) drag.value / widthPx * 4f else 0f
                }
                .pointerInput(prompt) {
                    val velocity = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { velocity.resetTracking() },
                        onDragEnd = {
                            val speed = velocity.calculateVelocity().x
                            val far = abs(drag.value) > widthPx * 0.35f
                            val thrown = abs(speed) > 1000f && sign(speed) == sign(drag.value)
                            scope.launch {
                                if (far || thrown) {
                                    val side = if (drag.value != 0f) sign(drag.value) else sign(speed)
                                    drag.animateTo(side * (widthPx + 48.dp.toPx()), tween(if (reduced) 0 else 180), initialVelocity = speed)
                                    state.dismiss(prompt)
                                } else {
                                    drag.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 500f))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { drag.animateTo(0f, spring()) } }
                    ) { change, amount ->
                        change.consume()
                        // The card moves with the finger, so its own travel is what is tracked
                        val next = drag.value + amount
                        velocity.addPosition(change.uptimeMillis, Offset(next, 0f))
                        scope.launch { drag.snapTo(next) }
                    }
                }
                .then(
                    if (liquid) Modifier
                    else Modifier
                        .glass(shape)
                        .background(container, shape)
                        .border(
                            0.75.dp,
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.02f))),
                            shape
                        )
                )
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            // Liquid glass: the nav bar's glass as a layer under the text
            if (liquid) GlassBackdrop(shape, Modifier.matchParentSize(), alpha = 0.72f, color = MaterialTheme.colorScheme.surfaceContainerHighest)
            Text(
                prompt.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }
    }
}
