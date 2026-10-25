package dev.hhsnopek.photopen.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * What the app may read right now.
 *
 * - [granted] false means the library is invisible and the user must be asked.
 * - [full] false with [granted] true is the API 34 "select photos" grant: the
 *   library is partial, and the UI should offer to widen it.
 */
data class MediaAccess(
    val granted: Boolean,
    val full: Boolean,
) {
    companion object {
        val None = MediaAccess(granted = false, full = false)
    }
}

/**
 * Runtime permissions for MediaStore differ across three API bands, and from
 * API 34 the OS can grant a hand-picked subset instead of everything.
 */
object MediaPermissions {
    /** What to request next; on 34+ the OS also offers "select photos". */
    fun requested(): Array<String> =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun snapshot(context: Context): MediaAccess =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val images = context.isGranted(Manifest.permission.READ_MEDIA_IMAGES)
                val videos = context.isGranted(Manifest.permission.READ_MEDIA_VIDEO)
                val selected = context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                val full = images && videos
                MediaAccess(granted = full || images || videos || selected, full = full)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val images = context.isGranted(Manifest.permission.READ_MEDIA_IMAGES)
                val videos = context.isGranted(Manifest.permission.READ_MEDIA_VIDEO)
                MediaAccess(granted = images || videos, full = images && videos)
            }
            else -> {
                val legacy = context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
                MediaAccess(granted = legacy, full = legacy)
            }
        }

    private fun Context.isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
