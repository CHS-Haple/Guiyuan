package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlotLayoutTest {
    @Test
    fun ltrSlotAnchorsToHostEnd() {
        val layout = requireNotNull(SteadyLayoutResolver.resolve(587, 108, 105, false))
        assertEquals(482, layout.left)
        assertEquals(587, layout.right)
        assertEquals(105, layout.right - layout.left)
        assertEquals(105, layout.visualWidth)
    }

    @Test
    fun rtlSlotAnchorsToHostStart() {
        val layout = requireNotNull(SteadyLayoutResolver.resolve(587, 108, 105, true))
        assertEquals(0, layout.left)
        assertEquals(105, layout.right)
    }

    @Test
    fun carrierWidthIsClampedByHost() {
        val layout = requireNotNull(SteadyLayoutResolver.resolve(80, 108, 105, false))
        assertEquals(0, layout.left)
        assertEquals(80, layout.right)
        assertEquals(80, layout.right - layout.left)
    }

    @Test
    fun shortHostLimitsVisualButNotCarrierWidth() {
        val layout = requireNotNull(SteadyLayoutResolver.resolve(587, 64, 105, false))
        assertEquals(64, layout.visualWidth)
        assertEquals(105, layout.right - layout.left)
        assertEquals(482, layout.left)
        assertEquals(587, layout.right)
    }

    @Test
    fun invalidHostOrCarrierCannotClaimLayout() {
        assertNull(SteadyLayoutResolver.resolve(0, 108, 105, false))
        assertNull(SteadyLayoutResolver.resolve(587, 0, 105, false))
        assertNull(SteadyLayoutResolver.resolve(587, 108, 0, false))
    }

    @Test
    fun nativePrivacyOccupancyDoesNotDependOnBatteryHide() {
        assertEquals(true, HomePrivacyLayoutPolicy.occupiesEnd("START_SHOW_PRIVACY"))
        assertEquals(true, HomePrivacyLayoutPolicy.occupiesEnd("COMPLETE_SHOW_PRIVACY"))
        for (state in listOf(
            "START_HIDE_PRIVACY", "COMPLETE_HIDE_PRIVACY",
            "START_HOME_TO_DOT", "COMPLETE_HOME_TO_DOT",
        )) {
            assertEquals(false, HomePrivacyLayoutPolicy.occupiesEnd(state))
        }
        assertEquals(false, HomePrivacyLayoutPolicy.occupiesEnd(null))
    }

    @Test
    fun privacyAnchorFollowsNativeStatusIconsBoundaryInBothDirections() {
        val normal = requireNotNull(SteadyLayoutResolver.resolve(587, 108, 105, false))
        assertEquals(587, normal.right)
        val privacy = requireNotNull(HomePrivacyAnchorPolicy.resolve(587, 108, 105, 524, false))
        assertEquals(419, privacy.left)
        assertEquals(524, privacy.right)
        assertEquals(105, privacy.visualWidth)
        val rtl = requireNotNull(HomePrivacyAnchorPolicy.resolve(587, 108, 105, 63, true))
        assertEquals(63, rtl.left)
        assertEquals(168, rtl.right)
        assertNull(HomePrivacyAnchorPolicy.resolve(587, 108, 105, 80, false))
        assertNull(HomePrivacyAnchorPolicy.resolve(587, 108, 105, 510, true))
    }

    @Test
    fun centeredShrinkReservationTracksVisibleLeadingEdge() {
        assertEquals(
            105,
            CompactReservationPolicy.resolveCenteredVisualWidth(105, 1f),
        )
        assertEquals(
            92,
            CompactReservationPolicy.resolveCenteredVisualWidth(105, 0.75f),
        )
    }
}
