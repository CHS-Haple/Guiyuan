package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailState
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults

/** Keep a full-width page beside the upstream expanded rail; insets are relative to logical start. */
internal fun canPlaceGlassRail(
    windowSize: androidx.compose.ui.unit.DpSize,
    startInset: androidx.compose.ui.unit.Dp,
    endInset: androidx.compose.ui.unit.Dp,
    verticalInsets: androidx.compose.ui.unit.Dp,
): Boolean {
    val occupiedWidth = GlassNavigationRailDefaults.ExpandedWidth +
        GlassNavigationRailDefaults.Margin * 2 + startInset
    return windowSize.width >= occupiedWidth + endInset + 360.dp &&
        windowSize.height >= verticalInsets + 320.dp
}

/** Native rail replaces the current bottom bar only when the whole page still fits. */
internal fun canPlaceNativeRail(
    window: DpSize,
    startInset: Dp,
    endInset: Dp,
    verticalInsets: Dp,
): Boolean = window.width >= NavigationRailDefaults.ExpandedWidth + startInset + endInset + 360.dp &&
    window.height >= verticalInsets + 320.dp

/** A style-level policy with no orientation or device-brand assumptions. */
internal fun useNativeRailFor(
    appearance: Appearance,
    glassBackdropAvailable: Boolean,
    window: DpSize,
    safePadding: PaddingValues,
    direction: LayoutDirection,
): Boolean {
    if (!appearance.navEnabled) return false
    // OS4 Glass and Liquid share the original GlassRail layout; OS3 uses native Rail.
    if (appearance.uiStyle == UiStyle.OS4 && glassBackdropAvailable &&
        (appearance.navStyle == NavStyle.Glass || appearance.navStyle == NavStyle.Liquid)
    ) return false
    return canPlaceNativeRail(
        window,
        safePadding.calculateStartPadding(direction),
        safePadding.calculateEndPadding(direction),
        safePadding.calculateTopPadding() + safePadding.calculateBottomPadding(),
    )
}

internal data class MainNavItem(
    val label: String,
    val icon: ImageVector,
)

/** Map app destinations to the MIUIX-native rail; no parallel press or selected lens. */
@Composable
internal fun HubNativeRail(
    state: NavigationRailState,
    items: List<MainNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    NavigationRail(state = state, modifier = Modifier.fillMaxHeight()) {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                icon = item.icon,
                label = item.label,
            )
        }
    }
}
