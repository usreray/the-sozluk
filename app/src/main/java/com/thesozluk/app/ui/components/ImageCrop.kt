package com.thesozluk.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Reads a picked picture, scaled down on decode so large photos don't run out of memory. */
fun decodeForCrop(bytes: ByteArray, maxSide: Int = 2048): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

/**
 * Choosing the part of a picture to use as a profile photo, like the site's cropper: a round
 * window over the picture, which is moved with a finger and zoomed with two (a double tap
 * zooms in and back). The picture always fills the window. [onCrop] gets the square under it.
 */
@Composable
fun ProfilePhotoCropDialog(bitmap: Bitmap, onCrop: (Bitmap) -> Unit, onDismiss: () -> Unit) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // The round window: a square in the middle, a little inset from the edges
    val viewport = min(canvasSize.width, canvasSize.height) * 0.82f
    // Scale at which the picture just covers the window
    val baseScale = if (viewport > 0f) max(viewport / bitmap.width, viewport / bitmap.height) else 1f
    val scale = baseScale * zoom

    /** Keeps the window covered: the picture can't be moved past its edges. */
    fun clamp(value: Offset, z: Float): Offset {
        val s = baseScale * z
        val maxX = max(0f, (bitmap.width * s - viewport) / 2f)
        val maxY = max(0f, (bitmap.height * s - viewport) / 2f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false,
        // Edge to edge: the black reaches under the navigation bar instead of stopping above it
        decorFitsSystemWindows = false
    )) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Text(
                "fotoğrafı kırp",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(20.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                            zoom = newZoom
                            offset = clamp(offset + pan, newZoom)
                        }
                    }
                    .pointerInput(bitmap) {
                        detectTapGestures(onDoubleTap = {
                            zoom = if (zoom > 1.5f) 1f else 2.5f
                            offset = clamp(offset, zoom)
                        })
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // Offscreen, so the window can be cut out of the shade
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    val w = bitmap.width * scale
                    val h = bitmap.height * scale
                    val left = size.width / 2f - w / 2f + offset.x
                    val top = size.height / 2f - h / 2f + offset.y
                    drawImage(
                        image,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(w.roundToInt(), h.roundToInt())
                    )
                    // Darken all but the round window
                    drawRect(Color.Black.copy(alpha = 0.6f))
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(Color.Transparent, radius = viewport / 2f, center = center, blendMode = BlendMode.Clear)
                    drawImage(
                        image,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                        blendMode = BlendMode.DstOver
                    )
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = viewport / 2f, center = center, style = Stroke(2.dp.toPx()))
                }
            }
            Text(
                "sürükleyerek kaydır, iki parmakla yakınlaştır",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                TextButton(onClick = onDismiss) { Text("vazgeç", color = Color.White) }
                Button(
                    enabled = viewport > 0f,
                    onClick = {
                        // The window's square in the picture's own pixels
                        val side = (viewport / scale).roundToInt().coerceIn(1, min(bitmap.width, bitmap.height))
                        val cx = bitmap.width / 2f - offset.x / scale
                        val cy = bitmap.height / 2f - offset.y / scale
                        val x = (cx - side / 2f).roundToInt().coerceIn(0, bitmap.width - side)
                        val y = (cy - side / 2f).roundToInt().coerceIn(0, bitmap.height - side)
                        onCrop(Bitmap.createBitmap(bitmap, x, y, side, side))
                    }
                ) { Text("yükle") }
            }
        }
    }
}
