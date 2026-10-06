package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardRenderSessionTest {
    @Test
    fun overlayNeedsCompletedHandoff() {
        assertFalse(KeyguardRenderSession.resolveOverlayVisible(false, false, false))
        assertFalse(KeyguardRenderSession.resolveOverlayVisible(true, true, false))
        assertFalse(KeyguardRenderSession.resolveOverlayVisible(true, false, true))
        assertTrue(KeyguardRenderSession.resolveOverlayVisible(true, false, false))
    }

    @Test
    fun familyChildGatesStayIndependent() {
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
    fun familyChildIgnoresBatteryAodAlpha() {
        assertEquals(
            1f,
            KeyguardRenderSession.resolveFamilyChildAlpha(),
            0.0001f,
        )
    }

    @Test
    fun readyNeedsCompleteKeyguardSurface() {
        assertTrue(KeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, false))
        assertFalse(KeyguardRenderSession.resolveOwnerReady(true, true, true, false, true, false))
        assertFalse(KeyguardRenderSession.resolveOwnerReady(true, true, true, true, false, false))
        assertFalse(KeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, true))
    }

    @Test
    fun retargetForcesReadyOnSceneChange() {
        assertFalse(
            KeyguardRenderSession.shouldForceReadinessDispatch(
                sceneChanged = false,
            ),
        )
        assertTrue(
            KeyguardRenderSession.shouldForceReadinessDispatch(
                sceneChanged = true,
            ),
        )
    }
}
