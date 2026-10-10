package com.chaners.guiyuan.xposed

import android.view.View

internal data class TransitionSourceWitness(
    val renderView: View,
    val logicalLeftPx: Int,
    val logicalTopPx: Int,
    val logicalWidthPx: Int,
    val logicalHeightPx: Int,
    val positionHost: View,
    val motionCarrier: View,
    val representedSlots: Set<String>,
)

internal enum class SourceScene {
    HOME,
    KEYGUARD,
    UNKNOWN,
}

internal object ScenePolicy {
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

    fun shouldRetainKeyguardCcLease(
        leaseActive: Boolean,
        sourceScene: SourceScene,
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        hostAttached: Boolean,
        aodBlocked: Boolean,
        incomingBoundaryPresentationReady: Boolean = false,
        nativeFraction: Float,
    ): Boolean =
        leaseActive &&
            sourceScene == SourceScene.KEYGUARD &&
            featureEnabled &&
            keyguardEnabled &&
            hostAttached &&
            (!aodBlocked || incomingBoundaryPresentationReady) &&
            nativeFraction > 0f

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

    fun aodProjectionEligible(
        featureEnabled: Boolean,
        aodEnabled: Boolean,
        stableAod: Boolean,
        homeTransitionPrearm: Boolean = false,
    ): Boolean =
        featureEnabled &&
            aodEnabled &&
            (stableAod || homeTransitionPrearm)

    enum class KeyguardAodProjection {
        NATIVE,
        KEYGUARD,
        AOD,
    }

    enum class StableKeyguardAodScene {
        UNKNOWN,
        KEYGUARD,
        AOD,
    }

