package dev.hhsnopek.photopen.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoldersTest {
    private fun item(
        id: Long,
        bucketId: String,
        bucketName: String,
    ) = MediaItem(
        id = id,
        uri = android.net.Uri.EMPTY,
        isVideo = false,
        dateMillis = 0L,
        durationMs = 0L,
        width = 10,
        height = 10,
        bucketId = bucketId,
        bucketName = bucketName,
        mimeType = "image/jpeg",
    )

    @Test
    fun foldersAreGroupedCountedAndSortedByName() {
        val folders =
            foldersOf(
                listOf(
                    item(1, "10", "Camera"),
                    item(2, "10", "Camera"),
                    item(3, "20", "Movies"),
                    item(4, "30", "Download"),
                ),
            )

        assertEquals(listOf("Camera", "Download", "Movies"), folders.map { it.name })
        assertEquals(listOf(2, 1, 1), folders.map { it.count })
    }

    @Test
    fun sameNameOnTwoVolumesStaysTwoFolders() {
        val folders = foldersOf(listOf(item(1, "10", "Camera"), item(2, "99", "Camera")))
        assertEquals(2, folders.size)
        assertEquals(setOf("id:10", "id:99"), folders.map { it.key }.toSet())
    }

    @Test
    fun filteringKeepsOnlyTheSelectedFolder() {
        val items = listOf(item(1, "10", "Camera"), item(2, "20", "Movies"), item(3, "10", "Camera"))
        assertEquals(listOf(1L, 3L), filterByFolder(items, "id:10").map { it.id })
        assertEquals(items, filterByFolder(items, null))
        assertTrue(filterByFolder(items, "id:missing").isEmpty())
    }

    @Test
    fun rowsWithoutBucketIdFallBackToName() {
        assertEquals("name:Camera", item(1, "", "Camera").folderKey)
    }

    @Test
    fun unnamedBucketGetsAReadableLabel() {
        val folders = foldersOf(listOf(item(1, "10", "")))
        assertEquals(UNTITLED, folders.single().name)
    }
}
