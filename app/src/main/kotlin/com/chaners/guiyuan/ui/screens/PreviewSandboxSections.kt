package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.SignalDotArrangement
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.ui.components.signalValueText
import com.chaners.guiyuan.xposed.RenderModel
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.previewSandboxRows(
    display: PreviewSandboxDisplay,
    choices: PreviewSandboxOptions,
    renderModel: RenderModel,
    visual: VisualCfg,
    actions: PreviewSandboxActions,
) {
    val state = display.state
    val parasiteActive = display.parasiteActive
    val onParasiteChange = actions.ui.onParasiteChange
                item(key = "fullPreview") {
                    SmallTitle(stringResource(R.string.home_preview_section_preview))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_preview_live_title),
                            style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium),
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        SandboxFullPreview(state, renderModel, visual)
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        SwitchPreference(
                            checked = parasiteActive,
                            onCheckedChange = { enabled ->
                                onParasiteChange(enabled, state)
                            },
                            title = stringResource(R.string.home_preview_parasite_title),
                            summary = stringResource(R.string.home_preview_parasite_summary),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bacteriophage) },
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                        )
                    }
                }
    previewSandboxNetworkRows(state, choices, visual, actions)
    previewSandboxBatteryRows(state, choices, actions)
}

private fun LazyListScope.previewSandboxNetworkRows(
    state: PreviewSandboxUiState,
    choices: PreviewSandboxOptions,
    visual: VisualCfg,
    actions: PreviewSandboxActions,
) {
    val onNetworkModeChange = actions.network.onNetworkModeChange
                item {
                    SmallTitle(stringResource(R.string.home_preview_section_network))
                    UiTabRow(
                        tabs = choices.network.networkMode,
                        selectedTabIndex = previewNetworkTabs.indexOf(state.networkMode),
                        onTabSelected = { index ->
                            previewNetworkTabs.getOrNull(index)?.let { mode ->
                                onNetworkModeChange(mode)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
                        inCard = false,
                    )
                }
                item {
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    ) {
                        SandboxNetworkContents(state, visual, choices, actions)
                    }
                }
}

@Composable
private fun SandboxNetworkContents(
    state: PreviewSandboxUiState,
    visual: VisualCfg,
    choices: PreviewSandboxOptions,
    actions: PreviewSandboxActions,
) {
    SandboxSimSelection(state, choices, actions)
    SandboxWifiControls(state, choices, actions)
    SandboxMobileControls(state, choices, actions)
    SandboxActivityControls(state, visual, choices, actions)
}

@Composable
private fun SandboxSimSelection(
    state: PreviewSandboxUiState,
    choices: PreviewSandboxOptions,
    actions: PreviewSandboxActions,
) {
    val onSimCardsChange = actions.sims.selection.onSimCardsChange
    val onDataSimChange = actions.sims.selection.onDataSimChange
                        SandboxSegmentedField(
                            field = SandboxField.Sim,
                            options = choices.network.dualSim,
                            selectedIndex = state.simCards.ordinal,
                            onSelected = { index ->
                                PreviewSimCards.entries.getOrNull(index)?.let(onSimCardsChange)
                            },
                        )
                        AnimatedPreferenceGroup(visible = state.mobileOptionsVisible && state.simCards == PreviewSimCards.BOTH) {
                            UiDropdownPreference(
                                title = stringResource(R.string.home_preview_data_sim_title),
                                startAction = {
                                    SemanticLeadingIcon(
                                        R.drawable.ic_material_symbol_sim_card,
                                        detailRes = R.drawable.ic_material_symbol_swap_horiz_w500,
                                    )
                                },
                                items = choices.network.dataSim,
                                selectedIndex = state.dataSim.ordinal,
                                onSelectedIndexChange = { index ->
                                    PreviewDataSim.entries.getOrNull(index)?.let { selected ->
                                        onDataSimChange(selected)
                                    }
                                },
                                insideMargin = SandboxPreferenceInsideMargin,
                                maxHeight = 240.dp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

}

@Composable
private fun SandboxWifiControls(
    state: PreviewSandboxUiState,
    choices: PreviewSandboxOptions,
    actions: PreviewSandboxActions,
) {
    val onWifiStateChange = actions.network.onWifiStateChange
    val onWifiSignalLevelChange = actions.network.onWifiSignalLevelChange
                        AnimatedPreferenceGroup(visible = state.networkMode == PreviewNetworkMode.WIFI) {
                            UiDropdownPreference(
                                title = stringResource(R.string.home_preview_wifi_state_title),
                                startAction = {
                                    SemanticLeadingIcon(
                                        iconRes = R.drawable.ic_material_symbol_wifi,
                                        detailRes = R.drawable.ic_material_symbol_swap_horiz,
                                    )
                                },
                                items = choices.network.wifi,
                                selectedIndex = state.wifiState.ordinal,
                                onSelectedIndexChange = { index ->
                                    PreviewWifiState.entries.getOrNull(index)?.let(onWifiStateChange)
                                },
                                insideMargin = SandboxPreferenceInsideMargin,
                                maxHeight = 280.dp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            SliderPreference(
                                value = state.wifiSignalLevel.toFloat(),
                                onValueChange = { value ->
                                    onWifiSignalLevelChange(value.roundToInt().coerceIn(0, 3))
                                },
                                title = stringResource(R.string.home_preview_wifi_signal_title),
                                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_wifi) },
                                valueText = signalValueText(state.wifiSignalLevel),
                                insideMargin = SandboxPreferenceInsideMargin,
                                valueRange = 0f..3f,
                                steps = 2,
                                showKeyPoints = true,
                                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                keyPoints = listOf(0f, 1f, 2f, 3f),
                            )
                        }


}

@Composable
private fun SandboxMobileControls(
    state: PreviewSandboxUiState,
    choices: PreviewSandboxOptions,
    actions: PreviewSandboxActions,
) {
    val onMobileNetworkChange = actions.sims.first.onMobileNetworkChange
    val onMobileSignalLevelChange = actions.sims.first.onMobileSignalLevelChange
    val onSim2SignalLevelChange = actions.sims.second.onSim2SignalLevelChange
    val onSim2NetworkChange = actions.sims.second.onSim2NetworkChange
                        AnimatedPreferenceGroup(visible = state.mobileOptionsVisible && state.sim1Active) {
                            SandboxMobileNetworkDropdown(
                                titleRes = if (state.simCards == PreviewSimCards.BOTH) R.string.home_preview_sim1_network_title
                                    else R.string.home_preview_mobile_network_title,
                                choices = choices.network.mobileChoices,
                                options = choices.network.mobileOptions,
                                network = state.mobileNetwork,
                                onChange = onMobileNetworkChange,
                            )
                        }
                        AnimatedPreferenceGroup(visible = state.sim1Active || !state.hasSim) {
                            SandboxMobileSignalSlider(
                                titleRes = if (state.simCards == PreviewSimCards.BOTH) R.string.home_preview_sim1_signal_title
                                    else R.string.home_preview_mobile_signal_title,
                                level = state.mobileSignalLevel,
                                enabled = state.mobileControlsEnabled && state.sim1Active,
                                summary = when {
                                    state.airplaneMode -> stringResource(R.string.home_preview_mobile_disabled_airplane)
                                    !state.sim1Active -> stringResource(R.string.home_preview_mobile_disabled_no_sim)
                                    else -> null
                                },
                                onChange = onMobileSignalLevelChange,
                            )
                        }
                        AnimatedPreferenceGroup(visible = state.sim2Active) {
                            AnimatedPreferenceGroup(visible = state.mobileOptionsVisible && state.sim2Active) {
                                SandboxMobileNetworkDropdown(
                                    titleRes = R.string.home_preview_sim2_network_title,
                                    choices = choices.network.mobileChoices,
                                    options = choices.network.mobileOptions,
                                    network = state.sim2MobileNetwork,
                                    onChange = onSim2NetworkChange,
                                )
                            }
                            SandboxMobileSignalSlider(
                                titleRes = R.string.home_preview_sim2_signal_title,
                                level = state.sim2SignalLevel,
                                enabled = state.mobileControlsEnabled && state.sim2Active,
                                summary = when {
                                    state.airplaneMode -> stringResource(R.string.home_preview_mobile_disabled_airplane)
                                    !state.sim2Active -> stringResource(R.string.home_preview_mobile_disabled_no_sim)
                                    else -> null
                                },
                                onChange = onSim2SignalLevelChange,
                            )
                        }

}

@Composable
private fun SandboxActivityControls(
    state: PreviewSandboxUiState,
    visual: VisualCfg,
    choices: PreviewSandboxOptions,
    actions: PreviewSandboxActions,
) {
    val onNetworkActivityChange = actions.network.onNetworkActivityChange
    val onAirplaneModeChange = actions.network.onAirplaneModeChange
    val activitySupported = visual.dualSignal &&
        visual.dualRowDotArrangement == SignalDotArrangement.BOTH_SIDES && state.dualLevels != null
                        UiDropdownPreference(
                            title = stringResource(R.string.home_preview_activity_state_title),
                            summary = if (activitySupported) null
                                else stringResource(R.string.home_preview_activity_unavailable_summary),
                            enabled = activitySupported,
                            startAction = {
                                SemanticLeadingIcon(
                                    iconRes = R.drawable.ic_guiyuan_network_activity_triangles,
                                    detailRes = R.drawable.ic_material_symbol_swap_horiz,
                                )
                            },
                            items = choices.network.activity,
                            selectedIndex = state.networkActivity.ordinal,
                            onSelectedIndexChange = { index ->
                                if (activitySupported) {
                                    PreviewNetworkActivity.entries.getOrNull(index)
                                        ?.let(onNetworkActivityChange)
                                }
                            },
                            insideMargin = SandboxPreferenceInsideMargin,
                            maxHeight = 280.dp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        SwitchPreference(
                            checked = state.airplaneMode,
                            onCheckedChange = { enabled ->
                                onAirplaneModeChange(enabled)
                            },
                            title = stringResource(R.string.home_preview_airplane_title),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_airplanemode_active) },
                            summary = stringResource(R.string.home_preview_airplane_summary),
                            insideMargin = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 18.dp),
                        )
}


