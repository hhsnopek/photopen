package dev.hhsnopek.photopen.ui.player

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hhsnopek.photopen.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The lock control: hold it for three seconds and a ring fills, then the player
 * flips between locked and unlocked.
 *
 * It stays visible in both states on purpose. A locked viewer with no visible
 * control would have no way back, and the whole point of the lock is that the
 * person holding the phone cannot leave the viewer.
 */
@Composable
fun HoldToLockButton(
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit = {},
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val label = stringResource(if (locked) R.string.hold_to_unlock else R.string.hold_to_lock)
    val hold = remember { HoldState() }
    // The effect below restarts on every press; a stale lambda from an older
    // composition would otherwise flip the wrong state.
    val latestOnLockedChange = rememberUpdatedState(onLockedChange)

    // The countdown runs on a timer rather than on pointer events: a finger
    // held still sends no moves, so a movement-driven countdown would never
    // reach three seconds. The timer also drives the ring.
    LaunchedEffect(hold.pressed, hold.fired) {
        if (!hold.pressed) {
            progress = 0f
            return@LaunchedEffect
        }
        if (hold.fired) return@LaunchedEffect
        while (isActive && hold.pressed && !hold.fired) {
            val elapsed = SystemClock.uptimeMillis() - hold.startedAt
            progress = (elapsed.toFloat() / HOLD_MS.toFloat()).coerceIn(0f, 1f)
            if (elapsed >= HOLD_MS) {
                hold.fired = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                latestOnLockedChange.value(!locked)
                break
            }
            delay(TICK_MS)
        }
    }

    Column(
        modifier =
            modifier
                .testTag(HOLD_TO_LOCK_TAG)
                .semantics {
                    contentDescription = label
                    stateDescription = if (locked) "locked" else "unlocked"
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        hold.startedAt = SystemClock.uptimeMillis()
                        hold.fired = false
                        hold.pressed = true
                        onPressChange(true)
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                        }
                        hold.pressed = false
                        onPressChange(false)
                    }
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(RING_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            Ring(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (locked) 0.95f else 0.8f),
                modifier = Modifier.size(ICON_SIZE),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * Drawn under the icon: a full circle of travel while the press is held, so a
 * long press has a visible deadline instead of an invisible one.
 */
@Composable
private fun Ring(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val inset = stroke / 2f
        if (progress > 0f) {
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke),
            )
        } else {
            drawArc(
                color = Color.White.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke),
            )
        }
    }
}

private class HoldState {
    var pressed by mutableStateOf(false)
    var fired by mutableStateOf(false)
    var startedAt: Long = 0L
}

const val HOLD_TO_LOCK_TAG = "hold-to-lock"

/** Three seconds: long enough that a swipe that starts on the control cannot trip it. */
const val HOLD_MS = 3_000L

private const val TICK_MS = 16L

private val ICON_SIZE = 26.dp
private val RING_SIZE = 44.dp
