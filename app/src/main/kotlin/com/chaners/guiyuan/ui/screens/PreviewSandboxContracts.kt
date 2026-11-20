package com.chaners.guiyuan.ui.screens

/** Preview values never write real radio or battery state. */
internal data class PreviewSandboxDisplay(
    val state: PreviewSandboxUiState,
    val parasiteActive: Boolean,
    val dark: Boolean,
)

internal data class PreviewSandboxUiActions(
    val onParasiteChange: (Boolean, PreviewSandboxUiState) -> Unit,
    val onToggleTheme: () -> Unit,
    val onBack: () -> Unit,
)

internal data class PreviewSandboxNetworkActions(
    val onAirplaneModeChange: (Boolean) -> Unit,
    val onNetworkModeChange: (PreviewNetworkMode) -> Unit,
    val onWifiStateChange: (PreviewWifiState) -> Unit,
    val onWifiSignalLevelChange: (Int) -> Unit,
    val onNetworkActivityChange: (PreviewNetworkActivity) -> Unit,
)

internal data class PreviewSandboxSim1Actions(
    val onMobileNetworkChange: (PreviewMobileNetwork) -> Unit,
    val onMobileSignalLevelChange: (Int) -> Unit,
)

internal data class PreviewSandboxSim2Actions(
    val onSim2SignalLevelChange: (Int) -> Unit,
    val onSim2NetworkChange: (PreviewMobileNetwork) -> Unit,
)

internal data class PreviewSandboxSimSelectionActions(
    val onDataSimChange: (PreviewDataSim) -> Unit,
    val onSimCardsChange: (PreviewSimCards) -> Unit,
)

internal data class PreviewSandboxSimActions(
    val first: PreviewSandboxSim1Actions,
    val second: PreviewSandboxSim2Actions,
    val selection: PreviewSandboxSimSelectionActions,
)

internal data class PreviewSandboxBatteryActions(
    val onBatteryPercentChange: (Int) -> Unit,
    val onBatteryModeChange: (PreviewBatteryMode) -> Unit,
    val onChargingStateChange: (PreviewChargingState) -> Unit,
)

internal data class PreviewSandboxActions(
    val ui: PreviewSandboxUiActions,
    val network: PreviewSandboxNetworkActions,
    val sims: PreviewSandboxSimActions,
    val battery: PreviewSandboxBatteryActions,
)
