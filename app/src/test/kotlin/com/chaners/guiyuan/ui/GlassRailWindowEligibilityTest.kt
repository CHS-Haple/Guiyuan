package com.chaners.guiyuan.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Width and safe-area decisions must not depend on device label or orientation. */
class GlassRailWindowEligibilityTest {
    @Test
    fun compactPortraitKeepsBottomNavigation() {
        assertFalse(canPlaceGlassRail(DpSize(400.dp, 850.dp), 0.dp, 0.dp))
    }

    @Test
    fun qualifyingWideWindowAllowsExpandedRail() {
        assertTrue(canPlaceGlassRail(DpSize(700.dp, 420.dp), 0.dp, 0.dp))
    }

    @Test
    fun splitWindowFallsBackEvenWhenLandscape() {
        assertFalse(canPlaceGlassRail(DpSize(610.dp, 450.dp), 0.dp, 0.dp))
    }

    @Test
    fun startCutoutReducesUsablePageWidth() {
        assertFalse(canPlaceGlassRail(DpSize(650.dp, 420.dp), 30.dp, 0.dp))
        assertTrue(canPlaceGlassRail(DpSize(670.dp, 420.dp), 30.dp, 0.dp))
    }

    @Test
    fun topBottomSystemInsetsReduceAvailableRailHeight() {
        assertFalse(canPlaceGlassRail(DpSize(700.dp, 350.dp), 0.dp, 44.dp))
        assertTrue(canPlaceGlassRail(DpSize(700.dp, 400.dp), 0.dp, 44.dp))
    }

    @Test
    fun bothThresholdsAreInclusive() {
        assertTrue(canPlaceGlassRail(DpSize(634.dp, 320.dp), 0.dp, 0.dp))
        assertFalse(canPlaceGlassRail(DpSize(633.dp, 320.dp), 0.dp, 0.dp))
    }
}
