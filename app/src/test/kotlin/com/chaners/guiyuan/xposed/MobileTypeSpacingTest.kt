package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobileTypeSpacingTest {
    private fun ink(left: Float, right: Float, advance: Float) =
        MobileTypeSpacing.Ink(left, right, advance)

    @Test
    fun usesNativeAdvanceWhenSideBearingsAlreadyProvideReadableDistance() {
        val fit =
            MobileTypeSpacing.fit(
                main = ink(0f, 8f, 11f),
                suffix = ink(1f, 5f, 6f),
                clearance = 1.5f,
            )!!
        assertEquals(11f, fit.suffixX, 0.0001f)
        assertEquals(0f, fit.left, 0.0001f)
        assertEquals(16f, fit.right, 0.0001f)
    }

    @Test
    fun doesNotAllowTheSmallAIntoTheLargerGlyphHorizontalFootprint() {
        val fit =
            MobileTypeSpacing.fit(
                main = ink(0f, 10f, 9f),
                suffix = ink(1f, 4f, 5f),
                clearance = 1.8f,
            )!!
        assertEquals(10.8f, fit.suffixX, 0.0001f)
        assertEquals(1.8f, fit.suffixX + 1f - 10f, 0.0001f)
    }

    @Test
    fun increasingInkWeightCannotCreateArtificialInterlocking() {
        val a = ink(0.5f, 4f, 5f)
        val lighter = MobileTypeSpacing.fit(ink(0f, 8.2f, 10f), a, 1.8f)!!
        val heavier = MobileTypeSpacing.fit(ink(0f, 10.5f, 10f), a, 1.8f)!!
        assertEquals(10f, lighter.suffixX, 0.0001f)
        assertEquals(11.8f, heavier.suffixX, 0.0001f)
        assertEquals(1.8f, heavier.suffixX + a.left - 10.5f, 0.0001f)
    }

    @Test
    fun spacingAndCenterBoundsScaleWithTheActualFont() {
        val base = MobileTypeSpacing.fit(ink(-1f, 10f, 9f), ink(0.6f, 5f, 6f), 1.8f)!!
        val twice = MobileTypeSpacing.fit(ink(-2f, 20f, 18f), ink(1.2f, 10f, 12f), 3.6f)!!
        assertEquals(base.suffixX * 2f, twice.suffixX, 0.0001f)
        assertEquals(base.left * 2f, twice.left, 0.0001f)
        assertEquals(base.right * 2f, twice.right, 0.0001f)
    }

    @Test
    fun invalidFontMetricsReturnNullForThePreviousLayoutFallback() {
        assertNull(MobileTypeSpacing.fit(ink(Float.NaN, 10f, 11f), ink(0f, 3f, 4f), 1f))
        assertNull(MobileTypeSpacing.fit(ink(0f, 10f, 11f), ink(0f, 3f, 4f), Float.POSITIVE_INFINITY))
        assertNull(MobileTypeSpacing.fit(ink(0f, 0f, 11f), ink(0f, 3f, 4f), 1f))
        assertNull(MobileTypeSpacing.fit(ink(0f, 10f, 0f), ink(0f, 3f, 4f), 1f))
    }
}
