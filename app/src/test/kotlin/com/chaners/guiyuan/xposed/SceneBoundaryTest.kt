package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneBoundaryTest {
    @Test
    fun singleChildKeepsOutgoingVisual() {
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
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
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
    fun pendingAodKeepsOutgoingChild() {
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
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
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
    fun aodTargetRespectsUnknownOrigin() {
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
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = true,
            ),
        )
    }

    @Test
    fun latchedHomeSurvivesSceneChanges() {
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
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                nativeToLockScreenTarget = false,
                homeAodTransitionOrigin = true,
            ),
        )
    }

    @Test
    fun keyguardHandoffNeedsIncomingEnabled() {
        assertTrue(
            ScenePolicy.shouldUseKeyguardHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardHandoff(
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
            ScenePolicy.shouldUseKeyguardHandoff(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldUseKeyguardHandoff(
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
    fun armedHandoffCanPrecedeAodState() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = false,
                steadySourceScene = SourceScene.KEYGUARD,
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
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodVisualBoundary = false,
            ),
        )
    }

    @Test
    fun keyguardPrecommitNeedsHiddenNative() {
        assertTrue(
            ScenePolicy.shouldPrecommitKeyguardLayout(
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
            ScenePolicy.shouldPrecommitKeyguardLayout(
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
            ScenePolicy.shouldPrecommitKeyguardLayout(
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
            ScenePolicy.shouldPrecommitKeyguardLayout(
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
    fun aodOffFallbackNeedsVisibleHome() {
        assertTrue(
            ScenePolicy.shouldArmHomeAodFallback(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeAodFallback(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = false,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeAodFallback(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homePresentationOwned = false,
                homeCarrierPresentationVisible = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeAodFallback(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                homePresentationOwned = true,
                homeCarrierPresentationVisible = true,
            ),
        )
    }

    @Test
    fun aodOffFallbackConsumesOnAodAnimation() {
        assertTrue(
            ScenePolicy.shouldConsumeHomeAodFallback(
                candidateActive = true,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldConsumeHomeAodFallback(
                candidateActive = true,
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldConsumeHomeAodFallback(
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
    fun aodOffTargetReleasesArmedFallback() {
        assertTrue(
            ScenePolicy.shouldReleaseHomeForAodOff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeAodFallbackCandidate = true,
                homeOwnedAtAodStart = true,
                nativeToLockScreenTarget = false,
            ),
        )
        assertFalse(
            ScenePolicy.shouldReleaseHomeForAodOff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeAodFallbackCandidate = true,
                homeOwnedAtAodStart = true,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldReleaseHomeForAodOff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                homeAodFallbackCandidate = false,
                homeOwnedAtAodStart = true,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun homeAodFallbackBeatsStaleKeyguard() {
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
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 1f,
                homeNativeAodFallbackActive = true,
            ),
        )
    }

    @Test
    fun pendingAodClosesAtMatchingEndpoint() {
        assertFalse(
            ScenePolicy.aodTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = true,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            ScenePolicy.aodTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = false,
                isAodAnimate = false,
            ),
        )
        assertFalse(
            ScenePolicy.aodTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = false,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            ScenePolicy.aodTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = true,
                isAodAnimate = false,
            ),
        )
        assertFalse(
            ScenePolicy.aodTargetReachedStableState(
                pendingTargetToLockScreen = true,
                toAod = false,
                isAodAnimate = true,
            ),
        )
    }
}
