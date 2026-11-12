package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.ScenePolicy.AodPeerAction
import com.chaners.guiyuan.xposed.ScenePolicy.KeyguardAodProjection
import com.chaners.guiyuan.xposed.ScenePolicy.StableKeyguardAodScene

// Stateless eligibility and AOD boundary decisions; ScenePolicy owns the main projection.
internal object SceneCcPolicy {
    fun shouldAcquireKeyguardCcLease(
        sourceScene: SourceScene,
        keyguardPresentationReady: Boolean,
        nativeFraction: Float,
    ): Boolean =
        sourceScene == SourceScene.KEYGUARD &&
            keyguardPresentationReady &&
            nativeFraction > 0f

    fun shouldReconcileCcForKeyguard(
        ccVisible: Boolean,
        nativeFraction: Float,
        leaseActive: Boolean,
    ): Boolean =
        ccVisible ||
            nativeFraction > 0f ||
            leaseActive

    fun incomingKeyguardPresentationReady(
        visualHandoffActive: Boolean,
        layoutPrecommitActive: Boolean,
        compactLayoutReady: Boolean,
        visualBoundaryReached: Boolean,
        hostAttached: Boolean,
    ): Boolean =
        visualHandoffActive &&
            layoutPrecommitActive &&
            compactLayoutReady &&
            visualBoundaryReached &&
            hostAttached

    fun retainedTransitionSourceWitnessAvailable(
        widthPx: Int,
        heightPx: Int,
        hostAttached: Boolean,
    ): Boolean =
        widthPx > 0 &&
            heightPx > 0 &&
            hostAttached

    fun resolveControlCenterSourceScene(
        reportedSourceScene: SourceScene,
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene = StableKeyguardAodScene.UNKNOWN,
        incomingKeyguardPresentationReady: Boolean = false,
    ): SourceScene {
        if (
            incomingKeyguardPresentationReady &&
            (
                reportedSourceScene == SourceScene.KEYGUARD ||
                    steadySourceScene == SourceScene.KEYGUARD
            )
        ) {
            return SourceScene.KEYGUARD
        }
        if (reportedSourceScene == steadySourceScene) return reportedSourceScene
        if (reportedSourceScene == SourceScene.UNKNOWN) return steadySourceScene
        if (steadySourceScene == SourceScene.UNKNOWN) return reportedSourceScene

        // A HOME/KEYGUARD disagreement is a lifecycle-boundary race between two
        // native witnesses. Family history provides direction without borrowing
        // mutable presentation ownership:
        // - a latched family child means Keyguard/AOD is the outgoing side, so
        //   HOME is the unlock target;
        // - UNKNOWN means stable Home was the prior family state, so KEYGUARD
        //   is the lock/AOD target.
        return if (lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN) {
            SourceScene.KEYGUARD
        } else {
            SourceScene.HOME
        }
    }

    fun controlCenterProjectionEligible(
        featureEnabled: Boolean,
        sourceScene: SourceScene,
        keyguardEnabled: Boolean,
    ): Boolean =
        featureEnabled &&
            when (sourceScene) {
                SourceScene.HOME -> true
                SourceScene.KEYGUARD -> keyguardEnabled
                SourceScene.UNKNOWN -> false
            }
}

internal object SceneAodBoundaryPolicy {
    // Follow native battery alpha only after it leaves AOD mode.
    fun outgoingAodAlpha(
        nativeToAod: Boolean?,
        nativeBatteryAlpha: Float?,
    ): Float =
        if (nativeToAod == false && nativeBatteryAlpha?.isFinite() == true) {
            1f - nativeBatteryAlpha.coerceIn(0f, 1f)
        } else {
            1f
        }

    fun aodPeerAction(
        enabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        aodClaimed: Boolean,
        nativeToLockScreenTarget: Boolean?,
    ): AodPeerAction {
        if (!enabled || keyguardEnabled || !aodEnabled || !aodClaimed) {
            return AodPeerAction.NONE
        }
        return when (nativeToLockScreenTarget) {
            true -> AodPeerAction.RELEASE
            false -> AodPeerAction.RECLAIM
            null -> AodPeerAction.NONE
        }
    }

    fun shouldArmHomeNativeAodFallbackCandidate(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        homePresentationOwned: Boolean,
        homeOriginConfirmed: Boolean,
    ): Boolean =
        featureEnabled &&
            keyguardEnabled &&
            !aodEnabled &&
            homePresentationOwned &&
            homeOriginConfirmed

    fun fullAodPendingTargetReachedStableState(
        pendingTargetToLockScreen: Boolean?,
        toAod: Boolean,
        isAodAnimate: Boolean,
    ): Boolean {
        if (pendingTargetToLockScreen == null || isAodAnimate) return false
        return if (pendingTargetToLockScreen) {
            !toAod
        } else {
            SysUiKeyguardAodSource.isStableAod(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
            )
        }
    }

