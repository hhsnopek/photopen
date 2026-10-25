package dev.hhsnopek.photopen.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import dev.hhsnopek.photopen.data.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ZoomableImageTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val landscape =
        MediaItem(
            id = 1L,
            uri = android.net.Uri.parse("file:///android_asset/missing.jpg"),
            isVideo = false,
            dateMillis = 0L,
            durationMs = 0L,
            width = 1600,
            height = 1200,
            bucketName = "test",
            mimeType = "image/jpeg",
        )

    @Test
    fun pinchToFourTimesReportsAboutFour() {
        var scale = 1f
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                ZoomableImage(
                    item = landscape,
                    onScaleChange = { scale = it },
                )
            }
        }

        composeRule.onNodeWithTag(ZOOMABLE_TAG).performTouchInput {
            pinch(
                start0 = center + Offset(-50f, 0f),
                start1 = center + Offset(50f, 0f),
                end0 = center + Offset(-200f, 0f),
                end1 = center + Offset(200f, 0f),
            )
        }
        composeRule.waitForIdle()

        // 100px -> 400px is a 4x spread; the clamp allows it, so the reported
        // scale must be near 4 (not 1, and not the 5x ceiling).
        assertEquals(4f, scale, 0.3f)
    }

    @Test
    fun pinchBeyondTheCeilingStopsAtFive() {
        var scale = 1f
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                ZoomableImage(
                    item = landscape,
                    onScaleChange = { scale = it },
                )
            }
        }

        composeRule.onNodeWithTag(ZOOMABLE_TAG).performTouchInput {
            pinch(
                start0 = center + Offset(-20f, 0f),
                start1 = center + Offset(20f, 0f),
                end0 = center + Offset(-380f, 0f),
                end1 = center + Offset(380f, 0f),
            )
        }
        composeRule.waitForIdle()

        assertEquals(MAX_SCALE, scale, 0.01f)
    }
}
