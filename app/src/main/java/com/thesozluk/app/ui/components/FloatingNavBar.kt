package com.thesozluk.app.ui.components

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import com.thesozluk.app.settings.NavBarStyle
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Room the bar takes over the content of top-level screens. Lists add it to their bottom
 * padding so the last rows can scroll out from under the bar.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

val FloatingNavBarInset: Dp = 96.dp

data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

/**
 * Scrolling down a tab slims the floating bar to its icons; scrolling back up brings the labels
 * back. Hooked to the app's content with [connection]; [labels] is 1 for labels, 0 for icons only.
 */
object NavBarScroll {
    var labels by mutableFloatStateOf(1f)
        private set
    private var travel = 0f

    fun expand() {
        labels = 1f
        travel = 0f
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            // A change of direction starts the count again
            if (travel != 0f && (travel < 0f) != (available.y < 0f)) travel = 0f
            travel += available.y
            if (travel < -120f) labels = 0f else if (travel > 120f) labels = 1f
            return Offset.Zero
        }
    }
}

/** The app's floating bottom navigation, with or without labels as chosen in settings. */
@Composable
fun FloatingNavBar(
    destinations: List<NavDestination>,
    selectedRoute: String?,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    /** The app content behind the bar, blurred into it */
    glassState: HazeState? = null
) {
    val selected = destinations.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    JellyBar(
        destinations = destinations,
        selected = selected,
        onSelect = { onSelect(destinations[it]) },
        labelsAllowed = AppSettings.navBarStyle.value == NavBarStyle.Floating,
        glassState = glassState,
        modifier = modifier.navigationBarsPadding().padding(bottom = 16.dp)
    )
}

private val BarInset = 4.dp

private val PreviewDestinations = listOf(
    NavDestination("preview/home", "akış", Icons.Rounded.Home, Icons.Outlined.Home),
    NavDestination("preview/search", "ara", Icons.Rounded.Search, Icons.Rounded.Search),
    NavDestination("preview/random", "rastgele", Icons.Rounded.Casino, Icons.Outlined.Casino),
    NavDestination("preview/profile", "profil", Icons.Rounded.Person, Icons.Outlined.Person)
)

/** The bar in the chosen style, working but going nowhere, for the settings page. */
@Composable
fun NavBarPreview(modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(0) }
    JellyBar(
        destinations = PreviewDestinations,
        selected = selected,
        onSelect = { selected = it },
        labelsAllowed = AppSettings.navBarStyle.value == NavBarStyle.Floating,
        glassState = null,
        modifier = modifier,
        followScroll = false
    )
}

/** A damped spring stepped by hand each frame, so many of them can share one clock. */
private class FrameSpring(var value: Double, private val stiffness: Double, private val damping: Double) {
    var velocity = 0.0

    fun settled(target: Double) = abs(value - target) < 1e-4 && abs(velocity) < 1e-4

    fun snap(to: Double) {
        value = to
        velocity = 0.0
    }

    fun step(target: Double, dt: Double) {
        if (settled(target)) return snap(target)
        val x = value - target
        val w = sqrt(stiffness)
        if (damping >= 1.0) {
            // Critically damped
            val e = exp(-w * dt)
            val c = velocity + w * x
            value = target + (x + c * dt) * e
            velocity = (velocity - w * c * dt) * e
        } else {
            val a = damping * w
            val wd = w * sqrt(1 - damping * damping)
            val e = exp(-a * dt)
            val cos = cos(wd * dt)
            val sin = sin(wd * dt)
            val newX = e * (x * cos + (velocity + a * x) / wd * sin)
            velocity = e * (velocity * cos - (a * velocity + stiffness * x) / wd * sin)
            value = target + newX
        }
    }
}

/** What the bar looks like on one frame; sizes in px. */
private data class BarFrame(
    val position: Float,
    val pillScaleX: Float = 1f,
    val pillScaleY: Float = 1f,
    val litScale: Float = 1f,
    val lean: Float = 0f,
    val swell: Float = 1f,
    val narrow: Float = 1f,
    val pull: Float = 0f,
    val originX: Float = 0f
)

/**
 * The bar's motion. Touching it picks the pill up onto the tab under the finger, puffed up to
 * 1.3×, with the lit icons grown; the whole bar swells a little. Moving the finger carries the
 * pill (stretched by its smoothed speed), leans the bar a few pixels along, and pulling up or down
 * drags the bar that way like rubber while it narrows around the finger. Letting go settles the
 * pill on the nearest tab, and the pill stays puffed until it gets there.
 */
