package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativePresentationResolverTest {
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
