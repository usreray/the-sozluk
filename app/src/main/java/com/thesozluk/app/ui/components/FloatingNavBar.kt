package com.thesozluk.app.ui.components

import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Room the floating bar takes over the content of top-level screens. Lists add it to their
 * bottom padding so the last rows can scroll out from under the bar.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

val FloatingNavBarInset: Dp = 96.dp

data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

/** Expressive floating toolbar used as navigation: the open tab grows into a labelled pill. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavBar(
    destinations: List<NavDestination>,
    selectedRoute: String?,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    /** The app content behind the bar, blurred into it */
    glassState: dev.chrisbanes.haze.HazeState? = null
) {
    val glass = glassState != null && com.thesozluk.app.settings.AppSettings.blurEffects.value
    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.glass(CircleShape, glassState),
        colors = if (glass) colors.copy(toolbarContainerColor = colors.toolbarContainerColor.copy(alpha = 0.62f)) else colors
    ) {
        destinations.forEach { destination ->
            NavPill(destination, selected = destination.route == selectedRoute) { onSelect(destination) }
        }
    }
}

@Composable
private fun NavPill(destination: NavDestination, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    // Each tap gives the icon a small move: the dice rolls a full turn, the others wiggle
    val turn = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val reduced = motionReduced()
    val play: () -> Unit = play@{
        if (reduced) return@play
        scope.launch {
            if (destination.icon == Icons.Outlined.Casino || destination.selectedIcon == Icons.Rounded.Casino) {
                turn.snapTo(0f)
                turn.animateTo(360f, spring(dampingRatio = 0.55f, stiffness = 180f))
                turn.snapTo(0f)
            } else {
                turn.snapTo(-14f)
                turn.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 500f))
            }
        }
        scope.launch {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 600f))
        }
    }
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "navPill"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navPillContent"
    )
    Surface(
        onClick = { play(); onClick() },
        shape = CircleShape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
        modifier = Modifier.height(48.dp).pressBounce(interaction, pressedScale = 0.9f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(24.dp).graphicsLayer {
                    rotationZ = turn.value
                    scaleX = pop.value
                    scaleY = pop.value
                }
            )
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
