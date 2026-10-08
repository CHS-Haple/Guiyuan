package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutPolicyTest {
    @Test
    fun ltrSlotAnchorsToHostEnd() {
        val layout = requireNotNull(SteadyLayoutResolver.resolve(587, 108, 105, false))
        assertEquals(482, layout.left)
        assertEquals(587, layout.right)
        assertEquals(105, layout.carrierWidth)
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
        assertEquals(80, layout.carrierWidth)
    }

    @Test
    fun shortHostLimitsVisualButNotCarrierWidth() {
        val layout = requireNotNull(SteadyLayoutResolver.resolve(587, 64, 105, false))
        assertEquals(64, layout.visualWidth)
        assertEquals(105, layout.carrierWidth)
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
