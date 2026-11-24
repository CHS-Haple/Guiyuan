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
import com.chaners.guiyuan.settings.UiStyle
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

// App-owned bottom-bar routing. Each surface keeps its native/MIUIX component and gesture owner.
@Composable
internal fun HubGlassBottomBar(
    appearance: Appearance,
    dark: Boolean,
    items: List<MainNavItem>,
    pageSelection: HubPagerSelection,
    navBackdrop: Backdrop,
    navigationAction: NavigationAction?,
    actionControls: NavigationActionControls,
) {
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
    appearance: Appearance,
    items: List<MainNavItem>,
    pageSelection: HubPagerSelection,
    os4Glass: Boolean,
) {
    NavigationBar(
        mode = if (os4Glass && appearance.navContent == NavContent.IconOnly) {
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
    appearance: Appearance,
    dark: Boolean,
    items: List<MainNavItem>,
    pageSelection: HubPagerSelection,
    liquidBackdrop: Backdrop,
    navigationAction: NavigationAction?,
    actionControls: NavigationActionControls,
    liquidBottomPadding: Dp,
) {
    val os4 = appearance.uiStyle == UiStyle.OS4
    val liquidPressLink = remember { LiquidNavPressLink() }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.displayCutout.union(WindowInsets.navigationBars)
                        .only(WindowInsetsSides.Horizontal),
                )
                .then(
                    if (appearance.navTouchExtension) {
                        Modifier.navTouchExtension(
                            geometry = NavTouchStripGeometry(
                                bottomPadding = liquidBottomPadding,
                                horizontalPadding = LiquidNavSpec.sidePadding,
                                contentPadding = 4.dp,
                                itemCount = items.size,
                                rtl = LocalLayoutDirection.current == LayoutDirection.Rtl,
                            ),
                            onSelect = pageSelection::select,
                            trailing = NavTouchTrailingAction(
                                width = if (navigationAction != null) {
                                    LiquidNavSpec.height + GlassNavigationBarDefaults.ActionSpacing
                                } else 0.dp,
                                size = if (navigationAction != null) {
                                    LiquidNavSpec.height
                                } else 0.dp,
                                onTap = navigationAction?.let { action -> actionControls.callbackFor(action) },
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(bottom = liquidBottomPadding),
    ) {
        val liquidBar: @Composable () -> Unit = {
            LiquidNavBar(
                selectedIndex = { pageSelection.selectedPage },
                onSelected = pageSelection::select,
                backdrop = liquidBackdrop,
                tabsCount = items.size,
                dark = dark,
                mode = appearance.liquidMode,
                pressLink = if (os4) liquidPressLink else null,
                // OS4's shared layout owns the outer margin; OS3 keeps its original.
                modifier = if (os4) Modifier else Modifier.padding(horizontal = LiquidNavSpec.sidePadding),
            ) {
                items.forEachIndexed { index, item ->
                    LiquidNavEntry(
                        contentMode = appearance.navContent,
                        selected = pageSelection.selectedPage == index,
                        onClick = { pageSelection.select(index) },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        }
        if (os4) {
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
            ) { liquidBar() }
        } else {
            liquidBar()
        }
    }
}

@Composable
internal fun HubFloatingBottomBar(
    appearance: Appearance,
    items: List<MainNavItem>,
    pageSelection: HubPagerSelection,
    navigationBarModifier: Modifier,
    navBackdrop: Backdrop?,
) {
    val barBounds = remember(appearance.navStyle, appearance.uiStyle) {
        mutableStateOf<Rect?>(null)
    }
    val hostBounds = remember(appearance.navStyle, appearance.uiStyle) {
        mutableStateOf<Rect?>(null)
    }
    val itemBounds = remember(appearance.navStyle, appearance.uiStyle, items.size) {
        mutableStateListOf<Rect?>().apply {
            repeat(items.size) { add(null) }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (appearance.navTouchExtension) {
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            if (hostBounds.value != bounds) hostBounds.value = bounds
                        }
                        .measuredNavTouchExtension(
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
            modifier = navigationBarModifier.then(
                if (appearance.navTouchExtension) {
                    Modifier.onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInRoot()
                        if (barBounds.value != bounds) barBounds.value = bounds
                    }
                } else {
                    Modifier
                },
            ),
            color =
                if (navBackdrop != null) {
                    Color.Transparent
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
        ) {
            items.forEachIndexed { index, item ->
                val selected = pageSelection.selectedPage == index
                NavContentItem(
                    content = appearance.navContent,
                    selected = selected,
                    onClick = { pageSelection.select(index) },
                    icon = item.icon,
                    label = item.label,
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
    }
}
