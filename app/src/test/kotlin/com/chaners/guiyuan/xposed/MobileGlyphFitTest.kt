package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobileGlyphFitTest {
    private fun rect(x0: Float, y0: Float, x1: Float, y1: Float) =
        floatArrayOf(
            0f, x0, y0,
            0.25f, x1, y0,
            0.5f, x1, y1,
            0.75f, x0, y1,
            1f, x0, y0,
        )

    @Test
    fun rectangularContoursTouchWithoutFixedTwoUnitGap() {
        assertEquals(
            8.5f,
            MobileGlyphFit.contactOffset(rect(0f, 0f, 8f, 10f), rect(0f, 0f, 4f, 6f), 2f, 0.5f)!!,
            0.0001f,
        )
    }

    @Test
    fun raisedOrLoweredSuffixTracksSharedContourNotOverallBounds() {
        val sloped =
            floatArrayOf(
                0f, 0f, 0f,
                0.25f, 10f, 0f,
                0.5f, 6f, 10f,
                0.75f, 0f, 10f,
                1f, 0f, 0f,
            )
        val suffix = rect(0f, 0f, 3f, 5f)
        assertEquals(10.4f, MobileGlyphFit.contactOffset(sloped, suffix, 0f, 0.4f)!!, 0.0001f)
        assertEquals(8.4f, MobileGlyphFit.contactOffset(sloped, suffix, 5f, 0.4f)!!, 0.0001f)
    }

    @Test
    fun movementBetweenContoursDoesNotIntroducePhantomEdges() {
        val glyph =
            floatArrayOf(
                0f, 0f, 0f, 0.125f, 5f, 0f, 0.25f, 5f, 8f,
                0.375f, 0f, 8f, 0.5f, 0f, 0f,
                0.5f, 9f, 1f, 0.625f, 10f, 1f, 0.75f, 10f, 2f,
                0.875f, 9f, 2f, 1f, 9f, 1f,
            )
        assertEquals(
            5f,
            MobileGlyphFit.contactOffset(glyph, rect(0f, 3f, 2f, 5f), 0f, 0f)!!,
            0.0001f,
        )
    }

    @Test
    fun scalesWithGlyphGeometry() {
        val main = rect(0f, 0f, 8f, 10f)
        val suffix = rect(0f, 0f, 4f, 6f)
        val unscaled = MobileGlyphFit.contactOffset(main, suffix, 2f, 0.5f)!!
        val mainDouble = main.copyOf().also { for (i in it.indices) if (i % 3 != 0) it[i] *= 2f }
        val suffixDouble = suffix.copyOf().also { for (i in it.indices) if (i % 3 != 0) it[i] *= 2f }
        assertEquals(
            unscaled * 2f,
            MobileGlyphFit.contactOffset(mainDouble, suffixDouble, 4f, 1f)!!,
            0.0001f,
        )
    }

    @Test
    fun ambiguousOrDisjointContoursFailToOriginalLayout() {
        assertNull(MobileGlyphFit.contactOffset(rect(0f, 0f, 8f, 5f), rect(0f, 0f, 4f, 2f), 10f, 0.4f))
        assertNull(MobileGlyphFit.contactOffset(floatArrayOf(0f, 0f), rect(0f, 0f, 4f, 2f), 0f, 0f))
        assertNull(
            MobileGlyphFit.contactOffset(
                rect(0f, Float.NaN, 8f, 5f),
                rect(0f, 0f, 4f, 2f),
                0f,
                0f,
            ),
        )
    }
}
