package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusSceneAodPolicyTest {
    @Test
    fun keyguardAndStableAodAreIndependentProjectedCandidates() {
        val keyguard = CombinedStatusScenePolicy.capability(CombinedStatusScene.KEYGUARD)
        assertEquals(CombinedStatusRenderMode.PROJECTED, keyguard.renderMode)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, keyguard.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.STATIC_VERIFIED, keyguard.evidence)

        val aod = CombinedStatusScenePolicy.capability(CombinedStatusScene.AOD)
        assertEquals(CombinedStatusRenderMode.PROJECTED, aod.renderMode)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, aod.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.STATIC_VERIFIED, aod.evidence)

        assertTrue(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = false,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = false,
                aodEnabled = true,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = false,
            ),
        )
    }

    @Test
    fun keyguardAndAodProjectionMatrixKeepsChildPreferencesIndependent() {
        fun resolveStable(
            feature: Boolean,
            keyguard: Boolean,
            aod: Boolean,
            toAod: Boolean,
            source: CombinedStatusSourceScene,
        ) =
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = feature,
                keyguardEnabled = keyguard,
                aodEnabled = aod,
                toAod = toAod,
                isAodAnimate = false,
                steadySourceScene = source,
            )

        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            resolveStable(true, true, false, false, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, false, true, false, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            resolveStable(true, false, true, true, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, false, true, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, true, false, CombinedStatusSourceScene.HOME),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(false, true, true, false, CombinedStatusSourceScene.KEYGUARD),
        )
    }

    @Test
    fun aodAnimationRoutesFromLatchedStableFamilySceneNotMutableOwnership() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
    }

    @Test
    fun directAodToHomeUnlockCannotReversePrearmAod() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
    }


    @Test
    fun nativeAodTargetCanPrearmHomeBeforeAnimationFlagCatchesUp() {
        assertTrue(
            CombinedStatusScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                nativeToLockScreenTarget = false,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                homeAodTargetPrearm = true,
            ),
        )
    }

    @Test
    fun homeAodTargetPrearmRejectsReverseOrKnownFamilyOrigin() {
        assertFalse(
            CombinedStatusScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun singleEnabledFamilyUsesNativeKeyguardStatusIconsBoundary() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0.99f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0.005f,
            ),
        )
    }

    @Test
    fun singleEnabledIncomingChildWaitsForNativeStatusIconsTakeover() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0.01f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0.99f,
            ),
        )
    }


    @Test
    fun aodOnlyHomePrearmBeatsTransientKeyguardStatusIconsBoundary() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
    }

    @Test
    fun controlCenterSourceConflictUsesStableFamilyHistoryAsDirectionEvidence() {
        assertEquals(
            CombinedStatusSourceScene.HOME,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.HOME,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.HOME,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.KEYGUARD,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.KEYGUARD,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.HOME,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.HOME,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.HOME,
                steadySourceScene = CombinedStatusSourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.HOME,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.HOME,
            CombinedStatusScenePolicy.resolveControlCenterSourceScene(
                panelSourceScene = CombinedStatusSourceScene.HOME,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
    }

    @Test
    fun singleEnabledFamilyFallsBackConservativelyWhenVisualEvidenceIsUnavailable() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardStatusIconsAlpha = null,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 1f,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                homePresentationOwned = true,
                keyguardStatusIconsAlpha = 0f,
            ),
        )
    }

    @Test
    fun latchedAodOriginBeatsStaleHomeOwnershipDuringAodToKeyguard() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
            ),
        )
    }

    @Test
    fun aodOnlyPrearmRequiresAnActualAnimationSignal() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
    }
    @Test
    fun aodPrearmRequiresAodFeatureAndVisibleHomeOwnership() {
        assertTrue(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = false,
                homeTransitionPrearm = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = false,
                stableAod = false,
                homeTransitionPrearm = true,
            ),
        )
    }
}
