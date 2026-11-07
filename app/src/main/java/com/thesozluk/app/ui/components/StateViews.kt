package com.thesozluk.app.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Morphing M3 Expressive loading indicator. With several [messages] the caption rotates while
 * the user waits, so a slow ekşi response still feels alive.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingState(modifier: Modifier = Modifier, messages: List<String> = listOf("yükleniyor")) {
    var index by remember { mutableIntStateOf(0) }
    if (messages.size > 1) {
        LaunchedEffect(messages) {
            while (true) {
                delay(1800)
                index = (index + 1) % messages.size
            }
        }
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ContainedLoadingIndicator(modifier = Modifier.size(72.dp))
            AnimatedContent(
                targetState = messages[index % messages.size],
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "loadingCaption"
            ) { caption ->
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A small drawn scene behind a state's icon instead of a bare icon: a slowly turning soft
 * shape, a second shape peeking from behind, a few dots around, and the icon in front of
 * them on its own shape, bobbing gently. Colors come from the theme, so it fits any palette;
 * [isError] gives it the error tones. Still when animations are reduced.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StateIllustration(icon: ImageVector, isError: Boolean = false, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val back = if (isError) colors.errorContainer else colors.tertiaryContainer
    val side = if (isError) colors.surfaceContainerHighest else colors.primaryContainer
    val front = if (isError) colors.error else colors.primary
    val onFront = if (isError) colors.onError else colors.onPrimary
    val still = motionReduced()
    val motion = rememberInfiniteTransition(label = "stateIllustration")
    val turn by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "turn"
    )
    val bob by motion.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    Box(modifier = modifier.size(176.dp), contentAlignment = Alignment.Center) {
        // The big soft shape behind, turning slowly
        Surface(
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = back,
            modifier = Modifier.size(148.dp).graphicsLayer { rotationZ = if (still) 0f else turn }
        ) {}
        // A second, smaller shape peeking out at the top left
        Surface(
            shape = MaterialShapes.Pill.toShape(),
            color = side,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 18.dp)
                .size(width = 54.dp, height = 30.dp)
                .graphicsLayer { rotationZ = -24f }
        ) {}
        // Dots, like specks of light around it
        Canvas(Modifier.fillMaxSize()) {
            val dot = front.copy(alpha = 0.55f)
            drawCircle(dot, radius = 4.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.22f))
            drawCircle(dot, radius = 3.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.74f))
            drawCircle(dot.copy(alpha = 0.35f), radius = 6.dp.toPx(), center = Offset(size.width * 0.82f, size.height * 0.84f))
        }
        // The icon in front, gently floating
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = front,
            shadowElevation = 6.dp,
            modifier = Modifier.size(84.dp).graphicsLayer { translationY = if (still) 0f else bob.dp.toPx() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = onFront, modifier = Modifier.size(38.dp))
            }
        }
    }
}

/** A drawn illustration, a title, an explanation and an optional action. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    action: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StateIllustration(icon, isError = isError, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                // Explicit: inside the search bar the inherited content color isn't the theme's
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (action != null) Box(modifier = Modifier.padding(top = 8.dp)) { action() }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    MessageState(
        icon = Icons.Rounded.CloudOff,
        title = "bağlantı kurulamadı",
        message = message,
        modifier = modifier,
        isError = true
    ) {
        Button(onClick = onRetry, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("tekrar dene", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
