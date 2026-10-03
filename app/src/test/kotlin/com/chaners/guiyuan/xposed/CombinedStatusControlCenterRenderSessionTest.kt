package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterRenderSessionTest {
    @Test
    fun projectionReadinessRequiresPreparedFakeRootAndCompactPresentation() {
        assertTrue(
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
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
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
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
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
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
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
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
            CombinedStatusControlCenterRenderSession.isFirstLayoutRetryable(
                "battery-core-width-unavailable",
            ),
        )
        assertTrue(
            CombinedStatusControlCenterRenderSession.isFirstLayoutRetryable(
                "fake-status-bar-area-unresolved",
            ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession.isFirstLayoutRetryable(
                "fake-root-type-mismatch",
            ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession.isFirstLayoutRetryable(
                "ignored-slots-field-unavailable",
            ),
        )
    }


    @Test
    fun hotReloadRestoreRequiresAttachedLaidOutHostOutsideNativeLayout() {
        assertTrue(
            CombinedStatusControlCenterRenderSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = true,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldRestoreLaidOutHostAfterHotReload(
                    attached = true,
                    inLayout = false,
                    width = 0,
                    height = 169,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
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
            CombinedStatusControlCenterRenderSession
                .shouldAdoptTransferredCompactReadiness(
                    transferredCompactReady = true,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldAdoptTransferredCompactReadiness(
                    transferredCompactReady = false,
                ),
        )
    }

    @Test
    fun transientLayoutLossRetainsPreparedFakePresentationWhileRootStaysAttached() {
        assertTrue(
            CombinedStatusControlCenterRenderSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = true,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = false,
                    nativePresentationReady = true,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = true,
                    nativePresentationReady = false,
                ),
        )
    }

    @Test
    fun carrierCapacityLeaseEndsOnlyWhenVisibleCycleActuallyCloses() {
        assertTrue(
            CombinedStatusControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = false,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = false,
                    nextRequestedVisible = true,
                ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession
                .shouldEndCapacityLeaseOnVisibilityChange(
                    previousRequestedVisible = true,
                    nextRequestedVisible = true,
                ),
        )
    }

}
