package com.chaners.guiyuan.ui.components.liquid

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidFeedbackSpecTest {
    @Test fun portraitActionMaintainsGapFromTheVisiblePanelOrLensEdge() {
        // The selected lens is a sibling of the panel; their growth must not be added.
        val pressed = LiquidFeedbackSpec.PressedLensScale
        assertEquals(12.5f, liquidPortraitActionReachPx(
            16f, 1f, 84f, pressed, 2f, 2, 4f,
        ), 0.001f)
        assertEquals(8f, liquidPortraitActionReachPx(
            16f, 1f, 84f, pressed, 0f, 2, 4f,
        ), 0.001f)
        assertEquals(0f, liquidPortraitActionReachPx(
            16f, 0f, 84f, 1f, 2f, 2, 4f,
        ), 0.001f)
        assertEquals(4.4f, liquidPortraitActionReachPx(
            16f, 0.5f, 84f, 1.2f, 2f, 2, 4f,
        ), 0.001f)
    }

    @Test fun portraitGapFollowsTheExistingVelocityDeformationInsteadOfGuessing() {
        // The renderer's velocity stretch can exceed the old fixed 10% allowance.
        val stretched = liquidRenderedLensScaleX(LiquidFeedbackSpec.PressedLensScale, 10f)
        assertEquals(1.7410715f, stretched, 0.0001f)
        assertEquals(27.125f, liquidPortraitActionReachPx(
            16f, 1f, 84f, stretched, 2f, 2, 4f,
        ), 0.01f)
        assertEquals(0f, liquidPortraitActionReachPx(
            16f, 0f, 0f, stretched, 2f, 2, 4f,
        ), 0.001f)
    }

    @Test fun tallRailScaleKeepsTheLensInsideItsReservedActionGap() {
        assertEquals(17.555f, liquidRailActionReachPx(320f, 54f, 16f, 22f, 3f), 0.001f)
        assertEquals(17.85833f, liquidRailActionReachPx(192f, 54f, 16f, 22f, 3f), 0.001f)
        assertEquals(0f, liquidRailActionReachPx(320f, 54f, 0f, 0f, 3f), 0.001f)
    }

    @Test fun compactPanelUsesThePortraitPressGrowth() {
        assertEquals(1f, liquidPanelPressScale(160f, 56f, 16f, 0f), 0.0001f)
        assertEquals(1.1f, liquidPanelPressScale(160f, 56f, 16f, 1f), 0.0001f)
    }

    @Test fun tallRailOnlyScalesByItsLongSide() {
        assertEquals(1.05f, liquidPanelPressScale(200f, 320f, 16f, 1f), 0.0001f)
        assertEquals(1f, liquidPanelPressScale(200f, 320f, 16f, -1f), 0.0001f)
    }
}
