package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.ui.components.NavContentItem
import com.chaners.guiyuan.ui.components.floatingNavMaterial
import com.chaners.guiyuan.ui.components.liquid.LiquidNavBar
import com.chaners.guiyuan.ui.components.liquid.LiquidNavEntry
import com.chaners.guiyuan.ui.components.liquid.LiquidNavSpec
import com.chaners.guiyuan.ui.components.liquid.liquidNavBackdropSource
import com.chaners.guiyuan.ui.components.liquid.liquidNavBottomPadding
import com.chaners.guiyuan.ui.components.liquid.liquidNavSupported
import com.chaners.guiyuan.ui.components.liquid.rememberLiquidNavBackdrop
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.FeaturesScreen
import com.chaners.guiyuan.ui.screens.HomeScreen
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.SettingsHubScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

private const val TopLevelPageCount = 3

private data class WeightedNavigationItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
internal fun MainHub(
    appearance: Appearance,
    dark: Boolean,
    lang: AppLang,
    iconHidden: Boolean,
    onHotReload: (() -> Unit) -> Boolean,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    previewState: PreviewSandboxUiState,
    onNavigate: (AppRoute) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { TopLevelPageCount })
    val scope = rememberCoroutineScope()
    var hotReloadInProgress by remember { mutableStateOf(false) }
    val miuixMaterialActive =
        appearance.navEnabled &&
            appearance.navStyle.requiresTextureBackdrop &&
            isRuntimeShaderSupported()
    val liquidMaterialActive =
        appearance.navEnabled &&
            appearance.navStyle == NavStyle.Liquid &&
            liquidNavSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val miuixBackdrop =
        if (miuixMaterialActive) {
            rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }
    val liquidBackdrop =
        if (liquidMaterialActive) {
            rememberLiquidNavBackdrop(surfaceColor)
        } else {
            null
        }

    val items = listOf(
        WeightedNavigationItem(
            label = stringResource(R.string.nav_home),
            icon = MiuixIcons.Normal.Home,
            selectedIcon = MiuixIcons.Medium.Home,
        ),
        WeightedNavigationItem(
            label = stringResource(R.string.nav_features),
            icon = MiuixIcons.Normal.Tune,
            selectedIcon = MiuixIcons.Medium.Tune,
        ),
        WeightedNavigationItem(
            label = stringResource(R.string.nav_settings),
            icon = MiuixIcons.Normal.Settings,
            selectedIcon = MiuixIcons.Medium.Settings,
        ),
    )

    fun selectPage(index: Int) {
        if (pagerState.currentPage != index) {
            scope.launch {
                pagerState.springAnimateToPage(index)
            }
        }
    }

    TopLevelBackHandler(
        pagerState = pagerState,
        onBackToHome = { selectPage(0) },
    )

    val navigationBarModifier =
        if (miuixBackdrop != null) {
            Modifier.floatingNavMaterial(
                backdrop = miuixBackdrop,
                dark = dark,
                style = appearance.navStyle,
            )
        } else {
            Modifier
        }
    val liquidBottomPadding = liquidNavBottomPadding()

    Scaffold(
        bottomBar = {
            when {
                !appearance.navEnabled -> {
                    NavigationBar {
                        items.forEachIndexed { index, item ->
                            val selected = pagerState.currentPage == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectPage(index) },
                                icon = if (selected) item.selectedIcon else item.icon,
                                label = item.label,
                            )
                        }
                    }
                }

                liquidBackdrop != null -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = liquidBottomPadding),
                    ) {
                        LiquidNavBar(
                            selectedIndex = { pagerState.currentPage },
                            onSelected = ::selectPage,
                            backdrop = liquidBackdrop,
                            tabsCount = items.size,
                            dark = dark,
                            mode = appearance.liquidMode,
                            modifier = Modifier.padding(horizontal = LiquidNavSpec.sidePadding),
                        ) {
                            items.forEachIndexed { index, item ->
                                LiquidNavEntry(
                                    contentMode = appearance.activeNavContent,
                                    onClick = { selectPage(index) },
                                    icon = item.icon,
                                    label = item.label,
                                    dark = dark,
                                )
                            }
                        }
                    }
                }

                else -> {
                    FloatingNavigationBar(
                        modifier = navigationBarModifier,
                        color =
                            if (miuixBackdrop != null) {
                                Color.Transparent
                            } else {
                                MiuixTheme.colorScheme.surfaceContainer
                            },
                    ) {
                        items.forEachIndexed { index, item ->
                            val selected = pagerState.currentPage == index
                            NavContentItem(
                                content = appearance.activeNavContent,
                                selected = selected,
                                onClick = { selectPage(index) },
                                icon = if (selected) item.selectedIcon else item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (miuixBackdrop != null) {
                            Modifier.layerBackdrop(miuixBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (liquidBackdrop != null) {
                            Modifier.liquidNavBackdropSource(liquidBackdrop)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            TopLevelPager(
                pagerState = pagerState,
                bottomPadding = innerPadding,
                lang = lang,
                iconHidden = iconHidden,
                swipeBackEnabled = appearance.swipeBack,
                hotReloadInProgress = hotReloadInProgress,
                onHotReload = {
                    if (!hotReloadInProgress) {
                        hotReloadInProgress = true
                        val accepted = onHotReload {
                            hotReloadInProgress = false
                        }
                        if (!accepted) {
                            hotReloadInProgress = false
                        }
                    }
                },
                onLangChange = onLangChange,
                onIconHiddenChange = onIconHiddenChange,
                onSwipeBackChange = onSwipeBackChange,
                previewState = previewState,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun TopLevelBackHandler(
    pagerState: PagerState,
    onBackToHome: () -> Unit,
) {
    val isBackEnabled by remember {
        derivedStateOf { pagerState.currentPage != 0 }
    }
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = isBackEnabled,
        onBackCompleted = onBackToHome,
    )
}

@Composable
private fun TopLevelPager(
    pagerState: PagerState,
    bottomPadding: PaddingValues,
    lang: AppLang,
    iconHidden: Boolean,
    swipeBackEnabled: Boolean,
    hotReloadInProgress: Boolean,
    onHotReload: () -> Unit,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    previewState: PreviewSandboxUiState,
    onNavigate: (AppRoute) -> Unit,
) {
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )

    HorizontalPager(
        state = pagerState,
        modifier =
            Modifier
                .fillMaxSize()
                .pagerGestureOverride(
                    pagerState = pagerState,
                    flingBehavior = flingBehavior,
                ),
        verticalAlignment = Alignment.Top,
        userScrollEnabled = false,
        flingBehavior = flingBehavior,
        pageNestedScrollConnection = PagerGestureNestedScrollConnection,
    ) { page ->
        val bottom = bottomPadding.calculateBottomPadding()
        when (page) {
            0 -> HomeScreen(
                bottomContentPadding = bottom,
                hotReloadInProgress = hotReloadInProgress,
                previewState = previewState,
                onHotReload = onHotReload,
                onOpenPreviewSandbox = { onNavigate(AppRoute.PreviewSandbox) },
            )
            1 -> FeaturesScreen(
                bottomContentPadding = bottom,
                onNavigate = onNavigate,
            )
            2 -> SettingsHubScreen(
                bottomContentPadding = bottom,
                lang = lang,
                iconHidden = iconHidden,
                swipeBackEnabled = swipeBackEnabled,
                onLangChange = onLangChange,
                onIconHiddenChange = onIconHiddenChange,
                onSwipeBackChange = onSwipeBackChange,
                onNavigate = onNavigate,
            )
        }
    }
}
