package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class NativeRenderGeometryTest {
    @Test
    fun steadyBoundsUseFinalPixels() {
        val transform =
            NativeRenderTransform(
                scale = 0.875f,
                offsetX = 0f,
                offsetY = 1.5f,
            )

        val bounds =
            NativeRenderGeometry.resolvePixelBounds(
                centerX = 60f,
                centerY = 58f,
                drawWidth = 58f,
                drawHeight = 45f,
                transform = transform,
            )

        assertEquals((58f * transform.scale).roundToInt(), bounds.right - bounds.left)
        assertEquals((45f * transform.scale).roundToInt(), bounds.bottom - bounds.top)

        val expectedCenterX = transform.offsetX + 60f * transform.scale
        val expectedCenterY = transform.offsetY + 58f * transform.scale
        val resolvedCenterX = (bounds.left + bounds.right) / 2f
        val resolvedCenterY = (bounds.top + bounds.bottom) / 2f

        assertTrue(kotlin.math.abs(expectedCenterX - resolvedCenterX) <= 0.5f)
        assertTrue(kotlin.math.abs(expectedCenterY - resolvedCenterY) <= 0.5f)
    }

    @Test
    fun smallScaleKeepsPixelBounds() {
        val bounds =
            NativeRenderGeometry.resolvePixelBounds(
                centerX = 60f,
                centerY = 60f,
                drawWidth = 1f,
                drawHeight = 1f,
                transform =
                    NativeRenderTransform(
                        scale = 0.7f,
                        offsetX = 0f,
                        offsetY = 0f,
                    ),
            )

        assertTrue(bounds.right > bounds.left)
        assertTrue(bounds.bottom > bounds.top)
    }
}
