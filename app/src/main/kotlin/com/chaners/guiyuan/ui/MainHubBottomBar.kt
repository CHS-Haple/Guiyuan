package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.ui.components.NavContentItem
import com.chaners.guiyuan.ui.components.NavTouchStripGeometry
import com.chaners.guiyuan.ui.components.NavTouchTrailingAction
import com.chaners.guiyuan.ui.components.liquid.LiquidNavBar
import com.chaners.guiyuan.ui.components.liquid.LiquidNavEntry
import com.chaners.guiyuan.ui.components.liquid.LiquidNavPressLink
import com.chaners.guiyuan.ui.components.liquid.LiquidNavSpec
import com.chaners.guiyuan.ui.components.measuredNavTouchExtension
import com.chaners.guiyuan.ui.components.navTouchExtension
import com.chaners.guiyuan.ui.glass.GlassNavigationBar
import com.chaners.guiyuan.ui.glass.GlassNavigationBarLayout
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Shared destination and action ownership for all bottom navigation surfaces. */
internal data class HubBottomNavigation(
    val appearance: Appearance,
    val dark: Boolean,
    val items: List<MainNavItem>,
    val pageSelection: HubPagerSelection,
    val navigationAction: NavigationAction?,
    val actionControls: NavigationActionControls,
)

// App-owned bottom-bar routing. Each surface keeps its native/MIUIX component and gesture owner.
@Composable
internal fun HubGlassBottomBar(
    bar: HubBottomNavigation,
    navBackdrop: Backdrop,
) {
    val (appearance, dark, items, pageSelection, navigationAction, actionControls) = bar
    val selectedPage = pageSelection.selectedPage
    val includeLabels = appearance.navContent == NavContent.IconAndText
    val glassItems = remember(items, includeLabels) {
        items.map { item ->
            GlassNavigationItem(
                icon = item.icon,
                label = item.label.takeIf { includeLabels },
                contentDescription = item.label,
            )
        }
    }
    GlassNavigationBar(
        items = glassItems,
        selectedIndex = selectedPage,
        onSelect = pageSelection::select,
        backdrop = navBackdrop,
        touchExtension = appearance.navTouchExtension,
        // Exclude the measured 54dp action and 12dp native MIUIX gap.
        trailingActionTouchExclusion = if (navigationAction != null) {
            GlassNavigationBarDefaults.Height + GlassNavigationBarDefaults.ActionSpacing
        } else 0.dp,
        trailingActionTouchSize = if (navigationAction != null) {
            GlassNavigationBarDefaults.Height
        } else 0.dp,
        onTrailingActionTouch = navigationAction?.let { action -> actionControls.callbackFor(action) },
        trailingAction = navigationAction?.let { action ->
            {
                NavigationTrailingAction(
                    action = action,
                    surface = NavigationActionSurface(
                        navBackdrop, null, dark, GlassNavigationBarDefaults.Height,
                    ),
                    controls = actionControls,
                )
            }
        },
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 20.dp),
    )
}

@Composable
internal fun HubNativeBottomBar(
    bar: HubBottomNavigation,
    glassNavigation: Boolean,
) {
    val (appearance, _, items, pageSelection) = bar
    NavigationBar(
        mode = if (glassNavigation && appearance.navContent == NavContent.IconOnly) {
            NavigationBarDisplayMode.IconOnly
        } else {
            NavigationBarDisplayMode.IconAndText
        },
    ) {
        items.forEachIndexed { index, item ->
            val selected = pageSelection.selectedPage == index
            NavigationBarItem(
                selected = selected,
                onClick = { pageSelection.select(index) },
                icon = item.icon,
                label = item.label,
            )
        }
    }
}

@Composable
internal fun HubLiquidBottomBar(
    bar: HubBottomNavigation,
    liquidBackdrop: Backdrop,
    liquidBottomPadding: Dp,
) {
    val (appearance, dark, _, _, navigationAction, actionControls) = bar
    val liquidPressLink = remember { LiquidNavPressLink() }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                        .only(WindowInsetsSides.Horizontal),
                )
                .then(liquidTouchExtension(bar, liquidBottomPadding))
                .padding(bottom = liquidBottomPadding),
    ) {
        GlassNavigationBarLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LiquidNavSpec.sidePadding),
            trailingAction = navigationAction?.let { action ->
                {
                    NavigationTrailingAction(
                        action = action,
                        surface = NavigationActionSurface(
                            liquidBackdrop, appearance.liquidMode, dark, LiquidNavSpec.height,
                        ),
                        controls = actionControls,
                        pressLink = liquidPressLink,
                    )
                }
            },
        ) { HubLiquidNavItems(bar, liquidBackdrop, liquidPressLink) }
    }
}

