package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeNetworkSuppressionTest {
    @Test
    fun activeMobileVisualMaskMakesNativeSignalContainerTransparent() {
        assertEquals(
            0f,
            NativeNetworkSuppression.resolveMobileVisualMaskAlpha(
                nativeAlpha = 1f,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveMobileVisualMaskPreservesNativeAlpha() {
        assertEquals(
            0.65f,
            NativeNetworkSuppression.resolveMobileVisualMaskAlpha(
                nativeAlpha = 0.65f,
                suppressionActive = false,
            ),
        )
    }
    @Test
    fun airplaneModeKeepsNativeMobileSuppressedWhileRootsDisappear() {
        assertEquals(
            true,
            NativeNetworkSuppressionPolicy.suppressMobile(
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
            NativeNetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = unknown,
                wasSuppressed = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionPolicy.suppressMobile(
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
            NativeNetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = dualSeparate,
                wasSuppressed = true,
            ),
        )
    }


    @Test
    fun observedNoSimCanBecomeSuppressedInTheSameVisibilityEvent() {
        assertEquals(
            true,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
    }

    @Test
    fun staticSystemSlotsAreSuppressedOnlyWhenTheirReplacementIsReady() {
        assertEquals(
            true,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            true,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "alarm_clock",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
    }
    @Test
    fun locationAwareTintWinsOverPeerManagerAndCachedFallback() {
        assertEquals(
            0xe6ffffff.toInt(),
            NativeNetworkSuppression.selectStatusIconTint(
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
            NativeNetworkSuppression.selectStatusIconTint(
                locationAwareTint = null,
                peerAppliedTint = 0xfff2f2f2.toInt(),
                managerFallbackTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
        assertEquals(
            0xdee5e5e5.toInt(),
            NativeNetworkSuppression.selectStatusIconTint(
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
            NativeNetworkSuppression.shouldPreMaskMobileSignal(
                suppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldPreMaskMobileSignal(
                suppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.shouldPreMaskMobileSignal(
                suppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
    }

    @Test
    fun representedSlotsAreNotEligibleVisibleTintAuthorities() {
        listOf("combined_status", "wifi", "mobile", "stacked_mobile", "airplane", "no_sim").forEach { slot ->
            assertEquals(
                false,
                NativeNetworkSuppression.isTintAuthorityCandidate(
                    slot = slot,
                    visible = true,
                    width = 75,
                    height = 75,
                ),
            )
        }
    }

    @Test
    fun visibleNonRepresentedPeerCanAnchorHomeTint() {
        assertEquals(
            true,
            NativeNetworkSuppression.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.isTintAuthorityCandidate(
                slot = "vpn",
                visible = false,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppression.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 0,
                height = 75,
            ),
        )
    }

}
