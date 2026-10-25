package dev.hhsnopek.photopen.ui

import android.net.Uri
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hhsnopek.photopen.data.MediaItem
import dev.hhsnopek.photopen.data.foldersOf
import dev.hhsnopek.photopen.ui.theme.PhotoPenTheme
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val items =
        listOf(
            item(1, "10", "Camera"),
            item(2, "10", "Camera"),
            item(3, "20", "Movies"),
        )

    @Test
    fun folderSheetListsEveryFolderAndReportsTheChoice() {
        val selections = mutableListOf<String?>()
        composeRule.setContent {
            PhotoPenTheme {
                LibraryScreen(
                    state = LibraryState.Content(items.toPersistentList()),
                    partialAccess = false,
                    folders = foldersOf(items).toPersistentList(),
                    selectedFolder = null,
                    onSelectFolder = { selections += it },
                    actions = LibraryActions(onRequestAccess = {}, onOpenItem = {}),
                )
            }
        }

        composeRule.onNodeWithTag(FOLDER_CHIP_TAG).assertTextContains("All photos")
        composeRule.onNodeWithTag(FOLDER_CHIP_TAG).performClick()
        composeRule.onNodeWithTag(FOLDER_SHEET_TAG).assertExists()
        composeRule.onNodeWithTag(FOLDER_ROW_TAG_PREFIX + "id:20").performClick()

        composeRule.runOnIdle { assertEquals(listOf("id:20"), selections) }
    }

    @Test
    fun choosingAllPhotosClearsTheFilter() {
        val selections = mutableListOf<String?>()
        composeRule.setContent {
            PhotoPenTheme {
                LibraryScreen(
                    state = LibraryState.Content(items.toPersistentList()),
                    partialAccess = false,
                    folders = foldersOf(items).toPersistentList(),
                    selectedFolder = "id:20",
                    onSelectFolder = { selections += it },
                    actions = LibraryActions(onRequestAccess = {}, onOpenItem = {}),
                )
            }
        }

        composeRule.onNodeWithTag(FOLDER_CHIP_TAG).assertTextContains("Movies")
        composeRule.onNodeWithTag(FOLDER_CHIP_TAG).performClick()
        composeRule.onNodeWithTag(FOLDER_ALL_TAG).performClick()

        composeRule.runOnIdle { assertEquals(listOf(null), selections) }
    }

    private fun item(
        id: Long,
        bucketId: String,
        bucketName: String,
    ) = MediaItem(
        id = id,
        uri = Uri.parse("file:///tmp/$id.jpg"),
        isVideo = false,
        dateMillis = id,
        durationMs = 0L,
        width = 100,
        height = 100,
        bucketId = bucketId,
        bucketName = bucketName,
        mimeType = "image/jpeg",
    )
}
