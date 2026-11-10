package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneControlCenterTest {
    @Test
    fun controlCenterRequiresEligibleSourceAndSettings() {
        assertTrue(
            SceneCcPolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.HOME,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            SceneCcPolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            SceneCcPolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.UNKNOWN,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = SourceScene.HOME,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = SourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseRejectsEveryIndependentInvalidBoundary() {
        val base =
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            )
        assertTrue(base)

        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = false,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = false,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = false,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = false,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
    }

    @Test
    fun hiddenControlCenterIgnoresKeyguardLifecycleChurnUntilItActuallyOpens() {
        assertFalse(
            SceneCcPolicy.shouldReconcileCcForKeyguard(
                ccVisible = false,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            SceneCcPolicy.shouldReconcileCcForKeyguard(
                ccVisible = true,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            SceneCcPolicy.shouldReconcileCcForKeyguard(
                ccVisible = false,
                nativeFraction = 0.1f,
                leaseActive = false,
            ),
        )
        assertTrue(
            SceneCcPolicy.shouldReconcileCcForKeyguard(
                ccVisible = false,
                nativeFraction = 0f,
                leaseActive = true,
            ),
        )
    }

    @Test
    fun incomingKeyguardBoundaryPresentationCanBridgeStableReadiness() {
        assertTrue(
            SceneCcPolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = false,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = false,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = false,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = false,
                hostAttached = true,
            ),
        )
        assertFalse(
            SceneCcPolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = false,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseMaySpanIncomingBoundaryBeforeStableFamily() {
        assertTrue(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                incomingBoundaryPresentationReady = true,
                nativeFraction = 0.2f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                incomingBoundaryPresentationReady = false,
                nativeFraction = 0.2f,
            ),
        )
    }

    @Test
    fun keyguardCcLeaseAcquiresOnlyWithNativeTransitionProgress() {
        assertTrue(
            SceneCcPolicy.shouldAcquireKeyguardCcLease(
                sourceScene = SourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            SceneCcPolicy.shouldAcquireKeyguardCcLease(
                sourceScene = SourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0f,
            ),
        )
    }

    @Test
    fun keyguardCcLeaseRetainsOnlyWhileNativeOwnerIsEligible() {
        assertTrue(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.HOME,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldRetainKeyguardCcLease(
                leaseActive = true,
                sourceScene = SourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0f,
            ),
        )
    }
}
