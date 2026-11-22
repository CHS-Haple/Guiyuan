package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationIslandReservationTest {
    @Test
    fun nativeFakePeerCapacityIsOnlyCheckedAfterItsOwnNativeMeasure() {
        assertFalse(EndReservationPolicy.fakeNativePeerMeasureReady(
            carrierSettled = false,
            peerRowLayoutRequested = false,
        ))
        assertFalse(EndReservationPolicy.fakeNativePeerMeasureReady(
            carrierSettled = true,
            peerRowLayoutRequested = true,
        ))
        assertTrue(EndReservationPolicy.fakeNativePeerMeasureReady(
            carrierSettled = true,
            peerRowLayoutRequested = false,
        ))
    }

    @Test
    fun nativeFakePeerRowUsesMeasuredNativeSlackWithoutHostWidthLease() {
        // Synthetic native measure boundaries, not a claim about device pixels.
        assertTrue(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 200, contentWidthPx = 120, targetPaddingEndPx = 80,
        ))
        assertFalse(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 200, contentWidthPx = 121, targetPaddingEndPx = 80,
        ))
        assertFalse(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 478, contentWidthPx = 392, targetPaddingEndPx = 87,
        ))
        assertTrue(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 478, contentWidthPx = 391, targetPaddingEndPx = 87,
        ))
        assertFalse(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 0, contentWidthPx = 0, targetPaddingEndPx = 0,
        ))
    }

    @Test
    fun steadyPeerMirrorUsesOnlyHomeNativeIslandHiddenState() {
        assertTrue(
            SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 10,
                ),
        )
        assertFalse(
            SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 20,
                ),
        )
        assertFalse(
            SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 0,
                    inIslandState = 10,
                ),
        )
    }

    @Test
    fun steadyPeerMirrorSuppressesOnlyFakeSecondIslandDecision() {
        assertFalse(
            SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = true,
                ),
        )
        assertTrue(
            SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = false,
                ),
        )
        assertFalse(
            SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = false,
                    steadyMirrorActive = true,
                ),
        )
    }
}
