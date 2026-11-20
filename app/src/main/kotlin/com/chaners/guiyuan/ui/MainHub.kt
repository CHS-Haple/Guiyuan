package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.settings.NavContent
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.system.RestartTarget
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.ui.components.NavContentItem
import com.chaners.guiyuan.ui.components.floatingNavMaterial
import com.chaners.guiyuan.ui.components.navTouchExtension
import com.chaners.guiyuan.ui.components.measuredNavTouchExtension
import com.chaners.guiyuan.ui.components.liquid.LiquidNavBar
import com.chaners.guiyuan.ui.components.liquid.LiquidNavEntry
import com.chaners.guiyuan.ui.components.liquid.LiquidNavSpec
import com.chaners.guiyuan.ui.components.liquid.LiquidNavPressLink
import com.chaners.guiyuan.ui.components.liquid.liquidRailSurface
import com.chaners.guiyuan.ui.components.liquid.liquidRailSelectedSurface
import com.chaners.guiyuan.ui.components.liquid.liquidNavBottomPadding
import com.chaners.guiyuan.ui.components.liquid.rememberLiquidNavBackdrop
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.FeaturesScreen
import com.chaners.guiyuan.ui.screens.PageSearch
import com.chaners.guiyuan.ui.screens.rememberPageSearch
import com.chaners.guiyuan.ui.screens.HomeScreen
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.SettingsHubScreen
import com.chaners.guiyuan.ui.screens.SettingsHubSnapshot
import com.chaners.guiyuan.ui.screens.SettingsHubActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import com.chaners.guiyuan.ui.glass.GlassNavigationBar
import com.chaners.guiyuan.ui.glass.GlassNavigationBarLayout
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import com.chaners.guiyuan.ui.glass.GlassNavigationRail
import com.chaners.guiyuan.ui.glass.GlassRailDestinations
import com.chaners.guiyuan.ui.glass.RailLiquidPaint
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import com.chaners.guiyuan.ui.glass.rememberGlassNavigationRailState
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailState
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
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
import kotlin.math.roundToInt

private const val TopLevelPageCount = 3

/** Read the shared rail morph in drawing/layout; non-rail routes remain fully visible. */
internal val LocalGlassRailProgress = staticCompositionLocalOf<() -> Float> { { 1f } }

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

/**
 * Follows MIUIX's main-pager example: commit a destination before page motion begins.
 * This owner writes selection, while PagerState owns the actual scroll position.
 */
