package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativePresentationResolverTest {
    @Test
    fun activeBindingsDeduplicateAndRejectNegativeSubscriptionIds() {
        val resolved =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = listOf(4, 4, -1, 9, 7, 4),
                semanticActiveSubscriptionIds = setOf(-1, 4, 7),
                authoritativeActiveSubscriptionIds = null,
            )
        assertEquals(listOf(4, 7), resolved.subscriptionIds.toList())
        assertEquals(false, resolved.authoritative)

        val ordered =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = listOf(9, 7, 9, 4),
                semanticActiveSubscriptionIds = setOf(7, 4),
                authoritativeActiveSubscriptionIds = null,
            ).subscriptionIds
        assertEquals(listOf(7, 4), ordered.toList())
        assertEquals(
            7,
            NativeSubscriptionPolicy.selectNetworkTypeSubscriptionId(
                effectiveDataSubscriptionId = null,
                presentationRootSubscriptionId = null,
                boundSubscriptionIds = ordered,
            ),
        )
    }

    @Test
    fun availablePlatformSourceOverridesSemanticFallbackEvenWhenEmpty() {
        val authoritative =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = listOf(4, 9, 7),
                semanticActiveSubscriptionIds = setOf(4, 7),
                authoritativeActiveSubscriptionIds = setOf(9, 7),
            )
        assertEquals(listOf(9, 7), authoritative.subscriptionIds.toList())
        assertEquals(true, authoritative.authoritative)

        val empty =
            NativeSubscriptionPolicy.resolveActiveSubIds(
                boundSubscriptionIds = listOf(4),
                semanticActiveSubscriptionIds = setOf(4),
                authoritativeActiveSubscriptionIds = emptySet(),
            )
        assertEquals(emptySet<Int>(), empty.subscriptionIds)
        assertEquals(true, empty.authoritative)
    }

    @Test
    fun dualSimSlotMappingUsesNativePhysicalOrder() {
        assertEquals(
            mapOf(1 to 4, 0 to 9),
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeSubscriptionIds = setOf(4, 9),
                platformSubscriptions = listOf(4 to 1, 9 to 0),
            ),
        )
        assertEquals(
            mapOf(0 to 4, 1 to 9),
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeSubscriptionIds = setOf(4, 9),
                platformSubscriptions = listOf(4 to 0, 9 to 1),
            ),
        )
    }

    @Test
    fun ambiguousOrIncompletePlatformSlotsNeverAuthorizeDualSim() {
        val active = setOf(4, 9)
        val invalid =
            listOf(
                null,
                emptyList(),
                listOf(4 to 0),
                listOf(4 to 0, 9 to 1, 11 to 2),
                listOf(4 to 0, 4 to 1),
                listOf(4 to 0, 9 to 0),
                listOf(4 to -1, 9 to 1),
                listOf(4 to 0, 9 to 2),
                listOf(4 to 0, 11 to 1),
            )
        invalid.forEach { platform ->
            assertEquals(
                "Ambiguous native slots must not enable a dual-card layout: $platform",
                emptyMap<Int, Int>(),
                NativeSubscriptionPolicy.resolveSlotSubscriptions(active, platform),
            )
        }
        assertEquals(
            emptyMap<Int, Int>(),
            NativeSubscriptionPolicy.resolveSlotSubscriptions(
                activeSubscriptionIds = setOf(4),
                platformSubscriptions = listOf(4 to 0, 9 to 1),
            ),
        )
    }

    @Test
    fun aggregatedNetworkTypeFollowsDefaultDataSubscription() {
        assertEquals(
            4,
            NativeSubscriptionPolicy.selectNetworkTypeSubscriptionId(
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
            NativeSubscriptionPolicy.selectNetworkTypeSubscriptionId(
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
            NativeSubscriptionPolicy.classify(
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
            NativeSubscriptionPolicy.classify(
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
            NativeSubscriptionPolicy.classify(
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptions = 2,
            ),
        )
    }

    @Test
    fun preMeasureDoublePlusMatchesNativeNormalization() {
        val networkType =
            NativeMobileTypePresentation.normalizeDrawableNetworkType(
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
            NativeMobileTypePresentation.normalizeDrawableNetworkType(
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
                NativeMobileTypePresentation.normalizeDrawableNetworkType(
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
            NativeMobileTypePresentation.normalizeDrawableNetworkType(
                rawLabel = "5G",
                enhanced = true,
                beforeMeasure = false,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }
}
