package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneAodFamilyEvidenceTest {
    @Test
    fun aodOnlyHomePrearmBeatsTransientKeyguardStatusIconsBoundary() {
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
                            ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                        homePresentationOwned = true,
                    ),
                    signal = ScenePolicy.AodNativeSignalEvidence(
                        keyguardStatusIconsAlpha = 1f,
                    ),

                ),
            ),
        )
    }

    @Test
    fun ccSourceUsesFamilyHistoryForDirection() {
        assertEquals(
            SourceScene.HOME,
            SceneCcPolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            SceneCcPolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.KEYGUARD,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SceneCcPolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.KEYGUARD,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SceneCcPolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
            ),
        )
    }

    @Test
    fun ccSourceFallsBackOrUsesIncomingPresentation() {
        assertEquals(
            SourceScene.HOME,
            SceneCcPolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            SourceScene.KEYGUARD,
            SceneCcPolicy.resolveControlCenterSourceScene(
                reportedSourceScene = SourceScene.HOME,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                incomingKeyguardPresentationReady = true,
            ),
        )
        assertEquals(
            SourceScene.HOME,
            SceneCcPolicy.resolveControlCenterSourceScene(
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
        data class Case(
            val keyguard: Boolean,
            val alpha: Float?,
            val homeOwned: Boolean,
            val expected: ScenePolicy.KeyguardAodProjection,
        )
        val cases = listOf(
            Case(true, null, false, ScenePolicy.KeyguardAodProjection.KEYGUARD),
            Case(false, 1f, false, ScenePolicy.KeyguardAodProjection.NATIVE),
            Case(false, 0f, true, ScenePolicy.KeyguardAodProjection.AOD),
        )
        cases.forEach { (keyguard, alpha, homeOwned, expected) ->
            val input = ScenePolicy.KeyguardAodProjectionInputs(
                feature = ScenePolicy.AodFeatureFlags(
                    featureEnabled = true,
                    keyguardEnabled = keyguard,
                    aodEnabled = !keyguard,
                    toAod = false,
                    isAodAnimate = true,
                ),
                scene = ScenePolicy.AodSceneEvidence(
                    steadySourceScene = SourceScene.KEYGUARD,
                    homePresentationOwned = homeOwned,
                ),
                signal = ScenePolicy.AodNativeSignalEvidence(keyguardStatusIconsAlpha = alpha),
            )
            assertEquals(expected, ScenePolicy.resolveKeyguardAodProjection(input))
        }
    }

    @Test
    fun latchedAodOriginBeatsStaleHomeOwnershipDuringAodToKeyguard() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                ScenePolicy.KeyguardAodProjectionInputs(
                    feature = ScenePolicy.AodFeatureFlags(
                        featureEnabled = true,
                        keyguardEnabled = true,
                        aodEnabled = true,
                        toAod = false,
                        isAodAnimate = true,
                    ),
                    scene = ScenePolicy.AodSceneEvidence(
                        steadySourceScene = SourceScene.KEYGUARD,
                        lastStableFamilyScene =
                            ScenePolicy.StableKeyguardAodScene.AOD,
                        homePresentationOwned = true,
                    ),

                ),
            ),
        )
    }
}
