package com.chaners.guiyuan.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
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
    fun standardAndBlurKeepNativeRailWhenWindowFits() {
        for (style in listOf(NavStyle.Standard, NavStyle.Blur)) {
            assertTrue(useNativeRailFor(
                Appearance(navStyle = style), true,
                DpSize(750.dp, 420.dp), PaddingValues(), LayoutDirection.Ltr,
            ))
        }
    }

    @Test
    fun glassKeepsExistingGlassRailWhenMaterialAvailable() {
        assertFalse(useNativeRailFor(
            Appearance(navStyle = NavStyle.Glass),
            true, DpSize(750.dp, 420.dp), PaddingValues(), LayoutDirection.Ltr,
        ))
        assertTrue(useNativeRailFor(
            Appearance(navStyle = NavStyle.Glass),
            false, DpSize(750.dp, 420.dp), PaddingValues(), LayoutDirection.Ltr,
        ))
    }

    @Test
    fun liquidUsesGlassRailAndDisabledNavRemainsHidden() {
        val window = DpSize(750.dp, 420.dp)
        assertFalse(useNativeRailFor(
            Appearance(navStyle = NavStyle.Liquid),
            true, window, PaddingValues(), LayoutDirection.Ltr,
        ))
        // No shader: preserve the native fallback instead of showing an empty rail.
        assertTrue(useNativeRailFor(
            Appearance(navStyle = NavStyle.Liquid),
            false, window, PaddingValues(), LayoutDirection.Ltr,
        ))
        assertFalse(useNativeRailFor(
            Appearance(navStyle = NavStyle.Standard, navEnabled = false),
            false, window, PaddingValues(), LayoutDirection.Ltr,
        ))
    }

    @Test
    fun thresholdIsInclusive() {
        assertTrue(canPlaceNativeRail(DpSize(600.dp, 320.dp), 0.dp, 0.dp, 0.dp))
        assertFalse(canPlaceNativeRail(DpSize(599.dp, 320.dp), 0.dp, 0.dp, 0.dp))
    }
}
