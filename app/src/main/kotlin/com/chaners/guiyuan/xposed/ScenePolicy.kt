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

    enum class AodPeerAction {
        NONE,
        RELEASE,
        RECLAIM,
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
        val homeOrigin =
            SceneAodBoundaryPolicy.resolveAnimatingHomeOrigin(
                steadySourceScene = steadySourceScene,
                lastStableFamilyScene = lastStableFamilyScene,
                homePresentationOwned = homePresentationOwned,
                aodEnabled = aodEnabled,
            )
        if (homeOrigin != null) return homeOrigin

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
                return SceneAodBoundaryPolicy.resolveFullAodVisualBoundary(
                    nativeToLockScreenTarget = nativeToLockScreenTarget,
                    keyguardEnabled = keyguardEnabled,
                    aodEnabled = aodEnabled,
                    lastStableFamilyScene = lastStableFamilyScene,
                    keyguardStatusIconsAlpha = keyguardStatusIconsAlpha,
                )
            }
            if (fullAodTargetPending) {
                return SceneAodBoundaryPolicy.resolvePendingFullAodTarget(
                    lastStableFamilyScene = lastStableFamilyScene,
                    keyguardEnabled = keyguardEnabled,
                    aodEnabled = aodEnabled,
                )
            }
        }

        val alphaDecision =
            SceneAodBoundaryPolicy.resolveAnimatingNativeAlpha(
                steadySourceScene = steadySourceScene,
                lastStableFamilyScene = lastStableFamilyScene,
                keyguardEnabled = keyguardEnabled,
                aodEnabled = aodEnabled,
                keyguardStatusIconsAlpha = keyguardStatusIconsAlpha,
            )
        if (alphaDecision != null) return alphaDecision

        return SceneAodBoundaryPolicy.resolveAnimatingStableFamily(
            steadySourceScene = steadySourceScene,
            lastStableFamilyScene = lastStableFamilyScene,
            keyguardEnabled = keyguardEnabled,
            aodEnabled = aodEnabled,
        )
    }

}
