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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = true,
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = false,
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 1f,
                        nativeToLockScreenTarget = false,
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = true,
                        fullAodTargetSourceReady = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 0.5f,
                        nativeToLockScreenTarget = false,
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        keyguardStatusIconsAlpha = 0f,
                        nativeToLockScreenTarget = false,
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        nativeToLockScreenTarget = true,
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
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
                    boundary = ScenePolicy.AodNativeBoundaryEvidence(
                        nativeToLockScreenTarget = true,
                        fullAodTargetSourceReady = true,
                        fullAodTargetPending = true,
                        fullAodVisualBoundary = true,
                    ),
                    home = ScenePolicy.AodHomeEvidence(
                    ),
                ),
            ),
        )
    }

}
