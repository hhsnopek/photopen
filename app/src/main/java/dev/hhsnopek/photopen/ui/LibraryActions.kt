package dev.hhsnopek.photopen.ui

import androidx.compose.runtime.Immutable
import dev.hhsnopek.photopen.data.MediaItem

/**
 * The callbacks the library grid needs, grouped so the screen keeps a small
 * parameter list and Compose can treat them as one stable value.
 */
@Immutable
class LibraryActions(
    val onRequestAccess: () -> Unit,
    val onOpenItem: (MediaItem) -> Unit,
)