@Stable
private class BarMotion(initial: Int, private var count: Int, private val dp: Float) {
    private val position = FrameSpring(initial.toDouble(), 1000.0, 1.0)
    private val speed = FrameSpring(0.0, 300.0, 0.5)
    private val press = FrameSpring(0.0, 1000.0, 1.0)
    private val puffX = FrameSpring(1.0, 250.0, 0.6)
    private val puffY = FrameSpring(1.0, 250.0, 0.7)
    private val lean = FrameSpring(0.0, 300.0, 1.0)
    // The whole bar's give settles with a little wobble
    private val wobble = 18.0 / (2 * sqrt(216.0))
    private val pull = FrameSpring(0.0, 267.0, wobble)
    private val narrow = FrameSpring(1.0, 267.0, wobble)
    private val swell = FrameSpring(1.0, 267.0, wobble)

    private var target = position.value
    private var pressTarget = 0.0
    private var puffTarget = 1.0
    private var settling = false
    private var downX = 0.0
    private var startTarget = 0.0
    private var startLean = 0.0
    private var startPull = 0.0
    private var moved = 0.0
    private var originX = 0.0
    private var width = 0.0
    private var height = 0.0
    private val last get() = (count - 1).coerceAtLeast(0)
    private val tabWidth get() = ((width - 8 * dp) / count.coerceAtLeast(1)).coerceAtLeast(0.0)

    var held = false
        private set
    var running by mutableStateOf(false)
        private set
    var frame by mutableStateOf(BarFrame(initial.toFloat()))
        private set
    /** The tab the pill is over while held, for a tick when it changes */
    val hoveredTab get() = floor(target + 0.5).toInt().coerceIn(0, last)

    fun resize(width: Int, height: Int, count: Int) {
        this.width = width.toDouble()
        this.height = height.toDouble()
        this.count = count
        target = target.coerceIn(0.0, last.toDouble())
        if (!held) originX = this.width / 2
        publish()
    }

    fun select(index: Int) {
        held = false
        target = index.coerceIn(0, last).toDouble()
        settling = true
        pressTarget = 0.0
        puffTarget = 1.0
        running = true
    }

    fun begin(x: Float) {
        downX = x.toDouble()
        originX = downX.coerceIn(0.0, width)
        startPull = pull.value
        moved = 0.0
        if (tabWidth > 0) target = tabAt(downX).toDouble()
        startTarget = target
        startLean = lean.value
        held = true
        settling = false
        pressTarget = 1.0
        puffTarget = 1.3
        lean.velocity = 0.0
        running = true
    }

    fun drag(dx: Float, dy: Float) {
        if (!held || tabWidth <= 0) return
        target = (startTarget + dx / tabWidth).coerceIn(0.0, last.toDouble())
        lean.snap(startLean + dx)
        pull.snap(startPull + rubber(dy.toDouble(), height) * 0.25)
        narrow.snap(1 - (abs(dy) / (700.0 * dp)).coerceAtMost(1.0) * 0.08)
        originX = (downX + dx).coerceIn(0.0, width)
        moved = max(moved, max(abs(dx), abs(dy)).toDouble())
        publish()
    }

    /** Lets go; returns the tab picked: the one tapped, or the one under the pill. */
    fun release(): Int {
        val index = if (moved < 4 * dp && tabWidth > 0) tabAt(downX) else floor(target + 0.5).toInt().coerceIn(0, last)
        held = false
        lean.velocity = 0.0
        target = index.toDouble()
        settling = true
        running = true
        return index
    }

    fun cancel(selected: Int) {
        lean.velocity = 0.0
        select(selected)
    }

    fun advance(seconds: Double) {
        val dt = seconds.coerceIn(0.0, 0.064)
        target = target.coerceIn(0.0, last.toDouble())
        position.step(target, dt)
        speed.step(if (held && last > 0) position.velocity / last else 0.0, dt)
        if (!held) lean.step(0.0, dt)
        if (settling && abs(position.value - target) < max(1, last) * 0.025) {
            settling = false
            pressTarget = 0.0
            puffTarget = 1.0
        }
        press.step(pressTarget, dt)
        puffX.step(puffTarget, dt)
        puffY.step(puffTarget, dt)
        swell.step(if (held) 1.025 else 1.0, dt)
        if (!held) {
            pull.step(0.0, dt)
            narrow.step(1.0, dt)
            if (narrow.settled(1.0)) originX = width / 2
        }
        publish()
        running = held || settling || !position.settled(target) || !speed.settled(0.0) || !press.settled(0.0) ||
            !puffX.settled(1.0) || !puffY.settled(1.0) || !lean.settled(0.0) || !pull.settled(0.0) ||
            !narrow.settled(1.0) || !swell.settled(1.0)
    }

