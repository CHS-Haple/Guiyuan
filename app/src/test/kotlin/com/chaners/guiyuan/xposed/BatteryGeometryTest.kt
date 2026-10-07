package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryGeometryTest {
    @Test
    fun emptyBatteryUsesOnlyInactiveArc() {
        val result = arc(0)

        assertEquals(0f, result.activeSweep, 0.001f)
        assertEquals(150f, result.inactiveStart, 0.001f)
        assertEquals(240f, result.inactiveSweep, 0.001f)
    }

    @Test
    fun halfBatteryPartitionsTheRingWithoutOverlap() {
        val result = arc(50)

        assertEquals(120f, result.activeSweep, 0.001f)
        assertEquals(270f, result.inactiveStart, 0.001f)
        assertEquals(120f, result.inactiveSweep, 0.001f)
        assertEquals(240f, result.activeSweep + result.inactiveSweep, 0.001f)
    }

    @Test
    fun fullBatteryUsesOnlyActiveArc() {
        val result = arc(100)

        assertEquals(240f, result.activeSweep, 0.001f)
        assertEquals(390f, result.inactiveStart, 0.001f)
        assertEquals(0f, result.inactiveSweep, 0.001f)
    }

    @Test
    fun percentIsClampedBeforePartitioning() {
        assertEquals(0f, arc(-10).activeSweep, 0.001f)
        assertEquals(240f, arc(140).activeSweep, 0.001f)
    }

    @Test
    fun opticalDefaultKeepsDesignPlacementIndependentFromClipping() {
        val base =
            batteryTopBaseCenterY(
                preferredCenterY = 16f,
                defaultOpticalRise = 1.5f,
            )

        assertEquals(14.5f, base, 0.0001f)
    }

    @Test
    fun uiZeroKeepsAcceptedRawPlusThreePosition() {
        val center =
            batteryTopCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 3f,
            )

        assertEquals(11.5f, center, 0.0001f)
    }

    @Test
    fun positiveOffsetRemainsLiteralInsteadOfFlatteningAtOldSafeTop() {
        val center =
            batteryTopCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 13f,
            )

        assertEquals(1.5f, center, 0.0001f)
    }

    @Test
    fun negativeOffsetRemainsLiteralDownwardTravel() {
        val center =
            batteryTopCenterY(
                baseCenterY = 14.5f,
                requestedOffset = -7f,
            )

        assertEquals(21.5f, center, 0.0001f)
    }

    @Test
    fun overflowIsZeroWhenVisibleInkStaysInsideLogicalViewport() {
        val overflow =
            batteryTopOverflowPx(
                transformScale = 0.875f,
                transformOffsetY = 1.5f,
                contentTopY = 2f,
            )

        assertEquals(0, overflow)
    }

    @Test
    fun overflowExpandsPhysicalSurfaceInsteadOfClampingRequestedY() {
        val overflow =
            batteryTopOverflowPx(
                transformScale = 0.875f,
                transformOffsetY = 1.5f,
                contentTopY = -8f,
            )

        assertEquals(7, overflow)
    }

    @Test
    fun nonFiniteManualOffsetFallsBackToOpticalBase() {
        val center =
            batteryTopCenterY(
                baseCenterY = 14.5f,
                requestedOffset = Float.NaN,
            )

        assertEquals(14.5f, center, 0.0001f)
    }

    private fun arc(percent: Int) =
        batteryArcSegments(
            batteryPercent = percent,
            startDegrees = 150f,
            maxSweep = 240f,
            degreesPerPercent = 2.4f,
        )
}
