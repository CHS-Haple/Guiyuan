package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NativeStatusBarSlotGeometryTest {
    @Test
    fun capturedStableWidthWinsWhenChargingLayoutHasAlreadyCollapsed() {
        val resolvedWidth =
            NativeStatusBarSlotGeometry.resolveCapturedOrLiveChildWidth(
                capturedWidth = 478,
                layoutWidth = 448,
                measuredWidth = 448,
            )

        assertEquals(478, resolvedWidth)

        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = resolvedWidth ?: -1,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertNotNull(resolved)
        assertEquals(105, resolved?.slotWidth)
    }

    @Test
    fun liveWidthRemainsFallbackWhenNoStableCaptureExists() {
        assertEquals(
            448,
            NativeStatusBarSlotGeometry.resolveCapturedOrLiveChildWidth(
                capturedWidth = null,
                layoutWidth = 448,
                measuredWidth = 448,
            ),
        )
    }

    @Test
    fun laidOutStatusIconWidthWinsOverTransientChargingMeasurement() {
        val stableWidth =
            NativeStatusBarSlotGeometry.resolveStableChildWidth(
                layoutWidth = 478,
                measuredWidth = 448,
            )

        assertEquals(478, stableWidth)

        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = stableWidth ?: -1,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertNotNull(resolved)
        assertEquals(105, resolved?.slotWidth)
    }

    @Test
    fun measuredWidthIsOnlyFallbackBeforeLayout() {
        assertEquals(
            448,
            NativeStatusBarSlotGeometry.resolveStableChildWidth(
                layoutWidth = 0,
                measuredWidth = 448,
            ),
        )
        assertNull(
            NativeStatusBarSlotGeometry.resolveStableChildWidth(
                layoutWidth = 0,
                measuredWidth = 0,
            ),
        )
    }

    @Test
    fun stableHomeMeasurementResolvesNativeBatteryOccupancy() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 478,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertNotNull(resolved)
        assertEquals(105, resolved?.slotWidth)
        assertEquals(108, resolved?.slotHeight)
    }

    @Test
    fun visiblePrivacyOccupancyIsExcludedFromBatterySlot() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 448,
                privacyMeasuredWidth = 30,
                containerHeight = 108,
            )

        assertEquals(105, resolved?.slotWidth)
    }

    @Test
    fun unavailableSlotFailsClosed() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 583,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertNull(resolved)
    }
}