private fun LazyListScope.previewSandboxBatteryRows(
    state: PreviewSandboxUiState,
    choices: PreviewSandboxOptions,
    actions: PreviewSandboxActions,
) {
    val onBatteryPercentChange = actions.battery.onBatteryPercentChange
    val onBatteryModeChange = actions.battery.onBatteryModeChange
    val onChargingStateChange = actions.battery.onChargingStateChange
                item {
                    SmallTitle(stringResource(R.string.home_preview_section_battery))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(vertical = 8.dp),
                    ) {
                        SliderPreference(
                            value = state.batteryPercent.toFloat(),
                            onValueChange = { value ->
                                onBatteryPercentChange(value.roundToInt().coerceIn(0, 100))
                            },
                            title = stringResource(R.string.home_preview_battery_level_title),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar) },
                            valueText =
                                stringResource(
                                    R.string.home_preview_battery_percent,
                                    state.batteryPercent,
                                ),
                            insideMargin = SandboxPreferenceInsideMargin,
                            valueRange = 0f..100f,
                        )
                        SandboxSegmentedField(
                            field = SandboxField.BatteryMode,
                            options = choices.battery.modes,
                            selectedIndex = state.batteryMode.ordinal,
                            onSelected = { index ->
                                onBatteryModeChange(PreviewBatteryMode.entries[index])
                            },
                        )
                        SandboxSegmentedField(
                            field = SandboxField.ChargingState,
                            options = choices.battery.charging,
                            selectedIndex = state.chargingState.ordinal,
                            onSelected = { index ->
                                onChargingStateChange(PreviewChargingState.entries[index])
                            },
                        )
                    }
                }
}
