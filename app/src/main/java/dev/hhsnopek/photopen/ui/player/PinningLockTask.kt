package dev.hhsnopek.photopen.ui.player

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.hhsnopek.photopen.R
import dev.hhsnopek.photopen.ui.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Keeps the phone inside the app while the viewer is locked.
 *
 * Lock task mode is the only mechanism an ordinary app has to stop Home,
 * Recents and the notification shade. Android may ask the user to confirm
 * pinning the first time, and some builds keep pinning behind a device setting;
 * when that happens the in-app lock still holds, it just cannot cover the system
 * exits, so the user is told once and offered the settings screen.
 */
@Composable
fun PinningLockTask(locked: Boolean) {
    val activity = LocalContext.current.findActivity()
    var showHint by remember { mutableStateOf(false) }
    var requestedSettings by remember { mutableStateOf(false) }

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

    LaunchedEffect(locked, activity) {
        if (!locked || activity == null) {
            showHint = false
            return@LaunchedEffect
        }
        var waited = 0L
        while (isActive) {
            if (activity.isPinned()) {
                showHint = false
                break
            }
            if (waited >= HINT_DELAY_MS) showHint = true
            delay(POLL_MS)
            waited += POLL_MS
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, locked, activity) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && locked && activity != null && requestedSettings) {
                    requestedSettings = false
                    runCatching { if (!activity.isPinned()) activity.startLockTask() }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showHint && locked) {
        AlertDialog(
            onDismissRequest = { showHint = false },
            title = { Text(text = stringResource(R.string.pinning_hint_title)) },
            text = { Text(text = stringResource(R.string.pinning_hint_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHint = false
                        requestedSettings = true
                        runCatching {
                            activity?.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.pinning_hint_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHint = false }) {
                    Text(text = stringResource(R.string.pinning_hint_not_now))
                }
            },
        )
    }
}

private const val HINT_DELAY_MS = 3_000L
private const val POLL_MS = 500L

private fun Activity.isPinned(): Boolean =
    getSystemService(ActivityManager::class.java)?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
