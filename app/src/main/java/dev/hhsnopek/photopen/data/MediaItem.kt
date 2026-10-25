package dev.hhsnopek.photopen.data

import android.net.Uri

/**
 * One row from MediaStore: either an image or a video. The player only needs
 * the URI, the kind and the geometry, so nothing else is kept in memory.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateMillis: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val bucketId: String = "",
    val bucketName: String,
    val mimeType: String,
) {
    /**
     * Width over height, or 1 when MediaStore never recorded a size (some
     * older camera apps do not). The grid uses it to reserve the right box
     * before the bitmap arrives.
     */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f

    val key: String
        get() = if (isVideo) "v$id" else "i$id"
}
