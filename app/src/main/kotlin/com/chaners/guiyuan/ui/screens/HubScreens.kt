package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.glass.glassSearchBarTransition
import top.yukonga.miuix.kmp.glass.rememberGlassSearchBarAlpha
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.LocalGlassRailProgress
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassSearch
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.navigation.AppRoute
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassMotion
import com.chaners.guiyuan.ui.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import com.chaners.guiyuan.ui.glass.GlassIconButton
import top.yukonga.miuix.kmp.material.GlassShadows
import com.chaners.guiyuan.ui.glass.popupShadowOutsets
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SettingsHubSnapshot(
    val lang: AppLang,
    val iconHidden: Boolean,
    val swipeBackEnabled: Boolean,
    val runtimeBusy: Boolean,
)

internal data class SettingsHubActions(
    val onRestart: () -> Unit,
    val onLangChange: (AppLang) -> Unit,
    val onIconHiddenChange: (Boolean) -> Unit,
    val onSwipeBackChange: (Boolean) -> Unit,
    val onNavigate: (AppRoute) -> Unit,
)

@Composable
internal fun SettingsHubScreen(
    bottomContentPadding: Dp,
    snapshot: SettingsHubSnapshot,
    actions: SettingsHubActions,
) {
    val entryState = rememberAppSettingsEntryState(snapshot.runtimeBusy)
    HubPage(
        title = stringResource(R.string.settings_title),
        bottomContentPadding = bottomContentPadding,
        overlay = { AppSettingsEntryDialogs(entryState) },
    ) {
        Section(R.string.section_appearance_interaction) {
            ArrowPreference(
                title = stringResource(R.string.appearance_title),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_display_settings,
                    )
                },
                summary = stringResource(R.string.appearance_summary),
                onClick = { actions.onNavigate(AppRoute.Appearance) },
            )
            SwitchPreference(
                title = stringResource(R.string.swipe_back),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_swipe,
                    )
                },
                summary = stringResource(R.string.swipe_back_summary),
                checked = snapshot.swipeBackEnabled,
                onCheckedChange = actions.onSwipeBackChange,
            )
        }
        Section(R.string.section_app) {
            AppSettingsRows(
                snapshot.lang = snapshot.lang,
                snapshot.iconHidden = snapshot.iconHidden,
                actions.onLangChange = actions.onLangChange,
                actions.onIconHiddenChange = actions.onIconHiddenChange,
                entryState = entryState,
            )
        }
        settingsHubMaintenanceSection(entryState, actions.onRestart, actions.onNavigate)
    }
}

private fun LazyListScope.settingsHubMaintenanceSection(
    entryState: AppSettingsEntryState,
    onRestart: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    Section(R.string.section_diagnostics_maintenance) {
        ArrowPreference(
            title = stringResource(R.string.about_title),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_info,
                )
            },
            summary = stringResource(R.string.about_summary),
            onClick = { onNavigate(AppRoute.About) },
        )
        ArrowPreference(
            title = stringResource(R.string.diagnostics_title),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_troubleshoot_outlined,
                )
            },
            summary = stringResource(R.string.diagnostics_summary),
            onClick = { onNavigate(AppRoute.Diagnostics) },
        )
        BasicComponent(
            title = stringResource(R.string.restart_scope),
            startAction = {
                SemanticLeadingIcon(
                    iconRes = R.drawable.ic_material_symbol_restart_alt,
                    enabled = !entryState.busy,
                )
            },
            summary = stringResource(R.string.restart_scope_summary),
            enabled = !entryState.busy,
            onClick = onRestart,
        )
    }
}

