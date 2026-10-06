package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeNetworkSuppressionOwnerTest {





    @Test
    fun observedNoSimCanBecomeSuppressedInTheSameVisibilityEvent() {
        assertEquals(
            true,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
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
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            true,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "alarm_clock",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
    }



    @Test
    fun representedSlotsAreNotEligibleVisibleTintAuthorities() {
        listOf("combined_status", "wifi", "mobile", "stacked_mobile", "airplane", "no_sim").forEach { slot ->
            assertEquals(
                false,
                NativeNetworkSuppressionOwner.isTintAuthorityCandidate(
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
            NativeNetworkSuppressionOwner.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.isTintAuthorityCandidate(
                slot = "vpn",
                visible = false,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionOwner.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 0,
                height = 75,
            ),
        )
    }

}
