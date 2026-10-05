package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterRenderSessionTest {
    @Test
    fun projectionReadinessRequiresPreparedFakeRootAndCompactPresentation() {
        assertTrue(
            ControlCenterRenderSession.resolveProjectionReady(
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
            ControlCenterRenderSession.resolveProjectionReady(
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
            ControlCenterRenderSession.resolveProjectionReady(
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
            ControlCenterRenderSession.resolveProjectionReady(
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
            ControlCenterRenderSession.isFirstLayoutRetryable(
                "battery-core-width-unavailable",
            ),
        )
        assertTrue(
            ControlCenterRenderSession.isFirstLayoutRetryable(
                "fake-status-bar-area-unresolved",
            ),
        )
        assertFalse(
            ControlCenterRenderSession.isFirstLayoutRetryable(
                "fake-root-type-mismatch",
            ),
        )
        assertFalse(
            ControlCenterRenderSession.isFirstLayoutRetryable(
                "ignored-slots-field-unavailable",
            ),
        )
    }


    @Test
    fun hotReloadRestoreRequiresAttachedLaidOutHostOutsideNativeLayout() {
        assertTrue(
            ControlCenterRenderSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = true,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = false,
                    width = 0,
                    height = 169,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
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
            ControlCenterRenderSession
                .shouldAdoptTransferredCompactReadiness(
                    transferredCompactReady = true,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldAdoptTransferredCompactReadiness(
                    transferredCompactReady = false,
                ),
        )
    }

    @Test
    fun transientLayoutLossRetainsPreparedFakePresentationWhileRootStaysAttached() {
        assertTrue(
            ControlCenterRenderSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = true,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = false,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = true,
                    nativePresentationReady = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseBeginsOnlyWhenAttachedSessionActuallyBecomesVisible() {
        assertTrue(
            ControlCenterRenderSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldBeginCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseEndsOnlyWhenVisibleCycleActuallyCloses() {
        assertTrue(
            ControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            ControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
    }

}
