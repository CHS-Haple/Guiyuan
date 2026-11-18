package com.chaners.guiyuan.ui

import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSearchActionPolicyTest {
    @Test
    fun os4GlassAndLiquidUseTheirExistingBottomNavActionLayout() {
        val appearance = Appearance(uiStyle = UiStyle.OS4, navEnabled = true, navStyle = NavStyle.Glass)
        assertTrue(hasOs4NavigationSearchSlot(appearance, true, false, false, false))
        assertTrue(hasOs4NavigationSearchSlot(appearance.copy(navStyle = NavStyle.Liquid), false, true, false, false))
        assertFalse(hasOs4NavigationSearchSlot(appearance.copy(navStyle = NavStyle.Liquid), true, false, false, false))
        assertFalse(hasOs4NavigationSearchSlot(appearance, false, true, false, false))
    }

    @Test
    fun unsupportedStylesOs3AndRailFallbackPreserveTopSearch() {
        val appearance = Appearance(uiStyle = UiStyle.OS4, navEnabled = true)
        for (style in listOf(NavStyle.Standard, NavStyle.Blur)) {
            assertFalse(hasOs4NavigationSearchSlot(appearance.copy(navStyle = style), true, true, false, false))
        }
        assertFalse(hasOs4NavigationSearchSlot(appearance.copy(uiStyle = UiStyle.OS3), true, true, false, false))
        assertFalse(hasOs4NavigationSearchSlot(appearance.copy(navEnabled = false), true, true, false, false))
        for (style in listOf(NavStyle.Glass, NavStyle.Liquid)) {
            val styled = appearance.copy(navStyle = style)
            assertFalse(hasOs4NavigationSearchSlot(styled, true, true, true, false))
            assertFalse(hasOs4NavigationSearchSlot(styled, true, true, false, true))
        }
    }

    @Test
    fun onlyIdleFeaturesDestinationDisplaysTrailingSearch() {
        assertFalse(showOs4NavigationSearchAction(true, 0, false))
        assertTrue(showOs4NavigationSearchAction(true, 1, false))
        assertFalse(showOs4NavigationSearchAction(true, 2, false))
        assertFalse(showOs4NavigationSearchAction(true, 1, true))
        assertFalse(showOs4NavigationSearchAction(false, 1, false))
    }
}
