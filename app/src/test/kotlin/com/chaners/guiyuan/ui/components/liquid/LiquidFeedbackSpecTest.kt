package com.chaners.guiyuan.ui.components.liquid

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidFeedbackSpecTest {
    @Test fun actionClearanceUsesMeasuredLensWidthAndExpectedSpringOvershoot() {
        // Rail's fixed 56dp lens has 22dp extra width at the press target.
        assertEquals(17.1f, liquidActionPressReachPx(16f, 22f, 3f), 0.001f)
        assertEquals(16.1f, liquidActionPressReachPx(16f, 22f, 4f), 0.001f)
        // A wider portrait slot actually stretches 33dp, not the fixed 22dp Rail value.
        val actualGrowth = 84f * (LiquidFeedbackSpec.PressedLensScale - 1f)
        assertEquals(22.15f, liquidActionPressReachPx(16f, actualGrowth, 4f), 0.001f)
        assertEquals(0f, liquidActionPressReachPx(0f, 0f, 5f), 0.001f)
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
