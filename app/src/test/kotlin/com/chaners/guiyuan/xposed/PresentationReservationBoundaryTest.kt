package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationReservationBoundaryTest {
    @Test
    fun transitionReservationCannotShrinkBelowCompactWidth() {
        assertEquals(
            105,
            EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = 80,
            ),
        )
        assertEquals(
            168,
            EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = 168,
            ),
        )
        assertEquals(
            105,
            EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = null,
            ),
        )
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
    fun endReservationKeepsOneResolvedEndBoundaryAcrossBatteryStates() {
        assertEquals(
            0,
            EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 105,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            -30,
            EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            105,
            EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = true,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
    }

    @Test
    fun overhangDoesNotBypassMeasuredPeerEndReservationCapacity() {
        assertFalse(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 1230, contentWidthPx = 1190, targetPaddingEndPx = 105,
        ))
        assertTrue(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 1230, contentWidthPx = 900, targetPaddingEndPx = 105,
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
                capacityLeaseAwaitingLayout = true,
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
