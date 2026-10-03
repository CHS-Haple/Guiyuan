package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusScenePolicyTest {
    @Test
    fun everySceneHasExactlyOneCapability() {
        val capabilities = CombinedStatusScenePolicy.all()

        assertEquals(CombinedStatusScene.entries.size, capabilities.size)
        assertEquals(
            CombinedStatusScene.entries.toSet(),
            capabilities.map { it.scene }.toSet(),
        )
    }

    @Test
    fun homeStableUsesProjectedOverlayWithoutNativeSlotMutation() {
        val home = CombinedStatusScenePolicy.capability(CombinedStatusScene.HOME_STABLE)

        assertEquals(CombinedStatusRenderMode.PROJECTED, home.renderMode)
        assertEquals(CombinedStatusMotionOwnership.NONE, home.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.RUNTIME_VERIFIED, home.evidence)
    }

    @Test
    fun systemUiOwnedTransitionsDoNotRequestCombinedSlotMutation() {
        val systemUiOwned =
            CombinedStatusScenePolicy.all()
                .filter { it.motionOwnership == CombinedStatusMotionOwnership.SYSTEM_UI }

        assertTrue(systemUiOwned.isNotEmpty())
        systemUiOwned.forEach { capability ->
            assertTrue(capability.renderMode in CombinedStatusRenderMode.entries)
        }
    }

    @Test
    fun chargingIsNotModeledAsAnIndependentScene() {
        assertTrue(
            CombinedStatusScene.entries.none {
                it.name.contains("CHARG", ignoreCase = true)
            },
        )
    }

    @Test
    fun retainedTransitionSourceWitnessSurvivesPresentationHandoff() {
        assertTrue(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 0,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = false,
            ),
        )
    }

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

    @Test
    fun controlCenterProjectionInheritsVerifiedSourceSceneCapability() {
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.UNKNOWN,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseRejectsEveryIndependentInvalidBoundary() {
        val base =
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            )
        assertTrue(base)

        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = false,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = false,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = false,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = false,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
    }

    @Test
    fun hiddenControlCenterIgnoresKeyguardLifecycleChurnUntilItActuallyOpens() {
        assertFalse(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = true,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0.1f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = true,
            ),
        )
    }

    @Test
    fun incomingKeyguardBoundaryPresentationCanBridgeStableReadiness() {
        assertTrue(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = false,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = false,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = false,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = false,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = false,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseMaySpanIncomingBoundaryBeforeStableFamily() {
        assertTrue(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                incomingBoundaryPresentationReady = true,
                nativeFraction = 0.2f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                incomingBoundaryPresentationReady = false,
                nativeFraction = 0.2f,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseExistsOnlyInsideVerifiedNativeTransitionLifetime() {
        assertTrue(
            CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0f,
            ),
        )

        assertTrue(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.HOME,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0f,
            ),
        )
    }
    @Test
    fun singleChildNativeTargetPreservesOutgoingVisualLifetime() {
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
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
    }

    @Test
    fun fullAodTargetPendingRetainsOutgoingSingleChildThroughFadeLifetime() {
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
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
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
                keyguardStatusIconsAlpha = 0.5f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
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
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
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
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
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
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
            ),
        )
    }

    @Test
    fun fullAodTargetDoesNotOverrideUnknownOriginOrDualEnabledFamily() {
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
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
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
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
    }

    @Test
    fun latchedHomeOriginSurvivesMutableSceneAndOwnershipChanges() {
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
                homeAodTransitionOrigin = true,
            ),
        )
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
                    CombinedStatusScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                nativeToLockScreenTarget = false,
                homeAodTransitionOrigin = true,
            ),
        )
    }

    @Test
    fun keyguardBoundaryVisualHandoffIsOnlyForIncomingEnabledKeyguard() {
        assertTrue(
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                homeNativeAodFallbackActive = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.KEYGUARD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun armedKeyguardVisualHandoffCanPrecedeAodAnimateStateChange() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = false,
            ),
        )
    }

    @Test
    fun keyguardBoundaryLayoutPrecommitRequiresHiddenNativeStatusIcons() {
        assertTrue(
            CombinedStatusScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = 0f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = 0f,
                homeNativeAodFallbackActive = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = null,
            ),
        )
    }

    @Test
    fun disabledAodHomeFallbackArmsOnlyFromVisibleNativeHomeCarrier() {
        assertTrue(
            CombinedStatusScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = false,
                homeCarrierPresentationVisible = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = true,
            ),
        )
    }

    @Test
    fun disabledAodHomeFallbackConsumesOnNativeAodAnimationNotTransientKeyguard() {
        assertTrue(
            CombinedStatusScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = true,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = true,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = false,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
            ),
        )
    }

    @Test
    fun disabledAodDirectTargetReleasesOnlyArmedHomeFallback() {
        assertTrue(
            CombinedStatusScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeNativeAodFallbackCandidate = true,
                homePresentationOwnedAtFullAodStart = true,
                nativeToLockScreenTarget = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeNativeAodFallbackCandidate = true,
                homePresentationOwnedAtFullAodStart = true,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeNativeAodFallbackCandidate = false,
                homePresentationOwnedAtFullAodStart = true,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun activeHomeNativeAodFallbackOverridesStaleKeyguardFamilyEvidence() {
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
                    CombinedStatusScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 1f,
                homeNativeAodFallbackActive = true,
            ),
        )
    }

    @Test
    fun pendingFullAodTargetClosesOnlyAtItsMatchingStableEndpoint() {
        assertFalse(
            CombinedStatusScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = true,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = false,
                isAodAnimate = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = false,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = true,
                isAodAnimate = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = false,
                isAodAnimate = true,
            ),
        )
    }


}
