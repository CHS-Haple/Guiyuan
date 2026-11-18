package com.chaners.guiyuan.ui

import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle

/** Use only an OS4 bottom bar with an actual action layout, never a rail or fallback. */
internal fun hasOs4NavigationSearchSlot(
    appearance: Appearance,
    glassBackdropAvailable: Boolean,
    liquidBackdropAvailable: Boolean,
    glassRailActive: Boolean,
    nativeRailActive: Boolean,
): Boolean {
    if (appearance.uiStyle != UiStyle.OS4 || !appearance.navEnabled ||
        glassRailActive || nativeRailActive
    ) return false
    return when (appearance.navStyle) {
        NavStyle.Glass -> glassBackdropAvailable
        NavStyle.Liquid -> liquidBackdropAvailable
        NavStyle.Standard, NavStyle.Blur -> false
    }
}

/** The selected destination and the existing PageSearch own the action's visibility. */
internal fun showOs4NavigationSearchAction(
    slotAvailable: Boolean,
    selectedPage: Int,
    searchActive: Boolean,
): Boolean = slotAvailable && selectedPage == 1 && !searchActive
