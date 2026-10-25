package dev.hhsnopek.photopen.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomMathTest {
    @Test
    fun `scale is clamped to the usable range`() {
        assertEquals(MIN_SCALE, clampScale(0.2f), 0.001f)
        assertEquals(MAX_SCALE, clampScale(9f), 0.001f)
        assertEquals(2.5f, clampScale(2.5f), 0.001f)
    }

    @Test
    fun `offset is pinned to zero at fit scale`() {
        val offset =
            clampOffset(Offset(400f, -400f), MIN_SCALE, IntSize(1000, 2000), IntSize(1000, 2000))
        assertEquals(Offset.Zero, offset)
    }

    @Test
    fun `offset is bounded by half the overflow in each direction`() {
        val offset =
            clampOffset(Offset(10_000f, -10_000f), 2f, IntSize(1000, 2000), IntSize(1000, 2000))
        assertEquals(500f, offset.x, 0.001f)
        assertEquals(-1000f, offset.y, 0.001f)
    }

    @Test
    fun `a legal offset is left alone`() {
        val offset =
            clampOffset(Offset(120f, -80f), 2f, IntSize(1000, 2000), IntSize(1000, 2000))
        assertEquals(120f, offset.x, 0.001f)
        assertEquals(-80f, offset.y, 0.001f)
    }

    @Test
    fun `a letterboxed photo cannot be panned into its bars`() {
        // 4:3 photo fitted into a 1080x1920 phone: 1080x810. At 2.5x it is
        // 2700x2025, so there is 810px of horizontal room and only 52px
        // vertically, no matter how far the finger travels.
        val offset =
            clampOffset(Offset(0f, 900f), 2.5f, IntSize(1080, 810), IntSize(1080, 1920))
        assertEquals(0f, offset.x, 0.001f)
        assertEquals(52.5f, offset.y, 0.001f)
    }

    @Test
    fun `an axis that is still smaller than the viewport stays centred`() {
        // The same photo at 2x is 1620px tall in a 1920px window.
        val offset =
            clampOffset(Offset(0f, 900f), 2f, IntSize(1080, 810), IntSize(1080, 1920))
        assertEquals(0f, offset.y, 0.001f)
    }

    @Test
    fun `a wide photo is letterboxed to width`() {
        val fitted = fittedSize(IntSize(1080, 1920), 16f / 9f)
        assertEquals(1080, fitted.width)
        assertEquals(607, fitted.height)
    }

    @Test
    fun `a tall video fills the height`() {
        val fitted = fittedSize(IntSize(1080, 1920), 9f / 16f)
        assertEquals(1080, fitted.width)
        assertEquals(1920, fitted.height)
    }

    @Test
    fun `an unmeasured container cannot pan`() {
        val offset = clampOffset(Offset(50f, 50f), 3f, IntSize.Zero, IntSize(1000, 1000))
        assertEquals(Offset.Zero, offset)
    }
}
