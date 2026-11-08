package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneAodTargetPhaseTest {
    @Test
    fun visualBoundaryWinsPendingTargetAndPreservesKeyguardAlpha() {
        fun project(alpha: Float) =
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = true,
                aodEnabled = false,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = alpha,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
            )

        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, project(0f))
        assertEquals(ScenePolicy.KeyguardAodProjection.KEYGUARD, project(0.25f))
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = true,
                aodEnabled = false,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = true,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
            ),
        )
    }

    @Test
    fun pendingTargetHoldsStableOriginOnlyWithNativeTargetEvidence() {
        fun projectAod(ready: Boolean, target: Boolean?) =
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = false,
                aodEnabled = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = target,
                fullAodTargetSourceReady = ready,
                fullAodTargetPending = true,
                fullAodVisualBoundary = false,
            )

        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projectAod(true, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projectAod(false, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projectAod(true, null))
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = true,
                aodEnabled = false,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 0f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = false,
            ),
        )
    }
}
