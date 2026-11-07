package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SceneAodNativeBoundaryTest {
    @Test
    fun homeAodTargetPrearmRejectsReverseOrKnownFamilyOrigin() {
        assertFalse(
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                nativeToLockScreenTarget = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
                nativeToLockScreenTarget = false,
            ),
        )
    }

    @Test
    fun aodOnlyHandsNativePeersBackAtIconBoundaryAndReclaimsOnReverse() {
        fun action(
            target: Boolean?,
            enabled: Boolean = true,
            keyguard: Boolean = false,
            aod: Boolean = true,
            claimed: Boolean = true,
        ) = ScenePolicy.aodPeerAction(
            enabled = enabled,
            keyguardEnabled = keyguard,
            aodEnabled = aod,
            aodClaimed = claimed,
            nativeToLockScreenTarget = target,
        )

        assertEquals(ScenePolicy.AodPeerAction.RELEASE, action(true))
        assertEquals(ScenePolicy.AodPeerAction.RECLAIM, action(false))
        assertEquals(ScenePolicy.AodPeerAction.NONE, action(null))
        assertEquals(ScenePolicy.AodPeerAction.NONE, action(true, claimed = false))
        assertEquals(ScenePolicy.AodPeerAction.NONE, action(true, keyguard = true))
        assertEquals(ScenePolicy.AodPeerAction.NONE, action(true, aod = false))
        assertEquals(ScenePolicy.AodPeerAction.NONE, action(true, enabled = false))
    }

    @Test
    fun outgoingAodClaimSurvivesCanceledBatteryAnimationAcrossQuickReverse() {
        fun projection(
            toAod: Boolean,
            animated: Boolean,
            owned: Boolean = true,
            target: Boolean = true,
            ready: Boolean = true,
            alpha: Float = 0.5f,
        ) = ScenePolicy.resolveKeyguardAodProjection(
            featureEnabled = true,
            keyguardEnabled = false,
            aodEnabled = true,
            toAod = toAod,
            isAodAnimate = animated,
            steadySourceScene = SourceScene.KEYGUARD,
            // Rapid reversal may never produce a stable AOD callback.
            lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            keyguardStatusIconsAlpha = alpha,
            nativeToLockScreenTarget = target,
            fullAodTargetSourceReady = ready,
            fullAodTargetPending = false,
            fullAodVisualBoundary = true,
            outgoingAodOwned = owned,
        )

        // A canceled earlier animation clears isAodAnimate before the new one begins.
        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projection(true, false))
        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projection(true, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projection(false, true))
        // Original battery has now committed a stable Keyguard mode.
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projection(false, false))

        // Not a general override: ownership and the authoritative target are required.
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projection(true, true, owned = false))
        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projection(true, true, target = false))
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            projection(true, true, ready = false, alpha = 1f),
        )
    }

    @Test
    fun keyguardOnlyProjectionTracksNativeIconBoundary() {
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
                keyguardStatusIconsAlpha = 1f,
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
                keyguardStatusIconsAlpha = 0.99f,
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
            ),
        )
    }

    @Test
    fun aodOnlyProjectionTracksNativeIconBoundary() {
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
                keyguardStatusIconsAlpha = 0f,
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
                keyguardStatusIconsAlpha = 0.005f,
            ),
        )
    }

    @Test
    fun singleEnabledIncomingChildWaitsForNativeStatusIconsTakeover() {
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
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0f,
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
                    ScenePolicy.StableKeyguardAodScene.AOD,
                keyguardStatusIconsAlpha = 0.01f,
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
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 1f,
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
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
                keyguardStatusIconsAlpha = 0.99f,
            ),
        )
    }


}
