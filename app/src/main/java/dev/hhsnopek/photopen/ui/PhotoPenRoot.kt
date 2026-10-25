package dev.hhsnopek.photopen.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hhsnopek.photopen.data.MediaItem
import dev.hhsnopek.photopen.data.MediaPermissions
import dev.hhsnopek.photopen.data.filterByFolder
import dev.hhsnopek.photopen.data.foldersOf
import dev.hhsnopek.photopen.ui.player.PlayerScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Owns the permission handshake and the switch between the library and the
 * viewer.
 *
 * The open item is stored by its stable key rather than by index: a photo taken
 * while the viewer is open shifts every index, but the key still resolves.
 *
 * The media permission is re-read on every resume because two grants never come
 * back through the launcher callback: the API 34 "select photos" flow and a
 * change made in system settings.
 */
@Composable
fun PhotoPenRoot() {
    val context = LocalContext.current
    val viewModel: LibraryViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    var access by remember { mutableStateOf(MediaPermissions.snapshot(context)) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            access = MediaPermissions.snapshot(context)
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Reload even when the flags look unchanged: the picker
                    // returns a new selection without touching permissions.
                    val snapshot = MediaPermissions.snapshot(context)
                    access = snapshot
                    viewModel.onAccessChanged(snapshot)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(access) { viewModel.onAccessChanged(access) }

    val allItems = (state as? LibraryState.Content)?.items
    val folders = remember(allItems) { allItems?.let { foldersOf(it) }.orEmpty().toPersistentList() }
    val visibleItems: ImmutableList<MediaItem>? =
        allItems?.let { filterByFolder(it, selectedFolder).toPersistentList() }

    // A folder can disappear (deleted, revoked access); fall back to everything
    // instead of showing an empty grid forever. Only judge that once items have
    // loaded: at first composition the list is still empty and every folder
    // would look missing.
    LaunchedEffect(selectedFolder, folders, allItems) {
        if (allItems != null && allItems.isNotEmpty() && selectedFolder != null &&
            folders.none { it.key == selectedFolder }
        ) {
            viewModel.selectFolder(null)
        }
    }

    val openIndex = if (visibleItems != null && openKey != null) visibleItems.indexOfFirst { it.key == openKey } else -1

    if (visibleItems != null && openIndex >= 0) {
        PlayerScreen(
            items = visibleItems,
            initialIndex = openIndex,
            onClose = { openKey = null },
        )
    } else {
        LibraryScreen(
            state = if (visibleItems == null) state else LibraryState.Content(visibleItems),
            partialAccess = access.granted && !access.full,
            folders = folders,
            selectedFolder = selectedFolder,
            onSelectFolder = viewModel::selectFolder,
            actions =
                LibraryActions(
                    onRequestAccess = { permissionLauncher.launch(MediaPermissions.requested()) },
                    onOpenItem = { item -> openKey = item.key },
                ),
        )
    }
}
