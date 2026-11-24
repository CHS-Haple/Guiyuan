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
    fun detachedFakeNativeAnchorCannotRemainProjectionReady() {
        assertFalse(CcRenderSession.resolveProjectionReady(
            featureEnabled = true,
            sceneEligible = true,
            modelReady = true,
            tintReady = true,
            layoutReady = true,
            hostAttached = false,
            nativePresentationReady = true,
        ))
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


    @Test
    fun fakeVisualRetiresAtHomeCutoverWithoutRetiringNativePanelLease() {
        assertTrue(
            CcRenderSession.resolveFakeVisible(
                nativeRequested = true,
                projectionReady = true,
                panelVisualOwner = true,
            ),
        )
        // Native visible=true may outlive native fraction zero; it must not
        // keep Guiyuan FAKE painting over the newly returned Home projection.
        assertFalse(
            CcRenderSession.resolveFakeVisible(
                nativeRequested = true,
                projectionReady = true,
                panelVisualOwner = false,
            ),
        )
        assertFalse(
            CcRenderSession.resolveFakeVisible(
                nativeRequested = false,
                projectionReady = true,
                panelVisualOwner = true,
            ),
        )
        assertFalse(
            CcRenderSession.resolveFakeVisible(
                nativeRequested = true,
                projectionReady = false,
                panelVisualOwner = true,
            ),
        )
    }
}
