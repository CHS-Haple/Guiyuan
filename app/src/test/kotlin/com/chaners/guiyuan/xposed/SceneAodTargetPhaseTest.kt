package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneAodTargetPhaseTest {
    @Test
    fun visualBoundaryWinsPendingTargetAndPreservesKeyguardAlpha() {
        fun project(alpha: Float) =
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = alpha,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),
                ),
            )

        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, project(0f))
        assertEquals(ScenePolicy.KeyguardAodProjection.KEYGUARD, project(0.25f))
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = true,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),
                ),
            ),
        )
    }

    @Test
    fun pendingTargetHoldsStableOriginOnlyWithNativeTargetEvidence() {
        fun projectAod(ready: Boolean, target: Boolean?) =
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = false,
                        aodEnabled = true,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = target,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = ready,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = false,
                    ),
                ),
            )

        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projectAod(true, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projectAod(false, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projectAod(true, null))
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = false,
                    ),
                ),
            ),
        )
    }

    @Test
    fun visualBoundaryRetainsAodOnlyWhenNativeDirectionAllowsIt() {
        fun project(target: Boolean, origin: ScenePolicy.StableKeyguardAodScene) =
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = false,
                        aodEnabled = true,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = origin,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = target,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),
                ),
            )

        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            project(false, ScenePolicy.StableKeyguardAodScene.AOD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            project(true, ScenePolicy.StableKeyguardAodScene.AOD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = false,
                        aodEnabled = true,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = false,
                    ),
                ),
            ),
        )
    }

    @Test
    fun nativeStatusIconAlphaMaintainsStableOriginFallbacks() {
        fun expectProjection(
            keyguard: Boolean,
            origin: ScenePolicy.StableKeyguardAodScene,
            alpha: Float?,
            expected: ScenePolicy.KeyguardAodProjection,
        ) {
            assertEquals(
                "keyguard=${keyguard} origin=${origin} alpha=${alpha}",
                expected,
                ScenePolicy.resolveAnimatingKeyguardAodProjection(
                    ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = keyguard,
                        aodEnabled = !keyguard,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene = origin,
                        homePresentationOwned = false,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = alpha,
                        nativeToLockScreenTarget = null,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = false,
                        fullAodTargetPending = false,
                        fullAodVisualBoundary = false,
                    ),
                ),
                ),
            )
        }

        val keyguard = ScenePolicy.StableKeyguardAodScene.KEYGUARD
        val aod = ScenePolicy.StableKeyguardAodScene.AOD
        val unknown = ScenePolicy.StableKeyguardAodScene.UNKNOWN
        val native = ScenePolicy.KeyguardAodProjection.NATIVE
        val showKeyguard = ScenePolicy.KeyguardAodProjection.KEYGUARD
        val showAod = ScenePolicy.KeyguardAodProjection.AOD

        expectProjection(true, keyguard, 0f, native)
        expectProjection(true, keyguard, 0.5f, showKeyguard)
        expectProjection(true, aod, 0f, native)
        expectProjection(true, aod, 0.5f, showKeyguard)
        expectProjection(true, unknown, 0f, showKeyguard)
        expectProjection(true, aod, null, showKeyguard)
        expectProjection(false, aod, 0f, showAod)
        expectProjection(false, aod, 0.5f, native)
        expectProjection(false, keyguard, 0.5f, showAod)
        expectProjection(false, keyguard, 1f, native)
        expectProjection(false, unknown, 0.5f, native)
    }
}