    fun resolveKeyguardAodProjection(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        toAod: Boolean,
        isAodAnimate: Boolean,
        steadySourceScene: SourceScene = SourceScene.UNKNOWN,
        lastStableFamilyScene: StableKeyguardAodScene = StableKeyguardAodScene.UNKNOWN,
        homePresentationOwned: Boolean = false,
        keyguardStatusIconsAlpha: Float? = null,
        nativeToLockScreenTarget: Boolean? = null,
        fullAodTargetSourceReady: Boolean = false,
        fullAodTargetPending: Boolean = false,
        fullAodVisualBoundary: Boolean = false,
        outgoingAodOwned: Boolean = false,
        homeAodTransitionOrigin: Boolean = false,
        homeAodTargetPrearm: Boolean = false,
        homeNativeAodFallbackActive: Boolean = false,
    ): KeyguardAodProjection {
        if (!featureEnabled) return KeyguardAodProjection.NATIVE
        if (
            homeNativeAodFallbackActive &&
            keyguardEnabled &&
            !aodEnabled
        ) {
            return KeyguardAodProjection.NATIVE
        }
        if (
            homeAodTransitionOrigin &&
            nativeToLockScreenTarget == false &&
            lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN
        ) {
            return if (aodEnabled) {
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }
        if (
            homeAodTargetPrearm &&
            aodEnabled &&
            lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN
        ) {
            // The latch is armed only from an authoritative native AOD target
            // while Home still owns represented slots. Once armed, it may span
            // transient KEYGUARD ancestry until native AOD state catches up.
            return KeyguardAodProjection.AOD
        }
        if (
            fullAodVisualBoundary &&
            fullAodTargetSourceReady &&
            nativeToLockScreenTarget == true &&
            steadySourceScene == SourceScene.KEYGUARD &&
            lastStableFamilyScene == StableKeyguardAodScene.AOD &&
            keyguardEnabled &&
            !aodEnabled
        ) {
            // An explicitly armed AOD -> Keyguard visual-only lease may start
            // immediately after native mToLockScreen commits, before
            // setIsAodAnimate(true) reaches the battery state source. This
            // authorizes only the replacement renderer / clip mask; native
            // ignored-slot and reservation ownership remain deferred.
            return KeyguardAodProjection.KEYGUARD
        }
        if (
            aodEnabled && !keyguardEnabled &&
            outgoingAodOwned && fullAodTargetSourceReady &&
            nativeToLockScreenTarget == true &&
            (toAod || isAodAnimate)
        ) {
            // Canceled AOD animation callbacks can precede the new Keyguard battery mode.
            return KeyguardAodProjection.AOD
        }
        if (isAodAnimate) {
            return resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = keyguardEnabled,
                aodEnabled = aodEnabled,
                steadySourceScene = steadySourceScene,
                lastStableFamilyScene = lastStableFamilyScene,
                homePresentationOwned = homePresentationOwned,
                keyguardStatusIconsAlpha = keyguardStatusIconsAlpha,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
                fullAodTargetSourceReady = fullAodTargetSourceReady,
                fullAodTargetPending = fullAodTargetPending,
                fullAodVisualBoundary = fullAodVisualBoundary,
            )
        }
        if (
            SysUiKeyguardAodSource.isStableAod(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
            )
        ) {
            return if (aodEnabled) {
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }
        if (
            SysUiKeyguardAodSource.blocksKeyguardProjection(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
            )
        ) {
            return KeyguardAodProjection.NATIVE
        }
        if (steadySourceScene == SourceScene.HOME) {
            return KeyguardAodProjection.NATIVE
        }
        return if (
            steadySourceScene == SourceScene.KEYGUARD &&
            keyguardEnabled
        ) {
            KeyguardAodProjection.KEYGUARD
        } else {
            KeyguardAodProjection.NATIVE
        }
    }

    fun shouldArmHomeAodTargetPrearm(
        featureEnabled: Boolean,
        aodEnabled: Boolean,
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene,
        homePresentationOwned: Boolean,
        nativeToLockScreenTarget: Boolean?,
    ): Boolean =
        featureEnabled &&
            aodEnabled &&
            steadySourceScene == SourceScene.HOME &&
            lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN &&
            homePresentationOwned &&
            nativeToLockScreenTarget == false

    fun shouldUseKeyguardBoundaryVisualHandoff(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        lastStableFamilyScene: StableKeyguardAodScene,
        nativeToLockScreenTarget: Boolean?,
        homeNativeAodFallbackActive: Boolean = false,
    ): Boolean =
        featureEnabled &&
            !homeNativeAodFallbackActive &&
            keyguardEnabled &&
            !aodEnabled &&
            lastStableFamilyScene == StableKeyguardAodScene.AOD &&
            nativeToLockScreenTarget == true

    fun shouldPrecommitKeyguardBoundaryLayout(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        lastStableFamilyScene: StableKeyguardAodScene,
        nativeToLockScreenTarget: Boolean?,
        statusIconsPresentationAlpha: Float?,
        nativeAodLayout: Boolean = false,
        homeNativeAodFallbackActive: Boolean = false,
    ): Boolean =
        shouldUseKeyguardBoundaryVisualHandoff(
            featureEnabled = featureEnabled,
            keyguardEnabled = keyguardEnabled,
            aodEnabled = aodEnabled,
            lastStableFamilyScene = lastStableFamilyScene,
            nativeToLockScreenTarget = nativeToLockScreenTarget,
            homeNativeAodFallbackActive = homeNativeAodFallbackActive,
        ) &&
            // When the native battery still has AOD geometry, claim the
            // existing reservation before revealing the Keyguard replacement.
            (
                statusIconsPresentationAlpha == 0f ||
                    (statusIconsPresentationAlpha == 1f && nativeAodLayout)
            )

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

    fun shouldConsumeHomeNativeAodFallbackOnAodState(
        candidateActive: Boolean,
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        toAod: Boolean,
        isAodAnimate: Boolean,
    ): Boolean =
        candidateActive &&
            featureEnabled &&
            keyguardEnabled &&
            !aodEnabled &&
            toAod &&
            isAodAnimate

    fun shouldReleaseTransientHomeKeyguardForDisabledAod(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        homeNativeAodFallbackCandidate: Boolean,
        homePresentationOwnedAtFullAodStart: Boolean,
        nativeToLockScreenTarget: Boolean?,
    ): Boolean =
        featureEnabled &&
            keyguardEnabled &&
            !aodEnabled &&
            homeNativeAodFallbackCandidate &&
            homePresentationOwnedAtFullAodStart &&
            nativeToLockScreenTarget == false

    fun shouldHoldKeyguardForAodBattery(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        sourceScene: SourceScene,
        nativeToLockScreenTarget: Boolean?,
        batteryInAodMode: Boolean,
        nativeHandoffActive: Boolean,
        keyguardClaimed: Boolean,
        animatedBatteryMode: Boolean,
    ): Boolean =
        featureEnabled &&
            keyguardEnabled &&
            !aodEnabled &&
            sourceScene == SourceScene.KEYGUARD &&
            nativeToLockScreenTarget == false &&
            !batteryInAodMode &&
            nativeHandoffActive &&
            keyguardClaimed &&
            animatedBatteryMode

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

    internal fun resolveAnimatingKeyguardAodProjection(
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene,
        homePresentationOwned: Boolean,
        keyguardStatusIconsAlpha: Float? = null,
        nativeToLockScreenTarget: Boolean? = null,
        fullAodTargetSourceReady: Boolean = false,
        fullAodTargetPending: Boolean = false,
        fullAodVisualBoundary: Boolean = false,
    ): KeyguardAodProjection {
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

        // mToLockScreen tells us the direction, not that the visible handoff is done.
        // While the target is pending, keep the enabled outgoing child (otherwise Native);
        // only animateIconContainer may consume the target.
        if (
            fullAodTargetSourceReady &&
            nativeToLockScreenTarget != null &&
            steadySourceScene == SourceScene.KEYGUARD &&
            lastStableFamilyScene != StableKeyguardAodScene.UNKNOWN &&
            keyguardEnabled != aodEnabled
        ) {
            if (fullAodVisualBoundary) {
                return if (nativeToLockScreenTarget) {
                    if (keyguardEnabled) {
                        KeyguardAodProjection.KEYGUARD
                    } else {
                        KeyguardAodProjection.NATIVE
                    }
                } else {
                    when {
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
            }
            if (fullAodTargetPending) {
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
        }

        // Single-child handoff follows the native Keyguard status-icons
        // layer in both directions. Alpha 1 belongs to the Keyguard endpoint;
        // alpha 0 belongs to the AOD endpoint. Battery AOD alpha is a separate
        // native animation and is not a whole-combined-scene lifetime signal.
        if (
            keyguardEnabled &&
            !aodEnabled &&
            steadySourceScene == SourceScene.KEYGUARD
        ) {
            val alpha = keyguardStatusIconsAlpha
            if (alpha != null) {
                when (lastStableFamilyScene) {
                    StableKeyguardAodScene.KEYGUARD ->
                        return if (alpha > 0f) {
                            KeyguardAodProjection.KEYGUARD
                        } else {
                            KeyguardAodProjection.NATIVE
                        }

                    StableKeyguardAodScene.AOD ->
                        return if (alpha > 0f) {
                            KeyguardAodProjection.KEYGUARD
                        } else {
                            KeyguardAodProjection.NATIVE
                        }

                    StableKeyguardAodScene.UNKNOWN -> Unit
                }
            }
        }
        if (
            !keyguardEnabled &&
            aodEnabled &&
            steadySourceScene == SourceScene.KEYGUARD
        ) {
            val alpha = keyguardStatusIconsAlpha
            if (alpha != null) {
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
        }

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
