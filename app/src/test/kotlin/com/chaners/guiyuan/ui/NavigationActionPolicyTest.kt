package com.chaners.guiyuan.ui

import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationActionPolicyTest {
    @Test
    fun os4GlassAndLiquidUseTheirExistingBottomNavActionLayout() {
        val appearance = Appearance(uiStyle = UiStyle.OS4, navEnabled = true, navStyle = NavStyle.Glass)
        assertTrue(hasOs4NavigationActionSlot(appearance, true, false, false, false))
        assertTrue(hasOs4NavigationActionSlot(appearance.copy(navStyle = NavStyle.Liquid), false, true, false, false))
        assertFalse(hasOs4NavigationActionSlot(appearance.copy(navStyle = NavStyle.Liquid), true, false, false, false))
        assertFalse(hasOs4NavigationActionSlot(appearance, false, true, false, false))
    }

    @Test
    fun unsupportedStylesOs3AndRailFallbackPreserveTopSearch() {
        val appearance = Appearance(uiStyle = UiStyle.OS4, navEnabled = true)
        for (style in listOf(NavStyle.Standard, NavStyle.Blur)) {
            assertFalse(hasOs4NavigationActionSlot(appearance.copy(navStyle = style), true, true, false, false))
        }
        assertFalse(hasOs4NavigationActionSlot(appearance.copy(uiStyle = UiStyle.OS3), true, true, false, false))
        assertFalse(hasOs4NavigationActionSlot(appearance.copy(navEnabled = false), true, true, false, false))
        for (style in listOf(NavStyle.Glass, NavStyle.Liquid)) {
            val styled = appearance.copy(navStyle = style)
            assertFalse(hasOs4NavigationActionSlot(styled, true, true, true, false))
            assertFalse(hasOs4NavigationActionSlot(styled, true, true, false, true))
        }
    }

    @Test
    fun os4RailOnlyExposesSearchInGlassAndLiquid() {
        val glass = Appearance(uiStyle = UiStyle.OS4, navEnabled = true, navStyle = NavStyle.Glass)
        assertTrue(hasOs4RailActionSlot(glass, true))
        assertTrue(hasOs4RailActionSlot(glass.copy(navStyle = NavStyle.Liquid), true))
        assertFalse(hasOs4RailActionSlot(glass.copy(navStyle = NavStyle.Standard), true))
        assertFalse(hasOs4RailActionSlot(glass.copy(uiStyle = UiStyle.OS3), true))
        assertFalse(hasOs4RailActionSlot(glass.copy(navEnabled = false), true))
        assertFalse(hasOs4RailActionSlot(glass, false))
    }

    @Test
    fun sharedActionSlotRoutesHomeReloadAndFeaturesSearchWithoutSecondSlot() {
        assertEquals(NavigationAction.HotReload, navigationActionFor(true, 0, false))
        assertEquals(NavigationAction.HotReload, navigationActionFor(true, 0, true))
        assertEquals(NavigationAction.Search, navigationActionFor(true, 1, false))
        assertNull(navigationActionFor(true, 1, true))
        assertNull(navigationActionFor(true, 2, false))
        assertNull(navigationActionFor(false, 0, false))
        assertNull(navigationActionFor(false, 1, false))
        assertNull(navigationActionFor(true, -1, false))
    }
}
