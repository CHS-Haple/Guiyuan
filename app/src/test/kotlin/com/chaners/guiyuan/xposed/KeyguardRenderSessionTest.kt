package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardRenderSessionTest {
    @Test
    fun overlayRequiresFeatureAndCompletedNativeHandoff() {
        assertFalse(KeyguardRenderSession.resolveOverlayVisible(false, false, sceneEligible = true))
        assertFalse(KeyguardRenderSession.resolveOverlayVisible(true, true, sceneEligible = true))
        assertFalse(KeyguardRenderSession.resolveOverlayVisible(true, false, sceneEligible = false))
        assertTrue(KeyguardRenderSession.resolveOverlayVisible(true, false, sceneEligible = true))
    }

    @Test
    fun keyguardAndAodFamilyKeepChildFeatureGatesIndependent() {
        assertTrue(
            KeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sceneIsAod = false,
            ),
        )
        assertFalse(
            KeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                sceneIsAod = false,
            ),
        )
        assertTrue(
            KeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                sceneIsAod = true,
            ),
        )
        assertFalse(
            KeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sceneIsAod = true,
            ),
        )
    }

    @Test
    fun readinessRequiresCompleteAttachedKeyguardSurface() {
        assertTrue(KeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, sceneEligible = true))
        assertFalse(KeyguardRenderSession.resolveOwnerReady(true, true, true, false, true, sceneEligible = true))
        assertFalse(KeyguardRenderSession.resolveOwnerReady(true, true, true, true, false, sceneEligible = true))
        assertFalse(KeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, sceneEligible = false))
    }
}
