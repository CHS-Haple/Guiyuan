package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSuppressionPolicyTest {
    @Test
    fun duplicateRootKeepsSuppression() {
        val presentation =
            snapshot(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                boundRoots = 3,
                activeBoundRoots = 3,
                visibleRoots = 2,
                activeSubscriptionIds = listOf(1, 4),
            )

        assertTrue(
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = presentation,
                wasSuppressed = true,
            ),
        )
    }

    @Test
    fun settledDualSeparateStillFailsNative() {
        val presentation =
            snapshot(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                boundRoots = 2,
                activeBoundRoots = 2,
                visibleRoots = 2,
                activeSubscriptionIds = listOf(1, 4),
            )

        assertFalse(
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = presentation,
                wasSuppressed = true,
            ),
        )
    }

    @Test
    fun duplicateRootDoesNotCreateSuppression() {
        val presentation =
            snapshot(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                boundRoots = 3,
                activeBoundRoots = 3,
                visibleRoots = 2,
                activeSubscriptionIds = listOf(1, 4),
            )

        assertFalse(
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = presentation,
                wasSuppressed = false,
            ),
        )
    }

    private fun snapshot(
        mode: NativePresentationResolver.Mode,
        boundRoots: Int,
        activeBoundRoots: Int,
        visibleRoots: Int,
        activeSubscriptionIds: List<Int>,
    ) =
        NativePresentationResolver.Snapshot(
            mode = mode,
            boundRoots = boundRoots,
            activeBoundRoots = activeBoundRoots,
            visibleRoots = visibleRoots,
            activeSubscriptionIds = activeSubscriptionIds,
            activeSubscriptionAuthority = "subscription-manager",
            presentationRootSubscriptionId = 4,
            effectiveDataSubscriptionId = 4,
            networkTypeSubscriptionId = 4,
            networkType =
                NativePresentationResolver.NetworkType(
                    label = "5G",
                    enhanced = false,
                    source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                ),
        )
}
