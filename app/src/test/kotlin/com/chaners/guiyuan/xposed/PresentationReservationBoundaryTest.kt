package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationReservationBoundaryTest {
    @Test
    fun onlySteadyCompactWidthIsReservedForNativePeers() {
        for (compact in listOf(105, 135)) {
            assertEquals(0, EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = compact,
                requestedSlotWidthPx = compact,
            ))
            assertEquals(compact, EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = true,
                actualBatteryWidthPx = compact,
                requestedSlotWidthPx = compact,
            ))
        }
    }

    @Test
    fun homePrivacyReservesGuiyuanEndWidthIndependentlyOfBatteryHideFlag() {
        val privacyVisible = HomePrivacyLayoutPolicy.occupiesEnd("START_SHOW_PRIVACY")
        val privacyHidden = HomePrivacyLayoutPolicy.occupiesEnd("COMPLETE_HIDE_PRIVACY")
        assertTrue(privacyVisible)
        assertFalse(privacyHidden)

        for (nativeHide in listOf(false, true)) {
            assertEquals(
                "Privacy must retain the end slot when nativeHide=$nativeHide",
                105,
                EndReservationPolicy.resolvePaddingEndDelta(
                    nativeHide = nativeHide || privacyVisible,
                    actualBatteryWidthPx = 105,
                    requestedSlotWidthPx = 105,
                ),
            )
            assertEquals(
                "Without privacy, only nativeHide reserves the end slot",
                if (nativeHide) 105 else 0,
                EndReservationPolicy.resolvePaddingEndDelta(
                    nativeHide = nativeHide || privacyHidden,
                    actualBatteryWidthPx = 105,
                    requestedSlotWidthPx = 105,
                ),
            )
        }
    }

    @Test
    fun nativeEndReservationTracksSteadyCarrierNotExpandedGeometry() {
        assertEquals(0, EndReservationPolicy.resolvePaddingEndDelta(
            nativeHide = false, actualBatteryWidthPx = 135, requestedSlotWidthPx = 135,
        ))
        assertEquals(135, EndReservationPolicy.resolvePaddingEndDelta(
            nativeHide = true, actualBatteryWidthPx = 135, requestedSlotWidthPx = 135,
        ))
        assertEquals(-30, EndReservationPolicy.resolvePaddingEndDelta(
            nativeHide = false, actualBatteryWidthPx = 135, requestedSlotWidthPx = 105,
        ))
        assertEquals(105, EndReservationPolicy.resolvePaddingEndDelta(
            nativeHide = true, actualBatteryWidthPx = 135, requestedSlotWidthPx = 105,
        ))
    }

    @Test
    fun lateEligibleControlCenterCanAdoptAlreadyCompletedNativeLayout() {
        assertTrue(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = false,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 0,
                height = 108,
            ),
        )
    }
}
