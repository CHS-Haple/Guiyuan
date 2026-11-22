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
    fun nativePeerFootprintMatchesOemPaddingAndNetworkSpeedLayout() {
        assertEquals(32, EndReservationPolicy.measuredNativePeerFootprint(
            measuredWidthPx = 22, paddingStartPx = 6, paddingEndPx = 4,
        ))
        assertEquals(60, EndReservationPolicy.measuredNativePeerFootprint(
            measuredWidthPx = 14, paddingStartPx = 6, paddingEndPx = 6,
            networkSpeedLayoutWidthPx = 60,
        ))
        assertEquals(26, EndReservationPolicy.measuredNativePeerFootprint(
            measuredWidthPx = 14, paddingStartPx = 6, paddingEndPx = 6,
            networkSpeedLayoutWidthPx = 18,
        ))
        assertEquals(null, EndReservationPolicy.measuredNativePeerFootprint(
            measuredWidthPx = Int.MAX_VALUE, paddingStartPx = 1, paddingEndPx = 1,
        ))
        assertEquals(null, EndReservationPolicy.measuredNativePeerFootprint(
            measuredWidthPx = 20, paddingStartPx = -1, paddingEndPx = 0,
        ))
    }

    @Test
    fun measuredFakeCapacityBoundsOnlyOptionalTransitionExpansion() {
        // B1912 observed 11 small excesses after network switching.
        assertEquals(62, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 448, contentWidthPx = 386,
            requestedPaddingEndPx = 69, minimumPaddingEndPx = 0,
        ))
        assertEquals(155, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 448, contentWidthPx = 293,
            requestedPaddingEndPx = 156, minimumPaddingEndPx = 0,
        ))
        assertEquals(62, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 448, contentWidthPx = 386,
            requestedPaddingEndPx = 62, minimumPaddingEndPx = 0,
        ))
        // A crowded native row can still show its compact projection by
        // giving up only optional padding. An unsafe required slot cannot.
        assertEquals(0, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 448, contentWidthPx = 500,
            requestedPaddingEndPx = 76, minimumPaddingEndPx = 0,
        ))
        assertEquals(null, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 448, contentWidthPx = 500,
            requestedPaddingEndPx = 150, minimumPaddingEndPx = 105,
        ))
        // The compact slot cannot be silently shrunk.
        assertEquals(null, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 448, contentWidthPx = 386,
            requestedPaddingEndPx = 150, minimumPaddingEndPx = 105,
        ))
        assertEquals(null, EndReservationPolicy.boundedFakeNativeTransitionPadding(
            rowWidthPx = 0, contentWidthPx = 386,
            requestedPaddingEndPx = 69, minimumPaddingEndPx = 0,
        ))
    }

    @Test
    fun boundedNativePeerLaneConstrainsTheWholeTransitionInBothDirections() {
        // A 17 px native grant reduction makes a corresponding 17 px fence.
        assertEquals(391f, CcCarrierGeometryPolicy.fakePeerClipBoundaryX(
            nativeHostCenterX = 293.5f, nativeHostWidthX = 587f,
            requestedWidthPx = 213, grantedWidthPx = 196, isRtl = false,
        )!!, 0.0001f)
        assertEquals(196f, CcCarrierGeometryPolicy.fakePeerClipBoundaryX(
            nativeHostCenterX = 293.5f, nativeHostWidthX = 587f,
            requestedWidthPx = 213, grantedWidthPx = 196, isRtl = true,
        )!!, 0.0001f)
        assertEquals(null, CcCarrierGeometryPolicy.fakePeerClipBoundaryX(
            293.5f, 587f, 196, 196, false,
        ))
        assertEquals(null, CcCarrierGeometryPolicy.fakePeerClipBoundaryX(
            293.5f, 587f, 196, 197, false,
        ))
        assertEquals(null, CcCarrierGeometryPolicy.fakePeerClipBoundaryX(
            293.5f, 0f, 196, 180, false,
        ))
        assertEquals(null, CcCarrierGeometryPolicy.fakePeerClipBoundaryX(
            Float.NaN, 587f, 196, 180, false,
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
    @Test
    fun nativeBoundedPaddingReportsActualGrantedSlotWidth() {
        assertEquals(197, EndReservationPolicy.appliedTransitionSlotWidth(
            requestedSlotWidthPx = 202,
            requestedReservationDeltaPx = 67,
            appliedReservationDeltaPx = 62,
        ))
        assertEquals(105, EndReservationPolicy.appliedTransitionSlotWidth(
            requestedSlotWidthPx = 105,
            requestedReservationDeltaPx = 0,
            appliedReservationDeltaPx = 0,
        ))
        assertEquals(160, EndReservationPolicy.appliedTransitionSlotWidth(
            requestedSlotWidthPx = 170,
            requestedReservationDeltaPx = 170,
            appliedReservationDeltaPx = 160,
        ))
    }
}
