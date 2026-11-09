package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeMobileReplacementTest {
    @Test
    fun singleActiveSubscriptionCanOwnNativeMobileReplacement() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.SINGLE,
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptionIds = listOf(4),
                presentationRootSubscriptionId = 4,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(true, snapshot.nativeMobileReplacementReady)
    }

    @Test
    fun incompletePlatformBindingCannotAuthorizeMobileReplacement() {
        val partial =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.SINGLE,
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptionIds = listOf(1),
                activeSubsAuthoritative = true,
                platformActiveSubscriptionIds = setOf(1, 4),
                presentationRootSubscriptionId = 1,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 1,
                networkType = null,
            )

        // Both SIMs are active, but only one native View is bound at this moment.
        assertFalse(partial.nativeMobileReplacementReady)
        assertTrue(
            partial.copy(platformActiveSubscriptionIds = setOf(1))
                .nativeMobileReplacementReady,
        )
        // Do not change the established fallback when the platform list is unavailable.
        assertTrue(
            partial.copy(platformActiveSubscriptionIds = null, activeSubsAuthoritative = false)
                .nativeMobileReplacementReady,
        )

        val aggregated =
            partial.copy(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                boundRoots = 2,
                activeSubscriptionIds = listOf(1, 4),
            )
        assertTrue(aggregated.nativeMobileReplacementReady)
        assertFalse(
            aggregated.copy(platformActiveSubscriptionIds = setOf(1, 4, 5))
                .nativeMobileReplacementReady,
        )
    }

    @Test
    fun aggregatedDualPresentationCanOwnSingleVisibleMobileRootReplacement() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = 1,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(true, snapshot.nativeMobileReplacementReady)
    }

    @Test
    fun separateDualPresentationMustPreserveNativeMobileParticipant() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                boundRoots = 2,
                visibleRoots = 2,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = 4,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(false, snapshot.nativeMobileReplacementReady)
    }

    @Test
    fun unknownMobilePresentationFailsNative() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.UNKNOWN,
                boundRoots = 0,
                visibleRoots = 0,
                activeSubscriptionIds = emptyList(),
                presentationRootSubscriptionId = null,
                effectiveDataSubscriptionId = null,
                networkTypeSubscriptionId = null,
                networkType = null,
            )

        assertEquals(false, snapshot.nativeMobileReplacementReady)
    }

    @Test
    fun singleModeWithoutActiveSubscriptionFailsNativeReplacementReadiness() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.SINGLE,
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptionIds = emptyList(),
                presentationRootSubscriptionId = 4,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(false, snapshot.nativeMobileReplacementReady)
    }
}
