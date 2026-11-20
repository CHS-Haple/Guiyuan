package com.chaners.guiyuan.ui

import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.Appearance
import com.chaners.guiyuan.ui.navigation.AppRoute
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState

/** Read-only parent values; MainHub retains ownership of navigation and runtime leases. */
internal data class MainHubInputs(
    val appearance: Appearance,
    val dark: Boolean,
    val lang: AppLang,
    val iconHidden: Boolean,
    val previewState: PreviewSandboxUiState,
)

internal data class MainHubCallbacks(
    val onHotReload: () -> Unit,
    val onLangChange: (AppLang) -> Unit,
    val onIconHiddenChange: (Boolean) -> Unit,
    val onSwipeBackChange: (Boolean) -> Unit,
    val onNavigate: (AppRoute) -> Unit,
)
