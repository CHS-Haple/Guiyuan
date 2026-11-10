package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativePresentationSimAuthorityTest {
    @Test
    fun platformAuthorityKeepsExistingDiagnosticLabels() {
        val snapshot = NativePresentationResolver.Snapshot(
            mode = NativePresentationResolver.Mode.UNKNOWN,
            boundRoots = 0,
            visibleRoots = 0,
            activeSubscriptionIds = emptyList(),
            presentationRootSubscriptionId = null,
            effectiveDataSubscriptionId = null,
            networkTypeSubscriptionId = null,
            networkType = null,
        )
        assertEquals("pipeline-semantic-fallback", snapshot.activeSubscriptionAuthority)
        assertEquals(
            "subscription-manager",
            snapshot.copy(activeSubsAuthoritative = true).activeSubscriptionAuthority,
        )
    }

    @Test
    fun slotMappingUsesPlatformSimSlotRatherThanSubscriptionIdOrder() {
        assertEquals(
            mapOf(0 to 14, 1 to 3),
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeSubscriptionIds = setOf(3, 14),
                platformSubscriptions = listOf(3 to 1, 14 to 0),
            ),
        )
    }

    @Test
    fun ambiguousOrUnavailableSimSlotsDoNotBecomeGuessedRows() {
        val active = setOf(3, 14)
        val cases = listOf(
            null,
            listOf(3 to 0, 14 to 0),
            listOf(3 to -1, 14 to 1),
            listOf(3 to 0),
            listOf(3 to 0, 14 to 1, 3 to 1),
            listOf(3 to 0, 3 to 1),
        )
        for (pairList in cases) {
            assertEquals(
                emptyMap<Int, Int>(),
                NativeSubscriptionPolicy.resolveSlotSubscriptions(active, pairList),
            )
        }
    }

    @Test
    fun extraPlatformSubscriptionCannotDisguiseIncompleteDualBinding() {
        assertEquals(
            emptyMap<Int, Int>(),
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeSubscriptionIds = setOf(1, 4),
                platformSubscriptions = listOf(1 to 0, 4 to 1, 7 to 2),
            ),
        )
    }

    @Test
    fun oneActiveSubscriptionDoesNotPretendToBeAggregatedDual() {
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativeSubscriptionPolicy.classify(
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptions = 1,
            ),
        )
    }

    @Test
    fun authoritativeActiveSubscriptionDropsDisabledStaleRoot() {
        val resolved =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = listOf(1, 4),
                semanticActiveSubscriptionIds = setOf(1, 4),
                authoritativeActiveSubscriptionIds = setOf(4),
            )

        assertEquals(setOf(4), resolved.subscriptionIds)
        assertEquals(true, resolved.authoritative)
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativeSubscriptionPolicy.classify(
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptions = resolved.subscriptionIds.size,
            ),
        )
    }

    @Test
    fun ambiguousMultiSubscriptionSetCannotMasqueradeAsTwoRows() {
        assertEquals(
            emptyMap<Int, Int>(),
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeSubscriptionIds = setOf(3, 14, 27),
                platformSubscriptions = listOf(3 to 0, 14 to 1, 27 to -1),
            ),
        )
    }

    @Test
    fun semanticActiveSubscriptionsRemainFallbackWhenPlatformAuthorityUnavailable() {
        val resolved =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = listOf(1, 4),
                semanticActiveSubscriptionIds = setOf(4),
                authoritativeActiveSubscriptionIds = null,
            )

        assertEquals(setOf(4), resolved.subscriptionIds)
        assertEquals(false, resolved.authoritative)
    }
}
