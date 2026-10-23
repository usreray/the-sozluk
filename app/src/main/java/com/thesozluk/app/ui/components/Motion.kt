package com.thesozluk.app.ui.components

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
