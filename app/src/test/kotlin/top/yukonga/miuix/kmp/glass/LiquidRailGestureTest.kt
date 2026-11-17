package top.yukonga.miuix.kmp.glass

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

    @Test fun singleDirectionAndPauseDoNotFlipTheSign() {
        assertEquals(1f, railPhysicalDragDirection(0f, 10f), 0.0001f)
        assertEquals(-1f, railPhysicalDragDirection(1f, -10f), 0.0001f)
        assertEquals(1f, railPhysicalDragDirection(1f, 0f), 0.0001f)
        assertEquals(-1f, railPhysicalDragDirection(-1f, 0f), 0.0001f)
    }

    @Test fun renderedGapNaturallyDecaysAndOvershootCannotReversePhysicalSign() {
        assertEquals(0.25f, railRenderedMotion(1f, 1f, 0.75f, 1f), 0.0001f)
        assertEquals(-0.25f, railRenderedMotion(-1f, 1f, 0.75f, 1f), 0.0001f)
        assertEquals(0f, railRenderedMotion(1f, 1f, 1f, 1f), 0.0001f)
        assertEquals(0.25f, railRenderedMotion(1f, 1f, 1.25f, 1f), 0.0001f)
        assertEquals(0.5f, railRenderedMotion(1f, 100f, 0f, 80f), 0.0001f)
        assertEquals(0f, railRenderedMotion(0f, 100f, 0f, 80f), 0.0001f)
    }

    @Test fun rowPreviewContinuouslyInterpolatesAndClamps() {
        val centres = listOf(50f, 130f, 210f)
        assertEquals(0f, liquidRailPreviewAt(10f, centres), 0.001f)
        assertEquals(0.5f, liquidRailPreviewAt(90f, centres), 0.001f)
        assertEquals(1.5f, liquidRailPreviewAt(170f, centres), 0.001f)
        assertEquals(2f, liquidRailPreviewAt(300f, centres), 0.001f)
    }
}
