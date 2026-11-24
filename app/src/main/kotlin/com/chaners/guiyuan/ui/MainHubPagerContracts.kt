package com.chaners.guiyuan.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.PagerState
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.PageSearch
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import top.yukonga.miuix.kmp.basic.SnackbarHostState

internal data class TopLevelPageSearch(
    val state: PageSearch,
    val inNavigationBar: Boolean,
)

internal data class TopLevelPagerScope(
    val pagerState: PagerState,
    val bottomPadding: PaddingValues,
    val snackbar: SnackbarHostState,
    val selectedPage: Int,
    val search: TopLevelPageSearch,
)

internal data class TopLevelPagerUser(
    val lang: AppLang,
    val iconHidden: Boolean,
    val swipeBackEnabled: Boolean,
    val preview: PreviewSandboxUiState,
)

internal data class TopLevelPagerRuntime(
    val hotReloadInProgress: Boolean,
    val restartInProgress: Boolean,
    val scopeRequestInProgress: Boolean,
    val hotReloadInNavigation: Boolean,
)

internal data class TopLevelPagerRuntimeActions(
    val restart: () -> Unit,
    val requestScope: () -> Unit,
    val hotReload: () -> Unit,
)

internal data class TopLevelPagerSettingsActions(
    val changeLang: (AppLang) -> Unit,
    val changeIconHidden: (Boolean) -> Unit,
    val changeSwipeBack: (Boolean) -> Unit,
    val navigate: (AppRoute) -> Unit,
)

internal data class TopLevelPagerBindings(
    val scope: TopLevelPagerScope,
    val user: TopLevelPagerUser,
    val runtime: TopLevelPagerRuntime,
    val runtimeActions: TopLevelPagerRuntimeActions,
    val settingsActions: TopLevelPagerSettingsActions,
)
