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
    @Test fun extendedEndpointsKeepShapeAndOrder() {
        // A 5×5 opaque mark has room for both thinning passes.
        val source = IntArray(81) { index ->
            val x = index % 9
            val y = index / 9
            if (x in 2..6 && y in 2..6) -1 else 0
        }
        fun visible(weight: Int): Int =
            WifiStrokeMask.adjust(source, 9, 9, weight).count { it ushr 24 > 0 }
        assertTrue(visible(60) < visible(80))
        assertTrue(visible(80) < visible(100))
        assertTrue(visible(100) < visible(120))
        assertTrue(visible(120) <= visible(130))
        assertEquals(1, visible(60))
        assertEquals(81, WifiStrokeMask.adjust(source, 9, 9, 130).size)
    }

    @Test fun transitionReturnsBothNewEndpointsToNativeWeight() {
        assertEquals(60, WifiStrokeMask.duringTransition(60, 0f))
        assertEquals(80, WifiStrokeMask.duringTransition(60, 0.5f))
        assertEquals(100, WifiStrokeMask.duringTransition(60, 1f))
        assertEquals(130, WifiStrokeMask.duringTransition(130, 0f))
        assertEquals(115, WifiStrokeMask.duringTransition(130, 0.5f))
        assertEquals(100, WifiStrokeMask.duringTransition(130, 1f))
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
