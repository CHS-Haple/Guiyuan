package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeOpticalGeometryTest {
    @Test
    fun opticalWidthStaysResourceSpecific() {
        val narrow =
            NativeOpticalGeometry.resolve(
                currentIntrinsicWidth = 100,
                currentIntrinsicHeight = 100,
                currentOpticalLeft = 0.35f,
                currentOpticalTop = 0.20f,
                currentOpticalRight = 0.65f,
                currentOpticalBottom = 0.80f,
                fitIntrinsicWidth = 100,
                fitIntrinsicHeight = 100,
                fitOpticalLeft = 0.10f,
                fitOpticalTop = 0.10f,
                fitOpticalRight = 0.90f,
                fitOpticalBottom = 0.90f,
                centerX = 60f,
                centerY = 58f,
                maxWidth = 58f,
                maxHeight = 45f,
            )
        val wide =
            NativeOpticalGeometry.resolve(
                currentIntrinsicWidth = 100,
                currentIntrinsicHeight = 100,
                currentOpticalLeft = 0.20f,
                currentOpticalTop = 0.20f,
                currentOpticalRight = 0.80f,
                currentOpticalBottom = 0.80f,
                fitIntrinsicWidth = 100,
                fitIntrinsicHeight = 100,
                fitOpticalLeft = 0.10f,
                fitOpticalTop = 0.10f,
                fitOpticalRight = 0.90f,
                fitOpticalBottom = 0.90f,
                centerX = 60f,
                centerY = 58f,
                maxWidth = 58f,
                maxHeight = 45f,
            )

        assertNotNull(narrow)
        assertNotNull(wide)
        assertEquals(wide!!.drawWidth, narrow!!.drawWidth, 0.0001f)
        assertTrue(narrow.opticalWidth < wide.opticalWidth)
    }

    @Test
    fun opticalBoundsKeepAsymmetry() {
        val resolved =
            NativeOpticalGeometry.resolve(
                currentIntrinsicWidth = 120,
                currentIntrinsicHeight = 80,
                currentOpticalLeft = 0.10f,
                currentOpticalTop = 0.20f,
                currentOpticalRight = 0.70f,
                currentOpticalBottom = 0.90f,
                fitIntrinsicWidth = 120,
                fitIntrinsicHeight = 80,
                fitOpticalLeft = 0.10f,
                fitOpticalTop = 0.20f,
                fitOpticalRight = 0.70f,
                fitOpticalBottom = 0.90f,
                centerX = 60f,
                centerY = 58f,
                maxWidth = 58f,
                maxHeight = 45f,
            )!!

        assertTrue(resolved.opticalLeft < 60f)
        assertTrue((resolved.opticalLeft + resolved.opticalRight) / 2f < 60f)
        assertTrue(resolved.opticalWidth > 0f)
        assertTrue(resolved.opticalHeight > 0f)
    }

    @Test
    fun invalidIntrinsicSizeDoesNotInventGeometry() {
        val resolved =
            NativeOpticalGeometry.resolve(
                currentIntrinsicWidth = 0,
                currentIntrinsicHeight = 100,
                currentOpticalLeft = 0f,
                currentOpticalTop = 0f,
                currentOpticalRight = 1f,
                currentOpticalBottom = 1f,
                fitIntrinsicWidth = 100,
                fitIntrinsicHeight = 100,
                fitOpticalLeft = 0f,
                fitOpticalTop = 0f,
                fitOpticalRight = 1f,
                fitOpticalBottom = 1f,
                centerX = 60f,
                centerY = 58f,
                maxWidth = 58f,
                maxHeight = 45f,
            )

        assertEquals(null, resolved)
    }
}
