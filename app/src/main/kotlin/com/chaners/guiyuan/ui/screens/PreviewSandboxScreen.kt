package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.ui.components.StatusPreview
import com.chaners.guiyuan.ui.components.PreviewSummary
import com.chaners.guiyuan.ui.components.signalValueText
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PreviewSandboxScreen(
    state: PreviewSandboxUiState,
    onSimPresentChange: (Boolean) -> Unit,
    onAirplaneModeChange: (Boolean) -> Unit,
    onNetworkModeChange: (PreviewNetworkMode) -> Unit,
    onMobileNetworkChange: (PreviewMobileNetwork) -> Unit,
    onMobileSignalLevelChange: (Int) -> Unit,
    onDualMobileChange: (Boolean) -> Unit,
    onSim2SignalLevelChange: (Int) -> Unit,
    onSim2NetworkChange: (PreviewMobileNetwork) -> Unit,
    onDataSimChange: (PreviewDataSim) -> Unit,
    onSimCardsChange: (PreviewSimCards) -> Unit,
    onWifiStateChange: (PreviewWifiState) -> Unit,
    onWifiSignalLevelChange: (Int) -> Unit,
    onBatteryPercentChange: (Int) -> Unit,
    onBatteryModeChange: (PreviewBatteryMode) -> Unit,
    onChargingStateChange: (PreviewChargingState) -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val resourceResolver =
        remember(context.applicationContext) {
            PreviewSysUiResources(context.applicationContext)
        }
    val visualRepo =
        remember(context.applicationContext) {
            VisualRepo(context.applicationContext)
        }
    val visual by
        visualRepo.settings.collectAsState(
            initial = visualRepo.current(),
        )
    val renderModel = remember(state, resourceResolver, LocalConfiguration.current) {
        state.toRenderModel(resourceResolver)
    }

    val networkModeOptions =
        listOf(
            stringResource(R.string.home_preview_network_none),
            stringResource(R.string.home_preview_network_mode_mobile),
            stringResource(R.string.home_preview_network_mode_wifi),
        )
    val simOptions =
        listOf(
            stringResource(R.string.home_preview_sim_present),
            stringResource(R.string.home_preview_sim_absent),
        )
    val dataSimOptions =
        listOf(
            stringResource(R.string.home_preview_data_sim1),
            stringResource(R.string.home_preview_data_sim2),
        )
    val dualSimOptions =
        listOf(
            stringResource(R.string.home_preview_sim_both),
            stringResource(R.string.home_preview_sim1_only),
            stringResource(R.string.home_preview_sim2_only),
            stringResource(R.string.home_preview_sim_none),
        )
    val mobileNetworkChoices =
        listOf(
            PreviewMobileNetwork.TWO_G to stringResource(R.string.home_preview_network_2g),
            PreviewMobileNetwork.EDGE to stringResource(R.string.home_preview_network_edge),
            PreviewMobileNetwork.THREE_G to stringResource(R.string.home_preview_network_3g),
            PreviewMobileNetwork.H_PLUS to stringResource(R.string.home_preview_network_h_plus),
            PreviewMobileNetwork.FOUR_G to stringResource(R.string.home_preview_network_4g),
            PreviewMobileNetwork.LTE to stringResource(R.string.home_preview_network_lte),
            PreviewMobileNetwork.FIVE_G to stringResource(R.string.home_preview_network_5g),
            PreviewMobileNetwork.FIVE_GA to stringResource(R.string.home_preview_network_5ga),
        )
    val wifiOptions =
        listOf(
            stringResource(R.string.home_preview_wifi_connected),
            stringResource(R.string.home_preview_wifi_no_internet),
            stringResource(R.string.home_preview_wifi_hotspot),
        )
    val batteryModeOptions =
        listOf(
            stringResource(R.string.home_preview_battery_mode_balanced),
            stringResource(R.string.home_preview_battery_mode_power_save_compact),
            stringResource(R.string.home_preview_battery_mode_performance_compact),
            stringResource(R.string.home_preview_battery_mode_super_power_save_compact),
        )
    val chargingOptions =
        listOf(
            stringResource(R.string.home_preview_charging_none),
            stringResource(R.string.home_preview_charging_normal),
            stringResource(R.string.home_preview_charging_super_fast_compact),
        )

    val mobileDisabledSummary =
        when {
            state.airplaneMode -> stringResource(R.string.home_preview_mobile_disabled_airplane)
            !state.hasSim -> stringResource(R.string.home_preview_mobile_disabled_no_sim)
            else -> null
        }

    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                SmallTopAppBar(
                    title = stringResource(R.string.home_preview_sandbox_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        TooltipBox(text = stringResource(R.string.back)) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    MiuixIcons.Back,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    actions = {
                        val title = stringResource(
                            if (dark) R.string.home_preview_switch_light
                            else R.string.home_preview_switch_dark,
                        )
                        TooltipBox(text = title) {
                            IconButton(onClick = onToggleTheme) {
                                Image(
                                    painter = painterResource(
                                        if (dark) R.drawable.ic_material_symbol_light_mode
                                        else R.drawable.ic_material_symbol_dark_mode,
                                    ),
                                    contentDescription = title,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    pageContentPadding(
                        innerPadding = paddingValues,
                        extraBottom = 12.dp,
                    ),
            ) {
                item {
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
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        Text(
                            text = stringResource(R.string.home_preview_sandbox_summary),
                            modifier = Modifier.padding(top = 2.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .height(192.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            StatusPreview(
                                model = renderModel,
                                visual = visual,
                                modifier =
                                    Modifier
                                        .width(120.dp)
                                        .height(184.dp),
                            )
                        }
                        PreviewSummary(state, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                item {
                    SmallTitle(stringResource(R.string.home_preview_section_network))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(vertical = 8.dp),
                    ) {
                        TabRowWithContour(
                            tabs = networkModeOptions,
                            selectedTabIndex = previewNetworkTabs.indexOf(state.networkMode),
                            onTabSelected = { index ->
                                previewNetworkTabs.getOrNull(index)?.let(onNetworkModeChange)
                            },
                            modifier =
                                Modifier
                                    .widthIn(max = 300.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .padding(horizontal = 6.dp),
                        )

                        if (state.mobileControlsEnabled || state.dualMobile) {
                            SwitchPreference(
                                title = stringResource(R.string.home_preview_dual_mobile_title),
                                summary = stringResource(R.string.home_preview_dual_mobile_summary),
                                checked = state.dualMobile,
                                onCheckedChange = onDualMobileChange,
                                startAction = {
                                    SemanticLeadingIcon(R.drawable.ic_material_symbol_layers)
                                },
                                insideMargin = SandboxPreferenceInsideMargin,
                            )
                        }
                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_sim_title),
                            iconRes = R.drawable.ic_material_symbol_sim_card,
                            options = if (state.dualMobile) dualSimOptions else simOptions,
                            selectedIndex =
                                if (state.dualMobile) state.simCards.ordinal
                                else if (state.simPresent) 0 else 1,
                            onSelected = { index ->
                                if (state.dualMobile) {
                                    PreviewSimCards.entries.getOrNull(index)?.let(onSimCardsChange)
                                } else {
                                    onSimPresentChange(index == 0)
                                }
                            },
                            maxWidth = 300.dp,
                        )
                        if (state.mobileOptionsVisible && state.dualMobile) {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.home_preview_data_sim_title),
                                startAction = {
                                    SemanticLeadingIcon(
                                        R.drawable.ic_material_symbol_sim_card,
                                        detailRes = R.drawable.ic_material_symbol_swap_horiz,
                                    )
                                },
                                items = dataSimOptions,
                                selectedIndex = state.dataSim.ordinal,
                                onSelectedIndexChange = { index ->
                                    PreviewDataSim.entries.getOrNull(index)?.let(onDataSimChange)
                                },
                                insideMargin = SandboxPreferenceInsideMargin,
                                maxHeight = 240.dp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (state.networkMode == PreviewNetworkMode.WIFI) {
                            SandboxSegmentedField(
                                title = stringResource(R.string.home_preview_wifi_state_title),
                                iconRes = R.drawable.ic_material_symbol_wifi,
                                detailRes = R.drawable.ic_material_symbol_swap_horiz,
                                options = wifiOptions,
                                selectedIndex = state.wifiState.ordinal,
                                onSelected = { index ->
                                    onWifiStateChange(PreviewWifiState.entries[index])
                                },
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

                        if (state.mobileControlsEnabled) {
                            if (state.sim1Active) {
                                if (state.mobileOptionsVisible) {
                                    OverlayDropdownPreference(
                                        title = stringResource(
                                            if (state.dualMobile) R.string.home_preview_sim1_network_title
                                            else R.string.home_preview_mobile_network_title,
                                        ),
                                        startAction = {
                                            SemanticLeadingIcon(
                                                iconRes = R.drawable.ic_material_symbol_signal_cellular_alt,
                                                detailRes = R.drawable.ic_material_symbol_category,
                                            )
                                        },
                                        items = mobileNetworkChoices.map { it.second },
                                        selectedIndex =
                                            mobileNetworkChoices.indexOfFirst {
                                                it.first == state.mobileNetwork
                                            }.coerceAtLeast(0),
                                        onSelectedIndexChange = { index ->
                                            mobileNetworkChoices.getOrNull(index)
                                                ?.first
                                                ?.let(onMobileNetworkChange)
                                        },
                                        insideMargin = SandboxPreferenceInsideMargin,
                                        maxHeight = 360.dp,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                SliderPreference(
                                    value = state.mobileSignalLevel.toFloat(),
                                    onValueChange = { value ->
                                        onMobileSignalLevelChange(value.roundToInt().coerceIn(0, 4))
                                    },
                                    title = stringResource(
                                        if (state.dualMobile) R.string.home_preview_sim1_signal_title
                                        else R.string.home_preview_mobile_signal_title,
                                    ),
                                    startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt) },
                                    valueText = signalValueText(state.mobileSignalLevel),
                                    insideMargin = SandboxPreferenceInsideMargin,
                                    valueRange = 0f..4f,
                                    steps = 3,
                                    showKeyPoints = true,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                    keyPoints = listOf(0f, 1f, 2f, 3f, 4f),
                                )
                            }
                            if (state.sim2Active) {
                                if (state.mobileOptionsVisible) {
                                    OverlayDropdownPreference(
                                        title = stringResource(R.string.home_preview_sim2_network_title),
                                        startAction = {
                                            SemanticLeadingIcon(
                                                iconRes = R.drawable.ic_material_symbol_signal_cellular_alt,
                                                detailRes = R.drawable.ic_material_symbol_category,
                                            )
                                        },
                                        items = mobileNetworkChoices.map { it.second },
                                        selectedIndex =
                                            mobileNetworkChoices.indexOfFirst {
                                                it.first == state.sim2MobileNetwork
                                            }.coerceAtLeast(0),
                                        onSelectedIndexChange = { index ->
                                            mobileNetworkChoices.getOrNull(index)
                                                ?.first
                                                ?.let(onSim2NetworkChange)
                                        },
                                        insideMargin = SandboxPreferenceInsideMargin,
                                        maxHeight = 360.dp,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                SliderPreference(
                                    value = state.sim2SignalLevel.toFloat(),
                                    onValueChange = { value ->
                                        onSim2SignalLevelChange(value.roundToInt().coerceIn(0, 4))
                                    },
                                    title = stringResource(R.string.home_preview_sim2_signal_title),
                                    startAction = {
                                        SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt)
                                    },
                                    valueText = signalValueText(state.sim2SignalLevel),
                                    insideMargin = SandboxPreferenceInsideMargin,
                                    valueRange = 0f..4f,
                                    steps = 3,
                                    showKeyPoints = true,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                    keyPoints = listOf(0f, 1f, 2f, 3f, 4f),
                                )
                            }
                        } else {
                            BasicComponent(
                                title = stringResource(R.string.home_preview_mobile_signal_title),
                                summary = mobileDisabledSummary.orEmpty(),
                                startAction = {
                                    SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt)
                                },
                                insideMargin = SandboxPreferenceInsideMargin,
                            )
                        }
                        SwitchPreference(
                            checked = state.airplaneMode,
                            onCheckedChange = onAirplaneModeChange,
                            title = stringResource(R.string.home_preview_airplane_title),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_airplanemode_active) },
                            summary = stringResource(R.string.home_preview_airplane_summary),
                            insideMargin = SandboxPreferenceInsideMargin,
                        )
                    }
                }

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
                            title = stringResource(R.string.home_preview_battery_mode_title),
                            iconRes = R.drawable.ic_material_symbol_battery_5_bar,
                            detailRes = R.drawable.ic_material_symbol_tune,
                            options = batteryModeOptions,
                            selectedIndex = state.batteryMode.ordinal,
                            onSelected = { index ->
                                onBatteryModeChange(PreviewBatteryMode.entries[index])
                            },
                            modifier = Modifier,
                        )
                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_charging_state_title),
                            iconRes = R.drawable.ic_material_symbol_bolt,
                            options = chargingOptions,
                            selectedIndex = state.chargingState.ordinal,
                            onSelected = { index ->
                                onChargingStateChange(PreviewChargingState.entries[index])
                            },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SandboxSegmentedField(
    title: String,
    @DrawableRes iconRes: Int,
    @DrawableRes detailRes: Int? = null,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 300.dp,
) {
    BasicComponent(
        title = title,
        startAction = { SemanticLeadingIcon(iconRes, detailRes = detailRes) },
        modifier = modifier,
        insideMargin = SandboxPreferenceInsideMargin,
        bottomAction = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TabRowWithContour(
                    tabs = options,
                    selectedTabIndex = selectedIndex,
                    onTabSelected = onSelected,
                    modifier =
                        Modifier
                            .widthIn(max = maxWidth)
                            .fillMaxWidth(),
                )
            }
        },
    )
}

private val SandboxPreferenceInsideMargin =
    PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    )

