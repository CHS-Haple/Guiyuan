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
    @Test fun expandedRailDeformsAlongMotionInBothDirections() {
        val idle = liquidLensScales(224f, 56f, 22f, 1f, 0f, vertical = true)
        val down = liquidLensScales(224f, 56f, 22f, 1f, 0.25f, vertical = true)
        val up = liquidLensScales(224f, 56f, 22f, 1f, -0.25f, vertical = true)
        org.junit.Assert.assertTrue(down.y > idle.y)
        org.junit.Assert.assertTrue(up.y < idle.y)
        org.junit.Assert.assertTrue(down.x < idle.x)
        org.junit.Assert.assertTrue(up.x > idle.x)
    }

    @Test fun collapsedRailRotatesPortraitVelocityRuleAndReleasesToRest() {
        val idle = liquidLensScales(150f, 56f, 22f, 1f, 0f, vertical = false)
        val right = liquidLensScales(150f, 56f, 22f, 1f, 0.25f, vertical = false)
        val left = liquidLensScales(150f, 56f, 22f, 1f, -0.25f, vertical = false)
        org.junit.Assert.assertTrue(right.x > idle.x)
        org.junit.Assert.assertTrue(left.x < idle.x)
        org.junit.Assert.assertTrue(right.y < idle.y)
        org.junit.Assert.assertTrue(left.y > idle.y)
        val released = liquidLensScales(150f, 56f, 22f, 0f, 0f, vertical = false)
        assertEquals(1f, released.x, 0.0001f)
        assertEquals(1f, released.y, 0.0001f)
    }
}
