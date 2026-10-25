package dev.hhsnopek.photopen.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import dev.hhsnopek.photopen.data.MediaItem

/**
 * The frame shown for a video that is not the current page. It exists so a
 * swipe lands on a real picture instead of a black rectangle while the decoder
 * for the new page starts.
 */
@Composable
fun VideoPoster(
    item: MediaItem,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(item.uri)
                    .videoFrameMillis(VIDEO_POSTER_MS)
                    .build(),
            contentDescription = item.bucketName.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val VIDEO_POSTER_MS = 1_000L
