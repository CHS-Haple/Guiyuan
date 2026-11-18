package com.chaners.guiyuan.ui

import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle

/** Only the OS4 glass bottom bar has the upstream independent trailing-action slot. */
internal fun hasOs4NavigationSearchSlot(
    appearance: Appearance,
    glassBackdropAvailable: Boolean,
    glassRailActive: Boolean,
    nativeRailActive: Boolean,
): Boolean = appearance.uiStyle == UiStyle.OS4 &&
    appearance.navEnabled && appearance.navStyle == NavStyle.Glass &&
    glassBackdropAvailable && !glassRailActive && !nativeRailActive

/** Keep navigation selection as the owner of appearance and search as the owner of activation. */
internal fun showOs4NavigationSearchAction(
    slotAvailable: Boolean,
    selectedPage: Int,
    searchActive: Boolean,
): Boolean = slotAvailable && selectedPage == 1 && !searchActive
