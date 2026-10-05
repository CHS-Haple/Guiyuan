package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusSceneControlCenterTest {
    @Test
    fun controlCenterProjectionInheritsVerifiedSourceSceneCapability() {
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.UNKNOWN,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseRejectsEveryIndependentInvalidBoundary() {
        val base =
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            )
        assertTrue(base)

        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = false,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = false,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = false,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
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
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = true,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0.1f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = true,
            ),
        )
    }

    @Test
    fun incomingKeyguardBoundaryPresentationCanBridgeStableReadiness() {
        assertTrue(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = false,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = false,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = false,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = false,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.incomingKeyguardPresentationReady(
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
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                incomingBoundaryPresentationReady = true,
                nativeFraction = 0.2f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
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
    fun keyguardControlCenterLeaseExistsOnlyInsideVerifiedNativeTransitionLifetime() {
        assertTrue(
            CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0f,
            ),
        )

        assertTrue(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.HOME,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0f,
            ),
        )
    }
}
