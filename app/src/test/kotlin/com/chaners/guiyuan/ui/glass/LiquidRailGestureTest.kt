package com.chaners.guiyuan.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidRailGestureTest {
    @Test fun collapsedIsHorizontalAndExpandedIsVertical() {
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(16f, 2f, 8f, true))
        assertEquals(RailGestureDecision.Cancel, liquidRailGestureDecision(2f, 16f, 8f, true))
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(2f, 16f, 8f, false))
        assertEquals(RailGestureDecision.Cancel, liquidRailGestureDecision(16f, 2f, 8f, false))
        assertEquals(RailGestureDecision.Pending, liquidRailGestureDecision(3f, 4f, 8f, false))
    }

    @Test fun overflowingExpandedLiquidYieldsVerticalDragToNativeScroll() {
        assertEquals(RailGestureDecision.Cancel, liquidRailGestureDecision(1f, 16f, 8f, false, verticalScrollAvailable = true))
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(1f, 16f, 8f, false, verticalScrollAvailable = false))
        assertEquals(RailGestureDecision.Pending, liquidRailGestureDecision(1f, 2f, 8f, false, verticalScrollAvailable = true))
        assertEquals(RailGestureDecision.Cancel, liquidRailGestureDecision(16f, 1f, 8f, false, verticalScrollAvailable = true))
    }

    @Test fun bothDragDirectionsAreAcceptedOnTheirOwnAxis() {
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(-16f, 1f, 8f, true))
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(16f, 1f, 8f, true))
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(1f, -16f, 8f, false))
        assertEquals(RailGestureDecision.Drag, liquidRailGestureDecision(1f, 16f, 8f, false))
    }

    @Test fun rowPreviewContinuouslyInterpolatesAndClamps() {
        val centres = listOf(50f, 130f, 210f)
        assertEquals(0f, liquidRailPreviewAt(10f, centres), 0.001f)
        assertEquals(0.5f, liquidRailPreviewAt(90f, centres), 0.001f)
        assertEquals(1.5f, liquidRailPreviewAt(170f, centres), 0.001f)
        assertEquals(2f, liquidRailPreviewAt(300f, centres), 0.001f)
    }
}