private class HubPagerSelection(
    private val pagerState: PagerState,
    private val scope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    private var navigationJob: Job? = null

    fun select(index: Int) {
        if (index !in 0 until pagerState.pageCount || index == selectedPage) return
        navigationJob?.cancel()
        selectedPage = index

        navigationJob = scope.launch {
            val activeJob = coroutineContext.job
            try {
                pagerState.springAnimateToPage(index)
            } finally {
                if (navigationJob === activeJob) {
                    navigationJob = null
                    selectedPage = pagerState.currentPage
                }
            }
        }
    }

    fun syncActualPage() {
        if (navigationJob == null && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

private data class MainNavItem(
    val label: String,
    val icon: ImageVector,
)

/** Map app destinations to the MIUIX-native rail; no parallel press or selected lens. */
@Composable
private fun HubNativeRail(
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


@Composable
internal fun MainHub(
    appearance: Appearance,
    dark: Boolean,
    lang: AppLang,
    iconHidden: Boolean,
    onHotReload: () -> Unit,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    previewState: PreviewSandboxUiState,
    onNavigate: (AppRoute) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { TopLevelPageCount })
    val featureSnackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pageSelection = remember(pagerState, scope) { HubPagerSelection(pagerState, scope) }
    // One search owner for both the Features page and its optional OS4 navigation action.
    val featureSearch = rememberPageSearch()
    LaunchedEffect(pagerState.currentPage) { pageSelection.syncActualPage() }
    val context = LocalContext.current
    var restartPickerOpen by remember { mutableStateOf(false) }
    val restartState by ScopeRestart.state.collectAsState()
    val restartInProgress = restartState is ScopeRestart.State.Running
    var scopeRequestInProgress by remember { mutableStateOf(false) }
    var scopeRequestFailed by remember { mutableStateOf(false) }
    var availableTargets by remember { mutableStateOf(emptySet<RestartTarget>()) }
    var selectedTargets by remember { mutableStateOf(emptySet<RestartTarget>()) }
    var queuedRestart by remember { mutableStateOf(emptySet<RestartTarget>()) }
    val failedTargets = (restartState as? ScopeRestart.State.Failed)?.targets
    var failedTargetsForExit by remember { mutableStateOf(emptySet<RestartTarget>()) }
    if (failedTargets != null) {
        SideEffect { failedTargetsForExit = failedTargets }
    }
    val app = context.applicationContext as GyApplication
    val scopeBusy by app.scopeRequestInFlight.collectAsState(initial = app.scopeRequestActive)
    val hotReloadInProgress by app.hotReloadInFlight.collectAsState(initial = app.hotReloadActive)
    val onRestart = {
        if (!restartInProgress && !hotReloadInProgress && !scopeRequestInProgress &&
            !scopeBusy && app.canRestartScopes()
        ) {
            val scopes = app.enabledScopes()
            availableTargets = RestartTarget.entries.filterTo(mutableSetOf()) {
                scopes != null && it.pkg in scopes
            }
            selectedTargets =
                if (RestartTarget.SYS_UI in availableTargets) setOf(RestartTarget.SYS_UI)
                else availableTargets
            queuedRestart = emptySet()
            restartPickerOpen = true
        }
    }
    val os4 = appearance.uiStyle == UiStyle.OS4
    val os4Glass = os4 && appearance.navEnabled && appearance.navStyle == NavStyle.Glass
    val miuixMaterialActive =
        appearance.navEnabled &&
            appearance.navStyle.requiresTextureBackdrop &&
            isRuntimeShaderSupported()
    val liquidMaterialActive =
        appearance.navEnabled &&
            appearance.navStyle == NavStyle.Liquid &&
            isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    // The root dialog needs a clean page source even with native/hidden navigation.
    val miuixBackdrop =
        if (miuixMaterialActive || (os4 && isRuntimeShaderSupported())) {
            rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }
        } else {
            null
        }
    val navBackdrop = miuixBackdrop.takeIf { miuixMaterialActive }
    // Reserve the rail's physical panel width and margins plus a usable phone-width page.
    // Use the actual window, not orientation or device classification, for foldable/split-screen.
    val railSafePadding = WindowInsets.systemBars
        .union(WindowInsets.captionBar)
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
    val railStartInset = railSafePadding.calculateStartPadding(LocalLayoutDirection.current)
    val railEndInset = railSafePadding.calculateEndPadding(LocalLayoutDirection.current)
    val railTopInset = railSafePadding.calculateTopPadding()
    val railBottomInset = railSafePadding.calculateBottomPadding()
    val railVerticalInsets = railTopInset + railBottomInset
    val railContentWidth = GlassNavigationRailDefaults.ExpandedWidth +
        GlassNavigationRailDefaults.Margin * 2 + railStartInset
    val liquidBackdrop =
        if (liquidMaterialActive) rememberLiquidNavBackdrop(surfaceColor) else null
    val railBackdrop = if (os4Glass) navBackdrop else if (os4) liquidBackdrop else null
    // The UI style owns geometry. Liquid changes only the material of that style's rail.
    val useGlassRail = railBackdrop != null && canPlaceGlassRail(
        LocalWindowInfo.current.containerDpSize, railStartInset, railEndInset, railVerticalInsets,
    )
    val useNativeRail = useNativeRailFor(
        appearance = appearance,
        glassBackdropAvailable = navBackdrop != null || liquidBackdrop != null,
        window = LocalWindowInfo.current.containerDpSize,
        safePadding = railSafePadding,
        direction = LocalLayoutDirection.current,
    )
    val actionInRail = hasOs4RailActionSlot(appearance, useGlassRail)
    val actionInBottomNav = hasOs4NavigationActionSlot(
        appearance = appearance,
        glassBackdropAvailable = navBackdrop != null,
        liquidBackdropAvailable = liquidBackdrop != null,
        glassRailActive = useGlassRail,
        nativeRailActive = useNativeRail,
    )
    val navigationActionSlot = actionInBottomNav || actionInRail
    val navigationAction = navigationActionFor(
        slotAvailable = navigationActionSlot,
        selectedPage = pageSelection.selectedPage,
        searchActive = featureSearch.active,
    )
    val hotReloadEnabled = !restartInProgress && !scopeRequestInProgress && !scopeBusy
    val requestHotReload: () -> Unit = {
        if (hotReloadEnabled && !hotReloadInProgress) onHotReload()
    }
    val actionControls = NavigationActionControls(
        hotReloadEnabled,
        hotReloadInProgress,
        requestHotReload,
        featureSearch::open,
    )
    val nativeRailState = rememberNavigationRailState()
    val railState = rememberGlassNavigationRailState()
    val railProgress = remember(useGlassRail, railState) {
        { if (useGlassRail) railState.progress else 1f }
    }

    val homeLabel = stringResource(R.string.nav_home)
    val featuresLabel = stringResource(R.string.nav_features)
    val settingsLabel = stringResource(R.string.nav_settings)
    val items = remember(homeLabel, featuresLabel, settingsLabel) {
        listOf(
            MainNavItem(homeLabel, MiuixIcons.Normal.Home),
            MainNavItem(featuresLabel, MiuixIcons.Normal.Tune),
            MainNavItem(settingsLabel, MiuixIcons.Normal.Settings),
        )
    }

    fun selectPage(index: Int) = pageSelection.select(index)

    TopLevelBackHandler(
        selectedPage = pageSelection.selectedPage,
        onBackToHome = { selectPage(0) },
    )

    val navigationBarModifier =
        if (navBackdrop != null && !os4Glass) {
            Modifier.floatingNavMaterial(
                backdrop = navBackdrop,
                dark = dark,
                style = appearance.navStyle,
            )
        } else {
            Modifier
        }
    val liquidBottomPadding = liquidNavBottomPadding()
    Scaffold(
        // This Scaffold owns the bottom bar, so it must also position the snackbar.
        snackbarHost = { SnackbarHost(state = featureSnackbarState) },
        bottomBar = {
            when {
                useGlassRail || useNativeRail -> Unit
                os4Glass && navBackdrop != null -> {
                    val selectedPage = pageSelection.selectedPage
                    val includeLabels = appearance.activeNavContent == NavContent.IconAndText
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
                        onSelect = ::selectPage,
                        backdrop = navBackdrop,
                        touchExtension = appearance.navTouchExtension,
                        // Exclude the measured 54dp action and 12dp native MIUIX gap.
                        trailingActionTouchExclusion = if (navigationAction != null) {
                            GlassNavigationBarDefaults.Height + GlassNavigationBarDefaults.ActionSpacing
                        } else 0.dp,
                        trailingActionTouchSize = if (navigationAction != null) {
                            GlassNavigationBarDefaults.Height
                        } else 0.dp,
                        onTrailingActionTouch = navigationAction?.let { action ->
                            if (action == NavigationAction.HotReload) actionControls.onHotReload
                            else actionControls.onSearch
                        },
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
                // OS4 without a usable shader must fall back to native MIUIX,
                // not resurrect a previously saved OS3 floating-bar style.
                (os4Glass && navBackdrop == null) || !appearance.navEnabled -> {
                    NavigationBar(
                        mode = if (os4Glass && appearance.activeNavContent == NavContent.IconOnly) {
                            NavigationBarDisplayMode.IconOnly
                        } else {
                            NavigationBarDisplayMode.IconAndText
                        },
                    ) {
                        items.forEachIndexed { index, item ->
                            val selected = pageSelection.selectedPage == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectPage(index) },
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                }

                liquidBackdrop != null -> {
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
                                            bottomPadding = liquidBottomPadding,
                                            horizontalPadding = LiquidNavSpec.sidePadding,
                                            contentPadding = 4.dp,
                                            itemCount = items.size,
                                            rtl = LocalLayoutDirection.current == LayoutDirection.Rtl,
                                            onSelect = ::selectPage,
                                            trailingActionWidth = if (navigationAction != null) {
                                                LiquidNavSpec.height + GlassNavigationBarDefaults.ActionSpacing
                                            } else 0.dp,
                                            trailingActionSize = if (navigationAction != null) {
                                                LiquidNavSpec.height
                                            } else 0.dp,
                                            onTrailingAction = navigationAction?.let { action ->
                                                if (action == NavigationAction.HotReload) actionControls.onHotReload
                                                else actionControls.onSearch
                                            },
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
                                onSelected = ::selectPage,
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
                                        contentMode = appearance.activeNavContent,
                                        selected = pageSelection.selectedPage == index,
                                        onClick = { selectPage(index) },
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

                else -> {
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
                                            onSelect = ::selectPage,
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
                                    content = appearance.activeNavContent,
                                    selected = selected,
                                    onClick = { selectPage(index) },
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
            }
        },
    ) { innerPadding ->
        // The weighted page occupies a stable Compose slot even when the rail becomes visible
        // or disappears during rotation and split-screen resizing.
        Row(Modifier.fillMaxSize()) {
            if (useNativeRail) {
                // OS3 wide Liquid follows the native rail, not a second lens/press layer.
                HubNativeRail(
                    state = nativeRailState,
                    items = items,
                    selectedIndex = pageSelection.selectedPage,
                    onSelect = ::selectPage,
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
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
                        if (liquidBackdrop != null && !useNativeRail) {
                            Modifier.layerBackdrop(liquidBackdrop)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().then(
                    if (useGlassRail) {
                        Modifier.layout { measurable, constraints ->
                            val start = (railContentWidth.toPx() * railState.progress)
                                .roundToInt().coerceIn(0, constraints.maxWidth)
                            val page = measurable.measure(constraints.offset(horizontal = -start))
                            layout(constraints.maxWidth, page.height) {
                                page.placeRelative(start, 0)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            ) {
            CompositionLocalProvider(LocalGlassRailProgress provides railProgress) {
            TopLevelPager(
                bindings = TopLevelPagerBindings(
                    scope = TopLevelPagerScope(
                        pagerState = pagerState,
                        bottomPadding = innerPadding,
                        snackbar = featureSnackbarState,
                        selectedPage = pageSelection.selectedPage,
                        search = TopLevelPageSearch(featureSearch, navigationActionSlot),
                    ),
                    user = TopLevelPagerUser(
                        lang = lang,
                        iconHidden = iconHidden,
                        swipeBackEnabled = appearance.swipeBack,
                        preview = previewState,
                    ),
                    runtime = TopLevelPagerRuntime(
                        hotReloadInProgress = hotReloadInProgress,
                        restartInProgress = restartInProgress,
                        scopeRequestInProgress = scopeRequestInProgress || scopeBusy,
                        hotReloadInNavigation = navigationActionSlot,
                    ),
                    runtimeActions = TopLevelPagerRuntimeActions(
                        restart = onRestart,
                        requestScope = {
                            if (!scopeRequestInProgress && !restartInProgress && !hotReloadInProgress &&
                                !scopeBusy
                            ) {
                                scopeRequestInProgress = true
                                val accepted =
                                    (context.applicationContext as? GyApplication)?.requestSysUiScope { granted ->
                                        scopeRequestInProgress = false
                                        if (!granted) scopeRequestFailed = true
                                    } ?: false
                                if (!accepted) {
                                    scopeRequestInProgress = false
                                    scopeRequestFailed = true
                                }
                            }
                        },
                        hotReload = requestHotReload,
                    ),
                    settingsActions = TopLevelPagerSettingsActions(
                        changeLang = onLangChange,
                        changeIconHidden = onIconHiddenChange,
                        changeSwipeBack = onSwipeBackChange,
                        navigate = onNavigate,
                    ),
                ),
            )
            }
            }
        }
        // The rail is not recorded into the backdrop it samples.
        if (useGlassRail) {
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
                    onSelect = ::selectPage,
                ),
                backdrop = railBackdrop,
                state = railState,
                showToggle = true,
                trailingAction = if (actionInRail) navigationAction?.let { action ->
                    {
                        railBackdrop?.let { backdrop ->
                            NavigationTrailingAction(
                                action = action,
                                surface = NavigationActionSurface(
                                    backdrop,
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
            }
        }

        // Popups are hosted outside this page's backdrop recording.
        CompositionLocalProvider(LocalGlassBackdrop provides if (os4) miuixBackdrop else null) {
        UiOverlayDialog(
            title = stringResource(R.string.restart_scope),
            summary = stringResource(
                if (availableTargets.isEmpty()) R.string.restart_scope_no_targets
                else R.string.restart_scope_dialog_summary,
            ),
            show = restartPickerOpen,
            onDismissRequest = {
                queuedRestart = emptySet()
                restartPickerOpen = false
            },
            onDismissFinished = {
                if (queuedRestart.isNotEmpty() && !restartInProgress) {
                    val targets = queuedRestart
                    queuedRestart = emptySet()
                    // Scope may have changed in the framework while the dialog was open.
                    val liveScopes = app.enabledScopes().orEmpty()
                    if (targets.any { it.pkg !in liveScopes }) {
                        availableTargets = RestartTarget.entries.filterTo(mutableSetOf()) {
                            it.pkg in liveScopes
                        }
                        selectedTargets = targets intersect availableTargets
                        restartPickerOpen = true
                    } else {
                        if (!app.restartScopes(targets)) {
                            restartPickerOpen = true
                        }
                    }
                }
            },
        ) {
            RestartScopeSelection(
                availableTargets = availableTargets,
                selectedTargets = selectedTargets,
                onSelectionChange = { selectedTargets = it },
                onClose = { restartPickerOpen = false },
                onRestart = {
                    queuedRestart = selectedTargets intersect availableTargets
                    restartPickerOpen = false
                },
            )
        }

        UiOverlayDialog(
            title = stringResource(R.string.home_scope_request_failed),
            summary = stringResource(R.string.home_scope_request_failed_summary),
            show = scopeRequestFailed,
            onDismissRequest = { scopeRequestFailed = false },
        ) {
            UiDialogButton(
                text = stringResource(R.string.confirm),
                modifier = Modifier.fillMaxWidth(),
                role = GlassDialogButtonRole.Primary,
                onClick = { scopeRequestFailed = false },
            )
        }

        UiOverlayDialog(
            title = stringResource(R.string.restart_scope_failed),
            summary = stringResource(
                R.string.restart_scope_failed_summary,
                (failedTargets ?: failedTargetsForExit)
                    .map {
                        when (it) {
                            RestartTarget.SYS_UI -> context.getString(R.string.restart_scope_sysui)
                            RestartTarget.SETTINGS -> context.getString(R.string.restart_scope_settings)
                        }
                    }
                    .joinToString(", "),
            ),
            show = restartState is ScopeRestart.State.Failed,
            onDismissRequest = ScopeRestart::clearFailure,
            onDismissFinished = { failedTargetsForExit = emptySet() },
        ) {
            UiDialogButton(
                text = stringResource(R.string.confirm),
                modifier = Modifier.fillMaxWidth(),
                role = GlassDialogButtonRole.Primary,
                onClick = ScopeRestart::clearFailure,
            )
        }
        }
    }
}

@Composable
private fun TopLevelBackHandler(
    selectedPage: Int,
    onBackToHome: () -> Unit,
) {
    val isBackEnabled = selectedPage != 0
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = isBackEnabled,
        onBackCompleted = onBackToHome,
    )
}

@Composable
private fun TopLevelPager(bindings: TopLevelPagerBindings) {
    val pagerState = bindings.scope.pagerState
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().pagerGestureOverride(
            pagerState = pagerState,
            flingBehavior = flingBehavior,
        ),
        verticalAlignment = Alignment.Top,
        userScrollEnabled = false,
        flingBehavior = flingBehavior,
        pageNestedScrollConnection = PagerGestureNestedScrollConnection,
    ) { page ->
        TopLevelPagerContent(
            page = page,
            bottom = bindings.scope.bottomPadding.calculateBottomPadding(),
            bindings = bindings,
        )
    }
}

@Composable
private fun TopLevelPagerContent(
    page: Int,
    bottom: Dp,
    bindings: TopLevelPagerBindings,
) {
    val scope = bindings.scope
    val user = bindings.user
    val runtime = bindings.runtime
    val actions = bindings.runtimeActions
    val settings = bindings.settingsActions
    when (page) {
        0 -> HomeScreen(
            bottomContentPadding = bottom,
            hotReloadInProgress = runtime.hotReloadInProgress,
            restartInProgress = runtime.restartInProgress,
            scopeRequestInProgress = runtime.scopeRequestInProgress,
            onRestart = actions.restart,
            onRequestScope = actions.requestScope,
            previewState = user.preview,
            onHotReload = actions.hotReload,
            hotReloadInNavigation = runtime.hotReloadInNavigation,
            onOpenPreviewSandbox = { settings.navigate(AppRoute.PreviewSandbox) },
        )
        1 -> FeaturesScreen(
            bottomContentPadding = bottom,
            visible = scope.selectedPage == 1,
            snackbarState = scope.snackbar,
            search = scope.search.state,
            searchInNavigationBar = scope.search.inNavigationBar,
        )
        2 -> SettingsHubScreen(
            bottomContentPadding = bottom,
            snapshot = SettingsHubSnapshot(
                lang = user.lang,
                iconHidden = user.iconHidden,
                swipeBackEnabled = user.swipeBackEnabled,
                runtimeBusy = runtime.restartInProgress ||
                    runtime.hotReloadInProgress || runtime.scopeRequestInProgress,
            ),
            actions = SettingsHubActions(
                onRestart = actions.restart,
                onLangChange = settings.changeLang,
                onIconHiddenChange = settings.changeIconHidden,
                onSwipeBackChange = settings.changeSwipeBack,
                onNavigate = settings.navigate,
            ),
        )
    }
}
