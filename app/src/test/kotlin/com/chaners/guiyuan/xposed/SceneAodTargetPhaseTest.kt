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

    @Test
    fun visualBoundaryRetainsAodOnlyWhenNativeDirectionAllowsIt() {
        fun project(target: Boolean, origin: ScenePolicy.StableKeyguardAodScene) =
            ScenePolicy.resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = false,
                aodEnabled = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene = origin,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = target,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = true,
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
                keyguardEnabled = false,
                aodEnabled = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = false,
                keyguardStatusIconsAlpha = 1f,
                nativeToLockScreenTarget = false,
                fullAodTargetSourceReady = true,
                fullAodTargetPending = true,
                fullAodVisualBoundary = false,
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
                    keyguardEnabled = keyguard,
                    aodEnabled = !keyguard,
                    steadySourceScene = SourceScene.KEYGUARD,
                    lastStableFamilyScene = origin,
                    homePresentationOwned = false,
                    keyguardStatusIconsAlpha = alpha,
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
