package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneAodOutgoingBoundaryTest {
    @Test
    fun outgoingKeyguardSingleChildHonorsNativeTarget() {
        data class Case(
            val toAod: Boolean,
            val alpha: Float,
            val target: Boolean,
            val expected: ScenePolicy.KeyguardAodProjection,
        )
        val cases = listOf(
            Case(false, 0f, true, ScenePolicy.KeyguardAodProjection.KEYGUARD),
            Case(false, 1f, false, ScenePolicy.KeyguardAodProjection.KEYGUARD),
            Case(true, 0f, false, ScenePolicy.KeyguardAodProjection.NATIVE),
        )
        val scene = ScenePolicy.AodSceneEvidence(
            steadySourceScene = SourceScene.KEYGUARD,
            lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
        )
        val boundary = ScenePolicy.AodNativeBoundaryEvidence(
            fullAodTargetSourceReady = true,
            fullAodVisualBoundary = true,
        )
        cases.forEach { (toAod, alpha, target, expected) ->
            val input = ScenePolicy.KeyguardAodProjectionInputs(
                feature = ScenePolicy.AodFeatureFlags(true, true, false, toAod, true),
                scene = scene,
                signal = ScenePolicy.AodNativeSignalEvidence(alpha, target),
                boundary = boundary,
            )
            assertEquals(expected, ScenePolicy.resolveKeyguardAodProjection(input))
        }
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
        val flags = ScenePolicy.AodFeatureFlags(true, true, false, true, true)
        val scene = ScenePolicy.AodSceneEvidence(
            steadySourceScene = SourceScene.KEYGUARD,
            lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
        )
        val cases = listOf(
            Triple(0f, false, ScenePolicy.KeyguardAodProjection.KEYGUARD),
            Triple(0.5f, true, ScenePolicy.KeyguardAodProjection.KEYGUARD),
            Triple(0f, true, ScenePolicy.KeyguardAodProjection.NATIVE),
        )
        cases.forEach { (alpha, visualBoundary, expected) ->
            val input = ScenePolicy.KeyguardAodProjectionInputs(
                feature = flags,
                scene = scene,
                signal = ScenePolicy.AodNativeSignalEvidence(alpha, false),
                boundary = ScenePolicy.AodNativeBoundaryEvidence(
                    fullAodTargetSourceReady = true,
                    fullAodTargetPending = true,
                    fullAodVisualBoundary = visualBoundary,
                ),
            )
            assertEquals(expected, ScenePolicy.resolveKeyguardAodProjection(input))
        }
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