    internal fun resolveFullAodVisualBoundary(
        nativeToLockScreenTarget: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        lastStableFamilyScene: StableKeyguardAodScene,
        keyguardStatusIconsAlpha: Float?,
    ): KeyguardAodProjection {
        return when {
            nativeToLockScreenTarget == true ->
                if (keyguardEnabled) {
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }

            keyguardEnabled &&
                !aodEnabled &&
                lastStableFamilyScene == StableKeyguardAodScene.KEYGUARD ->
                if ((keyguardStatusIconsAlpha ?: 1f) > 0f) {
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }

            aodEnabled -> KeyguardAodProjection.AOD
            else -> KeyguardAodProjection.NATIVE
        }
    }

    internal fun resolvePendingFullAodTarget(
        lastStableFamilyScene: StableKeyguardAodScene,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
    ): KeyguardAodProjection {
        return when (lastStableFamilyScene) {
            StableKeyguardAodScene.KEYGUARD ->
                if (keyguardEnabled) {
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.AOD ->
                if (aodEnabled) {
                    KeyguardAodProjection.AOD
                } else {
                    KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.UNKNOWN -> KeyguardAodProjection.NATIVE
        }
    }


    // The Home-origin witness can prearm AOD only without a latched family child.
    internal fun resolveAnimatingHomeOrigin(
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene,
        homePresentationOwned: Boolean,
        aodEnabled: Boolean,
    ): KeyguardAodProjection? {
        if (steadySourceScene == SourceScene.HOME) {
            return if (
                lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN &&
                homePresentationOwned &&
                aodEnabled
            ) {
                // Home -> AOD is the only transition without a prior stable
                // Keyguard/AOD child. A latched AOD/Keyguard origin instead
                // means we are leaving that family for Home and must not
                // reverse-prearm AOD during the outgoing animation.
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }

        if (
            steadySourceScene == SourceScene.KEYGUARD &&
            lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN &&
            homePresentationOwned &&
            aodEnabled
        ) {
            // Home -> AOD can briefly expose KEYGUARD ancestry before the AOD
            // callback catches up. Only an UNKNOWN family origin may use Home
            // ownership as prearm evidence. A latched AOD/Keyguard origin is
            // stronger and must not be overridden by stale Home ownership.
            return KeyguardAodProjection.AOD
        }

        return null
    }

    // Native status-icon alpha is the visual endpoint, not battery AOD alpha.
    internal fun resolveAnimatingNativeAlpha(
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        keyguardStatusIconsAlpha: Float?,
    ): KeyguardAodProjection? {
        // Single-child handoff follows the native Keyguard status-icons
        // layer in both directions. Alpha 1 belongs to the Keyguard endpoint;
        // alpha 0 belongs to the AOD endpoint. Battery AOD alpha is a separate
        // native animation and is not a whole-combined-scene lifetime signal.
        val alpha = keyguardStatusIconsAlpha
        if (
            keyguardEnabled &&
            !aodEnabled &&
            steadySourceScene == SourceScene.KEYGUARD &&
            alpha != null &&
            lastStableFamilyScene != StableKeyguardAodScene.UNKNOWN
        ) {
            return if (alpha > 0f) {
                KeyguardAodProjection.KEYGUARD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }
        if (
            !keyguardEnabled &&
            aodEnabled &&
            steadySourceScene == SourceScene.KEYGUARD &&
            alpha != null
        ) {
            when (lastStableFamilyScene) {
                StableKeyguardAodScene.AOD ->
                    if (alpha > 0f) return KeyguardAodProjection.NATIVE

                StableKeyguardAodScene.KEYGUARD ->
                    return if (alpha < 1f) {
                        KeyguardAodProjection.AOD
                    } else {
                        KeyguardAodProjection.NATIVE
                    }

                StableKeyguardAodScene.UNKNOWN -> Unit
            }
        }
        return null
    }

    // Freeze animation direction from the last stable family witness.
    internal fun resolveAnimatingStableFamily(
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
    ): KeyguardAodProjection {
        // Don't infer AOD direction from presentation ownership; attach/cleanup changes
        // that ownership and can bounce KEYGUARD -> NATIVE -> KEYGUARD. Freeze the
        // direction from the last stable family scene for the whole animation.
        return when (lastStableFamilyScene) {
            StableKeyguardAodScene.KEYGUARD ->
                when {
                    aodEnabled -> KeyguardAodProjection.AOD
                    keyguardEnabled -> KeyguardAodProjection.KEYGUARD
                    else -> KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.AOD ->
                when {
                    keyguardEnabled -> KeyguardAodProjection.KEYGUARD
                    aodEnabled -> KeyguardAodProjection.AOD
                    else -> KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.UNKNOWN ->
                if (
                    steadySourceScene == SourceScene.KEYGUARD &&
                    keyguardEnabled
                ) {
                    // Cold-start / late-install fallback: an authoritative
                    // Keyguard source may acquire only the enabled Keyguard
                    // child. No cross-child inference is made.
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }
        }
    }

}
