package dev.hhsnopek.photopen.ui.player

import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HoldToLockButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun releasingBeforeThreeSecondsDoesNotLock() {
        var locked = false
        composeRule.setContent {
            HoldToLockButton(locked = false, onLockedChange = { locked = it })
        }

        composeRule.onNodeWithTag(HOLD_TO_LOCK_TAG).performTouchInput { down(center) }
        Thread.sleep(900)
        composeRule.onNodeWithTag(HOLD_TO_LOCK_TAG).performTouchInput { up() }
        composeRule.waitForIdle()

        assertFalse(locked)
    }

    @Test
    fun holdingThreeSecondsLocks() {
        var locked = false
        composeRule.setContent {
            HoldToLockButton(locked = false, onLockedChange = { locked = it })
        }

        composeRule.onNodeWithTag(HOLD_TO_LOCK_TAG).performTouchInput { down(center) }
        composeRule.waitUntil(timeoutMillis = 6_000) { locked }
        composeRule.onNodeWithTag(HOLD_TO_LOCK_TAG).performTouchInput { up() }

        assertTrue(locked)
    }
}
