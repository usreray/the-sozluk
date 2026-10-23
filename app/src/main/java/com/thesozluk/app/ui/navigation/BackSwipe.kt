package com.thesozluk.app.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.thesozluk.app.ui.components.LocalGlass
import dev.chrisbanes.haze.rememberHazeState

/**
 * Leaving a page by going back, as in current Android apps: the page follows the back gesture
 * to the right while it shrinks a little and rounds its corners, and the page below stays still
 * behind it, already in place. Letting go carries it off; pulling back brings it home.
 *
 * The navigation host drives this from the gesture itself (predictive back seeks the pop
 * transition), so this only maps the transition's progress to a layer transform: the page is not
 * redrawn while it moves. Pushing a new page doesn't use it; the new page slides over as before.
 */
@Composable
fun AnimatedContentScope.BackSwipe(
    navController: NavController,
    entry: NavBackStackEntry,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    // 0 while shown, 1 once gone; seeked with the finger during the gesture
    val progress by transition.animateFloat(
        transitionSpec = { tween(320, easing = LinearEasing) },
        label = "backProgress"
    ) { state -> if (state == EnterExitState.PostExit) 1f else 0f }
    // Only the page being popped moves: it is still the top of the stack when its exit starts.
    // Remembered for the whole exit, as the stack changes once the gesture completes.
    var popping by remember { mutableStateOf(false) }
    val exiting = transition.targetState == EnterExitState.PostExit
    if (exiting && !popping && navController.currentBackStackEntry?.id == entry.id) popping = true
    if (!exiting && transition.currentState == EnterExitState.Visible) popping = false
    val active = enabled && popping

    // The page coming back from below: it rises from slightly smaller and dimmed as the top page
    // leaves. It is not the top of the stack yet while the gesture runs, unlike a pushed page.
    val shown by transition.animateFloat(
        transitionSpec = { tween(320, easing = LinearEasing) },
        label = "backReveal"
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    var revealing by remember { mutableStateOf(false) }
    val entering = transition.targetState == EnterExitState.Visible && transition.currentState != EnterExitState.Visible
    if (entering && !revealing && navController.currentBackStackEntry?.id != entry.id) revealing = true
    if (!entering) revealing = false
    // Any page can be the one below (the home tabs too), so this does not depend on [enabled]
    val reveal = revealing

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (reveal) {
                    val s = 0.94f + 0.06f * shown
                    scaleX = s
                    scaleY = s
                    return@graphicsLayer
                }
                if (!active || progress == 0f) return@graphicsLayer
                // Shrink and round early in the swipe; the slide starts slow and gathers pace,
                // so a short swipe peeks and a long one throws the page off
                val early = (progress * 2.5f).coerceAtMost(1f)
                val eased = 1f - (1f - early) * (1f - early) * (1f - early)
                val scale = 1f - 0.1f * eased
                scaleX = scale
                scaleY = scale
                translationX = size.width * progress * progress * 1.05f
                shape = RoundedCornerShape((32 * eased).dp)
                clip = true
                shadowElevation = 32.dp.toPx() * eased
            }
            // A shade over the returning page that lifts as it comes forward
            .drawWithContent {
                drawContent()
                if (reveal && shown < 1f) drawRect(Color.Black.copy(alpha = 0.28f * (1f - shown)))
            }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // The floating bars of this page blur its content (see glassSource / glass)
        CompositionLocalProvider(LocalGlass provides rememberHazeState(), content = content)
    }
}

/** A destination whose page follows the back gesture (see [BackSwipe]). */
fun NavGraphBuilder.screen(
    navController: NavController,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    backSwipe: Boolean = true,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = enterTransition,
        popExitTransition = popExitTransition
    ) { entry ->
        BackSwipe(navController, entry, enabled = backSwipe) { content(entry) }
    }
}
