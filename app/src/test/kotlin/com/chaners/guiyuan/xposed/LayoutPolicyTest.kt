package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutPolicyTest {
    @Test
    fun scalingKeepsTheEndAnchorFixed() {
        val small = resolve(scale = 1f)
        val large = resolve(scale = 1.2f)

        assertEquals(587f, small.visualRightPx, 0.001f)
        assertEquals(587f, large.visualRightPx, 0.001f)
        assertTrue(large.visualLeftPx < small.visualLeftPx)
        assertTrue(large.requestedSlotWidthPx > small.requestedSlotWidthPx)
        assertTrue(large.neighborGapPx > small.neighborGapPx)
    }

    @Test
    fun projectedSceneKeepsNativeSlotButReusesTheSameVisualRule() {
        val projected = resolve(scale = 1.2f)

        assertEquals(105f, projected.appliedSlotWidthPx, 0.001f)
        assertTrue(projected.requestedSlotWidthPx > projected.appliedSlotWidthPx)
        assertEquals(587f, projected.visualRightPx, 0.001f)
    }

    @Test
    fun nativeOnlySceneDoesNotRenderCombinedVisuals() {
        val layout = resolve(
            scale = 1f,
            renderMode = RenderMode.NATIVE_ONLY,
            motionOwnership = MotionOwnership.SYSTEM_UI,
        )

        assertFalse(layout.renderCombined)
        assertEquals(105f, layout.appliedSlotWidthPx, 0.001f)
        assertEquals(MotionOwnership.SYSTEM_UI, layout.motionOwnership)
    }

    @Test
    fun sharedPolicyDoesNotChangeIdealGeometryBySceneCapability() {
        val projected = resolve(scale = 0.9f)
        val nativeOnly = resolve(
            scale = 0.9f,
            renderMode = RenderMode.NATIVE_ONLY,
            motionOwnership = MotionOwnership.SYSTEM_UI,
        )

        assertEquals(projected.visualSidePx, nativeOnly.visualSidePx, 0.001f)
        assertEquals(projected.neighborGapPx, nativeOnly.neighborGapPx, 0.001f)
        assertEquals(projected.requestedSlotWidthPx, nativeOnly.requestedSlotWidthPx, 0.001f)
        assertEquals(projected.visualLeftPx, nativeOnly.visualLeftPx, 0.001f)
        assertEquals(projected.visualRightPx, nativeOnly.visualRightPx, 0.001f)
        assertTrue(projected.renderCombined)
    }

    @Test
    fun centeredShrinkReservationTracksTheVisibleLeadingEdge() {
        assertEquals(
            105,
            CompactReservationPolicy.resolveCenteredVisualWidth(
                baseSlotWidthPx = 105,
                userScale = 1f,
            ),
        )
        assertEquals(
            92,
            CompactReservationPolicy.resolveCenteredVisualWidth(
                baseSlotWidthPx = 105,
                userScale = 0.75f,
            ),
        )
    }

    @Test
    fun homeResolverKeepsCurrentCarrierWidthAndHostHeightSeparated() {
        val layout =
            requireNotNull(
                HomeLayoutResolver.resolve(
                    hostWidthPx = 587,
                    hostHeightPx = 108,
                    baseCarrierWidthPx = 105,
                    isRtl = false,
                ),
            )

        assertEquals(105f, layout.requestedSlotWidthPx, 0.001f)
        assertEquals(105f, layout.appliedSlotWidthPx, 0.001f)
        assertEquals(482f, layout.slotLeftPx, 0.001f)
        assertEquals(587f, layout.slotRightPx, 0.001f)
        assertEquals(MotionOwnership.SYSTEM_UI, layout.motionOwnership)
    }

    @Test
    fun homeResolverUsesStableBaseSlotInsteadOfChargingInflatedWidth() {
        val layout =
            requireNotNull(
                HomeLayoutResolver.resolve(
                    hostWidthPx = 587,
                    hostHeightPx = 108,
                    baseCarrierWidthPx = 105,
                    isRtl = false,
                ),
            )

        assertEquals(105f, layout.requestedSlotWidthPx, 0.001f)
        assertEquals(482f, layout.slotLeftPx, 0.001f)
        assertEquals(587f, layout.slotRightPx, 0.001f)
    }

    private fun resolve(
        scale: Float,
        renderMode: RenderMode = RenderMode.PROJECTED,
        motionOwnership: MotionOwnership =
            MotionOwnership.NONE,
    ): ResolvedLayout =
        LayoutPolicy.resolve(
            settings = LayoutConfig(
                baseVisualSidePx = 105f,
                baseNeighborGapPx = 6f,
                userScale = scale,
            ),
            host = HostLayout(
                hostHeightPx = 108f,
                endAnchorPx = 587f,
                nativeSlotWidthPx = 105f,
                renderMode = renderMode,
                motionOwnership = motionOwnership,
            ),
        )
}
