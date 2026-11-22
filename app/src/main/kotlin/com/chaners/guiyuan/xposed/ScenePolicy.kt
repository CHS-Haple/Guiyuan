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

    internal data class AodFeatureFlags(
        val featureEnabled: Boolean,
        val keyguardEnabled: Boolean,
        val aodEnabled: Boolean,
        val toAod: Boolean,
        val isAodAnimate: Boolean,
    )

    internal data class AodSceneEvidence(
        val steadySourceScene: SourceScene = SourceScene.UNKNOWN,
        val lastStableFamilyScene: StableKeyguardAodScene = StableKeyguardAodScene.UNKNOWN,
        val homePresentationOwned: Boolean = false,
    )

    internal data class AodNativeSignalEvidence(
        val keyguardStatusIconsAlpha: Float? = null,
        val nativeToLockScreenTarget: Boolean? = null,
    )

    internal data class AodNativeBoundaryEvidence(
        val fullAodTargetSourceReady: Boolean = false,
        val fullAodTargetPending: Boolean = false,
        val fullAodVisualBoundary: Boolean = false,
        val outgoingAodOwned: Boolean = false,
    )

    internal data class AodHomeEvidence(
        val homeAodTransitionOrigin: Boolean = false,
        val homeAodTargetPrearm: Boolean = false,
        val homeNativeAodFallbackActive: Boolean = false,
    )

    internal data class KeyguardAodProjectionInputs(
        val feature: AodFeatureFlags,
        val scene: AodSceneEvidence = AodSceneEvidence(),
        val signal: AodNativeSignalEvidence = AodNativeSignalEvidence(),
        val boundary: AodNativeBoundaryEvidence = AodNativeBoundaryEvidence(),
        val home: AodHomeEvidence = AodHomeEvidence(),
    )

    fun resolveKeyguardAodProjection(input: KeyguardAodProjectionInputs): KeyguardAodProjection {
        if (!input.feature.featureEnabled) return KeyguardAodProjection.NATIVE
        SceneAodOverridePolicy.homeAodOverride(input)?.let { return it }
        SceneAodOverridePolicy.nativeAodBoundaryOverride(input)?.let { return it }
        return ongoingKeyguardAodProjection(input)
    }

    private fun ongoingKeyguardAodProjection(input: KeyguardAodProjectionInputs): KeyguardAodProjection {
        val feature = input.feature
        val scene = input.scene
        if (feature.isAodAnimate) {
            return resolveAnimatingKeyguardAodProjection(input)
        }
        if (SysUiKeyguardAodSource.isStableAod(feature.toAod, feature.isAodAnimate)) {
            return if (feature.aodEnabled) KeyguardAodProjection.AOD else KeyguardAodProjection.NATIVE
        }
        if (SysUiKeyguardAodSource.blocksKeyguardProjection(feature.toAod, feature.isAodAnimate)) {
            return KeyguardAodProjection.NATIVE
        }
        if (scene.steadySourceScene == SourceScene.HOME) return KeyguardAodProjection.NATIVE
        return if (scene.steadySourceScene == SourceScene.KEYGUARD && feature.keyguardEnabled) {
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
        input: KeyguardAodProjectionInputs,
    ): KeyguardAodProjection {
        val keyguardEnabled = input.feature.keyguardEnabled
        val aodEnabled = input.feature.aodEnabled
        val steadySourceScene = input.scene.steadySourceScene
        val lastStableFamilyScene = input.scene.lastStableFamilyScene
        val homePresentationOwned = input.scene.homePresentationOwned
        val keyguardStatusIconsAlpha = input.signal.keyguardStatusIconsAlpha
        val nativeToLockScreenTarget = input.signal.nativeToLockScreenTarget
        val fullAodTargetSourceReady = input.boundary.fullAodTargetSourceReady
        val fullAodTargetPending = input.boundary.fullAodTargetPending
        val fullAodVisualBoundary = input.boundary.fullAodVisualBoundary
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
