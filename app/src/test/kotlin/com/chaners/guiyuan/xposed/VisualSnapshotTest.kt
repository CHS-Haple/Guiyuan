package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualSnapshotTest {
    @Test
    fun weightedCenterTracksInkMass() {
        val center =
            VisualSnapshot.resolveAlphaWeightedCenter(
                pixels =
                    intArrayOf(
                        0xff000000.toInt(),
                        0x40000000,
                        0x00000000,
                    ),
                width = 1,
                height = 3,
            )

        requireNotNull(center)
        assertEquals(0.5f, center.x, 0.0001f)
        assertTrue(center.y < 1f / 3f)
        assertEquals(0.23354232f, center.y, 0.0001f)
    }

    @Test
    fun weightedCenterRejectsTransparent() {
        val center =
            VisualSnapshot.resolveAlphaWeightedCenter(
                pixels = IntArray(4),
                width = 2,
                height = 2,
            )

        assertNull(center)
    }
}
