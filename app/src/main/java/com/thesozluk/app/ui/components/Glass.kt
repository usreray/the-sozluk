package com.thesozluk.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource

/**
 * Frosted glass for the floating bars: a screen marks its scrolling content with [glassSource]
 * and the bars over it blur what passes underneath ([glass]). Each destination gets its own
 * state (see BackSwipe); outside one, or with the setting off, nothing changes.
 */
val LocalGlass = compositionLocalOf<HazeState?> { null }

private val glassStyle = HazeBlurStyle { blurRadius(22.dp) }

/** The content the bars of this screen blur. */
@Composable
fun Modifier.glassSource(): Modifier {
    val state = LocalGlass.current ?: return this
    if (!AppSettings.blurEffects.value) return this
    return hazeSource(state)
}

/** True when bars on this screen are drawn as glass (so they use a see-through color). */
@Composable
fun glassActive(): Boolean = LocalGlass.current != null && AppSettings.blurEffects.value

/** Blurs the content of [state] (or this screen's) behind this element, clipped to [shape]. */
@Composable
fun Modifier.glass(shape: Shape, state: HazeState? = LocalGlass.current): Modifier {
    if (state == null || !AppSettings.blurEffects.value) return this
    return clip(shape).hazeBlur(HazeInput.Sources(state), glassStyle)
}

/**
 * Open sheets blur the app behind them. A sheet calls [BlurBehind] in its content; the app's
 * root reads [count] and softens itself while any is open.
 */
object SheetBlur {
    var count by mutableIntStateOf(0)
        private set

    @Composable
    fun BlurBehind() {
        DisposableEffect(Unit) {
            count++
            onDispose { count-- }
        }
    }
}
