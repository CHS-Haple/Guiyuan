package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneControlCenterTest {
    @Test
    fun controlCenterProjectionInheritsVerifiedSourceSceneCapability() {
        assertTrue(
            ScenePolicy.ccProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.HOME,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            ScenePolicy.ccProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            ScenePolicy.ccProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            ScenePolicy.ccProjectionEligible(
                featureEnabled = true,
                sourceScene = SourceScene.UNKNOWN,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            ScenePolicy.ccProjectionEligible(
                featureEnabled = false,
                sourceScene = SourceScene.HOME,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            ScenePolicy.ccProjectionEligible(
                featureEnabled = false,
                sourceScene = SourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseRejectsEveryIndependentInvalidBoundary() {
        val base =
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldReconcileKeyguardControlCenter(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            ScenePolicy.shouldReconcileKeyguardControlCenter(
                controlCenterVisible = true,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            ScenePolicy.shouldReconcileKeyguardControlCenter(
                controlCenterVisible = false,
                nativeFraction = 0.1f,
                leaseActive = false,
            ),
        )
        assertTrue(
            ScenePolicy.shouldReconcileKeyguardControlCenter(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = true,
            ),
        )
    }

    @Test
    fun incomingKeyguardBoundaryPresentationCanBridgeStableReadiness() {
        assertTrue(
            ScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = false,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = false,
                compactLayoutReady = true,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = false,
                visualBoundaryReached = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.incomingKeyguardPresentationReady(
                visualHandoffActive = true,
                layoutPrecommitActive = true,
                compactLayoutReady = true,
                visualBoundaryReached = false,
                hostAttached = true,
            ),
        )
        assertFalse(
            ScenePolicy.incomingKeyguardPresentationReady(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
    fun keyguardControlCenterLeaseExistsOnlyInsideVerifiedNativeTransitionLifetime() {
        assertTrue(
            ScenePolicy.shouldAcquireKeyguardCcLease(
                sourceScene = SourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            ScenePolicy.shouldAcquireKeyguardCcLease(
                sourceScene = SourceScene.KEYGUARD,
                keyguardPresentationReady = true,
                nativeFraction = 0f,
            ),
        )

        assertTrue(
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
            ScenePolicy.shouldKeepKeyguardCcLease(
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
