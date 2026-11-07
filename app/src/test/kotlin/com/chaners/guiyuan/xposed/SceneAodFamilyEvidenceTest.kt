package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneAodFamilyEvidenceTest {
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
    fun ccSourceUsesFamilyHistoryForDirection() {
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
    }

    @Test
    fun ccSourceFallsBackOrUsesIncomingPresentation() {
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
}
