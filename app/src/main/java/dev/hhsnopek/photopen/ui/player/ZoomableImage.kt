package dev.hhsnopek.photopen.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.hhsnopek.photopen.data.MediaItem

/**
 * A photo that can be pinch-zoomed, dragged while zoomed, and toggled between
 * fit and 2.5x with a double tap.
 *
 * The transform sits on a box that is exactly the fitted picture, not on the
 * full-screen container. Scaling the container would scale its letterbox too,
 * so a 4:3 photo would keep black bars at 2.5x and the pan limits would point at
 * empty space instead of the picture edge.
 *
 * The gesture detector deliberately refuses to consume a single-finger drag at
 * 1x. Otherwise the image would swallow every horizontal swipe and the pager
 * could never change page.
 */
@Composable
fun ZoomableImage(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onScaleChange: (Float) -> Unit = {},
    onTap: () -> Unit = {},
) {
    val zoom = remember(item.key) { ZoomState() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val fitted = remember(size, item.aspectRatio) { fittedSize(size, item.aspectRatio) }
    val density = LocalDensity.current
    val pictureSize =
        if (fitted.width > 0 && fitted.height > 0) {
            with(density) {
                Modifier.size(width = fitted.width.toDp(), height = fitted.height.toDp())
            }
        } else {
            Modifier.fillMaxSize()
        }
    val scale = zoom.scale
    val offset = zoom.offset

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(ZOOMABLE_TAG)
                .onSizeChanged { size = it }
                .pointerInput(item.key) {
                    detectZoomPanAndDoubleTap(
                        currentScale = { zoom.scale },
                        onGesture = { pan, zoomChange ->
                            val nextScale = clampScale(zoom.scale * zoomChange)
                            zoom.scale = nextScale
                            zoom.offset = clampOffset(zoom.offset + pan, nextScale, fitted, size)
                            onScaleChange(nextScale)
                        },
                        onTap = onTap,
                        onDoubleTap = {
                            if (zoom.scale > MIN_SCALE) {
                                zoom.scale = MIN_SCALE
                                zoom.offset = Offset.Zero
                            } else {
                                zoom.scale = DOUBLE_TAP_SCALE
                                zoom.offset = Offset.Zero
                            }
                            onScaleChange(zoom.scale)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .then(pictureSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(item.uri)
                        .crossfade(120)
                        .build(),
                contentDescription = item.bucketName.ifBlank { null },
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private class ZoomState {
    var scale by mutableFloatStateOf(MIN_SCALE)
    var offset by mutableStateOf(Offset.Zero)
}

/**
 * Pinch, drag and double tap in one detector.
 *
 * Everything is handled here rather than by stacking detectTapGestures next to
 * detectTransformGestures because detectTapGestures consumes the initial press:
 * the pager would never see a swipe start. One finger at fit scale therefore
 * stays unconsumed, and only two fingers, or one finger on a zoomed picture, is
 * treated as ours.
 */
private suspend fun PointerInputScope.detectZoomPanAndDoubleTap(
    currentScale: () -> Float,
    onGesture: (pan: Offset, zoom: Float) -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    var lastTapAt = 0L
    var lastTapPosition = Offset.Unspecified
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var dragged = false
        var transformed = false
        var lastEventAt = down.uptimeMillis

        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            pressed = event.changes.any { it.pressed }
            if (!pressed) break
            lastEventAt = event.changes.first().uptimeMillis

            val zoomed = currentScale() > MIN_SCALE
            val multiTouch = event.changes.count { it.pressed } > 1
            if (zoomed || multiTouch) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    transformed = true
                    onGesture(panChange, zoomChange)
                    event.changes.forEach { it.consume() }
                }
            } else if (
                event.changes.any {
                    (it.position - it.previousPosition).getDistance() > viewConfiguration.touchSlop
                }
            ) {
                dragged = true
            }
        }

        if (!transformed && !dragged && lastEventAt - down.uptimeMillis <= TAP_TIMEOUT_MS) {
            // A reveal tap fires on the first tap of a double tap as well; the
            // reveal is idempotent, so a double tap still just zooms.
            onTap()
            val isDoubleTap =
                lastTapPosition != Offset.Unspecified &&
                    lastEventAt - lastTapAt <= viewConfiguration.doubleTapTimeoutMillis &&
                    (down.position - lastTapPosition).getDistance() <= viewConfiguration.touchSlop * 2f
            if (isDoubleTap) {
                onDoubleTap()
                lastTapAt = 0L
                lastTapPosition = Offset.Unspecified
            } else {
                lastTapAt = lastEventAt
                lastTapPosition = down.position
            }
        }
    }
}

const val ZOOMABLE_TAG = "viewer-image"

private const val DOUBLE_TAP_SCALE = 2.5f
private const val TAP_TIMEOUT_MS = 400L
