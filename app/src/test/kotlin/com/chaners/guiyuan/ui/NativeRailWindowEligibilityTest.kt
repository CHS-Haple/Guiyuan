package com.chaners.guiyuan.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.UiStyle
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
    fun os3GlassAndBlurWorkWithoutChangingNavStylePreference() {
        for (style in listOf(NavStyle.Glass, NavStyle.Blur)) {
            assertTrue(useNativeRailFor(
                Appearance(uiStyle = UiStyle.OS3, navStyle = style), true,
                DpSize(750.dp, 420.dp), PaddingValues(), LayoutDirection.Ltr,
            ))
        }
    }

    @Test
    fun os4GlassKeepsExistingGlassRailWhenMaterialAvailable() {
        assertFalse(useNativeRailFor(
            Appearance(uiStyle = UiStyle.OS4, navStyle = NavStyle.Glass),
            true, DpSize(750.dp, 420.dp), PaddingValues(), LayoutDirection.Ltr,
        ))
        assertTrue(useNativeRailFor(
            Appearance(uiStyle = UiStyle.OS4, navStyle = NavStyle.Glass),
            false, DpSize(750.dp, 420.dp), PaddingValues(), LayoutDirection.Ltr,
        ))
    }

    @Test
    fun liquidUsesTheOriginalUiStyleRailAndDisabledNavRemainsHidden() {
        val window = DpSize(750.dp, 420.dp)
        assertTrue(useNativeRailFor(
            Appearance(uiStyle = UiStyle.OS3, navStyle = NavStyle.Liquid),
            true, window, PaddingValues(), LayoutDirection.Ltr,
        ))
        assertFalse(useNativeRailFor(
            Appearance(uiStyle = UiStyle.OS4, navStyle = NavStyle.Liquid),
            true, window, PaddingValues(), LayoutDirection.Ltr,
        ))
        // No shader: preserve the native OS4 fallback instead of showing an empty rail.
        assertTrue(useNativeRailFor(
            Appearance(uiStyle = UiStyle.OS4, navStyle = NavStyle.Liquid),
            false, window, PaddingValues(), LayoutDirection.Ltr,
        ))
        assertFalse(useNativeRailFor(
            Appearance(uiStyle = UiStyle.OS4, navStyle = NavStyle.Standard, navEnabled = false),
            false, window, PaddingValues(), LayoutDirection.Ltr,
        ))
    }

    @Test
    fun thresholdIsInclusive() {
        assertTrue(canPlaceNativeRail(DpSize(600.dp, 320.dp), 0.dp, 0.dp, 0.dp))
        assertFalse(canPlaceNativeRail(DpSize(599.dp, 320.dp), 0.dp, 0.dp, 0.dp))
    }
}