@Composable
internal fun AnimatedPreferenceGroup(
    visible: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun HubPage(
    title: String,
    bottomContentPadding: Dp,
    search: PageSearch? = null,
    searchInNavigationBar: Boolean = false,
    overlay: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    // Browsing and filtered results must not overwrite each other's scroll position.
    val browsingState = rememberLazyListState()
    val resultsState = rememberLazyListState()
    val filtering = search?.filtering == true
    val listState = if (filtering) resultsState else browsingState
    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val os4 = LocalUiStyle.current == UiStyle.OS4
    val railProgress = LocalGlassRailProgress.current
    val density = LocalDensity.current
    val browsingScroll = MiuixScrollBehavior()
    val resultsScroll = if (os4) MiuixScrollBehavior() else browsingScroll
    val scrollBehavior = if (filtering) resultsScroll else browsingScroll
    val glassOverscroll = os4 && canScroll
    val onOverscrollChanged: (Float) -> Unit = remember(scrollBehavior) {
        { offset -> scrollBehavior.state.overscrollOffset = offset }
    }
    val topBarBackdrop = rememberTopBarBackdrop()
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(filtering, search?.query) {
        if (filtering) {
            if (os4) resultsState.requestScrollToItem(0)
            else resultsState.scrollToItem(0)
        }
    }
    val searchExpanded = os4 && search?.active == true
    val searchMotion = rememberOs4SearchMotion(searchExpanded)
    var revealTitleOnExit by remember { mutableStateOf(false) }
    // Navigation may open search directly; clear any title-reveal flag from its previous exit.
    LaunchedEffect(searchExpanded) { if (searchExpanded) revealTitleOnExit = false }
    val titleReveal = rememberOs4TitleReveal(
        expanded = searchExpanded,
        exitNeedsReveal = revealTitleOnExit,
        motion = searchMotion,
        collapsed = browsingScroll.state.collapsedFraction >= 1f,
    )
    val barAlpha = rememberGlassSearchBarAlpha(searchExpanded)
    val os3SearchActionAlpha by animateFloatAsState(
        targetValue = if (!os4 && search?.active == true) 0f else 1f,
        animationSpec = tween(150),
        label = "os3SearchAction",
    )
    // Only actual content overlap changes the surface; search geometry has its own clock.
    val searchShadow = GlassTopAppBarDefaults.collapseRamp(scrollBehavior)
    val searchMaterial = animateFloatAsState(
        targetValue = if (os4 && listState.canScrollBackward) 1f else 0f,
        animationSpec = GlassMotion.topBarButtonFloat(),
        label = "os4SearchMaterial",
    )
    // Match the upstream search transition on the entire action, not only the glyph.
    // Preserve the native Regular glass shadow's drawing outsets while it fades.
    val searchActionOutsets = remember(density) {
        with(density) { popupShadowOutsets(GlassShadows.Regular) }
    }
    val searchAction: @Composable RowScope.() -> Unit = {
        if (search != null && !searchInNavigationBar) {
            val label = stringResource(R.string.feature_search)
            val ready = !search.active && (!os4 || !searchMotion.mounted)
            TooltipBox(text = label, enabled = ready) {
                Box(
                    Modifier
                        .graphicsLayer {
                            alpha = if (os4) barAlpha.value else os3SearchActionAlpha
                            if (os4) outsets = searchActionOutsets
                        }
                        .then(if (ready) Modifier else Modifier.clearAndSetSemantics {}),
                ) {
                    if (os4) {
                        GlassIconButton(onClick = { if (ready) { revealTitleOnExit = false; search.open() } }) {
                            Icon(
                                Os4GlassSearch,
                                contentDescription = label,
                            )
                        }
                    } else IconButton(
                        onClick = search::open,
                        enabled = ready,
                    ) {
                        Icon(MiuixIcons.Search, contentDescription = label)
                    }
                }
            }
        }
    }
    val searchContent: @Composable () -> Unit = {
        if (search != null) {
            if (os4) {
                if (searchMotion.mounted) {
                    Os4SearchMode(
                        motion = searchMotion,
                        expanded = search.active,
                        query = search.query,
                        backdrop = topBarBackdrop,
                        materialProgress = searchMaterial,
                        shadowProgress = searchShadow,
                        onQueryChange = search::update,
                        onSearch = { keyboardController?.hide() },
                        onClose = {
                            // close() clears the query immediately; record which title was covered.
                            revealTitleOnExit = search.filtering || browsingScroll.state.collapsedFraction >= 1f
                            search.close()
                        },
                    )
                }
            } else {
                Os3HubSearch(search = search, onSearch = { keyboardController?.hide() })
            }
        }
    }
    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
    Scaffold(
        topBar = {
            if (os4) {
                Box(Modifier.fillMaxWidth()) {
                    GlassTopAppBar(
                        // The published transition owns foreground scale/blur/fade; the
                        // independently placed search action keeps its own opacity because
                        // the native title layout still clips glass button shadows.
                        contentModifier = Modifier.glassSearchBarTransition(
                            expanded = searchExpanded,
                            barAlpha = { barAlpha.value },
                        ),
                        progressiveBlur = LocalTopBarProgressiveBlur.current,
                        title = if (searchMotion.mounted && searchExpanded) "" else title,
                        titleAlpha = { titleReveal.value * railProgress() },
                        largeTitle = title,
                        compact = filtering,
                        isContentScrolled = listState.canScrollBackward,
                        backdrop = topBarBackdrop,
                        scrollBehavior = scrollBehavior,
                        actions = searchAction,
                        unclippedActions = search != null && !searchInNavigationBar,
                    )
                    if (search != null) {
                        // An overlaid search row keeps the bar's measured height stable.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding)
                                .padding(top = 4.dp)
                                .offset {
                                    val clearance = with(density) {
                                        (GlassNavigationRailDefaults.CollapsedTop +
                                            GlassNavigationRailDefaults.CollapsedHeight + 4.dp).toPx()
                                    }
                                    IntOffset(0, (clearance * (1f - railProgress())).roundToInt())
                                },
                        ) {
                            searchContent()
                        }
                    }
                }
            } else {
                MiuixBlurredTopBar(
                    backdrop = topBarBackdrop,
                    scrollBehavior = scrollBehavior,
                ) { barColor ->
                    TopAppBar(
                        title = title,
                        color = barColor,
                        scrollBehavior = scrollBehavior,
                        actions = searchAction,
                        bottomContent = searchContent,
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            LazyColumn(
                state = listState,
                overscrollEffect = if (glassOverscroll) null else rememberOverscrollEffect(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = pageContentPadding(
                    innerPadding = paddingValues,
                    outerBottomPadding = bottomContentPadding,
                    extraBottom = 12.dp,
                ),
                content = content,
            )
        }

        overlay()
    }
    }
}

@Composable
private fun Os3HubSearch(search: PageSearch, onSearch: () -> Unit) {
    AnimatedVisibility(
        visible = search.active,
        modifier = Modifier.fillMaxWidth(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            inputField = {
                InputField(
                    query = search.query,
                    onQueryChange = search::update,
                    onSearch = { onSearch() },
                    expanded = search.active,
                    onExpandedChange = { if (!it) search.close() },
                    label = stringResource(R.string.feature_search),
                )
            },
            expanded = search.active,
            onExpandedChange = { if (!it) search.close() },
            outsideEndAction = {
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 16.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = search::close,
                        ),
                    text = stringResource(R.string.cancel),
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                )
            },
            content = {},
        )
    }

}
