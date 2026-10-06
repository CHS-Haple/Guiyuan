package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLanguage
import com.chaners.guiyuan.settings.AppearanceSettings
import com.chaners.guiyuan.ui.components.FloatingNavItem
import com.chaners.guiyuan.ui.components.floatingNavigationMaterial
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

private data class WeightedNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
internal fun MainHub(
    settings: AppearanceSettings,
    darkMode: Boolean,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    onHotReload: (() -> Unit) -> Boolean,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    previewState: PreviewSandboxUiState,
    onNavigate: (AppRoute) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { TopLevelPageCount })
    val scope = rememberCoroutineScope()
    var hotReloadInProgress by remember { mutableStateOf(false) }
    val floatingMaterialActive =
        settings.floatingNavigationBarEnabled &&
            settings.floatingNavigationStyle.requiresTextureBackdrop &&
            isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop =
        if (floatingMaterialActive) {
            rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }

    val items = listOf(
        WeightedNavItem(
            label = stringResource(R.string.nav_home),
            icon = MiuixIcons.Normal.Home,
            selectedIcon = MiuixIcons.Medium.Home,
        ),
        WeightedNavItem(
            label = stringResource(R.string.nav_features),
            icon = MiuixIcons.Normal.Tune,
            selectedIcon = MiuixIcons.Medium.Tune,
        ),
        WeightedNavItem(
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
        if (backdrop != null) {
            Modifier.floatingNavigationMaterial(
                backdrop = backdrop,
                darkMode = darkMode,
                style = settings.floatingNavigationStyle,
            )
        } else {
            Modifier
        }

    Scaffold(
        bottomBar = {
            if (settings.floatingNavigationBarEnabled) {
                FloatingNavigationBar(
                    modifier = navigationBarModifier,
                    color =
                        if (backdrop != null) {
                            Color.Transparent
                        } else {
                            MiuixTheme.colorScheme.surfaceContainer
                        },
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = pagerState.currentPage == index
                        FloatingNavItem(
                            content = settings.floatingNavigationContent,
                            selected = selected,
                            onClick = { selectPage(index) },
                            icon = if (selected) item.selectedIcon else item.icon,
                            label = item.label,
                        )
                    }
                }
            } else {
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
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            TopLevelPager(
                pagerState = pagerState,
                bottomPadding = innerPadding,
                appLanguage = appLanguage,
                launcherIconHidden = launcherIconHidden,
                swipeBackEnabled = settings.swipeBackEnabled,
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
                onAppLanguageChange = onAppLanguageChange,
                onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                onSwipeBackEnabledChange = onSwipeBackEnabledChange,
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
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    swipeBackEnabled: Boolean,
    hotReloadInProgress: Boolean,
    onHotReload: () -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
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
                appLanguage = appLanguage,
                launcherIconHidden = launcherIconHidden,
                swipeBackEnabled = swipeBackEnabled,
                onAppLanguageChange = onAppLanguageChange,
                onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                onSwipeBackEnabledChange = onSwipeBackEnabledChange,
                onNavigate = onNavigate,
            )
        }
    }
}
