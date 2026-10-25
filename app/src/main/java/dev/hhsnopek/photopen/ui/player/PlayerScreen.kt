package dev.hhsnopek.photopen.ui.player

import android.app.Activity
import android.app.ActivityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.hhsnopek.photopen.R
import dev.hhsnopek.photopen.data.MediaItem
import dev.hhsnopek.photopen.ui.findActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

/**
 * Full-screen viewer: one page per photo or video, swiped horizontally.
 *
 * Only the current page is interactive. Neighbours are composed one page away
 * so a swipe lands on a decoded image or a ready poster frame, and video pages
 * that are not current release their decoder.
 */
@Composable
fun PlayerScreen(
    items: ImmutableList<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState =
        rememberPagerState(
            initialPage = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        ) { items.size }

    var locked by rememberSaveable { mutableStateOf(false) }

    // Lock task mode is the only way an ordinary app can stop Home, Recents and
    // the notification shade. Android asks the user to confirm pinning the first
    // time; if they decline (or the device forbids it) the in-app lock still
    // holds, it just cannot cover the system exits.
    val activity = LocalContext.current.findActivity()
    LaunchedEffect(locked, activity) {
        if (activity == null) return@LaunchedEffect
        runCatching {
            if (locked) {
                activity.startLockTask()
            } else if (activity.isPinned()) {
                activity.stopLockTask()
            }
        }
    }
    DisposableEffect(activity) {
        onDispose {
            // Never leave the device pinned after the player is gone.
            if (activity != null && activity.isPinned()) {
                runCatching { activity.stopLockTask() }
            }
        }
    }

    // While locked the unlock control gets out of the way: it lingers for a
    // moment after locking, fades out, and comes back on a tap. Taps do not
    // reveal the rest of the chrome, and a tap cannot unlock: only the hold can.
    var lockControlVisible by remember { mutableStateOf(false) }
    var revealRequest by remember { mutableIntStateOf(0) }
    var controlPressed by remember { mutableStateOf(false) }
    LaunchedEffect(locked, revealRequest, controlPressed) {
        if (!locked) {
            lockControlVisible = true
            return@LaunchedEffect
        }
        lockControlVisible = true
        // A finger on the control outranks the countdown: fading the control
        // out from under a hold would cancel the unlock halfway through.
        if (controlPressed) return@LaunchedEffect
        delay(if (revealRequest == 0) LOCK_HINT_LINGER_MS else LOCK_HINT_REVEAL_MS)
        lockControlVisible = false
    }

    // Back is a way out, so while locked it is swallowed rather than closing
    // the viewer. There is no drawer or toolbar to close; the only chrome is
    // removed by the `locked` branch below.
    BackHandler {
        if (!locked) onClose()
    }

    KeepScreenOn(enabled = locked)
    ImmersiveSystemBars()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items[page].key },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                if (page == pagerState.currentPage) {
                    VideoPage(
                        item = item,
                        showControls = !locked,
                        onTap = if (locked) ({ revealRequest++ }) else null,
                    )
                } else {
                    VideoPoster(item = item)
                }
            } else {
                ZoomableImage(
                    item = item,
                    zoomEnabled = !locked,
                    onTap = { if (locked) revealRequest++ },
                )
            }
        }
        if (!locked) {
            PlayerChrome(
                page = pagerState.currentPage,
                total = items.size,
                onClose = onClose,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
        AnimatedVisibility(
            visible = !locked || lockControlVisible,
            enter = fadeIn(animationSpec = tween(FADE_IN_MS)),
            exit = fadeOut(animationSpec = tween(FADE_OUT_MS)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            HoldToLockButton(
                locked = locked,
                onLockedChange = { nowLocked ->
                    locked = nowLocked
                    if (nowLocked) revealRequest = 0
                },
                onPressChange = { controlPressed = it },
            )
        }
    }
}

private fun Activity.isPinned(): Boolean =
    getSystemService(ActivityManager::class.java)?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

/**
 * A locked viewer is meant to be looked at, not to sleep after thirty seconds.
 * The flag is dropped on unlock and when the player leaves the screen.
 */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun PlayerChrome(
    page: Int,
    total: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.close),
                tint = Color.White,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.page_counter, page + 1, total),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

/**
 * The viewer owns the whole screen. Bars come back when the player is left, so
 * the library keeps a normal status bar.
 */
@Composable
private fun ImmersiveSystemBars() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private const val LOCK_HINT_LINGER_MS = 2_000L
private const val LOCK_HINT_REVEAL_MS = 3_000L
private const val FADE_IN_MS = 200
private const val FADE_OUT_MS = 400
