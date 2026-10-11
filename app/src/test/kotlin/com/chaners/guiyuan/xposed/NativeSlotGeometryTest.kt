package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeSlotGeometryTest {
    @Test
    fun laidOutCarrierWidthWinsOverTransientMeasurement() {
        assertEquals(
            478,
            NativeSlotGeometry.resolveStableChildWidth(
                layoutWidth = 478,
                measuredWidth = 448,
            ),
        )
    }

    @Test
    fun measuredWidthIsOnlyFallbackBeforeLayout() {
        assertEquals(
            448,
            NativeSlotGeometry.resolveStableChildWidth(
                layoutWidth = 0,
                measuredWidth = 448,
            ),
        )
        assertNull(
            NativeSlotGeometry.resolveStableChildWidth(
                layoutWidth = 0,
                measuredWidth = 0,
            ),
        )
    }
}
