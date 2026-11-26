package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.glass.glassSearchBarTransition
import top.yukonga.miuix.kmp.glass.rememberGlassSearchBarAlpha
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TooltipBox
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.LocalGlassRailProgress
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassSearch
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassMotion
import com.chaners.guiyuan.ui.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import com.chaners.guiyuan.ui.glass.GlassIconButton
import top.yukonga.miuix.kmp.material.GlassShadows
import com.chaners.guiyuan.ui.glass.popupShadowOutsets

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

@OptIn(ExperimentalScrollBarApi::class)
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
    // A rotation invalidates row heights; search has separate list and cache owners.
    val orientation = LocalConfiguration.current.orientation
    val browsingIndicator = rememberStableLazyListScrollBarAdapter(browsingState, orientation)
    val resultsIndicator = rememberStableLazyListScrollBarAdapter(resultsState, search?.query to orientation)
    val filtering = search?.filtering == true
    val listState = if (filtering) resultsState else browsingState
    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val railProgress = LocalGlassRailProgress.current
    val density = LocalDensity.current
    val browsingScroll = MiuixScrollBehavior()
    val resultsScroll = MiuixScrollBehavior()
    val scrollBehavior = if (filtering) resultsScroll else browsingScroll
    val glassOverscroll = canScroll
    val onOverscrollChanged: (Float) -> Unit = remember(scrollBehavior) {
        { offset -> scrollBehavior.state.overscrollOffset = offset }
    }
    val topBarBackdrop = rememberTopBarBackdrop()
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(filtering, search?.query) {
        if (filtering) {
            resultsState.requestScrollToItem(0)
        }
    }
    val searchExpanded = search?.active == true
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
    // Search shadow follows its surface material transition; geometry has its own motion.
    val searchMaterial = animateFloatAsState(
        targetValue = if (listState.canScrollBackward || scrollBehavior.state.overscrollOffset < -1f) 1f else 0f,
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
            val ready = !search.active && !searchMotion.mounted
            TooltipBox(text = label, enabled = ready) {
                Box(
                    Modifier
                        .graphicsLayer {
                            alpha = barAlpha.value
                            outsets = searchActionOutsets
                        }
                        .then(if (ready) Modifier else Modifier.clearAndSetSemantics {}),
                ) {
                    GlassIconButton(onClick = { if (ready) { revealTitleOnExit = false; search.open() } }) {
                        Icon(
                            Os4GlassSearch,
                            contentDescription = label,
                        )
                    }
                }
            }
        }
    }
    val searchContent: @Composable () -> Unit = {
        if (search != null) {
            if (searchMotion.mounted) {
                Os4SearchMode(
                    motion = searchMotion,
                    expanded = search.active,
                    query = search.query,
                    backdrop = topBarBackdrop,
                    materialProgress = searchMaterial,
                    onQueryChange = search::update,
                    onSearch = { keyboardController?.hide() },
                    onClose = {
                        // close() clears the query immediately; record which title was covered.
                        revealTitleOnExit = search.filtering || browsingScroll.state.collapsedFraction >= 1f
                        search.close()
                    },
                )
            }
        }
    }
    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
    Scaffold(
        topBar = {
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
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            val contentPadding = pageContentPadding(
                innerPadding = paddingValues,
                outerBottomPadding = bottomContentPadding,
                extraBottom = 12.dp,
            )
            LazyColumn(
                state = listState,
                overscrollEffect = if (glassOverscroll) null else rememberOverscrollEffect(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = contentPadding,
                content = content,
            )
            VerticalScrollBar(
                adapter = if (filtering) resultsIndicator else browsingIndicator,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
                topAppBarScrollBehavior = scrollBehavior,
            )
        }

        overlay()
    }
    }
}
