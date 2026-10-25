package dev.hhsnopek.photopen.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.hhsnopek.photopen.data.MediaItem
import androidx.media3.common.MediaItem as ExoMediaItem

/**
 * A video page that autoplays as soon as it becomes the current page.
 *
 * The player is created with the page and released when the page leaves the
 * composition, so paging through a library of clips never keeps more than one
 * decoder alive. Playback pauses when the app goes to the background instead
 * of continuing to burn battery with the screen off.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPage(
    item: MediaItem,
    showControls: Boolean,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val player =
        remember(item.key) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(ExoMediaItem.fromUri(item.uri))
                prepare()
                playWhenReady = true
            }
        }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> player.pause()
                    Lifecycle.Event.ON_START -> player.play()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tapModifier =
        if (onTap != null) {
            Modifier.pointerInput(Unit) { detectTapWithoutConsuming(onTap) }
        } else {
            Modifier
        }

    Box(modifier = modifier.fillMaxSize().then(tapModifier)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = showControls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                }
            },
            update = { view ->
                view.player = player
                // Locked mode hides the transport controls so a tap on the
                // video cannot reveal a way out of the viewer.
                view.useController = showControls
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
