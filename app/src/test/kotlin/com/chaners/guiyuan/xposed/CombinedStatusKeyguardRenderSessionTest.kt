package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusKeyguardRenderSessionTest {
    @Test
    fun overlayRequiresFeatureAndCompletedNativeHandoff() {
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(false, false, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, false, true))
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, false, false))
    }

    @Test
    fun keyguardAndAodFamilyKeepChildFeatureGatesIndependent() {
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sceneIsAod = false,
            ),
        )
        assertFalse(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                sceneIsAod = false,
            ),
        )
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                sceneIsAod = true,
            ),
        )
        assertFalse(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sceneIsAod = true,
            ),
        )
    }

    @Test
    fun keyguardFamilyChildDoesNotCopyIndependentBatteryAodAlpha() {
        assertEquals(
            1f,
            CombinedStatusKeyguardRenderSession.resolveFamilyChildAlpha(),
            0.0001f,
        )
    }

    @Test
    fun readinessRequiresCompleteAttachedKeyguardSurface() {
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, false, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, false, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, true))
    }

    @Test
    fun retargetOnlyForcesPresentationReadinessWhenFamilySceneChanges() {
        assertFalse(
            CombinedStatusKeyguardRenderSession.shouldForceReadinessDispatch(
                sceneChanged = false,
            ),
        )
        assertTrue(
            CombinedStatusKeyguardRenderSession.shouldForceReadinessDispatch(
                sceneChanged = true,
            ),
        )
    }
}
