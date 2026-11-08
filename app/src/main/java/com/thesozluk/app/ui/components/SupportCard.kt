package com.thesozluk.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thesozluk.app.R
import kotlin.math.PI
import kotlin.math.sin

private const val SUPPORT_URL = "https://buymeacoffee.com/usreray"

/**
 * "destek ol": a small drawn scene (a steaming cup with the app's mark on it, in front of soft
 * hills with a house and a tree), a line on why, and the way to Buy Me a Coffee. Flat shapes in
 * the theme's colors, so it suits every palette, light or dark; the steam drifts unless motion is
 * reduced.
 */
@Composable
fun SupportCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val shape = RoundedCornerShape(24.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.cardEdge(shape)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SupportIllustration(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Column(modifier = Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("destek ol", style = MaterialTheme.typography.titleLarge)
                Text(
                    "the sözlük ücretsiz ve açık kaynak. beğendiysen geliştirilmesine bir kahveyle destek olabilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp).padding(bottom = 4.dp)
            ) {
                Button(onClick = { uriHandler.openUri(SUPPORT_URL) }, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(R.drawable.buymeacoffee_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.height(20.dp).width(14.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("kahve ısmarla")
                }
                OutlinedButton(onClick = { uriHandler.openUri("https://github.com/usreray") }) {
                    Icon(painterResource(R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("usreray")
                }
            }
        }
    }
}

@Composable
private fun SupportIllustration(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val sky = colors.primaryContainer
    val farHill = colors.primary.copy(alpha = 0.22f)
    val nearHill = colors.primary.copy(alpha = 0.36f)
    val building = colors.primary.copy(alpha = 0.5f)
    val window = colors.primaryContainer
    val cup = Color(0xFFF7F5FF)
    val cupShade = Color(0xFFDCD6F5)
    val coffee = Color(0xFF6B4A3A)
    // The app's mark on the cup, in the palette's color: its deep tone, so it stands out on the
    // white cup in a dark theme too (where the palette's main color is light)
    val accent = colors.primary
    val markColor = if (accent.luminance() > 0.5f) colors.onPrimary else accent
    val logo = rememberVectorPainter(remember(markColor) { BrandIcons.logo(markColor, markColor) })

    val still = motionReduced()
    val drift = rememberInfiniteTransition(label = "steam")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "steamPhase"
    )
    val phase = if (still) 0.25f else t

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(sky)

        // Two rows of soft hills
        drawPath(hills(w, h, baseY = h * 0.66f, amp = h * 0.07f, waves = 1.6f, shift = 0.1f), farHill)
        drawPath(hills(w, h, baseY = h * 0.80f, amp = h * 0.05f, waves = 1.2f, shift = 0.55f), nearHill)

        // A little house on the left
        val hx = w * 0.12f
        val hy = h * 0.70f
        val hs = h * 0.13f
        drawRect(building, topLeft = Offset(hx, hy - hs), size = Size(hs * 1.1f, hs))
        drawPath(Path().apply {
            moveTo(hx - hs * 0.15f, hy - hs)
            lineTo(hx + hs * 0.55f, hy - hs * 1.6f)
            lineTo(hx + hs * 1.25f, hy - hs)
            close()
        }, building)
        drawRect(window, topLeft = Offset(hx + hs * 0.35f, hy - hs * 0.65f), size = Size(hs * 0.3f, hs * 0.3f))

        // A round tree on the right
        val tx = w * 0.86f
        val ty = h * 0.74f
        drawRect(building, topLeft = Offset(tx - h * 0.012f, ty - h * 0.14f), size = Size(h * 0.024f, h * 0.14f))
        drawCircle(building, radius = h * 0.075f, center = Offset(tx, ty - h * 0.18f))

        // The cup, standing on the near hill
        val cw = h * 0.42f
        val ch = h * 0.36f
        val cx = w * 0.5f - cw / 2f
        val cy = h * 0.86f - ch
        // Handle
        drawArc(
            cupShade,
            startAngle = -90f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx + cw - ch * 0.22f, cy + ch * 0.18f),
            size = Size(ch * 0.5f, ch * 0.5f),
            style = Stroke(width = h * 0.04f)
        )
        drawRoundRect(cup, topLeft = Offset(cx, cy), size = Size(cw, ch), cornerRadius = CornerRadius(cw * 0.12f, cw * 0.12f))
        // Shade along the right side
        drawRoundRect(
            cupShade,
            topLeft = Offset(cx + cw * 0.78f, cy + ch * 0.12f),
            size = Size(cw * 0.12f, ch * 0.76f),
            cornerRadius = CornerRadius(cw * 0.06f, cw * 0.06f)
        )
        // Coffee seen at the rim
        drawOval(coffee, topLeft = Offset(cx + cw * 0.06f, cy - ch * 0.07f), size = Size(cw * 0.88f, ch * 0.16f))
        drawOval(cup, topLeft = Offset(cx, cy - ch * 0.1f), size = Size(cw, ch * 0.2f), style = Stroke(width = h * 0.018f))
        // The app's mark on the cup
        val mark = ch * 0.6f
        translate(cx + cw * 0.42f - mark / 2f, cy + ch * 0.5f - mark / 2f) {
            with(logo) { draw(Size(mark, mark)) }
        }

        // Steam: three soft curls rising and fading
        for (i in 0..2) {
            val p = (phase + i / 3f) % 1f
            val sx = cx + cw * (0.28f + 0.22f * i)
            val baseY = cy - ch * 0.14f
            val rise = h * 0.22f
            val path = Path().apply {
                val steps = 16
                for (s in 0..steps) {
                    val f = s / steps.toFloat()
                    val y = baseY - rise * f - p * h * 0.06f
                    val x = sx + sin((f * 1.6f + p + i * 0.3f) * 2f * PI.toFloat()) * h * 0.025f
                    if (s == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path,
                Color.White.copy(alpha = 0.75f * (1f - p)),
                style = Stroke(width = h * 0.022f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun hills(w: Float, h: Float, baseY: Float, amp: Float, waves: Float, shift: Float): Path = Path().apply {
    moveTo(0f, h)
    val steps = 40
    for (s in 0..steps) {
        val x = w * s / steps
        val y = baseY - amp * sin(((s / steps.toFloat()) * waves + shift) * 2f * PI.toFloat())
        lineTo(x, y)
    }
    lineTo(w, h)
    close()
}
