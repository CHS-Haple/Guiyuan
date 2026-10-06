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

internal enum class StatusScene {
    HOME_STABLE,
    NOTIFICATION_SHADE_TRANSITION,
    CONTROL_CENTER,
    KEYGUARD,
    AOD,
}

internal enum class SceneEvidence {
    RUNTIME_VERIFIED,
    STATIC_VERIFIED,
}

internal enum class SourceScene {
    HOME,
    KEYGUARD,
    UNKNOWN,
}

internal data class SceneCapability(
    val scene: StatusScene,
    val renderMode: RenderMode,
    val motionOwnership: MotionOwnership,
    val evidence: SceneEvidence,
)

internal object ScenePolicy {
    private val capabilities =
        mapOf(
            StatusScene.HOME_STABLE to
                SceneCapability(
                    scene = StatusScene.HOME_STABLE,
                    renderMode = RenderMode.PROJECTED,
                    motionOwnership = MotionOwnership.NONE,
                    evidence = SceneEvidence.RUNTIME_VERIFIED,
                ),
            StatusScene.NOTIFICATION_SHADE_TRANSITION to
                SceneCapability(
                    scene = StatusScene.NOTIFICATION_SHADE_TRANSITION,
                    renderMode = RenderMode.NATIVE_ONLY,
                    motionOwnership = MotionOwnership.SYSTEM_UI,
                    evidence = SceneEvidence.STATIC_VERIFIED,
                ),
            StatusScene.CONTROL_CENTER to
                SceneCapability(
                    scene = StatusScene.CONTROL_CENTER,
                    renderMode = RenderMode.NATIVE_ONLY,
                    motionOwnership = MotionOwnership.SYSTEM_UI,
                    evidence = SceneEvidence.STATIC_VERIFIED,
                ),
            StatusScene.KEYGUARD to
                SceneCapability(
                    scene = StatusScene.KEYGUARD,
                    renderMode = RenderMode.PROJECTED,
                    motionOwnership = MotionOwnership.SYSTEM_UI,
                    evidence = SceneEvidence.STATIC_VERIFIED,
                ),
            StatusScene.AOD to
                SceneCapability(
                    scene = StatusScene.AOD,
                    renderMode = RenderMode.PROJECTED,
                    motionOwnership = MotionOwnership.SYSTEM_UI,
                    evidence = SceneEvidence.STATIC_VERIFIED,
                ),
        )

    fun capability(scene: StatusScene): SceneCapability =
        requireNotNull(capabilities[scene]) {
            "Missing CombinedStatus scene capability: $scene"
        }

    fun all(): List<SceneCapability> =
        StatusScene.entries.map(::capability)

    fun shouldAcquireKeyguardCcLease(
        sourceScene: SourceScene,
        keyguardPresentationReady: Boolean,
        nativeFraction: Float,
    ): Boolean =
        sourceScene == SourceScene.KEYGUARD &&
            keyguardPresentationReady &&
            nativeFraction > 0f

    fun shouldReconcileKeyguardControlCenter(
        controlCenterVisible: Boolean,
        nativeFraction: Float,
        leaseActive: Boolean,
    ): Boolean =
        controlCenterVisible ||
            nativeFraction > 0f ||
            leaseActive

    fun shouldKeepKeyguardCcLease(
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

    fun hasRetainedSourceWitness(
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
            (stableAod || homeTransitionPrearm) &&
            capability(StatusScene.AOD).renderMode ==
                RenderMode.PROJECTED

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
        if (isAodAnimate) {
            return resolveAnimatingAodProjection(
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
            KeyguardAodSource.isStableAod(
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
            KeyguardAodSource.blocksKeyguardProjection(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
                animToAod = null,
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

    fun shouldUseKeyguardHandoff(
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

    fun shouldPrecommitKeyguardLayout(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        lastStableFamilyScene: StableKeyguardAodScene,
        nativeToLockScreenTarget: Boolean?,
        statusIconsPresentationAlpha: Float?,
        homeNativeAodFallbackActive: Boolean = false,
    ): Boolean =
        shouldUseKeyguardHandoff(
            featureEnabled = featureEnabled,
            keyguardEnabled = keyguardEnabled,
            aodEnabled = aodEnabled,
            lastStableFamilyScene = lastStableFamilyScene,
            nativeToLockScreenTarget = nativeToLockScreenTarget,
            homeNativeAodFallbackActive = homeNativeAodFallbackActive,
        ) &&
            statusIconsPresentationAlpha != null &&
            statusIconsPresentationAlpha == 0f

    fun shouldArmHomeAodFallback(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        homePresentationOwned: Boolean,
        homeCarrierPresentationVisible: Boolean,
    ): Boolean =
        featureEnabled &&
            keyguardEnabled &&
            !aodEnabled &&
            homePresentationOwned &&
            homeCarrierPresentationVisible

    fun shouldConsumeHomeAodFallback(
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

    fun shouldReleaseHomeKeyguardForAodOff(
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

    fun aodTargetReachedStableState(
        pendingTargetToLockScreen: Boolean?,
        toAod: Boolean,
        isAodAnimate: Boolean,
    ): Boolean {
        if (pendingTargetToLockScreen == null || isAodAnimate) return false
        return if (pendingTargetToLockScreen) {
            !toAod
        } else {
            KeyguardAodSource.isStableAod(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
            )
        }
    }

    internal fun resolveAnimatingAodProjection(
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

        // Build 655 proved that mToLockScreen is direction evidence but
        // its animateFullAod commit is earlier than the visible status-icon
        // handoff. While that target is pending, retain the enabled outgoing
        // child (or Native when the outgoing child is disabled). Only the
        // native animateIconContainer lifecycle event may consume the target.
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

        // Do not derive AOD animation direction from current presentation
        // ownership. Attach/cleanup mutates ownership itself and Build 645
        // proved that doing so creates KEYGUARD -> NATIVE -> KEYGUARD
        // oscillation on repeated callbacks. Direction is instead derived from
        // the last non-animating, runtime-observed family scene and remains
        // frozen for the whole animation.
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
        panelSourceScene: SourceScene,
        steadySourceScene: SourceScene,
        lastStableFamilyScene: StableKeyguardAodScene = StableKeyguardAodScene.UNKNOWN,
        incomingKeyguardPresentationReady: Boolean = false,
    ): SourceScene {
        if (
            incomingKeyguardPresentationReady &&
            (
                panelSourceScene == SourceScene.KEYGUARD ||
                    steadySourceScene == SourceScene.KEYGUARD
            )
        ) {
            return SourceScene.KEYGUARD
        }
        if (panelSourceScene == steadySourceScene) return panelSourceScene
        if (panelSourceScene == SourceScene.UNKNOWN) return steadySourceScene
        if (steadySourceScene == SourceScene.UNKNOWN) return panelSourceScene

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
            SourceScene.HOME ->
                capability(StatusScene.HOME_STABLE).renderMode ==
                    RenderMode.PROJECTED

            SourceScene.KEYGUARD ->
                keyguardEnabled &&
                    capability(StatusScene.KEYGUARD).renderMode ==
                    RenderMode.PROJECTED

            SourceScene.UNKNOWN -> false
        }
}
