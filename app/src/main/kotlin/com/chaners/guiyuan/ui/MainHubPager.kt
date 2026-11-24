package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
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
internal fun TopLevelPager(bindings: TopLevelPagerBindings) {
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