    private fun tabAt(x: Double) = floor((x - 4 * dp) / tabWidth).toInt().coerceIn(0, last)

    private fun publish() {
        val s = speed.value / 10
        frame = BarFrame(
            position = position.value.toFloat(),
            pillScaleX = (puffX.value / (1 - (s * 0.75).coerceIn(-0.2, 0.2))).toFloat(),
            pillScaleY = (puffY.value * (1 - (s * 0.25).coerceIn(-0.2, 0.2))).toFloat(),
            litScale = (1 + 0.2 * press.value).toFloat(),
            lean = leanFor(lean.value),
            swell = swell.value.toFloat(),
            narrow = narrow.value.toFloat(),
            pull = pull.value.toFloat(),
            originX = originX.toFloat()
        )
    }

    /** Pulling against a limit: the further it goes the less it gives, never past [limit]. */
    private fun rubber(distance: Double, limit: Double): Double {
        if (distance == 0.0 || limit <= 0.0) return 0.0
        val give = (1 - 1 / (abs(distance) * 0.14 / limit + 1)) * limit
        return if (distance < 0) -give else give
    }

    /** The finger's sideways travel, eased into a lean of at most 4dp. */
    private fun leanFor(raw: Double): Float {
        if (width <= 0 || raw == 0.0) return 0f
        val f = (raw / width).coerceIn(-1.0, 1.0)
        val eased = LeanEasing.transform(abs(f).toFloat())
        return (if (f < 0) -4f else 4f) * dp * eased
    }
}

private val LeanEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/**
 * A floating glass bar with a jelly pill (see [BarMotion]). The tabs are drawn twice, plain over
 * the bar and lit inside the pill, so an icon lights up exactly as far as the pill covers it.
 */
@Composable
private fun JellyBar(
    destinations: List<NavDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    labelsAllowed: Boolean,
    glassState: HazeState?,
    modifier: Modifier,
    /** Labels slim away while the app scrolls; not in the settings preview */
    followScroll: Boolean = true
) {
    val reduced = motionReduced()
    val density = LocalDensity.current
    val count = destinations.size
    val labelFraction by animateFloatAsState(
        targetValue = if (!labelsAllowed) 0f else if (followScroll) NavBarScroll.labels else 1f,
        animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "navLabels"
    )
    val motion = remember(count) { BarMotion(selected, count, density.density) }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Each pick gives the icon a small move: the dice rolls a full turn, the others wiggle
    val scope = rememberCoroutineScope()
    val turns = remember(count) { List(count) { Animatable(0f) } }
    val pops = remember(count) { List(count) { Animatable(1f) } }
    val currentDestinations by rememberUpdatedState(destinations)
    fun play(index: Int) {
        if (reduced) return
        val destination = currentDestinations.getOrNull(index) ?: return
        val dice = destination.icon == Icons.Outlined.Casino || destination.selectedIcon == Icons.Rounded.Casino
        scope.launch {
            if (dice) {
                turns[index].snapTo(0f)
                turns[index].animateTo(360f, spring(dampingRatio = 0.55f, stiffness = 180f))
                turns[index].snapTo(0f)
            } else {
                turns[index].snapTo(-14f)
                turns[index].animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 500f))
            }
        }
        scope.launch {
            pops[index].snapTo(0.7f)
            pops[index].animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 600f))
        }
    }
    LaunchedEffect(selected, count) { motion.select(selected) }
    LaunchedEffect(motion.running) {
        if (!motion.running) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (motion.running) {
            withFrameNanos { now ->
                // Reduced motion: everything lands at once
                if (reduced) repeat(60) { motion.advance(0.064) } else motion.advance((now - previous) / 1e9)
                previous = now
            }
        }
    }

    val barHeight = 48.dp + 16.dp * labelFraction
    val sidePadding = 58.dp - 30.dp * labelFraction
    val accent = MaterialTheme.colorScheme.primary
    val plain = MaterialTheme.colorScheme.onSurfaceVariant
    val pillColor = accent.copy(alpha = 0.15f)

    Box(
        modifier.fillMaxWidth().padding(horizontal = sidePadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .height(barHeight)
                .onSizeChanged { motion.resize(it.width, it.height, count) }
                .pointerInput(motion) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        motion.begin(down.position.x)
                        var claimed = false
                        var done = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.count { it.pressed } > 1) break
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val delta = change.position - down.position
                                if (max(abs(delta.x), abs(delta.y)) > viewConfiguration.touchSlop) claimed = true
                                if (claimed) change.consume()
                                motion.drag(delta.x, delta.y)
                                if (!change.pressed) {
                                    val index = motion.release()
                                    done = true
                                    change.consume()
                                    // A tap always plays; a drag only when it lands on another tab
                                    if (!claimed || index != currentSelected) play(index)
                                    currentOnSelect(index)
                                    break
                                }
                            }
                        } finally {
                            if (!done) motion.cancel(currentSelected)
                        }
                    }
                }
        ) {
            val frame = { motion.frame }
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val f = frame()
                        transformOrigin = TransformOrigin(if (size.width > 0f) f.originX / size.width else 0.5f, 0.5f)
                        scaleX = f.swell * f.narrow
                        scaleY = f.swell
                        translationX = f.lean
                        translationY = f.pull
                    }
            ) {
                // The bar: the app's glass, with light on its rim from what passes behind it
                GlassBackdrop(CircleShape, Modifier.matchParentSize(), state = glassState)
                // Plain tabs everywhere but under the pill
                Box(
                    Modifier
                        .matchParentSize()
                        .drawWithContent {
                            clipPath(pillPath(frame(), count), ClipOp.Difference) { this@drawWithContent.drawContent() }
                        }
                ) {
                    TabRow(destinations, labelFraction, lit = false, color = plain, turns = turns, pops = pops, scale = { 1f })
                }
                // The pill, with the lit tabs seen only through it
                Box(
                    Modifier
                        .matchParentSize()
                        .clearAndSetSemantics { }
                        .drawWithContent {
                            clipPath(pillPath(frame(), count)) {
                                drawRect(pillColor, topLeft = Offset(-48.dp.toPx(), -16.dp.toPx()),
                                    size = Size(size.width + 96.dp.toPx(), size.height + 32.dp.toPx()))
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    TabRow(destinations, labelFraction, lit = true, color = accent, turns = turns, pops = pops, scale = { frame().litScale })
                }
                // What screen readers and keyboards use; touches are handled by the bar itself
                Row(Modifier.matchParentSize().padding(horizontal = BarInset).selectableGroup()) {
                    destinations.forEachIndexed { index, destination ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clearAndSetSemantics {
                                    role = Role.Tab
                                    this.selected = index == selected
                                    contentDescription = destination.label
                                    onClick { play(index); motion.select(index); onSelect(index); true }
                                }
                        )
                    }
                }
            }
        }
    }
}

