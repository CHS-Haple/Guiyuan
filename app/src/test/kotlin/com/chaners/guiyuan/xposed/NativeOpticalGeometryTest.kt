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
                val anchor = 108f - offset
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
        val anchorY = 108f
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
        fun resolveWithOpticalBounds(left: Float, right: Float) =
            NativeOpticalGeometry.resolve(
                NativeOpticalGeometry.OpticalFitInput(
                    current = NativeOpticalGeometry.OpticalAsset(
                        intrinsicWidth = 100,
                        intrinsicHeight = 100,
                        optical = NativeOpticalGeometry.OpticalRectangle(
                            left = left,
                            top = 0.20f,
                            right = right,
                            bottom = 0.80f,
                        ),
                    ),
                    reference = NativeOpticalGeometry.OpticalAsset(
                        intrinsicWidth = 100,
                        intrinsicHeight = 100,
                        optical = NativeOpticalGeometry.OpticalRectangle(
                            left = 0.10f,
                            top = 0.10f,
                            right = 0.90f,
                            bottom = 0.90f,
                        ),
                    ),
                    viewport = NativeOpticalGeometry.OpticalViewport(
                        centerX = 60f,
                        centerY = 58f,
                        maxWidth = 58f,
                        maxHeight = 45f,
                    ),
                ),
            )

        val narrow = resolveWithOpticalBounds(left = 0.35f, right = 0.65f)
        val wide = resolveWithOpticalBounds(left = 0.20f, right = 0.80f)
        assertNotNull(narrow)
        assertNotNull(wide)
        assertEquals(wide!!.drawWidth, narrow!!.drawWidth, 0.0001f)
        assertTrue(narrow.opticalWidth < wide.opticalWidth)
    }

    @Test
    fun opticalBoundsPreserveCurrentResourceAsymmetry() {
        val resolved =
            NativeOpticalGeometry.resolve(
                NativeOpticalGeometry.OpticalFitInput(
                current = NativeOpticalGeometry.OpticalAsset(
                    intrinsicWidth = 120,
                    intrinsicHeight = 80,
                    optical = NativeOpticalGeometry.OpticalRectangle(
                        left = 0.10f,
                        top = 0.20f,
                        right = 0.70f,
                        bottom = 0.90f,
                    ),
                ),
                reference = NativeOpticalGeometry.OpticalAsset(
                    intrinsicWidth = 120,
                    intrinsicHeight = 80,
                    optical = NativeOpticalGeometry.OpticalRectangle(
                        left = 0.10f,
                        top = 0.20f,
                        right = 0.70f,
                        bottom = 0.90f,
                    ),
                ),
                viewport = NativeOpticalGeometry.OpticalViewport(
                    centerX = 60f,
                    centerY = 58f,
                    maxWidth = 58f,
                    maxHeight = 45f,
                ),
                ),
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
                NativeOpticalGeometry.OpticalFitInput(
                current = NativeOpticalGeometry.OpticalAsset(
                    intrinsicWidth = 0,
                    intrinsicHeight = 100,
                    optical = NativeOpticalGeometry.OpticalRectangle(
                        left = 0f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                    ),
                ),
                reference = NativeOpticalGeometry.OpticalAsset(
                    intrinsicWidth = 100,
                    intrinsicHeight = 100,
                    optical = NativeOpticalGeometry.OpticalRectangle(
                        left = 0f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                    ),
                ),
                viewport = NativeOpticalGeometry.OpticalViewport(
                    centerX = 60f,
                    centerY = 58f,
                    maxWidth = 58f,
                    maxHeight = 45f,
                ),
                ),
            )

        assertEquals(null, resolved)
    }
}
