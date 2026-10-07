package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeNetworkSuppressorTest {





    @Test
    fun observedNoSimCanBecomeSuppressedInTheSameVisibilityEvent() {
        assertEquals(
            true,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
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
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            true,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
                slot = "alarm_clock",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.shouldSuppressStaticSlot(
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
                NativeNetworkSuppressor.isTintAuthorityCandidate(
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
            NativeNetworkSuppressor.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.isTintAuthorityCandidate(
                slot = "vpn",
                visible = false,
                width = 75,
                height = 75,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressor.isTintAuthorityCandidate(
                slot = "vpn",
                visible = true,
                width = 0,
                height = 75,
            ),
        )
    }

}