/** The pill's outline on this frame, in the bar's coordinates. */
private fun DrawScope.pillPath(frame: BarFrame, count: Int): Path {
    val inset = BarInset.toPx()
    val tabWidth = (size.width - 2 * inset) / count
    val itemHeight = size.height - 2 * inset
    val centerX = inset + (frame.position + 0.5f) * tabWidth
    val halfWidth = tabWidth * frame.pillScaleX / 2
    val halfHeight = itemHeight * frame.pillScaleY / 2
    val radius = minOf(tabWidth, itemHeight) / 2
    return Path().apply {
        addRoundRect(
            RoundRect(
                centerX - halfWidth, size.height / 2 - halfHeight, centerX + halfWidth, size.height / 2 + halfHeight,
                CornerRadius(radius * frame.pillScaleX, radius * frame.pillScaleY)
            )
        )
    }
}

/** One row of tab faces, plain or lit. */
@Composable
private fun TabRow(
    destinations: List<NavDestination>,
    labelFraction: Float,
    lit: Boolean,
    color: Color,
    turns: List<Animatable<Float, AnimationVector1D>>,
    pops: List<Animatable<Float, AnimationVector1D>>,
    scale: () -> Float
) {
    Row(Modifier.fillMaxSize().padding(BarInset), verticalAlignment = Alignment.CenterVertically) {
        destinations.forEachIndexed { index, destination ->
            Box(
                Modifier.weight(1f).fillMaxHeight().graphicsLayer {
                    val s = scale()
                    scaleX = s
                    scaleY = s
                },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (lit) destination.selectedIcon else destination.icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(26.dp).graphicsLayer {
                            translationY = 2.dp.toPx() * labelFraction
                            rotationZ = turns[index].value
                            scaleX = pops[index].value
                            scaleY = pops[index].value
                        }
                    )
                    Box(
                        Modifier
                            .height(16.dp * labelFraction)
                            .fillMaxWidth()
                            .clipToBounds()
                            .graphicsLayer { alpha = labelFraction }
                    ) {
                        Text(
                            destination.label,
                            color = color,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
