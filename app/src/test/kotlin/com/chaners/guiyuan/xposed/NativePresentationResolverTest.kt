package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativePresentationResolverTest {
    @Test
    fun presentationAndDataSubscriptionIdentitiesCanDiffer() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = 1,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 1,
                networkType = null,
            )

        assertEquals(1, snapshot.presentationRootSubscriptionId)
        assertEquals(4, snapshot.effectiveDataSubscriptionId)
        assertEquals(1, snapshot.networkTypeSubscriptionId)
    }

    @Test
    fun aggregatedNetworkTypeUsesVisibleNativeRoot() {
        assertEquals(
            1,
            NativePresentationResolver.selectNetworkTypeSubscriptionId(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                effectiveDataSubscriptionId = 4,
                presentationRootSubscriptionId = 1,
                boundSubscriptionIds = listOf(1, 4),
            ),
        )
    }

    @Test
    fun aggregatedNetworkTypeDoesNotFallBackToHiddenRoot() {
        assertEquals(
            null,
            NativePresentationResolver.selectNetworkTypeSubscriptionId(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                effectiveDataSubscriptionId = 4,
                presentationRootSubscriptionId = null,
                boundSubscriptionIds = listOf(1, 4),
            ),
        )
    }

    @Test
    fun separateDualModeStillPrefersDefaultDataSubscription() {
        assertEquals(
            4,
            NativePresentationResolver.selectNetworkTypeSubscriptionId(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                effectiveDataSubscriptionId = 4,
                presentationRootSubscriptionId = 1,
                boundSubscriptionIds = listOf(1, 4),
            ),
        )
    }

    @Test
    fun networkTypeFallsBackToPresentationRootWhenDefaultDataBindingIsMissing() {
        assertEquals(
            1,
            NativePresentationResolver.selectNetworkTypeSubscriptionId(
                mode = NativePresentationResolver.Mode.SINGLE,
                effectiveDataSubscriptionId = 4,
                presentationRootSubscriptionId = 1,
                boundSubscriptionIds = listOf(1),
            ),
        )
    }

    @Test
    fun singlePresentationIsDetected() {
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativePresentationResolver.classify(
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptions = 1,
            ),
        )
    }

    @Test
    fun nativeDualPresentationIsDetected() {
        assertEquals(
            NativePresentationResolver.Mode.DUAL_SEPARATE,
            NativePresentationResolver.classify(
                boundRoots = 2,
                visibleRoots = 2,
                activeSubscriptions = 2,
            ),
        )
    }

    @Test
    fun aggregatedDualPresentationRequiresTwoActiveSubscriptions() {
        assertEquals(
            NativePresentationResolver.Mode.DUAL_AGGREGATED,
            NativePresentationResolver.classify(
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptions = 2,
            ),
        )
    }

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
    fun aggregatedDualPresentationCanOwnSingleVisibleMobileRootReplacement() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = 1,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 1,
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

    @Test
    fun preMeasureDoublePlusMatchesNativeNormalization() {
        val networkType =
            NativePresentationResolver.normalizeDrawableNetworkType(
                rawLabel = "5G++",
                enhanced = false,
                beforeMeasure = true,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }

    @Test
    fun preMeasureRegularTypeDoesNotReuseStaleDoublePlusFlag() {
        val networkType =
            NativePresentationResolver.normalizeDrawableNetworkType(
                rawLabel = "4G",
                enhanced = true,
                beforeMeasure = true,
            )

        assertEquals("4G", networkType?.label)
        assertEquals(false, networkType?.enhanced)
    }

    @Test
    fun commonLegacyAndLteLabelsPassThroughNativeNormalization() {
        listOf("2G", "E", "3G", "H+", "4G", "LTE").forEach { label ->
            val networkType =
                NativePresentationResolver.normalizeDrawableNetworkType(
                    rawLabel = label,
                    enhanced = false,
                    beforeMeasure = false,
                )

            assertEquals(label, networkType?.label)
            assertEquals(false, networkType?.enhanced)
        }
    }

    @Test
    fun postMeasureKeepsNativeDoublePlusFlag() {
        val networkType =
            NativePresentationResolver.normalizeDrawableNetworkType(
                rawLabel = "5G",
                enhanced = true,
                beforeMeasure = false,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }

    @Test
    fun oneActiveSubscriptionDoesNotPretendToBeAggregatedDual() {
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativePresentationResolver.classify(
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptions = 1,
            ),
        )
    }
    @Test
    fun authoritativeActiveSubscriptionDropsDisabledStaleRoot() {
        val resolved =
            NativePresentationResolver.resolveActiveSubIds(
                boundSubscriptionIds = listOf(1, 4),
                semanticActiveSubscriptionIds = setOf(1, 4),
                authoritativeActiveSubscriptionIds = setOf(4),
            )

        assertEquals(setOf(4), resolved.subscriptionIds)
        assertEquals(true, resolved.authoritative)
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativePresentationResolver.classify(
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptions = resolved.subscriptionIds.size,
            ),
        )
    }

    @Test
    fun semanticActiveSubscriptionsRemainFallbackWhenPlatformAuthorityUnavailable() {
        val resolved =
            NativePresentationResolver.resolveActiveSubIds(
                boundSubscriptionIds = listOf(1, 4),
                semanticActiveSubscriptionIds = setOf(4),
                authoritativeActiveSubscriptionIds = null,
            )

        assertEquals(setOf(4), resolved.subscriptionIds)
        assertEquals(false, resolved.authoritative)
    }

}
