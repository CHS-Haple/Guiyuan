package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneAodOutgoingBoundaryTest {
    @Test
    fun outgoingKeyguardSingleChildHonorsNativeTarget() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
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
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = true,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),

                ),
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
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
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),

                ),
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = true,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),

                ),
            ),
        )
    }

    @Test
    fun outgoingAodSingleChildHonorsNativeLockscreenTarget() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = false,
                        aodEnabled = true,
                        toAod = true,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.AOD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),

                ),
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = false,
                        aodEnabled = true,
                        toAod = true,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.AOD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = true,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),

                ),
            ),
        )
    }

    @Test
    fun pendingAodTargetKeepsOutgoingKeyguardUntilFadeCompletes() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = true,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                    ),

                ),
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = true,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0.5f,
                        nativeToLockScreenTarget = false,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),

                ),
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = false,
                        toAod = true,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
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
    fun pendingAodTargetPreservesOutgoingAodUntilBoundary() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
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
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.AOD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        nativeToLockScreenTarget = true,
                    ),
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                    ),

                ),
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
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
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.AOD,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
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

}
