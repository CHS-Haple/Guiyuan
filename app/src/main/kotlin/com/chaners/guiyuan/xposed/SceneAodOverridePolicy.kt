package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.ScenePolicy.KeyguardAodProjection
import com.chaners.guiyuan.xposed.ScenePolicy.KeyguardAodProjectionInputs
import com.chaners.guiyuan.xposed.ScenePolicy.StableKeyguardAodScene

// Preemptive Home and native-AOD boundary witnesses take precedence over
// ongoing family projections. This is pure policy, not a state or callback owner.
internal object SceneAodOverridePolicy {
    /** Home ownership and prearm checks always precede native AOD handoff evidence. */
    fun homeAodOverride(input: KeyguardAodProjectionInputs): KeyguardAodProjection? {
        val feature = input.feature
        val home = input.home
        if (home.homeNativeAodFallbackActive && feature.keyguardEnabled && !feature.aodEnabled) {
            return KeyguardAodProjection.NATIVE
        }
        if (home.homeAodTransitionOrigin &&
            input.signal.nativeToLockScreenTarget == false &&
            input.scene.lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN
        ) {
            return if (feature.aodEnabled) KeyguardAodProjection.AOD else KeyguardAodProjection.NATIVE
        }
        if (home.homeAodTargetPrearm && feature.aodEnabled &&
            input.scene.lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN
        ) {
            // Native target latch may span transient KEYGUARD ancestry until AOD catches up.
            return KeyguardAodProjection.AOD
        }
        return null
    }

    /** Preserve boundary ordering before steady-state and animation decisions. */
    fun nativeAodBoundaryOverride(input: KeyguardAodProjectionInputs): KeyguardAodProjection? {
        val feature = input.feature
        val scene = input.scene
        val boundary = input.boundary
        val nativeTarget = input.signal.nativeToLockScreenTarget
        if (boundary.fullAodVisualBoundary && boundary.fullAodTargetSourceReady &&
            nativeTarget == true && scene.steadySourceScene == SourceScene.KEYGUARD &&
            scene.lastStableFamilyScene == StableKeyguardAodScene.AOD &&
            feature.keyguardEnabled && !feature.aodEnabled
        ) {
            // Visual-only lease; native ignored-slot and reservation ownership remain deferred.
            return KeyguardAodProjection.KEYGUARD
        }
        if (feature.aodEnabled && !feature.keyguardEnabled &&
            boundary.outgoingAodOwned && boundary.fullAodTargetSourceReady &&
            nativeTarget == true && (feature.toAod || feature.isAodAnimate)
        ) {
            // A canceled callback can precede the new Keyguard battery mode.
            return KeyguardAodProjection.AOD
        }
        return null
    }
}
