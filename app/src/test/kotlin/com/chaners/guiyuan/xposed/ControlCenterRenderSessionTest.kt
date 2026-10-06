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
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = true,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = false,
                    width = 0,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRestoreLaidOutHostAfterHotReload(
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
                .shouldAdoptTransferredCompactReadiness(
                    transferredCompactReady = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldAdoptTransferredCompactReadiness(
                    transferredCompactReady = false,
                ),
        )
    }

    @Test
    fun transientLayoutLossRetainsPreparedFakePresentationWhileRootStaysAttached() {
        assertTrue(
            ControlCenterSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = true,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = false,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = true,
                    nativePresentationReady = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseBeginsOnlyWhenAttachedSessionActuallyBecomesVisible() {
        assertTrue(
            ControlCenterSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseEndsOnlyWhenVisibleCycleActuallyCloses() {
        assertTrue(
            ControlCenterSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
    }

}
