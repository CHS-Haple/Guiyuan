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
    @Test fun horizontalMotionMatchesTheExistingPortraitFormula() {
        for (velocity in listOf(-20f, -3f, 0f, 3f, 20f)) {
            val result = liquidVelocityScales(1.32f, 1.28f, velocity, vertical = false)
            val signed = velocity / 10f
            val along = (signed * 0.75f).coerceIn(-0.2f, 0.2f)
            val across = (signed * 0.25f).coerceIn(-0.2f, 0.2f)
            assertEquals(1.32f / (1f - along), result.x, 0.0001f)
            assertEquals(1.28f * (1f - across), result.y, 0.0001f)
        }
    }

    @Test fun expandedRailRotatesThePortraitMotionAxesWithoutNewGeometry() {
        for (velocity in listOf(-10f, -2f, 0f, 2f, 10f)) {
            val portrait = liquidVelocityScales(1f, 1f, velocity, vertical = false)
            val expanded = liquidVelocityScales(1f, 1f, velocity, vertical = true)
            assertEquals(portrait.x, expanded.y, 0.0001f)
            assertEquals(portrait.y, expanded.x, 0.0001f)
        }
    }

    @Test fun stationaryMotionReturnsTheOriginalPressScale() {
        val idle = liquidVelocityScales(1.1f, 1.4f, 0f, vertical = false)
        val vertical = liquidVelocityScales(1.1f, 1.4f, 0f, vertical = true)
        assertEquals(1.1f, idle.x, 0.0001f)
        assertEquals(1.4f, idle.y, 0.0001f)
        assertEquals(1.1f, vertical.x, 0.0001f)
        assertEquals(1.4f, vertical.y, 0.0001f)
    }
}
