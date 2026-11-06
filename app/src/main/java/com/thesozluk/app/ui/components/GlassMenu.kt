package com.thesozluk.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

/**
 * The app's ⋮ menus. With liquid glass on they open inside the app, over its content, so they can
 * be glass like the bars (a system popup is a window of its own and can't see the app behind
 * it); otherwise they are Material's menus as before. Fill one with [AppMenuItem] and dividers.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    /** Material's grouped (expressive) menu when not glass; false for the plain one */
    grouped: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!liquidGlass()) {
        if (grouped) {
            DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) { content() }
            }
        } else {
            DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, content = content)
        }
        return
    }
    // Where the menu's anchor (the box around the ⋮ button) is on screen
    var anchor by remember { mutableStateOf(Rect.Zero) }
    Layout(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            anchor = (coordinates.parentLayoutCoordinates ?: coordinates).boundsInRoot()
        }
    ) { _, _ -> layout(0, 0) {} }
    val currentContent by rememberUpdatedState(content)
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val request = remember {
        GlassMenus.Request(anchor = { anchor }, onDismiss = { currentDismiss() }, content = { currentContent() })
    }
    DisposableEffect(expanded) {
        if (expanded) GlassMenus.current = request
        onDispose { if (GlassMenus.current === request) GlassMenus.current = null }
    }
}

/** One row of an [AppMenu]. */
@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    color: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified
) {
    if (!LocalInGlassMenu.current) {
        DropdownMenuItem(
            text = { Text(text, color = color) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = if (iconTint.isSpecified()) iconTint else androidx.compose.material3.LocalContentColor.current) } },
            onClick = onClick
        )
        return
    }
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = ripple(), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (iconTint.isSpecified()) iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (color.isSpecified()) color else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Color.isSpecified() = this != Color.Unspecified

private val LocalInGlassMenu = staticCompositionLocalOf { false }

/** The open glass menu, shown by [GlassMenuHost] at the app's root. */
object GlassMenus {
    class Request(
        val anchor: () -> Rect,
        val onDismiss: () -> Unit,
        val content: @Composable ColumnScope.() -> Unit
    )

    var current by mutableStateOf<Request?>(null)
        internal set
}

/**
 * Draws the open [AppMenu] over the app: a glass card that springs open from the corner of its
 * ⋮ button (below it, or above when there is no room), closing on a tap outside or back.
 */
@Composable
fun GlassMenuHost(glassState: HazeState?) {
    val request = GlassMenus.current
    // Kept while it closes, so it can shrink away with its items
    var shown by remember { mutableStateOf<GlassMenus.Request?>(null) }
    val progress = remember { Animatable(0f) }
    val reduced = motionReduced()
    LaunchedEffect(request) {
        if (request != null) {
            shown = request
            progress.snapTo(0f)
            if (reduced) progress.snapTo(1f) else progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 520f))
        } else if (shown != null) {
            if (reduced) progress.snapTo(0f) else progress.animateTo(0f, tween(140))
            shown = null
        }
    }
    val menu = shown ?: return
    BackHandler(enabled = request != null) { menu.onDismiss() }
    val density = LocalDensity.current
    var menuWidth by remember(menu) { mutableIntStateOf(0) }
    var menuHeight by remember(menu) { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .then(if (request != null) Modifier.pointerInput(menu) { detectTapGestures { menu.onDismiss() } } else Modifier)
    ) {
        val anchor = menu.anchor()
        val margin = with(density) { 8.dp.toPx() }
        val gap = with(density) { 4.dp.toPx() }
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        // Right edges lined up with the button; below it unless that runs off the screen
        val x = (anchor.right - menuWidth).coerceIn(margin, (maxW - menuWidth - margin).coerceAtLeast(margin))
        val below = anchor.bottom + gap + menuHeight <= maxH - margin
        val y = if (below) anchor.bottom + gap else (anchor.top - gap - menuHeight).coerceAtLeast(margin)
        val p = progress.value
        Box(
            Modifier
                .offset { IntOffset(x.toInt(), y.toInt()) }
                .widthIn(min = 196.dp, max = 300.dp)
                .width(IntrinsicSize.Max)
                .onSizeChanged { menuWidth = it.width; menuHeight = it.height }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(1f, if (below) 0f else 1f)
                    val s = 0.85f + 0.15f * p
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                    clip = true
                    this.shape = shape
                }
                // Taps on the card itself don't close it
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            GlassBackdrop(shape, Modifier.matchParentSize(), state = glassState)
            CompositionLocalProvider(LocalInGlassMenu provides true) {
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { menu.content(this) }
            }
        }
    }
}
