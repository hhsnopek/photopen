package dev.hhsnopek.photopen.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Reports a short tap without consuming any pointer change.
 *
 * A locked viewer has no visible chrome, so a tap has to be able to bring the
 * unlock control back. The obvious tool for that, `detectTapGestures`, consumes
 * the initial press, which would stop the pager from ever seeing a swipe and
 * the zoom surface from seeing a pinch. This detector therefore only observes:
 * the page can still be dragged, and a drag is not mistaken for a tap.
 */
suspend fun PointerInputScope.detectTapWithoutConsuming(onTap: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startedAt = down.uptimeMillis
        var moved = false
        var lastAt = startedAt
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            pressed = event.changes.any { it.pressed }
            lastAt = event.changes.first().uptimeMillis
            val slop = viewConfiguration.touchSlop
            if (event.changes.any { (it.position - it.previousPosition).getDistance() > slop }) {
                moved = true
            }
        }
        // The upper bound keeps a three second hold on the lock control (which
        // consumes its press anyway) from counting as a tap.
        if (!moved && lastAt - startedAt <= TAP_TIMEOUT_MS) onTap()
    }
}

private const val TAP_TIMEOUT_MS = 400L
