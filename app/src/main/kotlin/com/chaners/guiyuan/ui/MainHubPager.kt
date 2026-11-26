package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.FeaturesScreen
import com.chaners.guiyuan.ui.screens.HomeScreen
import com.chaners.guiyuan.ui.screens.HomeScreenActions
import com.chaners.guiyuan.ui.screens.HomeScreenLayout
import com.chaners.guiyuan.ui.screens.HomeScreenRuntime
import com.chaners.guiyuan.ui.screens.SettingsHubActions
import com.chaners.guiyuan.ui.screens.SettingsHubScreen
import com.chaners.guiyuan.ui.screens.SettingsHubSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/**
 * Follows MIUIX's main-pager example: commit a destination before page motion begins.
 * This owner writes selection, while PagerState owns the actual scroll position.
 */
internal class HubPagerSelection(
    private val pagerState: PagerState,
    private val scope: CoroutineScope,
    private val snapTransitions: () -> Boolean,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    private var navigationJob: Job? = null

    fun select(index: Int) {
        if (index !in 0 until pagerState.pageCount || index == selectedPage) return
        navigationJob?.cancel()
        selectedPage = index

        val job = scope.launch(start = CoroutineStart.LAZY) {
            val activeJob = coroutineContext.job
            try {
                if (snapTransitions()) pagerState.scrollToPage(index)
                else pagerState.springAnimateToPage(index)
            } finally {
                if (navigationJob === activeJob) {
                    navigationJob = null
                    selectedPage = pagerState.currentPage
                }
            }
        }
        navigationJob = job
        job.start()
    }

    fun syncActualPage() {
        if (navigationJob == null && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
internal fun TopLevelBackHandler(
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
internal fun TopLevelPager(
    bindings: TopLevelPagerBindings,
    railWidth: Dp,
    railProgress: () -> Float,
) {
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
        val hidden = remember(pagerState, page) {
            derivedStateOf {
                abs(page - pagerState.currentPage - pagerState.currentPageOffsetFraction) >= 1f
            }
        }
        HubRailPage(railWidth, railProgress, hidden) {
            TopLevelPagerContent(
                page = page,
                bottom = bindings.scope.bottomPadding.calculateBottomPadding(),
                bindings = bindings,
            )
        }
    }
}

// As in MIUIX's AppContent/PageUtils example, the pager host stays stable and
// completely offscreen pages retain their last rail clearance. Keep Guiyuan's
// existing physical inset contract; only the active page follows the morph.
@Composable
private fun HubRailPage(
    railWidth: Dp,
    railProgress: () -> Float,
    hidden: State<Boolean>,
    content: @Composable () -> Unit,
) {
    val pageProgress = remember(railProgress, hidden) {
        var last: Float? = null
        val readProgress: () -> Float = {
            val held = last
            if (hidden.value && held != null) held else railProgress().also { last = it }
        }
        readProgress
    }
    val clearance = if (railWidth > 0.dp) {
        Modifier.layout { measurable, constraints ->
            val start = (railWidth.toPx() * pageProgress())
                .roundToInt().coerceIn(0, constraints.maxWidth)
            val page = measurable.measure(constraints.offset(horizontal = -start))
            layout(constraints.maxWidth, page.height) { page.placeRelative(start, 0) }
        }
    } else Modifier
    CompositionLocalProvider(LocalGlassRailProgress provides pageProgress) {
        Box(Modifier.fillMaxSize().then(clearance)) { content() }
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
            layout = HomeScreenLayout(bottomContentPadding = bottom, previewState = user.preview),
            runtime = HomeScreenRuntime(
                hotReloadInProgress = runtime.hotReloadInProgress,
                restartInProgress = runtime.restartInProgress,
                scopeRequestInProgress = runtime.scopeRequestInProgress,
                hotReloadInNavigation = runtime.hotReloadInNavigation,
            ),
            actions = HomeScreenActions(
                onRestart = actions.restart,
                onRequestScope = actions.requestScope,
                onHotReload = actions.hotReload,
                onOpenPreviewSandbox = { settings.navigate(AppRoute.PreviewSandbox) },
            ),
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
