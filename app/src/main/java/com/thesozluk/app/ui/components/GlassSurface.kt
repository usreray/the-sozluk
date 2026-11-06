package com.thesozluk.app.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.thesozluk.app.settings.AppSettings
import dev.chrisbanes.haze.HazeState

/** "sıvı cam" is on (it is built on blur, so that has to be on too). */
@Composable
fun liquidGlass(): Boolean = AppSettings.blurEffects.value && AppSettings.liquidGlass.value

/**
 * The glass of a floating surface, as on the nav bar: what is behind it frosted, a see-through
 * tint of the surface color, light on the rim taken from the colors passing behind it ([rim],
 * Android 13+), and a fine light line along the top edge. It is a layer of its own, to be put
 * under the surface's content (`Modifier.matchParentSize()` in a Box), so only the backdrop is
 * ever bent and lit, never the surface's own text and icons. Without glass (no [state], or blur
 * off) it is the plain surface color.
 */
@Composable
fun GlassBackdrop(
    shape: Shape,
    modifier: Modifier = Modifier,
    state: HazeState? = LocalGlass.current,
    alpha: Float = 0.62f,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    rim: Boolean = true
) {
    val active = state != null && AppSettings.blurEffects.value
    Box(
        modifier
            .then(if (active && rim) Modifier.glassRim(shape) else Modifier)
            .glass(shape, state)
            .background(if (active) color.copy(alpha = alpha) else color, shape)
            .then(if (active) Modifier.glassEdge(shape) else Modifier)
    )
}

/**
 * A floating surface on [GlassBackdrop] when liquid glass is on; otherwise the frosted surface
 * the app has always had ([shadow] only without glass: over glass it shows as a dark rim).
 */
@Composable
fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shadow: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable () -> Unit
) {
    if (liquidGlass() && LocalGlass.current != null) {
        Box(modifier, propagateMinConstraints = true) {
            GlassBackdrop(shape, Modifier.matchParentSize())
            if (onClick != null) Surface(onClick = onClick, shape = shape, color = Color.Transparent, content = content)
            else Surface(shape = shape, color = Color.Transparent, content = content)
        }
        return
    }
    val glass = glassActive()
    val color = MaterialTheme.colorScheme.surfaceContainerHigh.let { if (glass) it.copy(alpha = 0.62f) else it }
    val elevation = if (glass) 0.dp else shadow
    val surfaceModifier = modifier.glass(shape)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, shadowElevation = elevation, modifier = surfaceModifier, content = content)
    } else {
        Surface(shape = shape, color = color, shadowElevation = elevation, modifier = surfaceModifier, content = content)
    }
}

/**
 * Room for a button (FAB, toolbar) to sit on liquid glass: the glass under it, and the button
 * drawn see-through and flat on top. Without liquid glass, just the button.
 */
@Composable
fun LiquidGlassBehind(shape: Shape, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (!(liquidGlass() && LocalGlass.current != null)) {
        Box(modifier, propagateMinConstraints = true) { content() }
        return
    }
    Box(modifier, propagateMinConstraints = true) {
        GlassBackdrop(shape, Modifier.matchParentSize())
        content()
    }
}

/** Cards and list rows under liquid glass: the bars' fine light line along their top edge. */
@Composable
fun Modifier.cardEdge(shape: Shape): Modifier = if (liquidGlass()) glassEdge(shape) else this

/** The light catching a glass surface's top edge, fading out down its sides. */
internal fun Modifier.glassEdge(shape: Shape): Modifier = drawWithCache {
    val stroke = 0.75.dp.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    val clip = Path().apply { addOutline(outline) }
    val edge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.27f), Color.White.copy(alpha = 0.02f)))
    onDrawWithContent {
        drawContent()
        // Centered on the outline and clipped to it: a hairline just inside the edge
        clipPath(clip) { drawOutline(outline, edge, style = Stroke(stroke * 2)) }
    }
}

/**
 * Light on a glass surface's rim, colored by what is behind it: near the edge the surface samples
 * its own (blurred) backdrop a little further in, one channel at a time, and adds that as a sheen
 * that is brighter along the top, with a thin bright line just inside the edge. Works for any
 * rounded rectangle (pills, circles, cards) through its corner [radius].
 *
 * On a light theme adding light would only wash the light surface out to white, so there the
 * rim takes on the backdrop's colors by blending towards them instead, and the bright line is
 * only a trace ([light] = 1).
 */
private const val GLASS_RIM = """
uniform shader content;
uniform float2 size;
uniform float density;
uniform float radius;
uniform float light;

half4 main(float2 p) {
    half4 base = content.eval(p);
    float2 h = size * 0.5;
    float r = min(radius, min(h.x, h.y));
    float2 local = p - h;
    float2 q = abs(local) - (h - r);
    float2 qc = max(q, float2(0.0));
    float sdf = length(qc) + min(max(q.x, q.y), 0.0) - r;
    float depth = -sdf / density;
    if (depth < 0.0 || depth > 14.0) return base;
    float2 s = float2(local.x < 0.0 ? -1.0 : 1.0, local.y < 0.0 ? -1.0 : 1.0);
    float2 n = float2(0.0);
    if (q.x > 0.0 && q.y > 0.0) {
        n = qc / max(length(qc), 0.001) * s;
    } else if (q.x > q.y) {
        n = float2(s.x, 0.0);
    } else {
        n = float2(0.0, s.y);
    }
    float glow = exp(-depth * 0.32);
    float top = 0.25 + 0.75 * max(-n.y, 0.0);
    half3 bent = half3(
        content.eval(p - n * density * 10.0).r,
        content.eval(p - n * density * 18.0).g,
        content.eval(p - n * density * 26.0).b
    );
    float amount = glow * top;
    float line = exp(-pow((depth - 0.6) / 0.5, 2.0));
    float lineTop = 0.4 + 0.6 * max(-n.y, 0.0);
    // Dark: the backdrop's colors added as light. Light: blended towards, so nothing turns white
    half3 added = base.rgb + bent * 0.55 * amount + half3(0.22) * line * lineTop;
    half3 blended = mix(base.rgb, bent, half(0.45 * amount)) + half3(0.05) * line * lineTop;
    half3 color = mix(added, blended, half(light));
    return half4(clamp(color, 0.0, 1.0), base.a);
}
"""

@Composable
private fun Modifier.glassRim(shape: Shape): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val shader = remember { RuntimeShader(GLASS_RIM) }
    val light = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) 1f else 0f
    return graphicsLayer {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("density", density)
        shader.setFloatUniform("radius", cornerRadius(shape, size, this))
        shader.setFloatUniform("light", light)
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        clip = true
        this.shape = shape
    }
}

/** The (top start) corner radius of [shape] at [size], which the rim light follows. */
private fun cornerRadius(shape: Shape, size: Size, density: androidx.compose.ui.unit.Density): Float =
    when (val outline = shape.createOutline(size, LayoutDirection.Ltr, density)) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Rectangle -> 0f
        is Outline.Generic -> minOf(size.width, size.height) / 2f
    }
