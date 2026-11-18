package com.chaners.guiyuan.ui

import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSearchActionPolicyTest {
    @Test
    fun onlySupportedOs4GlassBottomNavigationHostsSearchAction() {
        val os4 = Appearance(uiStyle = UiStyle.OS4, navEnabled = true, navStyle = NavStyle.Glass)
        assertTrue(hasOs4NavigationSearchSlot(os4, true, false, false))
        for (style in listOf(NavStyle.Standard, NavStyle.Blur, NavStyle.Liquid)) {
            assertFalse(hasOs4NavigationSearchSlot(os4.copy(navStyle = style), true, false, false))
        }
        assertFalse(hasOs4NavigationSearchSlot(os4.copy(uiStyle = UiStyle.OS3), true, false, false))
        assertFalse(hasOs4NavigationSearchSlot(os4.copy(navEnabled = false), true, false, false))
        assertFalse(hasOs4NavigationSearchSlot(os4, false, false, false))
    }

    @Test
    fun wideRailsAndFallbacksKeepTheExistingTopSearch() {
        val appearance = Appearance(uiStyle = UiStyle.OS4, navStyle = NavStyle.Glass)
        assertFalse(hasOs4NavigationSearchSlot(appearance, true, true, false))
        assertFalse(hasOs4NavigationSearchSlot(appearance, true, false, true))
    }

    @Test
    fun navigationSearchIsOnlyVisibleOnIdleFeaturesDestination() {
        assertFalse(showOs4NavigationSearchAction(true, 0, false))
        assertTrue(showOs4NavigationSearchAction(true, 1, false))
        assertFalse(showOs4NavigationSearchAction(true, 2, false))
        assertFalse(showOs4NavigationSearchAction(true, 1, true))
        assertFalse(showOs4NavigationSearchAction(false, 1, false))
    }
}
