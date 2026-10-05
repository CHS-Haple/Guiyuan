package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusSceneBoundaryTest {
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
