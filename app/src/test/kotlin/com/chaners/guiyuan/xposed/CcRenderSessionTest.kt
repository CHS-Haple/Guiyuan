package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcRenderSessionTest {
    @Test
    fun projectionReadinessRequiresPreparedFakeRootAndCompactPresentation() {
        assertTrue(
            CcRenderSession.resolveProjectionReady(
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
            CcRenderSession.resolveProjectionReady(
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
            CcRenderSession.resolveProjectionReady(
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
            CcRenderSession.resolveProjectionReady(
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
            CcRenderSession.isFirstLayoutRetryable(
                "battery-core-width-unavailable",
            ),
        )
        assertTrue(
            CcRenderSession.isFirstLayoutRetryable(
                "fake-status-bar-area-unresolved",
            ),
        )
        assertFalse(
            CcRenderSession.isFirstLayoutRetryable(
                "fake-root-type-mismatch",
            ),
        )
        assertFalse(
            CcRenderSession.isFirstLayoutRetryable(
                "ignored-slots-field-unavailable",
            ),
        )
    }


    @Test
    fun hotReloadRestoreRequiresAttachedLaidOutHostOutsideNativeLayout() {
        assertTrue(
            CcRenderSession
                .canRestoreAfterReload(
                    attached = true,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            CcRenderSession
                .canRestoreAfterReload(
                    attached = true,
                    inLayout = true,
                    width = 829,
                    height = 169,
                ),
        )
        assertFalse(
            CcRenderSession
                .canRestoreAfterReload(
                    attached = true,
                    inLayout = false,
                    width = 0,
                    height = 169,
                ),
        )
        assertFalse(
            CcRenderSession
                .canRestoreAfterReload(
                    attached = false,
                    inLayout = false,
                    width = 829,
                    height = 169,
                ),
        )
    }

}
