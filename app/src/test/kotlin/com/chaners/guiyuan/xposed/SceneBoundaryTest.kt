package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneBoundaryTest {
    @Test
    fun singleChildNativeTargetPreservesOutgoingVisualLifetime() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
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
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0.5f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
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
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
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
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
    }

    @Test
    fun latchedHomeOriginSurvivesMutableSceneAndOwnershipChanges() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                nativeToLockScreenTarget = false,
                homeAodTransitionOrigin = true,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                nativeToLockScreenTarget = false,
                homeAodTransitionOrigin = true,
            ),
        )
    }

    @Test
    fun keyguardBoundaryVisualHandoffIsOnlyForIncomingEnabledKeyguard() {
        assertTrue(
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                homeNativeAodFallbackActive = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun armedKeyguardVisualHandoffCanPrecedeAodAnimateStateChange() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = false,
            ),
        )
    }

    @Test
    fun keyguardBoundaryLayoutPrecommitRequiresHiddenNativeStatusIcons() {
        assertTrue(
            ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = 0f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = 0f,
                homeNativeAodFallbackActive = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = 1f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                statusIconsPresentationAlpha = null,
            ),
        )
    }

    @Test
    fun disabledAodHomeFallbackArmsOnlyFromVisibleNativeHomeCarrier() {
        assertTrue(
            ScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = false,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = false,
                homeCarrierPresentationVisible = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeNativeAodFallbackCandidate(
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
            ScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = true,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
                candidateActive = true,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldConsumeHomeNativeAodFallbackOnAodState(
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
            ScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeNativeAodFallbackCandidate = true,
                homePresentationOwnedAtFullAodStart = true,
                nativeToLockScreenTarget = false,
            ),
        )
        assertFalse(
            ScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeNativeAodFallbackCandidate = true,
                homePresentationOwnedAtFullAodStart = true,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldReleaseTransientHomeKeyguardForDisabledAod(
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
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 1f,
                homeNativeAodFallbackActive = true,
            ),
        )
    }

    @Test
    fun pendingFullAodTargetClosesOnlyAtItsMatchingStableEndpoint() {
        assertFalse(
            ScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = true,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            ScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = false,
                isAodAnimate = false,
            ),
        )
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
        assertFalse(
            ScenePolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = false,
                isAodAnimate = true,
            ),
        )
    }
}
