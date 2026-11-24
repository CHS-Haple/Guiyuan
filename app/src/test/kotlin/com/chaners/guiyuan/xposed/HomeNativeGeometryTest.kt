package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNativeGeometryTest {
    @Test
    fun topOverflowExpandsOnlyPhysicalSurfaceWithoutMovingLogicalViewport() {
        val resolved =
            VerticalOverflowPolicy.resolve(
                logicalTopPx = 0,
                logicalHeightPx = 108,
                requestedTopOverflowPx = 18,
            )

        assertEquals(-18, resolved.physicalTopPx)
        assertEquals(126, resolved.physicalHeightPx)
        assertEquals(18, resolved.logicalTopInsetPx)
        assertEquals(0, resolved.physicalTopPx + resolved.logicalTopInsetPx)
    }

    @Test
    fun bottomOverflowExtendsPhysicalSurfaceWithoutMovingLogicalOrigin() {
        val resolved = VerticalOverflowPolicy.resolve(
            logicalTopPx = 0,
            logicalHeightPx = 108,
            requestedTopOverflowPx = 18,
            requestedBottomOverflowPx = 7,
        )
        assertEquals(-18, resolved.physicalTopPx)
        assertEquals(133, resolved.physicalHeightPx)
        assertEquals(18, resolved.logicalTopInsetPx)
        assertEquals(115, resolved.physicalTopPx + resolved.physicalHeightPx)
    }

    @Test
    fun homeParentAnchorDoesNotFollowIndependentChargingBatteryMotion() {
        val parent = requireNotNull(SteadyLayoutResolver.resolve(587, 108, 105, false))
        assertEquals(482, parent.left)
        assertEquals(587, parent.right)
        assertTrue(610 > parent.right) // B1908 transient native Battery endpoint
        assertEquals(parent, SteadyLayoutResolver.resolve(587, 108, 105, false))
        val wide = requireNotNull(SteadyLayoutResolver.resolve(1361, 108, 105, false))
        assertEquals(1256, wide.left)
        assertEquals(1361, wide.right)
    }

    @Test
    fun chargeIslandCanRetainHomeWhileChildLayoutWidthIsTransientlyZero() {
        val nativeWidth = NativeSlotGeometry.resolveStableChildWidth(
            layoutWidth = 0,
            measuredWidth = 105,
        )
        assertEquals(105, nativeWidth)
        val stable = requireNotNull(
            SteadyLayoutResolver.resolve(587, 108, requireNotNull(nativeWidth), false),
        )
        assertEquals(482, stable.left)
        assertEquals(587, stable.right)
    }

    @Test
    fun invalidHomeHostDoesNotClaimTheOverlay() {
        assertEquals(null, SteadyLayoutResolver.resolve(0, 108, 105, false))
        assertEquals(null, SteadyLayoutResolver.resolve(587, 0, 105, false))
        assertEquals(null, SteadyLayoutResolver.resolve(587, 108, 0, false))
    }

    @Test
    fun noOverflowPreservesOriginalPhysicalBounds() {
        val resolved =
            VerticalOverflowPolicy.resolve(
                logicalTopPx = 0,
                logicalHeightPx = 108,
                requestedTopOverflowPx = 0,
            )

        assertEquals(0, resolved.physicalTopPx)
        assertEquals(108, resolved.physicalHeightPx)
        assertEquals(0, resolved.logicalTopInsetPx)
    }
}
