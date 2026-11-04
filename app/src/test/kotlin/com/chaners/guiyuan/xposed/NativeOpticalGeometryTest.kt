package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeOpticalGeometryTest {
    @Test
    fun lowerGroupOffsetMovesOpticalIconAnchorWithoutSizeDependentDrift() {
        for (offset in listOf(-5f, 0f, 5f)) {
            for (scale in listOf(0.7f, 1f, 1.2f)) {
                val w = 18f * scale
                val h = 26f * scale
                val anchor = 104f - offset
                val origin = NativeOpticalGeometry.viewportCenterForInkAnchor(
                    anchorX = 60f, anchorY = anchor,
                    drawWidth = w, drawHeight = h,
                    opticalCenterX = 0.65f, inkCenterY = 0.34f,
                )
                assertEquals(anchor, origin.second + (0.34f - 0.5f) * h, 0.0001f)
                assertEquals(60f, origin.first + (0.65f - 0.5f) * w, 0.0001f)
            }
        }
    }

    @Test
    fun opticalInkCenterStaysOnOneBottomAnchorAcrossIconSizes() {
        val anchorX = 60f
        val anchorY = 104f
        for (size in listOf(0.7f, 1f, 1.2f)) {
            val width = 23f * size
            val height = 28f * size
            val viewport = NativeOpticalGeometry.viewportCenterForInkAnchor(
                anchorX = anchorX, anchorY = anchorY,
                drawWidth = width, drawHeight = height,
                opticalCenterX = 0.65f, inkCenterY = 0.34f,
            )
            assertEquals(anchorX, viewport.first + (0.65f - 0.5f) * width, 0.0001f)
            assertEquals(anchorY, viewport.second + (0.34f - 0.5f) * height, 0.0001f)
        }
    }

    @Test
    fun currentOpticalWidthStaysResourceSpecificUnderSharedReferenceFit() {
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
    fun opticalBoundsPreserveCurrentResourceAsymmetry() {
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