/** The liquid bar alone owns the MIUIX tab/press interaction subtree. */
@Composable
private fun HubLiquidNavItems(
    bar: HubBottomNavigation,
    liquidBackdrop: Backdrop,
    liquidPressLink: LiquidNavPressLink,
) {
    val (appearance, dark, items, pageSelection) = bar
    LiquidNavBar(
        selectedIndex = { pageSelection.selectedPage },
        onSelected = pageSelection::select,
        backdrop = liquidBackdrop,
        tabsCount = items.size,
        dark = dark,
        mode = appearance.liquidMode,
        pressLink = liquidPressLink,
    ) {
        items.forEachIndexed { index, item ->
            LiquidNavEntry(
                contentMode = appearance.navContent,
                selected = pageSelection.selectedPage == index,
                onClick = { pageSelection.select(index) },
                item = item,
            )
        }
    }
}

/** The optional extended hit strip stays separate from MIUIX's visual/press owner. */
@Composable
private fun liquidTouchExtension(bar: HubBottomNavigation, bottomPadding: Dp): Modifier {
    if (!bar.appearance.navTouchExtension) return Modifier
    val action = bar.navigationAction
    val actionSize = LiquidNavSpec.height
    return Modifier.navTouchExtension(
        geometry = NavTouchStripGeometry(
            bottomPadding = bottomPadding,
            horizontalPadding = LiquidNavSpec.sidePadding,
            contentPadding = 4.dp,
            itemCount = bar.items.size,
            rtl = LocalLayoutDirection.current == LayoutDirection.Rtl,
        ),
        onSelect = bar.pageSelection::select,
        trailing = NavTouchTrailingAction(
            width = if (action != null) actionSize + GlassNavigationBarDefaults.ActionSpacing else 0.dp,
            size = if (action != null) actionSize else 0.dp,
            onTap = action?.let { bar.actionControls.callbackFor(it) },
        ),
    )
}

@Composable
internal fun HubFloatingBottomBar(
    bar: HubBottomNavigation,
    navigationBarModifier: Modifier,
    navBackdrop: Backdrop?,
) {
    val (appearance, _, items, pageSelection) = bar
    val barBounds = remember(appearance.navStyle) {
        mutableStateOf<Rect?>(null)
    }
    val hostBounds = remember(appearance.navStyle) {
        mutableStateOf<Rect?>(null)
    }
    val itemBounds = remember(appearance.navStyle, items.size) {
        mutableStateListOf<Rect?>().apply {
            repeat(items.size) { add(null) }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .recordFloatingNavBounds(appearance.navTouchExtension, hostBounds)
            .then(
                if (appearance.navTouchExtension) {
                    Modifier.measuredNavTouchExtension(
                        hostBounds = hostBounds.value,
                        barBounds = barBounds.value,
                        itemBounds = itemBounds.toList(),
                        onSelect = pageSelection::select,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        FloatingNavigationBar(
            modifier = navigationBarModifier.recordFloatingNavBounds(
                appearance.navTouchExtension, barBounds,
            ),
            color =
                if (navBackdrop != null) {
                    Color.Transparent
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
        ) {
            HubFloatingNavItems(bar, itemBounds)
        }
    }
}

/** Root/bar geometry shares one update policy; only changed layout publishes state. */
private fun Modifier.recordFloatingNavBounds(
    enabled: Boolean,
    bounds: MutableState<Rect?>,
): Modifier =
    then(
        if (enabled) {
            Modifier.onGloballyPositioned { coordinates ->
                val measured = coordinates.boundsInRoot()
                if (bounds.value != measured) bounds.value = measured
            }
        } else {
            Modifier
        },
    )

/** Measure each rendered tab using its actual MIUIX bounds, not a guessed hit width. */
@Composable
private fun HubFloatingNavItems(
    bar: HubBottomNavigation,
    itemBounds: androidx.compose.runtime.snapshots.SnapshotStateList<Rect?>,
) {
    val (appearance, _, items, pageSelection) = bar
    items.forEachIndexed { index, item ->
        val selected = pageSelection.selectedPage == index
        NavContentItem(
            content = appearance.navContent,
            selected = selected,
            onClick = { pageSelection.select(index) },
            item = item,
            modifier = if (appearance.navTouchExtension) {
                Modifier.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    if (itemBounds[index] != bounds) itemBounds[index] = bounds
                }
            } else {
                Modifier
            },
        )
    }
}
