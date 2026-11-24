package com.chaners.guiyuan.ui.screens

import androidx.compose.ui.unit.Dp

/** Display-only values; Preview Sandbox still owns its temporary Home lease. */
internal data class HomeScreenLayout(
    val bottomContentPadding: Dp,
    val previewState: PreviewSandboxUiState,
)

internal data class HomeScreenRuntime(
    val hotReloadInProgress: Boolean,
    val restartInProgress: Boolean,
    val scopeRequestInProgress: Boolean,
    val hotReloadInNavigation: Boolean,
)

internal data class HomeScreenActions(
    val onRestart: () -> Unit,
    val onRequestScope: () -> Unit,
    val onHotReload: () -> Unit,
    val onOpenPreviewSandbox: () -> Unit,
)
