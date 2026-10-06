package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcSessionTest {
    @Test
    fun projectionNeedsPreparedCompactRoot() {
        assertTrue(
            CcSession.resolveProjectionReady(
                featureEnabled = true,
                sceneEligible = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                nativePresentationReady = true,
            ),
        )
        assertFalse(
            CcSession.resolveProjectionReady(
                featureEnabled = true,
                sceneEligible = false,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                nativePresentationReady = true,
            ),
        )
        assertFalse(
            CcSession.resolveProjectionReady(
                featureEnabled = true,
                sceneEligible = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                nativePresentationReady = false,
            ),
        )
        assertFalse(
            CcSession.resolveProjectionReady(
                featureEnabled = true,
                sceneEligible = true,
                modelReady = true,
                tintReady = false,
                layoutReady = true,
                hostAttached = true,
                nativePresentationReady = true,
            ),
        )
    }
    @Test
    fun firstLayoutRetryIsGeometryOnly() {
        assertTrue(
            CcSession.isFirstLayoutRetryable(
                "battery-core-width-unavailable",
            ),
        )
        assertTrue(
            CcSession.isFirstLayoutRetryable(
                "fake-status-bar-area-unresolved",
            ),
        )
        assertFalse(
            CcSession.isFirstLayoutRetryable(
                "fake-root-type-mismatch",
            ),
        )
        assertFalse(
            CcSession.isFirstLayoutRetryable(
                "ignored-slots-field-unavailable",
            ),
        )
    }


    @Test
    fun reloadRestoreNeedsLaidOutHost() {
        assertTrue(
            CcSession
                .shouldRestoreLaidOutHost(
                    attached = true,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            CcSession
                .shouldRestoreLaidOutHost(
                    attached = true,
                    inLayout = true,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            CcSession
                .shouldRestoreLaidOutHost(
                    attached = true,
                    inLayout = false,
                    width = 0,
                    height = 169,
                ),
        )
        assertFalse(
            CcSession
                .shouldRestoreLaidOutHost(
                    attached = false,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
    }

    @Test
    fun compactReadyTransfersOnlyIfReady() {
        assertTrue(
            CcSession
                .shouldAdoptCompactReady(
                    transferredCompactReady = true,
                ),
        )
        assertFalse(
            CcSession
                .shouldAdoptCompactReady(
                    transferredCompactReady = false,
                ),
        )
    }

    @Test
    fun layoutLossKeepsPreparedRoot() {
        assertTrue(
            CcSession
                .keepNativeWhenLayoutMissing(
                    hostAttached = true,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            CcSession
                .keepNativeWhenLayoutMissing(
                    hostAttached = false,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            CcSession
                .keepNativeWhenLayoutMissing(
                    hostAttached = true,
                    nativePresentationReady = false,
                ),
        )
    }

    @Test
    fun capacityLeaseStartsWhenVisible() {
        assertTrue(
            CcSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            CcSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            CcSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            CcSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
    }

    @Test
    fun capacityLeaseEndsWithVisibleCycle() {
        assertTrue(
            CcSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            CcSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            CcSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            CcSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
    }

}
