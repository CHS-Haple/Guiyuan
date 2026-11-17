package com.chaners.guiyuan.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only the actual available window can enable the native expanded rail. */
class NativeRailWindowEligibilityTest {
    @Test
    fun narrowPortraitKeepsItsBottomBar() {
        assertFalse(canPlaceNativeRail(DpSize(400.dp, 850.dp), 0.dp, 0.dp, 0.dp))
    }

    @Test
    fun wideWindowHasRoomForRailAndPage() {
        assertTrue(canPlaceNativeRail(DpSize(700.dp, 420.dp), 0.dp, 0.dp, 0.dp))
    }

    @Test
    fun insetsOnBothEdgesCount() {
        assertFalse(canPlaceNativeRail(DpSize(620.dp, 420.dp), 14.dp, 12.dp, 0.dp))
        assertTrue(canPlaceNativeRail(DpSize(626.dp, 420.dp), 14.dp, 12.dp, 0.dp))
    }

    @Test
    fun systemVerticalInsetsReduceWindowEligibility() {
        assertFalse(canPlaceNativeRail(DpSize(700.dp, 350.dp), 0.dp, 0.dp, 40.dp))
        assertTrue(canPlaceNativeRail(DpSize(700.dp, 370.dp), 0.dp, 0.dp, 40.dp))
    }

    @Test
    fun thresholdIsInclusive() {
        assertTrue(canPlaceNativeRail(DpSize(600.dp, 320.dp), 0.dp, 0.dp, 0.dp))
        assertFalse(canPlaceNativeRail(DpSize(599.dp, 320.dp), 0.dp, 0.dp, 0.dp))
    }
}
