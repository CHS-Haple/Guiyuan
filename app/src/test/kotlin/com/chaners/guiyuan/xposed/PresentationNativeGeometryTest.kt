package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationNativeGeometryTest {
    private fun settled(
        nativeWidth: Int,
        laidOutWidth: Int = nativeWidth,
        attached: Boolean = true,
        layoutRequested: Boolean = false,
        exclusiveParent: Boolean = true,
        endAnchored: Boolean = true,
    ) = FakeCarrierNativeGeometryPolicy.isSettled(
        nativeWidthPx = nativeWidth,
        laidOutWidthPx = laidOutWidth,
        attached = attached,
        layoutRequested = layoutRequested,
        exclusiveParent = exclusiveParent,
        endAnchored = endAnchored,
    )

    @Test
    fun portraitNativeNarrowCarrierRemainsSystemUiOwned() {
        // B1899: QS_FAKE 587 within an 818-wide parent. No width mutation required.
        assertTrue(settled(nativeWidth = 587))
    }

    @Test
    fun landscapeNativeOverhangRemainsSystemUiOwned() {
        // B1899: QS_FAKE 1361 end-anchored within a 1309-wide parent.
        assertTrue(settled(nativeWidth = 1361))
    }

    @Test
    fun nativeRotationReflowDefersCutoverWithoutClaimingForeignWidth() {
        // SystemUI rewrites LayoutParams before the corresponding View layout.
        assertFalse(settled(nativeWidth = 1361, laidOutWidth = 818, layoutRequested = true))
        assertFalse(settled(nativeWidth = 1361, laidOutWidth = 818))
        assertTrue(settled(nativeWidth = 1361))
        assertFalse(settled(nativeWidth = 587, laidOutWidth = 1361, layoutRequested = true))
        assertTrue(settled(nativeWidth = 587))
    }

    @Test
    fun unsupportedOrUnfinishedNativeHostIsNotAdopted() {
        assertFalse(settled(nativeWidth = 0))
        assertFalse(settled(nativeWidth = 587, attached = false))
        assertFalse(settled(nativeWidth = 587, exclusiveParent = false))
        assertFalse(settled(nativeWidth = 587, endAnchored = false))
    }
}
