package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.offset
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.NavStyle
import com.chaners.guiyuan.system.ScopeRestart
import com.chaners.guiyuan.ui.components.floatingNavBlur
import com.chaners.guiyuan.ui.components.liquid.liquidRailSurface
import com.chaners.guiyuan.ui.components.liquid.liquidRailSelectedSurface
import com.chaners.guiyuan.ui.components.liquid.liquidNavBottomPadding
import com.chaners.guiyuan.ui.components.liquid.rememberLiquidNavBackdrop
import com.chaners.guiyuan.ui.components.requiresTextureBackdrop
import com.chaners.guiyuan.ui.screens.rememberPageSearch
import com.chaners.guiyuan.ui.glass.GlassNavigationRail
import com.chaners.guiyuan.ui.glass.GlassRailDestinations
import com.chaners.guiyuan.ui.glass.RailLiquidPaint
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import com.chaners.guiyuan.ui.glass.rememberGlassNavigationRailState
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
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
import kotlin.math.roundToInt

private const val TopLevelPageCount = 3

/** Read the shared rail morph in drawing/layout; non-rail routes remain fully visible. */
internal val LocalGlassRailProgress = staticCompositionLocalOf<() -> Float> { { 1f } }

@Composable
internal fun MainHub(
    inputs: MainHubInputs,
    callbacks: MainHubCallbacks,
) {
    val (appearance, dark, lang, iconHidden, previewState) = inputs
    val (onHotReload, onLangChange, onIconHiddenChange, onSwipeBackChange, onNavigate) = callbacks
    val pagerState = rememberPagerState(pageCount = { TopLevelPageCount })
    val featureSnackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pageSelection = remember(pagerState, scope) { HubPagerSelection(pagerState, scope) }
    // One search owner for both the Features page and its optional OS4 navigation action.
    val featureSearch = rememberPageSearch()
    LaunchedEffect(pagerState.currentPage) { pageSelection.syncActualPage() }
    val context = LocalContext.current
    val restartDialog = remember { RestartScopeDialogState() }
    val restartState by ScopeRestart.state.collectAsState()
    val restartInProgress = restartState is ScopeRestart.State.Running
    var scopeRequestInProgress by remember { mutableStateOf(false) }
    var scopeRequestFailed by remember { mutableStateOf(false) }
    val app = context.applicationContext as GyApplication
    val scopeBusy by app.scopeRequestInFlight.collectAsState(initial = app.scopeRequestActive)
    val hotReloadInProgress by app.hotReloadInFlight.collectAsState(initial = app.hotReloadActive)
    val onRestart = {
        restartDialog.openIfAllowed(
            app = app,
            blocked = restartInProgress || hotReloadInProgress || scopeRequestInProgress || scopeBusy,
        )
    }
    val os4Glass = appearance.navEnabled && appearance.navStyle == NavStyle.Glass
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
        if (isRuntimeShaderSupported()) {
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
    val railBackdrop = if (os4Glass) navBackdrop else liquidBackdrop
    // The Glass rail owns geometry; Liquid changes only its material.
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


    TopLevelBackHandler(
        selectedPage = pageSelection.selectedPage,
        onBackToHome = { pageSelection.select(0) },
    )

    val navigationBarModifier =
        if (navBackdrop != null && !os4Glass) {
            Modifier.floatingNavBlur(
                backdrop = navBackdrop,
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
                os4Glass && navBackdrop != null -> HubGlassBottomBar(
                    appearance, dark, items, pageSelection, navBackdrop, navigationAction, actionControls,
                )
                // Glass without a usable shader falls back to native MIUIX.
                (os4Glass && navBackdrop == null) || !appearance.navEnabled -> HubNativeBottomBar(
                    appearance, items, pageSelection, os4Glass,
                )

                liquidBackdrop != null -> HubLiquidBottomBar(
                    appearance, dark, items, pageSelection, liquidBackdrop, navigationAction, actionControls,
                    liquidBottomPadding,
                )

                else -> HubFloatingBottomBar(
                    appearance, items, pageSelection, navigationBarModifier, navBackdrop,
                )
            }
        },
    ) { innerPadding ->
        // The weighted page occupies a stable Compose slot even when the rail becomes visible
        // or disappears during rotation and split-screen resizing.
        Row(Modifier.fillMaxSize()) {
            if (useNativeRail) {
                // Native rail remains available for ordinary navigation and missing Glass material.
                HubNativeRail(
                    state = nativeRailState,
                    items = items,
                    selectedIndex = pageSelection.selectedPage,
                    onSelect = pageSelection::select,
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
                    onSelect = pageSelection::select,
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
        CompositionLocalProvider(LocalGlassBackdrop provides miuixBackdrop) {
        RestartScopeDialog(state = restartDialog, app = app, restartInProgress = restartInProgress)

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

        RestartFailureDialog(restartState)

        }
    }
}
