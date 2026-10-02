package com.example.eksiscraper.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.eksiscraper.network.EksiNetworkDataSource
import com.example.eksiscraper.ui.components.FloatingTopBar
import com.example.eksiscraper.ui.components.MessageState
import kotlinx.coroutines.CancellationException

/** An image from ekşi's uploader, full screen: pinch or double tap to zoom, drag to pan. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageViewerScreen(ref: String, navController: NavController) {
    var url by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(ref) {
        try {
            url = EksiNetworkDataSource.resolveImageUrl(ref)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "görsel açılamadı"
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale == 1f) Offset.Zero else offset + pan * scale
    }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val current = url
        when {
            error != null || loadFailed -> MessageState(
                icon = Icons.Rounded.BrokenImage,
                title = "görsel açılamadı",
                message = error
            )
            current == null -> LoadingIndicator()
            else -> AsyncImage(
                model = current,
                contentDescription = "görsel",
                contentScale = ContentScale.Fit,
                onError = { loadFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            // Double tap toggles between fit and 2.5x
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        })
                    }
                    .transformable(transform)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }

        FloatingTopBar(
            title = null,
            onBack = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopCenter),
            actions = if (url == null) null else ({
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = "paylaş", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { url?.let { runCatching { uriHandler.openUri(it) } } }) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "tarayıcıda aç", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        )
    }
}
