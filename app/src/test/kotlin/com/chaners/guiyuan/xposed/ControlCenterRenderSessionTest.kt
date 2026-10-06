package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterSessionTest {
    @Test
    fun projectionReadinessRequiresPreparedFakeRootAndCompactPresentation() {
        assertTrue(
            ControlCenterSession.resolveProjectionReady(
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
            ControlCenterSession.resolveProjectionReady(
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
            ControlCenterSession.resolveProjectionReady(
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
            ControlCenterSession.resolveProjectionReady(
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
    fun firstLayoutRetryOnlyCoversEarlyGeometryReadinessFailures() {
        assertTrue(
            ControlCenterSession.isFirstLayoutRetryable(
                "battery-core-width-unavailable",
            ),
        )
        assertTrue(
            ControlCenterSession.isFirstLayoutRetryable(
                "fake-status-bar-area-unresolved",
            ),
        )
        assertFalse(
            ControlCenterSession.isFirstLayoutRetryable(
                "fake-root-type-mismatch",
            ),
        )
        assertFalse(
            ControlCenterSession.isFirstLayoutRetryable(
                "ignored-slots-field-unavailable",
            ),
        )
    }


    @Test
    fun hotReloadRestoreRequiresAttachedLaidOutHostOutsideNativeLayout() {
        assertTrue(
            ControlCenterSession
                .shouldRestoreLaidOutHost(
                    attached = true,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRestoreLaidOutHost(
                    attached = true,
                    inLayout = true,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRestoreLaidOutHost(
                    attached = true,
                    inLayout = false,
                    width = 0,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRestoreLaidOutHost(
                    attached = false,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
    }

    @Test
    fun transferredCompactReadinessIsAdoptedOnlyWhenPreviouslyReady() {
        assertTrue(
            ControlCenterSession
                .shouldAdoptCompactReady(
                    transferredCompactReady = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldAdoptCompactReady(
                    transferredCompactReady = false,
                ),
        )
    }

    @Test
    fun transientLayoutLossRetainsPreparedFakePresentationWhileRootStaysAttached() {
        assertTrue(
            ControlCenterSession
                .keepNativeWhenLayoutMissing(
                    hostAttached = true,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .keepNativeWhenLayoutMissing(
                    hostAttached = false,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .keepNativeWhenLayoutMissing(
                    hostAttached = true,
                    nativePresentationReady = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseBeginsOnlyWhenAttachedSessionActuallyBecomesVisible() {
        assertTrue(
            ControlCenterSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldBeginCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseEndsOnlyWhenVisibleCycleActuallyCloses() {
        assertTrue(
            ControlCenterSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldEndCapacityLease(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
    }

}
