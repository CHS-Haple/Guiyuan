package top.yukonga.miuix.kmp.glass

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure rail touch policy: guard drag ownership during scroll, RTL and a morph. */
class NavigationRailGesturePolicyTest {
    @Test fun smallMotionRemainsPending() {
        assertEquals(RailGestureDecision.Pending, railGestureDecision(4f, 5f, 8f, true))
    }

    @Test fun horizontalDragInCollapsedRailIsAccepted() {
        assertEquals(RailGestureDecision.Drag, railGestureDecision(10f, 3f, 8f, true))
    }

    @Test fun verticalMovementWinsWhenDominant() {
        assertEquals(RailGestureDecision.Cancel, railGestureDecision(9f, 12f, 8f, true))
    }

    @Test fun expandedRailYieldsHorizontalDragToAncestor() {
        assertEquals(RailGestureDecision.Cancel, railGestureDecision(12f, 1f, 8f, false))
    }

    @Test fun verticalGestureAlwaysEscapes() {
        assertEquals(RailGestureDecision.Cancel, railGestureDecision(2f, 12f, 8f, false))
    }

    @Test fun equalAxisMotionUsesHorizontalGestureWhenBeyondSlop() {
        assertEquals(RailGestureDecision.Drag, railGestureDecision(10f, 10f, 8f, true))
    }

    @Test fun touchSlopBoundaryDoesNotStartDrag() {
        assertEquals(RailGestureDecision.Pending, railGestureDecision(8f, 0f, 8f, true))
    }

    @Test fun physicalDragDeltaFollowsLayoutDirection() {
        assertEquals(12f, railLogicalDelta(6f, 2f, LayoutDirection.Ltr), 0.001f)
        assertEquals(-12f, railLogicalDelta(6f, 2f, LayoutDirection.Rtl), 0.001f)
    }
}
