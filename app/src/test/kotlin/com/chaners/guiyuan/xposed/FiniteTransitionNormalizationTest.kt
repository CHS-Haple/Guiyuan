package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class FiniteTransitionNormalizationTest {
    @Test
    fun missingScaleRetainsFullSlotReservation() {
        assertEquals(100, CompactReservationPolicy.resolveCenteredVisualWidth(100, Float.NaN))
        assertEquals(100, CompactReservationPolicy.resolveCenteredVisualWidth(100, Float.POSITIVE_INFINITY))
    }

    @Test
    fun nonfiniteTransitionInputsFollowOriginalFallbacks() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(0f, CcTransitionPolicy.geometryProgress(value), 0f)
            assertEquals(0f, CcTransitionPolicy.acceleratedLatentRevealProgress(value), 0f)
            assertEquals(0f, BatteryRingTransitionPolicy.transitionProgress(value), 0f)
            assertEquals(1f, BatteryRingTransitionPolicy.remainingFraction(value), 0f)
            assertEquals(0f, MobileSignalStagePolicy.handoffProgress(value), 0f)
        }
    }
}
