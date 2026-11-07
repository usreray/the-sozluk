package com.thesozluk.app.ui.components

import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** True when animations should be kept to a minimum: the app's setting or the system's. */
@Composable
fun motionReduced(): Boolean {
    val context = LocalContext.current
    val systemOff = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    return AppSettings.reduceMotion.value || systemOff
}

/** A springy squeeze while pressed: the Expressive "give" on buttons, favorites and tabs. */
@Composable
fun Modifier.pressBounce(interactionSource: InteractionSource, pressedScale: Float = 0.88f): Modifier {
    if (motionReduced()) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 700f),
        label = "pressBounce"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A list's first rows coming in one after another when it first shows (or shows new content):
 * each fades and rises a little, a beat after the one above. Only the first [StaggerRows] rows,
 * and only right after the list appeared; rows scrolled to later just appear. Made per list
 * with [rememberStagger], keyed by what the list shows so a refresh plays it again.
 */
class Stagger internal constructor() {
    internal var start = 0L
}

private const val StaggerRows = 12

@Composable
fun rememberStagger(key: Any?): Stagger = remember(key) { Stagger() }

@Composable
fun Modifier.staggerIn(stagger: Stagger, index: Int): Modifier {
    if (motionReduced()) return this
    val composedAt = remember { android.os.SystemClock.uptimeMillis() }
    if (stagger.start == 0L) stagger.start = composedAt
    // Rows that join well after the list appeared (scrolling) don't animate
    val plays = remember { index < StaggerRows && composedAt - stagger.start < 300 }
    if (!plays) return this
    val shown = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
        shown.animateTo(1f, androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    return graphicsLayer {
        val t = shown.value
        alpha = t
        translationY = (1f - t) * 24.dp.toPx()
    }
}
