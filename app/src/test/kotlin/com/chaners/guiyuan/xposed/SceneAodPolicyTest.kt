package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneAodPolicyTest {
    @Test
    fun aodProjectionRequiresEnabledStableOrPrearmedTarget() {
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

    @Test
    fun aodExpandedBatteryCanPrecommitIncomingKeyguardLayout() {
        fun allow(
            alpha: Float?,
            nativeAodLayout: Boolean = false,
            target: Boolean? = true,
            aodEnabled: Boolean = false,
        ) = ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
            featureEnabled = true,
            keyguardEnabled = true,
            aodEnabled = aodEnabled,
            lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
            nativeToLockScreenTarget = target,
            statusIconsPresentationAlpha = alpha,
            nativeAodLayout = nativeAodLayout,
        )
        assertTrue(allow(alpha = 0f))
        assertTrue(allow(alpha = 1f, nativeAodLayout = true))
        assertFalse(allow(alpha = 1f))
        assertFalse(allow(alpha = 0.5f, nativeAodLayout = true))
        assertFalse(allow(alpha = null, nativeAodLayout = true))
        assertFalse(allow(alpha = 1f, nativeAodLayout = true, target = false))
        assertFalse(allow(alpha = 1f, nativeAodLayout = true, target = null))
        assertFalse(allow(alpha = 1f, nativeAodLayout = true, aodEnabled = true))
    }

    @Test
    fun nativeAodPeersNeedStableBatteryAndUnfinishedIconFade() {
        fun hold(
            target: Boolean? = false,
            toAod: Boolean = true,
            animating: Boolean = false,
            alpha: Float? = 1f,
            claimed: Boolean = true,
            keyguard: Boolean = true,
            aod: Boolean = false,
        ) = ScenePolicy.shouldKeepNativeAodPeers(
            featureEnabled = true,
            keyguardEnabled = keyguard,
            aodEnabled = aod,
            targetToLockScreen = target,
            batteryInAodMode = toAod,
            batteryAnimating = animating,
            statusIconsAlpha = alpha,
            keyguardClaimed = claimed,
        )
        assertTrue(hold())
        assertFalse(hold(alpha = 0f))
        assertFalse(hold(alpha = null))
        assertFalse(hold(toAod = false))
        assertFalse(hold(animating = true))
        assertFalse(hold(target = true))
        assertFalse(hold(target = null))
        assertFalse(hold(claimed = false))
        assertFalse(hold(keyguard = false))
        assertFalse(hold(aod = true))
        assertFalse(
            ScenePolicy.shouldKeepNativeAodPeers(
                featureEnabled = false,
                keyguardEnabled = true,
                aodEnabled = false,
                targetToLockScreen = false,
                batteryInAodMode = true,
                batteryAnimating = false,
                statusIconsAlpha = 1f,
                keyguardClaimed = true,
            ),
        )
    }

    @Test
    fun nativeAodBatteryModeOwnsTheFinalKeyguardClipRelease() {
        fun hold(
            target: Boolean? = false,
            batteryAod: Boolean = false,
            claimed: Boolean = true,
            animated: Boolean = true,
            aodEnabled: Boolean = false,
        ) = ScenePolicy.shouldHoldKeyguardForAodBattery(
            featureEnabled = true,
            keyguardEnabled = true,
            aodEnabled = aodEnabled,
            sourceScene = SourceScene.KEYGUARD,
            nativeToLockScreenTarget = target,
            batteryInAodMode = batteryAod,
            nativeHandoffActive = true,
            keyguardClaimed = claimed,
            animatedBatteryMode = animated,
        )

        assertTrue(hold())
        assertFalse(hold(batteryAod = true))
        assertFalse(hold(target = true))
        assertFalse(hold(target = null))
        assertFalse(hold(claimed = false))
        assertFalse(hold(animated = false))
        assertFalse(hold(aodEnabled = true))
        assertFalse(
            ScenePolicy.shouldHoldKeyguardForAodBattery(
                featureEnabled = false,
                keyguardEnabled = true,
                aodEnabled = false,
                sourceScene = SourceScene.KEYGUARD,
                nativeToLockScreenTarget = false,
                batteryInAodMode = false,
                nativeHandoffActive = true,
                keyguardClaimed = true,
                animatedBatteryMode = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldHoldKeyguardForAodBattery(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sourceScene = SourceScene.KEYGUARD,
                nativeToLockScreenTarget = false,
                batteryInAodMode = false,
                nativeHandoffActive = false,
                keyguardClaimed = true,
                animatedBatteryMode = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldHoldKeyguardForAodBattery(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sourceScene = SourceScene.HOME,
                nativeToLockScreenTarget = false,
                batteryInAodMode = false,
                nativeHandoffActive = true,
                keyguardClaimed = true,
                animatedBatteryMode = true,
            ),
        )
    }

    @Test
    fun pendingAodTargetRejectsPreviousKeyguardStableSignal() {
        assertFalse(
            ScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = false,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            ScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = true,
                isAodAnimate = false,
            ),
        )
    }

    @Test
    fun keyguardBoundaryHandoffStopsWhenNativeTargetChanges() {
        val eligible = { target: Boolean? ->
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = target,
            )
        }
        assertTrue(eligible(true))
        assertFalse(eligible(false))
        assertFalse(eligible(null))
    }
}
