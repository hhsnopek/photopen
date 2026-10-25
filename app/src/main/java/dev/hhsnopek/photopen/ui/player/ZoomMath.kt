package dev.hhsnopek.photopen.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

const val MIN_SCALE = 1f
const val MAX_SCALE = 5f

/** Zoom never goes below "fit" or above 5x, which is where the source pixels run out. */
fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

/**
 * The size a "fit" image actually occupies inside [container]. A 4:3 photo in a
 * tall phone is letterboxed, and clamping against the container instead of the
 * picture would let the user pan into the black bars.
 */
fun fittedSize(
    container: IntSize,
    aspectRatio: Float,
): IntSize {
    if (container.width <= 0 || container.height <= 0 || aspectRatio <= 0f) return IntSize.Zero
    val containerAspect = container.width.toFloat() / container.height.toFloat()
    return if (aspectRatio >= containerAspect) {
        IntSize(container.width, (container.width / aspectRatio).toInt())
    } else {
        IntSize((container.height * aspectRatio).toInt(), container.height)
    }
}

/**
 * Keeps the picture from being dragged off screen.
 *
 * [picture] is the fitted image and [viewport] is the screen; at [scale] the
 * image covers `picture * scale`. In an axis where it is larger than the
 * viewport it may pan by half the overflow, and in an axis where it is still
 * smaller (a 4:3 photo at 2x on a tall phone) it stays centred: the letterbox
 * is the picture's own shape, not free space to drag into.
 */
fun clampOffset(
    offset: Offset,
    scale: Float,
    picture: IntSize,
    viewport: IntSize,
): Offset {
    if (scale <= MIN_SCALE || picture.width <= 0 || picture.height <= 0) return Offset.Zero
    val overflowX = picture.width * scale - viewport.width
    val overflowY = picture.height * scale - viewport.height
    val maxX = (overflowX / 2f).coerceAtLeast(0f)
    val maxY = (overflowY / 2f).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}
