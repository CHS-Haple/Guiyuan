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
    @Test fun expandedDirectionMirrorsTheLeadingEdgeWithoutShrinking() {
        val idle = liquidDirectionalLensTransform(224f, 56f, 22f, 1f, 0f, true)
        val down = liquidDirectionalLensTransform(224f, 56f, 22f, 1f, 0.3f, true)
        val up = liquidDirectionalLensTransform(224f, 56f, 22f, 1f, -0.3f, true)
        assertEquals(down.y, up.y, 0.0001f)
        assertEquals(down.x, up.x, 0.0001f)
        assertEquals(down.translationY, -up.translationY, 0.0001f)
        assertEquals(0f, down.translationX, 0.0001f)
        org.junit.Assert.assertTrue(down.y > idle.y)
        assertEquals(-56f * idle.y / 2f, down.translationY - 56f * down.y / 2f, 0.0001f)
        assertEquals(56f * idle.y / 2f, up.translationY + 56f * up.y / 2f, 0.0001f)
    }

    @Test fun compactDirectionMirrorsTheLeadingEdgeAndStopsAtRest() {
        val idle = liquidDirectionalLensTransform(150f, 56f, 22f, 1f, 0f, false)
        val right = liquidDirectionalLensTransform(150f, 56f, 22f, 1f, 0.3f, false)
        val left = liquidDirectionalLensTransform(150f, 56f, 22f, 1f, -0.3f, false)
        assertEquals(right.x, left.x, 0.0001f)
        assertEquals(right.y, left.y, 0.0001f)
        assertEquals(right.translationX, -left.translationX, 0.0001f)
        org.junit.Assert.assertTrue(right.x > idle.x)
        assertEquals(-150f * idle.x / 2f, right.translationX - 150f * right.x / 2f, 0.0001f)
        assertEquals(150f * idle.x / 2f, left.translationX + 150f * left.x / 2f, 0.0001f)
        val stop = liquidDirectionalLensTransform(150f, 56f, 22f, 1f, 0f, false)
        val release = liquidDirectionalLensTransform(150f, 56f, 22f, 0f, 0f, false)
        assertEquals(0f, stop.translationX, 0.0001f)
        assertEquals(1f, release.x, 0.0001f)
        assertEquals(1f, release.y, 0.0001f)
    }

    @Test fun highMotionClampsSymmetrically() {
        val max = liquidDirectionalLensTransform(220f, 56f, 22f, 1f, 0.5f, true)
        val over = liquidDirectionalLensTransform(220f, 56f, 22f, 1f, 10f, true)
        assertEquals(max.y, over.y, 0.0001f)
        assertEquals(max.translationY, over.translationY, 0.0001f)
    }
}
