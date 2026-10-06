package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneAodPolicyTest {
    @Test
    fun keyguardAndAodStayIndependent() {
        val keyguard = ScenePolicy.capability(StatusScene.KEYGUARD)
        assertEquals(RenderMode.PROJECTED, keyguard.renderMode)
        assertEquals(MotionOwnership.SYSTEM_UI, keyguard.motionOwnership)
        assertEquals(SceneEvidence.STATIC_VERIFIED, keyguard.evidence)

        val aod = ScenePolicy.capability(StatusScene.AOD)
        assertEquals(RenderMode.PROJECTED, aod.renderMode)
        assertEquals(MotionOwnership.SYSTEM_UI, aod.motionOwnership)
        assertEquals(SceneEvidence.STATIC_VERIFIED, aod.evidence)

        assertTrue(
            ScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = true,
            ),
        )
        assertFalse(
            ScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = false,
                stableAod = true,
            ),
        )
        assertFalse(
            ScenePolicy.aodProjectionEligible(
                featureEnabled = false,
                aodEnabled = true,
                stableAod = true,
            ),
        )
        assertFalse(
            ScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = false,
            ),
        )
    }

    @Test
    fun projectionKeepsChildPrefs() {
        fun resolveStable(
            feature: Boolean,
            keyguard: Boolean,
            aod: Boolean,
            toAod: Boolean,
            source: SourceScene,
        ) =
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = feature,
                keyguardEnabled = keyguard,
                aodEnabled = aod,
                toAod = toAod,
                isAodAnimate = false,
                steadySourceScene = source,
            )

        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            resolveStable(true, true, false, false, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, false, true, false, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            resolveStable(true, false, true, true, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, false, true, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, true, false, SourceScene.HOME),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(false, true, true, false, SourceScene.KEYGUARD),
        )
    }

    @Test
    fun aodRouteUsesStableScene() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
    }

    @Test
    fun aodToHomeDoesNotReversePrearm() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
    }


    @Test
    fun aodTargetCanPrearmHome() {
        assertTrue(
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                nativeToLockScreenTarget = false,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                homeAodTargetPrearm = true,
            ),
        )
    }

    @Test
    fun homePrearmRejectsReverseOrigin() {
        assertFalse(
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun singleFamilyUsesNativeBoundary() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0.99f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0.005f,
            ),
        )
    }

    @Test
    fun incomingChildWaitsForNative() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0.01f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0.99f,
            ),
        )
    }


    @Test
    fun aodPrearmBeatsTransientKeyguard() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
    }

    @Test
    fun ccConflictUsesStableHistory() {
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.KEYGUARD,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.KEYGUARD,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
    }

    @Test
    fun singleFamilyFallsBackConservatively() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                keyguardStatusIconsAlpha = null,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                homePresentationOwned = true,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
    }

    @Test
    fun latchedAodBeatsStaleHome() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
            ),
        )
    }

    @Test
    fun aodPrearmNeedsAnimation() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
    }
    @Test
    fun aodPrearmNeedsVisibleHome() {
        assertTrue(
            ScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = false,
                homeTransitionPrearm = true,
            ),
        )
        assertFalse(
            ScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = false,
                stableAod = false,
                homeTransitionPrearm = true,
            ),
        )
    }
}
