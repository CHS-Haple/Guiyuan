package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileTypeSpacingTest {
    private fun ink(left: Float, right: Float) =
        MobileTypeSpacing.Ink(left, right)

    private fun visibleGap(main: MobileTypeSpacing.Ink, suffix: MobileTypeSpacing.Ink, clearance: Float): Float {
        val fit = MobileTypeSpacing.fit(main, suffix, clearance)!!
        return fit.suffixX + suffix.left - main.right
    }

    @Test
    fun visibleGapStaysConstantAcrossWeightsAndSideBearingChanges() {
        val suffix = ink(1f, 5f)
        val light = ink(0f, 8f)
        val heavy = ink(-0.5f, 10.5f)
        val clearance = 1.84f

        assertEquals(clearance, visibleGap(light, suffix, clearance), 0.0001f)
        assertEquals(clearance, visibleGap(heavy, suffix, clearance), 0.0001f)
        assertEquals(8.84f, MobileTypeSpacing.fit(light, suffix, clearance)!!.suffixX, 0.0001f)
        assertEquals(11.34f, MobileTypeSpacing.fit(heavy, suffix, clearance)!!.suffixX, 0.0001f)
    }

    @Test
    fun smallAStaysOutsideTheEntireMainInkBoxEvenWhenLowered() {
        val fit =
            MobileTypeSpacing.fit(
                main = ink(0f, 10f),
                suffix = ink(1f, 4f),
                clearance = 1.8f,
            )!!
        assertEquals(10.8f, fit.suffixX, 0.0001f)
        assertEquals(11.8f, fit.suffixX + 1f, 0.0001f)
        assertEquals(0f, fit.left, 0f)
        assertEquals(14.8f, fit.right, 0.0001f)
    }

    @Test
    fun negativeSideBearingsDoNotCreateOverlapOrAnArbitraryExtraGap() {
        val main = ink(-1f, 12f)
        val suffix = ink(-1.25f, 5f)
        val fit = MobileTypeSpacing.fit(main, suffix, 1.6f)!!
        assertEquals(14.85f, fit.suffixX, 0.0001f)
        assertEquals(1.6f, visibleGap(main, suffix, 1.6f), 0.0001f)
        assertTrue(fit.suffixX + suffix.left > main.right)
    }

    @Test
    fun allOffsetsScaleWithFontSizeRatherThanAbsolutePixels() {
        val base = MobileTypeSpacing.fit(ink(-1f, 10f), ink(0.6f, 5f), 1.8f)!!
        val double = MobileTypeSpacing.fit(ink(-2f, 20f), ink(1.2f, 10f), 3.6f)!!
        assertEquals(base.suffixX * 2f, double.suffixX, 0.0001f)
        assertEquals(base.left * 2f, double.left, 0.0001f)
        assertEquals(base.right * 2f, double.right, 0.0001f)
    }

    @Test
    fun zeroOrNegativeSafetyClearanceCannotPullGlyphsTogether() {
        val main = ink(0f, 10f)
        val suffix = ink(1f, 4f)
        assertEquals(0f, visibleGap(main, suffix, 0f), 0f)
        assertEquals(0f, visibleGap(main, suffix, -5f), 0f)
    }

    @Test
    fun invalidFontBoundsFallBackToTheOriginalLayout() {
        assertNull(MobileTypeSpacing.fit(ink(Float.NaN, 10f), ink(0f, 3f), 1f))
        assertNull(MobileTypeSpacing.fit(ink(0f, Float.POSITIVE_INFINITY), ink(0f, 3f), 1f))
        assertNull(MobileTypeSpacing.fit(ink(0f, 10f), ink(0f, 3f), Float.POSITIVE_INFINITY))
        assertNull(MobileTypeSpacing.fit(ink(0f, 0f), ink(0f, 3f), 1f))
        assertNull(MobileTypeSpacing.fit(ink(0f, 10f), ink(3f, 3f), 1f))
    }
}
