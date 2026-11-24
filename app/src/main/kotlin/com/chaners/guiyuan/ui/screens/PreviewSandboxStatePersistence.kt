package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

// The ordinal positions of the existing preview enums are part of the saved-instance-state
// contract. Keep all 14 fields in the same order and append new fields only with a migration.
internal fun encodePreviewSelection(state: PreviewSandboxUiState): List<Any> = listOf(
    state.airplaneMode,
    state.networkMode.ordinal,
    state.mobileNetwork.ordinal,
    state.mobileSignalLevel,
    state.sim2SignalLevel,
    state.sim2MobileNetwork.ordinal,
    state.dataSim.ordinal,
    state.simCards.ordinal,
    state.wifiState.ordinal,
    state.wifiSignalLevel,
    state.networkActivity.ordinal,
    state.batteryPercent,
    state.batteryMode.ordinal,
    state.chargingState.ordinal,
)

private fun <E : Enum<E>> List<E>.restoredOrdinal(index: Int): E =
    this[index.coerceIn(0, lastIndex)]

internal fun decodePreviewSelection(values: List<Any>): PreviewSandboxUiState? {
    if (values.size != 14 || values[0] !is Boolean ||
        values.drop(1).any { it !is Int }
    ) return null
    fun number(index: Int) = values[index] as Int
    return PreviewSandboxUiState(
        airplaneMode = values[0] as Boolean,
        networkMode = PreviewNetworkMode.entries.restoredOrdinal(number(1)),
        mobileNetwork = PreviewMobileNetwork.entries.restoredOrdinal(number(2)),
        mobileSignalLevel = number(3),
        sim2SignalLevel = number(4),
        sim2MobileNetwork = PreviewMobileNetwork.entries.restoredOrdinal(number(5)),
        dataSim = PreviewDataSim.entries.restoredOrdinal(number(6)),
        simCards = PreviewSimCards.entries.restoredOrdinal(number(7)),
        wifiState = PreviewWifiState.entries.restoredOrdinal(number(8)),
        wifiSignalLevel = number(9),
        networkActivity = PreviewNetworkActivity.entries.restoredOrdinal(number(10)),
        batteryPercent = number(11),
        batteryMode = PreviewBatteryMode.entries.restoredOrdinal(number(12)),
        chargingState = PreviewChargingState.entries.restoredOrdinal(number(13)),
    )
}

internal val PreviewSandboxUiStateSaver: Saver<PreviewSandboxUiState, Any> =
    listSaver(save = { encodePreviewSelection(it) }, restore = ::decodePreviewSelection)

/**
 * Only the rendered/native sandbox projection may become NONE. The user's saved MOBILE
 * selection must survive switching SIM presence or network type and back.
 */
internal fun PreviewSandboxUiState.previewForPresentation(): PreviewSandboxUiState =
    if (networkMode == PreviewNetworkMode.MOBILE &&
        simCards == PreviewSimCards.SIM1_ONLY &&
        mobileNetwork == PreviewMobileNetwork.NONE
    ) copy(networkMode = PreviewNetworkMode.NONE) else this

/** Match the original UI selection rule, with both updates in one immutable state write. */
internal fun PreviewSandboxUiState.selectPreviewNetworkMode(mode: PreviewNetworkMode): PreviewSandboxUiState =
    copy(
        networkMode = mode,
        mobileNetwork = if (mode == PreviewNetworkMode.MOBILE && mobileNetwork == PreviewMobileNetwork.NONE) {
            PreviewMobileNetwork.FIVE_G
        } else mobileNetwork,
    )
