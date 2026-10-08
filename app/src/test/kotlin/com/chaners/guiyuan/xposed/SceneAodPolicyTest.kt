package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneAodPolicyTest {
    @Test
    fun keyguardAndStableAodAreIndependentProjectedCandidates() {
        val keyguard = ScenePolicy.capability(StatusScene.KEYGUARD)
        assertEquals(RenderMode.PROJECTED, keyguard.renderMode)
        assertEquals(MotionOwnership.SYSTEM_UI, keyguard.motionOwnership)

        val aod = ScenePolicy.capability(StatusScene.AOD)
        assertEquals(RenderMode.PROJECTED, aod.renderMode)
        assertEquals(MotionOwnership.SYSTEM_UI, aod.motionOwnership)

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
    fun keyguardAndAodProjectionMatrixKeepsChildPreferencesIndependent() {
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
    fun aodAnimationRoutesFromLatchedStableFamilySceneNotMutableOwnership() {
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
    fun directAodToHomeUnlockCannotReversePrearmAod() {
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
    fun nativeAodTargetCanPrearmHomeBeforeAnimationFlagCatchesUp() {
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
    fun homeAodTargetPrearmRejectsReverseOrKnownFamilyOrigin() {
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
    fun singleEnabledFamilyUsesNativeKeyguardStatusIconsBoundary() {
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
    fun singleEnabledIncomingChildWaitsForNativeStatusIconsTakeover() {
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
    fun aodOnlyHomePrearmBeatsTransientKeyguardStatusIconsBoundary() {
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
    fun controlCenterSourceConflictUsesStableFamilyHistoryAsDirectionEvidence() {
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.KEYGUARD,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.KEYGUARD,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            ScenePolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
    }

    @Test
    fun singleEnabledFamilyFallsBackConservativelyWhenVisualEvidenceIsUnavailable() {
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
    fun latchedAodOriginBeatsStaleHomeOwnershipDuringAodToKeyguard() {
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
    fun aodOnlyPrearmRequiresAnActualAnimationSignal() {
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
    fun aodPrearmRequiresAodFeatureAndVisibleHomeOwnership() {
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
