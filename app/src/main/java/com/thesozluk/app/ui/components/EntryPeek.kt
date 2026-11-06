package com.thesozluk.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.thesozluk.app.model.Entry
import com.thesozluk.app.settings.AppSettings
import kotlinx.coroutines.launch

/** One thing an entry's menu can do; shared by the ⋮ menu and the long-press preview. */
class EntryMenuItem(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Long-pressing an entry lifts it out of the list: the app behind blurs and dims, the entry rises
 * from where it was to where it fits with room for its actions, and the actions open under it.
 * Tapping outside (or back) puts it back where it came from.
 */
object EntryPeek {
    class Peek(
        val entry: Entry,
        val number: Int?,
        val actions: EntryActions,
        /** Where the card sat on screen, in window coordinates */
        val bounds: Rect,
        val menu: List<EntryMenuItem>
    )

    var current by mutableStateOf<Peek?>(null)
        private set
    /** Set while the preview is going back, so the card in the list stays hidden until it lands */
    internal var closing by mutableStateOf(false)

    fun open(peek: Peek) {
        if (current != null) return
        closing = false
        current = peek
    }

    internal fun close() {
        current = null
        closing = false
    }

    fun isLifted(entryId: String) = current?.entry?.entryId == entryId
}

@Composable
fun EntryPeekOverlay(
    /** The app behind, for the menu's glass when liquid glass is on */
    glassState: dev.chrisbanes.haze.HazeState? = null
) {
    val peek = EntryPeek.current ?: return
    // Bumps the app's own blur, like a sheet does
    SheetBlur.BlurBehind()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val reduced = motionReduced()
    val progress = remember(peek) { Animatable(0f) }
    var cardHeight by remember(peek) { mutableIntStateOf(0) }
    var menuHeight by remember(peek) { mutableIntStateOf(0) }
    var leaving by remember(peek) { mutableStateOf(false) }

    fun dismiss(then: (() -> Unit)? = null) {
        if (leaving) return
        leaving = true
        EntryPeek.closing = true
        scope.launch {
            if (reduced) progress.snapTo(0f) else progress.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 600f))
            EntryPeek.close()
            then?.invoke()
        }
    }
    BackHandler { dismiss() }

    LaunchedEffect(peek, cardHeight > 0 && menuHeight > 0) {
        if (cardHeight == 0 || menuHeight == 0) return@LaunchedEffect
        if (reduced) progress.snapTo(1f) else progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 380f))
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(peek) { detectTapGestures { dismiss() } }
    ) {
        val screenH = constraints.maxHeight.toFloat()
        val topSafe = WindowInsets.statusBars.getTop(density) + with(density) { 16.dp.toPx() }
        val bottomSafe = WindowInsets.navigationBars.getBottom(density) + with(density) { 16.dp.toPx() }
        val gap = with(density) { 12.dp.toPx() }
        val bounds = peek.bounds
        // Shrink a tall card so it and its menu both fit; then find the nearest top where they do
        val room = screenH - topSafe - bottomSafe - gap - menuHeight
        val fit = if (cardHeight > 0 && room > 0) (room / cardHeight).coerceAtMost(1f) else 1f
        val lift = 1.03f
        val shownHeight = cardHeight * fit
        val targetTop = bounds.top.coerceIn(topSafe, (screenH - bottomSafe - menuHeight - gap - shownHeight).coerceAtLeast(topSafe))
        val p = progress.value

        // Dim over the blurred app
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.32f))
        )

        // The card itself, the same card as in the list, but only to look at
        Box(
            Modifier
                .offset { IntOffset(bounds.left.toInt(), bounds.top.toInt()) }
                .width(with(density) { bounds.width.toDp() })
                .onSizeChanged { cardHeight = it.height }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    translationY = (targetTop - bounds.top) * p
                    val s = 1f + (lift * fit - 1f) * p
                    scaleX = s
                    scaleY = s
                    shadowElevation = 24.dp.toPx() * p.coerceIn(0f, 1f)
                    shape = RoundedCornerShape(28.dp)
                    clip = false
                }
                .pointerInput(peek) {
                    // Looking only: nothing on the card reacts, a tap on it closes like outside
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { if (it.pressed != it.previousPressed && !it.pressed) dismiss() }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            EntryCard(
                entry = peek.entry,
                isExpanded = false,
                onToggleExpand = {},
                actions = peek.actions,
                number = peek.number,
                showExpandToggle = false,
                peekable = false
            )
        }

        // The actions, opening from under the card
        val menuTop = targetTop + shownHeight * lift + gap
        val menuShape = RoundedCornerShape(24.dp)
        val glass = AppSettings.blurEffects.value
        val liquid = liquidGlass() && glassState != null
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(-(constraints.maxWidth - bounds.right).toInt().coerceAtLeast(0), menuTop.toInt()) }
                .width(260.dp)
                .onSizeChanged { menuHeight = it.height }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(1f, 0f)
                    val s = 0.6f + 0.4f * p
                    scaleX = s
                    scaleY = s
                    alpha = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    // Over glass a shadow would show through as a dark rim
                    shadowElevation = if (liquid) 0f else 12.dp.toPx() * p.coerceIn(0f, 1f)
                    shape = menuShape
                    clip = true
                }
                .then(
                    if (liquid) Modifier
                    else Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.let { if (glass) it.copy(alpha = 0.96f) else it }, menuShape)
                        .border(0.75.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.02f))), menuShape)
                )
        ) {
            if (liquid) GlassBackdrop(menuShape, Modifier.matchParentSize(), state = glassState)
            Column(Modifier.padding(vertical = 6.dp)) {
            peek.menu.forEachIndexed { index, item ->
                if (item.destructive && index > 0) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp, horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                val color = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple()) {
                            dismiss(item.onClick)
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(item.icon, contentDescription = null, tint = if (item.destructive) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(item.label, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1)
                }
            }
            }
        }
    }
}
