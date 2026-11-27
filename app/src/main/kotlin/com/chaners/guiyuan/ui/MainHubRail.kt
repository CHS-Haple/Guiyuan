package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.ui.components.liquid.liquidRailSurface
import com.chaners.guiyuan.ui.components.liquid.liquidRailSelectedSurface
import com.chaners.guiyuan.ui.glass.GlassNavigationRail
import com.chaners.guiyuan.ui.glass.GlassNavigationRailState
import com.chaners.guiyuan.ui.glass.GlassRailDestinations
import com.chaners.guiyuan.ui.glass.RailLiquidPaint
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
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
    // Glass and Liquid share the Glass rail; preserve native fallback when material is unavailable.
    if (glassBackdropAvailable &&
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

/** Glass and Liquid rails share the hub's destinations and action owner. */
@Composable
internal fun HubGlassRail(
    navigation: HubNavigation,
    backdrop: Backdrop?,
    state: GlassNavigationRailState,
    actionInRail: Boolean,
) {
    val (appearance, dark, items, pageSelection, navigationAction, actionControls) = navigation
    // Wide rails use labeled destinations, independent of compact bottom-bar preference.
    val railItems = remember(items) {
        items.map { item ->
            GlassNavigationItem(
                icon = item.icon,
                label = item.label,
                contentDescription = item.label,
            )
        }
    }
    GlassNavigationRail(
        destinations = GlassRailDestinations(
            items = railItems,
            selectedIndex = pageSelection.selectedPage,
            onSelect = pageSelection::select,
        ),
        backdrop = backdrop,
        state = state,
        showToggle = true,
        trailingAction = if (actionInRail) navigationAction?.let { action ->
            {
                backdrop?.let { railSource ->
                    NavigationTrailingAction(
                        action = action,
                        surface = NavigationActionSurface(
                            railSource,
                            appearance.liquidMode.takeIf {
                                appearance.navStyle == NavStyle.Liquid
                            },
                            dark,
                            GlassNavigationRailDefaults.CollapsedHeight,
                        ),
                        controls = actionControls,
                    )
                }
            }
        } else null,
        // Keep upstream OS4 rail motion and indicator; substitute only the panel surface.
        materialOverride = if (appearance.navStyle == NavStyle.Liquid) {
            RailLiquidPaint(
                panel = { backdrop, shape ->
                    Modifier.liquidRailSurface(backdrop, shape, appearance.liquidMode)
                },
                indicator = { selectedBackdrop, pressed ->
                    Modifier.liquidRailSelectedSurface(
                        backdrop = selectedBackdrop,
                        shape = RoundedCornerShape(percent = 50),
                        pressed = pressed,
                    )
                },
            )
        } else null,
        modifier = Modifier.fillMaxSize(),
    )
}
