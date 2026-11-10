package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneAodPolicyTest {
    @Test
    fun outgoingAodRingFollowsNativeBatteryOnlyAfterModeExit() {
        assertEquals(1f, SceneAodBoundaryPolicy.outgoingAodAlpha(true, 0.85f), 0f)
        assertEquals(1f, SceneAodBoundaryPolicy.outgoingAodAlpha(null, 0f), 0f)
        assertEquals(1f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, 0f), 0f)
        assertEquals(0.6f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, 0.4f), 0.0001f)
        assertEquals(0f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, 1f), 0f)
        assertEquals(1f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, -0.5f), 0f)
        assertEquals(0f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, 1.5f), 0f)
        assertEquals(1f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, null), 0f)
        assertEquals(1f, SceneAodBoundaryPolicy.outgoingAodAlpha(false, Float.NaN), 0f)
    }

    @Test
    fun homeAodPrearmUsesLiveProjectionDecision() {
        fun projected(feature: Boolean, aod: Boolean, prearmed: Boolean) =
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = feature,
                keyguardEnabled = false,
                aodEnabled = aod,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homeAodTargetPrearm = prearmed,
            )

        assertEquals(ScenePolicy.KeyguardAodProjection.AOD, projected(true, true, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projected(true, false, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projected(false, true, true))
        assertEquals(ScenePolicy.KeyguardAodProjection.NATIVE, projected(true, true, false))
    }

    @Test
    fun keyguardAndAodProjectionMatrixKeepsChildPreferencesIndependent() {
        fun resolveStable(
            feature: Boolean,
            keyguard: Boolean,
            aod: Boolean,
            toAod: Boolean,
            source: SourceScene,
        ) =
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = feature,
                keyguardEnabled = keyguard,
                aodEnabled = aod,
                toAod = toAod,
                isAodAnimate = false,
                steadySourceScene = source,
            )

        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            resolveStable(true, true, false, false, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, false, true, false, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            resolveStable(true, false, true, true, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, false, true, SourceScene.KEYGUARD),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, true, false, SourceScene.HOME),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(false, true, true, false, SourceScene.KEYGUARD),
        )
    }

    @Test
    fun aodAnimationRoutesFromLatchedStableFamilySceneNotMutableOwnership() {
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
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.KEYGUARD,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.KEYGUARD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
            ),
        )
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
            ),
        )
    }

    @Test
    fun directAodToHomeUnlockCannotReversePrearmAod() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.AOD,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            ScenePolicy.KeyguardAodProjection.AOD,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
    }


    @Test
    fun nativeAodTargetCanPrearmHomeBeforeAnimationFlagCatchesUp() {
        assertTrue(
            ScenePolicy.shouldArmHomeAodTargetPrearm(
                featureEnabled = true,
                aodEnabled = true,
                steadySourceScene = SourceScene.HOME,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
                nativeToLockScreenTarget = false,
            ),
        )
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
                fullAodTargetSourceReady = true,
                homeAodTargetPrearm = true,
            ),
        )
    }

    @Test
    fun aodOnlyPrearmRequiresAnActualAnimationSignal() {
        assertEquals(
            ScenePolicy.KeyguardAodProjection.NATIVE,
            ScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = SourceScene.KEYGUARD,
                lastStableFamilyScene =
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
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
                    ScenePolicy.StableKeyguardAodScene.UNKNOWN,
                homePresentationOwned = true,
            ),
        )
    }
}

class SceneAodBoundaryTest {
    @Test
    fun aodExpandedBatteryCanPrecommitIncomingKeyguardLayout() {
        fun allow(
            alpha: Float?,
            nativeAodLayout: Boolean = false,
            target: Boolean? = true,
            aodEnabled: Boolean = false,
        ) = ScenePolicy.shouldPrecommitKeyguardBoundaryLayout(
            featureEnabled = true,
            keyguardEnabled = true,
            aodEnabled = aodEnabled,
            lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
            nativeToLockScreenTarget = target,
            statusIconsPresentationAlpha = alpha,
            nativeAodLayout = nativeAodLayout,
        )
        assertTrue(allow(alpha = 0f))
        assertTrue(allow(alpha = 1f, nativeAodLayout = true))
        assertFalse(allow(alpha = 1f))
        assertFalse(allow(alpha = 0.5f, nativeAodLayout = true))
        assertFalse(allow(alpha = null, nativeAodLayout = true))
        assertFalse(allow(alpha = 1f, nativeAodLayout = true, target = false))
        assertFalse(allow(alpha = 1f, nativeAodLayout = true, target = null))
        assertFalse(allow(alpha = 1f, nativeAodLayout = true, aodEnabled = true))
    }

    @Test
    fun nativeAodBatteryModeOwnsTheFinalKeyguardClipRelease() {
        fun hold(
            target: Boolean? = false,
            batteryAod: Boolean = false,
            claimed: Boolean = true,
            animated: Boolean = true,
            aodEnabled: Boolean = false,
        ) = ScenePolicy.shouldHoldKeyguardForAodBattery(
            featureEnabled = true,
            keyguardEnabled = true,
            aodEnabled = aodEnabled,
            sourceScene = SourceScene.KEYGUARD,
            nativeToLockScreenTarget = target,
            batteryInAodMode = batteryAod,
            nativeHandoffActive = true,
            keyguardClaimed = claimed,
            animatedBatteryMode = animated,
        )

        assertTrue(hold())
        assertFalse(hold(batteryAod = true))
        assertFalse(hold(target = true))
        assertFalse(hold(target = null))
        assertFalse(hold(claimed = false))
        assertFalse(hold(animated = false))
        assertFalse(hold(aodEnabled = true))
        assertFalse(
            ScenePolicy.shouldHoldKeyguardForAodBattery(
                featureEnabled = false,
                keyguardEnabled = true,
                aodEnabled = false,
                sourceScene = SourceScene.KEYGUARD,
                nativeToLockScreenTarget = false,
                batteryInAodMode = false,
                nativeHandoffActive = true,
                keyguardClaimed = true,
                animatedBatteryMode = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldHoldKeyguardForAodBattery(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sourceScene = SourceScene.KEYGUARD,
                nativeToLockScreenTarget = false,
                batteryInAodMode = false,
                nativeHandoffActive = false,
                keyguardClaimed = true,
                animatedBatteryMode = true,
            ),
        )
        assertFalse(
            ScenePolicy.shouldHoldKeyguardForAodBattery(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sourceScene = SourceScene.HOME,
                nativeToLockScreenTarget = false,
                batteryInAodMode = false,
                nativeHandoffActive = true,
                keyguardClaimed = true,
                animatedBatteryMode = true,
            ),
        )
    }

    @Test
    fun pendingAodTargetRejectsPreviousKeyguardStableSignal() {
        assertFalse(
            SceneAodBoundaryPolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = false,
                isAodAnimate = false,
            ),
        )
        assertTrue(
            SceneAodBoundaryPolicy.fullAodPendingTargetReachedStableState(
                pendingTargetToLockScreen = false,
                toAod = true,
                isAodAnimate = false,
            ),
        )
    }

    @Test
    fun keyguardBoundaryHandoffStopsWhenNativeTargetChanges() {
        val eligible = { target: Boolean? ->
            ScenePolicy.shouldUseKeyguardBoundaryVisualHandoff(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                lastStableFamilyScene = ScenePolicy.StableKeyguardAodScene.AOD,
                nativeToLockScreenTarget = target,
            )
        }
        assertTrue(eligible(true))
        assertFalse(eligible(false))
        assertFalse(eligible(null))
    }
}
