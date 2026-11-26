package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.ScrollState
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.LocalTopBarProgressiveBlur
import com.chaners.guiyuan.ui.LocalGlassRailProgress
import com.chaners.guiyuan.ui.components.UiTopBarNavigationIcon
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.RefreshState
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

internal data class SettingsPullToRefresh(
    val refreshing: Boolean,
    val onRefresh: () -> Unit,
    val texts: List<String>,
)

@OptIn(ExperimentalScrollBarApi::class)
@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    navigationActions: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    glassActions: Boolean = false,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    titleAlpha: Float = 1f,
    keepCompactTitle: Boolean = false,
    userScrollEnabled: Boolean = true,
    listState: LazyListState? = null,
    pullToRefresh: SettingsPullToRefresh? = null,
    background: @Composable BoxScope.(PaddingValues) -> Unit = {},
    foreground: @Composable BoxScope.(PaddingValues) -> Unit = {},
    overlay: @Composable () -> Unit = {},
    showLazyScrollbar: Boolean = false,
    columnState: ScrollState? = null,
    scrollContent: (@Composable ColumnScope.(PaddingValues) -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {},
) {
    val scrollBehavior = MiuixScrollBehavior()
    val resolvedListState = if (scrollContent == null) listState ?: rememberLazyListState() else null
    val scrollState = if (scrollContent != null) columnState ?: rememberScrollState() else null
    val topBarBackdrop = rememberTopBarBackdrop()
    val railProgress = LocalGlassRailProgress.current
    val onOverscrollChanged: (Float) -> Unit = remember(scrollBehavior) {
        { offset -> scrollBehavior.state.overscrollOffset = offset }
    }
    val navigationIcon: @Composable () -> Unit = {
        UiTopBarNavigationIcon(onBack = onBack, navigationActions = navigationActions)
    }

    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
    Scaffold(
        snackbarHost = snackbarHost,
        topBar = {
            GlassTopAppBar(
                progressiveBlur = LocalTopBarProgressiveBlur.current,
                isContentScrolled = scrollState?.canScrollBackward ?: (resolvedListState?.canScrollBackward == true),
                title = title,
                expandable = !keepCompactTitle,
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
                titleAlpha = { titleAlpha * railProgress() },
                titlePadding = titlePadding,
                navigationIcon = navigationIcon,
                actions = {
                    if (glassActions) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            actions()
                        }
                    } else {
                        actions()
                    }
                },
                unclippedNavigationIcon = true,
                unclippedActions = glassActions,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            val contentPadding =
                pageContentPadding(
                    innerPadding = paddingValues,
                    extraBottom = 12.dp,
                )
            background(contentPadding)

            @Composable
            fun SettingsList(allowEdgeHaptic: Boolean = true) {
                val lazyState = requireNotNull(resolvedListState)
                val canScroll = lazyState.canScrollBackward || lazyState.canScrollForward
                val glassOverscroll = pullToRefresh == null && userScrollEnabled && canScroll
                LazyColumn(
                    state = lazyState,
                    userScrollEnabled = userScrollEnabled,
                    overscrollEffect = if (glassOverscroll) null else rememberOverscrollEffect(),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(if (allowEdgeHaptic && canScroll) Modifier.scrollEndHaptic() else Modifier)
                            .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = contentPadding,
                    content = content,
                )
            }

            if (scrollState != null && scrollContent != null) {
                val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
                val glassOverscroll = userScrollEnabled && canScroll
                Column(
                    modifier = Modifier.fillMaxSize()
                        .then(if (glassOverscroll) Modifier.scrollEndHaptic() else Modifier)
                        .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(scrollState, enabled = userScrollEnabled, overscrollEffect = if (glassOverscroll) null else rememberOverscrollEffect())
                        .padding(contentPadding),
                    content = { scrollContent(contentPadding) },
                )
            } else if (pullToRefresh != null) {
                val pullState = rememberPullToRefreshState()
                PullToRefresh(
                    isRefreshing = pullToRefresh.refreshing,
                    onRefresh = pullToRefresh.onRefresh,
                    pullToRefreshState = pullState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    topAppBarScrollBehavior = scrollBehavior,
                    refreshTexts = pullToRefresh.texts,
                ) {
                    SettingsList(allowEdgeHaptic = pullState.refreshState == RefreshState.Idle)
                }
            } else {
                SettingsList()
            }
            foreground(contentPadding)
            val indicatorState: ScrollableState? = scrollState ?: resolvedListState?.takeIf { showLazyScrollbar }
            if (userScrollEnabled && indicatorState != null) {
                VerticalScrollBar(
                    adapter = rememberScrollBarAdapter(indicatorState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    trackPadding = contentPadding,
                    topAppBarScrollBehavior = scrollBehavior,
                )
            }
        }

        overlay()
    }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.Section(
    @StringRes titleRes: Int? = null,
    key: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    item(key = key) { PreferenceSection(titleRes, content) }
}

@Composable
internal fun PreferenceSection(
    @StringRes titleRes: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    titleRes?.let { SmallTitle(stringResource(it)) }
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        content = content,
    )
}
