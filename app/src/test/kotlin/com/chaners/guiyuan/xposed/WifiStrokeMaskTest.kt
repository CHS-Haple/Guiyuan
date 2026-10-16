package com.chaners.guiyuan.xposed

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiStrokeMaskTest {
    @Test fun nativeAtDefaultAndAtTransitionTarget() {
        val pixels = intArrayOf(0, -1, 0)
        assertArrayEquals(pixels, WifiStrokeMask.adjust(pixels, 3, 1, 100))
        assertEquals(120, WifiStrokeMask.duringTransition(120, 0f))
        assertEquals(110, WifiStrokeMask.duringTransition(120, 0.5f))
        assertEquals(100, WifiStrokeMask.duringTransition(120, 1f))
        assertEquals(100, WifiStrokeMask.duringTransition(80, 1f))
    }
    @Test fun thickerAddsEdgeWithoutMovingPixelGeometry() {
        val pixels = IntArray(25)
        pixels[12] = -1
        val thick = WifiStrokeMask.adjust(pixels, 5, 5, 120)
        assertEquals(255, thick[12] ushr 24)
        assertTrue((thick[11] ushr 24) > 0)
        assertEquals(0, thick[0] ushr 24)
    }
    @Test fun thinnerRemovesEdgeWithoutChangingCanvasDimensions() {
        val pixels = IntArray(25) { -1 }
        val thin = WifiStrokeMask.adjust(pixels, 5, 5, 80)
        assertEquals(pixels.size, thin.size)
        assertEquals(255, thin[12] ushr 24)
        assertEquals(0, thin[0] ushr 24)
    }
}
