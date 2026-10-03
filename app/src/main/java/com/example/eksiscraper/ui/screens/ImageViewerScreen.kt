package com.example.eksiscraper.ui.screens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.eksiscraper.network.EksiNetworkDataSource
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.MessageState
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Images opened from a list (an author's uploads): the viewer pages through them left and right.
 * Set right before navigating to the viewer; a single image needs nothing here.
 */
object ImageGallery {
    var refs: List<String> = emptyList()
}

/**
 * An image from ekşi's uploader, full screen: pinch or double tap to zoom, drag down or up to
 * close, swipe sideways to the next one when opened from a gallery.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageViewerScreen(ref: String, navController: NavController) {
    val refs = remember(ref) { ImageGallery.refs.takeIf { ref in it } ?: listOf(ref) }
    val pager = rememberPagerState(initialPage = refs.indexOf(ref).coerceAtLeast(0)) { refs.size }
    // Resolved file addresses per ref, shared by the pages and the share / open buttons
    val urls = remember { mutableStateMapOf<String, String>() }
    val errors = remember { mutableStateMapOf<String, String>() }

    // Drag-to-close: the image follows the finger and the black fades
    val dismiss = remember { Animatable(0f) }
    val density = LocalDensity.current
    val closeDistance = with(density) { 140.dp.toPx() }
    val scope = rememberCoroutineScope()
    var zoomed by remember { mutableStateOf(false) }

    val background = (1f - abs(dismiss.value) / (closeDistance * 2.5f)).coerceIn(0.3f, 1f)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = background))) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = !zoomed,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomed) {
                    if (zoomed) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (abs(dismiss.value) > closeDistance) navController.popBackStack()
                                else dismiss.animateTo(0f)
                            }
                        },
                        onDragCancel = { scope.launch { dismiss.animateTo(0f) } }
                    ) { change, amount ->
                        change.consume()
                        scope.launch { dismiss.snapTo(dismiss.value + amount) }
                    }
                }
                .graphicsLayer { translationY = dismiss.value }
        ) { page ->
            val pageRef = refs[page]
            LaunchedEffect(pageRef) {
                if (urls[pageRef] != null) return@LaunchedEffect
                try {
                    urls[pageRef] = EksiNetworkDataSource.resolveImageUrl(pageRef)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors[pageRef] = e.message ?: "görsel açılamadı"
                }
            }
            ZoomableImage(
                url = urls[pageRef],
                error = errors[pageRef],
                onZoomChange = { if (page == pager.currentPage) zoomed = it }
            )
        }

        val current = urls[refs.getOrNull(pager.currentPage)]
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        FloatingTopBar(
            title = if (refs.size > 1) "${pager.currentPage + 1} / ${refs.size}" else null,
            onBack = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopCenter),
            // Same icon color as the back button
            actions = if (current == null) null else ({
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, current)
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = "paylaş")
                }
                IconButton(onClick = { runCatching { uriHandler.openUri(current) } }) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "tarayıcıda aç")
                }
            })
        )
    }
}

/** One image: pinch to zoom, pan while zoomed, double tap to toggle 2.5x. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ZoomableImage(url: String?, error: String?, onZoomChange: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var loadFailed by remember { mutableStateOf(false) }
    fun setScale(value: Float) {
        scale = value
        if (value == 1f) offset = Offset.Zero
        onZoomChange(value > 1f)
    }
    val transform = rememberTransformableState { zoom, pan, _ ->
        setScale((scale * zoom).coerceIn(1f, 6f))
        if (scale > 1f) offset += pan * scale
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            error != null || loadFailed -> MessageState(icon = Icons.Rounded.BrokenImage, title = "görsel açılamadı", message = error)
            url == null -> LoadingIndicator()
            else -> AsyncImage(
                model = url,
                contentDescription = "görsel",
                contentScale = ContentScale.Fit,
                onError = { loadFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { setScale(if (scale > 1f) 1f else 2.5f) })
                    }
                    // Panning only while zoomed, so swipes go to the pager and drag-to-close
                    .transformable(transform, canPan = { scale > 1f })
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}
