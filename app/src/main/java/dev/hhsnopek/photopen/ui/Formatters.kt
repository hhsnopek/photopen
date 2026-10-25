package dev.hhsnopek.photopen.ui

import java.util.Locale

/**
 * "0:07" for short clips, "1:02:07" for long ones. MediaStore reports 0 for
 * videos it has not probed yet, which reads better as "0:00" than as blank.
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
