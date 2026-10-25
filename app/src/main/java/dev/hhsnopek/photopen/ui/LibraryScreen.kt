package dev.hhsnopek.photopen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import dev.hhsnopek.photopen.R
import dev.hhsnopek.photopen.data.Folder
import dev.hhsnopek.photopen.data.MediaItem
import kotlinx.collections.immutable.ImmutableList

/**
 * Grid of everything MediaStore will show, newest first, with a video badge and
 * duration on clips.
 */
@Composable
fun LibraryScreen(
    state: LibraryState,
    partialAccess: Boolean,
    folders: ImmutableList<Folder>,
    selectedFolder: String?,
    onSelectFolder: (String?) -> Unit,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    var showFolders by remember { mutableStateOf(false) }
    val folderName =
        if (selectedFolder == null) {
            stringResource(R.string.all_photos)
        } else {
            folders.firstOrNull { it.key == selectedFolder }?.name ?: stringResource(R.string.all_photos)
        }

    if (showFolders) {
        FolderSheet(
            folders = folders,
            selectedFolder = selectedFolder,
            onSelect = {
                onSelectFolder(it)
                showFolders = false
            },
            onDismiss = { showFolders = false },
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                // The app draws edge to edge; chrome must not hide under the
                // status bar, and the last row must clear the gesture bar.
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        LibraryHeader(
            count = (state as? LibraryState.Content)?.items?.size,
            partialAccess = partialAccess,
            folderName = folderName,
            onChooseFolder = { showFolders = true },
            onRequestAccess = actions.onRequestAccess,
        )
        when (state) {
            LibraryState.Loading -> CenteredBox { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            LibraryState.NoAccess ->
                AccessPrompt(
                    onRequestAccess = actions.onRequestAccess,
                )
            LibraryState.Empty ->
                CenteredBox {
                    Message(
                        icon = true,
                        title =
                            stringResource(
                                if (partialAccess) {
                                    R.string.empty_partial_title
                                } else {
                                    R.string.empty_library_title
                                },
                            ),
                        body =
                            stringResource(
                                if (partialAccess) {
                                    R.string.empty_partial_body
                                } else {
                                    R.string.empty_library_body
                                },
                            ),
                    )
                }
            is LibraryState.Content ->
                if (state.items.isEmpty()) {
                    CenteredBox {
                        Message(
                            icon = true,
                            title = stringResource(R.string.folder_empty_title),
                            body = stringResource(R.string.folder_empty_body),
                        )
                    }
                } else {
                    MediaGrid(
                        items = state.items,
                        onOpenItem = actions.onOpenItem,
                        gridState = gridState,
                    )
                }
        }
    }
}

@Composable
private fun LibraryHeader(
    count: Int?,
    partialAccess: Boolean,
    folderName: String,
    onChooseFolder: () -> Unit,
    onRequestAccess: () -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (count != null) {
                    Text(
                        text = pluralStringResource(R.plurals.item_count, count, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (partialAccess) {
                TextButton(onClick = onRequestAccess) {
                    Text(text = stringResource(R.string.limited_action))
                }
            }
        }
        TextButton(
            onClick = onChooseFolder,
            modifier = Modifier.padding(start = 4.dp).testTag(FOLDER_CHIP_TAG),
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = folderName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.choose_folder),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        if (partialAccess) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.limited_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSheet(
    folders: ImmutableList<Folder>,
    selectedFolder: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(FOLDER_SHEET_TAG),
    ) {
        Text(
            text = stringResource(R.string.choose_folder),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            item {
                FolderRow(
                    name = stringResource(R.string.all_photos),
                    count = folders.sumOf { it.count },
                    selected = selectedFolder == null,
                    onClick = { onSelect(null) },
                    tag = FOLDER_ALL_TAG,
                )
            }
            items(items = folders, key = { it.key }) { folder ->
                FolderRow(
                    name = folder.name,
                    count = folder.count,
                    selected = folder.key == selectedFolder,
                    onClick = { onSelect(folder.key) },
                    tag = FOLDER_ROW_TAG_PREFIX + folder.key,
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(tag)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.item_count, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp).size(20.dp),
            )
        }
    }
}

@Composable
private fun MediaGrid(
    items: ImmutableList<MediaItem>,
    onOpenItem: (MediaItem) -> Unit,
    gridState: LazyGridState,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = items, key = { it.key }) { item ->
            Thumbnail(item = item, onClick = { onOpenItem(item) })
        }
    }
}

@Composable
private fun Thumbnail(
    item: MediaItem,
    onClick: () -> Unit,
) {
    val request =
        ImageRequest
            .Builder(LocalContext.current)
            .data(item.uri)
            .apply { if (item.isVideo) videoFrameMillis(VIDEO_FRAME_MS) }
            .crossfade(120)
            .build()

    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = request,
            contentDescription = item.bucketName.ifBlank { null },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Badge(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun Badge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun AccessPrompt(onRequestAccess: () -> Unit) {
    CenteredBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.access_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.access_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onRequestAccess,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(text = stringResource(R.string.access_button))
            }
        }
    }
}

@Composable
private fun Message(
    icon: Boolean,
    title: String,
    body: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private const val VIDEO_FRAME_MS = 1_000L

const val FOLDER_CHIP_TAG = "folder-chip"
const val FOLDER_SHEET_TAG = "folder-sheet"
const val FOLDER_ALL_TAG = "folder-row-all"
const val FOLDER_ROW_TAG_PREFIX = "folder-row-"
