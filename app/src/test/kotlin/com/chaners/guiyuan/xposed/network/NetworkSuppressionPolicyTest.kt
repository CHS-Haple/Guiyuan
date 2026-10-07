package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.NativePresentationResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSuppressionPolicyTest {
    @Test
    fun duplicateRootDualSeparateWindowRetainsExistingSuppression() {
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
    fun duplicateRootWindowDoesNotCreateSuppressionFromNativeState() {
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

    @Test
    fun activeMobileVisualMaskMakesNativeSignalContainerTransparent() {
        assertEquals(
            0f,
            NetworkSuppressionPolicy.mobileVisualMaskAlpha(
                nativeAlpha = 1f,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveMobileVisualMaskPreservesNativeAlpha() {
        assertEquals(
            0.65f,
            NetworkSuppressionPolicy.mobileVisualMaskAlpha(
                nativeAlpha = 0.65f,
                suppressionActive = false,
            ),
        )
    }

    @Test
    fun airplaneModeKeepsNativeMobileSuppressedWhileRootsDisappear() {
        assertEquals(
            true,
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = true,
                presentation = null,
                wasSuppressed = false,
            ),
        )
    }

    @Test
    fun airplaneExitKeepsPreviousSuppressionThroughUnknownPresentationGap() {
        val unknown =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.UNKNOWN,
                boundRoots = 2,
                visibleRoots = 0,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = null,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(
            true,
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = unknown,
                wasSuppressed = true,
            ),
        )
        assertEquals(
            false,
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = unknown,
                wasSuppressed = false,
            ),
        )
    }

    @Test
    fun knownNonReplaceableMobilePresentationReleasesStickySuppression() {
        val dualSeparate =
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

        assertEquals(
            false,
            NetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = dualSeparate,
                wasSuppressed = true,
            ),
        )
    }

    @Test
    fun locationAwareTintWinsOverPeerManagerAndCachedFallback() {
        assertEquals(
            0xe6ffffff.toInt(),
            NetworkSuppressionPolicy.statusIconTint(
                locationAwareTint = 0xe6ffffff.toInt(),
                peerAppliedTint = 0xbf000000.toInt(),
                managerFallbackTint = 0xbf000000.toInt(),
                fallbackTint = 0xbf000000.toInt(),
            ),
        )
    }

    @Test
    fun peerTintWinsWhenLocationAwareTintIsUnavailable() {
        assertEquals(
            0xfff2f2f2.toInt(),
            NetworkSuppressionPolicy.statusIconTint(
                locationAwareTint = null,
                peerAppliedTint = 0xfff2f2f2.toInt(),
                managerFallbackTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
        assertEquals(
            0xdee5e5e5.toInt(),
            NetworkSuppressionPolicy.statusIconTint(
                locationAwareTint = null,
                peerAppliedTint = 0x00ffffff,
                managerFallbackTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
    }

    @Test
    fun mobilePreMaskRequiresActiveHomeOwnership() {
        assertEquals(
            true,
            NetworkSuppressionPolicy.shouldPreMaskMobileSignal(
                suppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NetworkSuppressionPolicy.shouldPreMaskMobileSignal(
                suppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            NetworkSuppressionPolicy.shouldPreMaskMobileSignal(
                suppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
    }

}
