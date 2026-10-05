package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTopLayoutPolicyTest {
    @Test
    fun opticalDefaultKeepsDesignPlacementIndependentFromClipping() {
        val base =
            BatteryTopLayoutPolicy.resolveOpticalBaseCenterY(
                preferredCenterY = 16f,
                defaultOpticalRise = 1.5f,
            )

        assertEquals(14.5f, base, 0.0001f)
    }

    @Test
    fun uiZeroKeepsAcceptedRawPlusThreePosition() {
        val center =
            BatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 3f,
            )

        assertEquals(11.5f, center, 0.0001f)
    }

    @Test
    fun positiveOffsetRemainsLiteralInsteadOfFlatteningAtOldSafeTop() {
        val center =
            BatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 13f,
            )

        assertEquals(1.5f, center, 0.0001f)
    }

    @Test
    fun negativeOffsetRemainsLiteralDownwardTravel() {
        val center =
            BatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = -7f,
            )

        assertEquals(21.5f, center, 0.0001f)
    }

    @Test
    fun overflowIsZeroWhenVisibleInkStaysInsideLogicalViewport() {
        val overflow =
            BatteryTopLayoutPolicy.resolveRequiredTopOverflowPx(
                transformScale = 0.875f,
                transformOffsetY = 1.5f,
                contentTopY = 2f,
            )

        assertEquals(0, overflow)
    }

    @Test
    fun overflowExpandsPhysicalSurfaceInsteadOfClampingRequestedY() {
        val overflow =
            BatteryTopLayoutPolicy.resolveRequiredTopOverflowPx(
                transformScale = 0.875f,
                transformOffsetY = 1.5f,
                contentTopY = -8f,
            )

        assertEquals(7, overflow)
    }

    @Test
    fun nonFiniteManualOffsetFallsBackToOpticalBase() {
        val center =
            BatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = Float.NaN,
            )

        assertEquals(14.5f, center, 0.0001f)
    }
}
