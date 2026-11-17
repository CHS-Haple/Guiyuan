package com.chaners.guiyuan.ui.components.liquid

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidFeedbackSpecTest {
    @Test fun compactPanelUsesThePortraitPressGrowth() {
        assertEquals(1f, liquidPanelPressScale(160f, 56f, 16f, 0f), 0.0001f)
        assertEquals(1.1f, liquidPanelPressScale(160f, 56f, 16f, 1f), 0.0001f)
    }

    @Test fun tallRailOnlyScalesByItsLongSide() {
        assertEquals(1.05f, liquidPanelPressScale(200f, 320f, 16f, 1f), 0.0001f)
        assertEquals(1f, liquidPanelPressScale(200f, 320f, 16f, -1f), 0.0001f)
    }
}
