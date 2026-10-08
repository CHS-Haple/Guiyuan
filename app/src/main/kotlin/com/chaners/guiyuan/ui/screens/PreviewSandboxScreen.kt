package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.ui.components.StatusPreview
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
    val renderModel = state.toRenderModel(resourceResolver)

    val networkModeOptions =
        listOf(
            stringResource(R.string.home_preview_network_mode_mobile),
            stringResource(R.string.home_preview_network_mode_wifi),
        )
    val simOptions =
        listOf(
            stringResource(R.string.home_preview_sim_present),
            stringResource(R.string.home_preview_sim_absent),
        )
    val mobileNetworkChoices =
        listOf(
            PreviewMobileNetwork.NONE to stringResource(R.string.home_preview_network_none),
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
            !state.simPresent -> stringResource(R.string.home_preview_mobile_disabled_no_sim)
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
                        PreviewStatusLine(
                            label = stringResource(R.string.home_preview_section_network),
                            value = previewNetworkSummary(state),
                            modifier = Modifier.padding(top = 7.dp),
                        )
                        PreviewStatusLine(
                            label = stringResource(R.string.home_preview_section_battery),
                            value = previewBatterySummary(state),
                        )
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
                            selectedTabIndex = state.networkMode.ordinal,
                            onTabSelected = { index ->
                                onNetworkModeChange(PreviewNetworkMode.entries[index])
                            },
                            modifier =
                                Modifier
                                    .widthIn(max = 300.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .padding(horizontal = 6.dp),
                        )

                        if (state.networkMode == PreviewNetworkMode.MOBILE) {
                            if (state.mobileOptionsVisible) {
                                OverlayDropdownPreference(
                                    title = stringResource(R.string.home_preview_mobile_network_title),
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
                                SliderPreference(
                                    value = state.mobileSignalLevel.toFloat(),
                                    onValueChange = { value ->
                                        onMobileSignalLevelChange(value.roundToInt().coerceIn(0, 4))
                                    },
                                    title = stringResource(R.string.home_preview_mobile_signal_title),
                                    startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt) },
                                    valueText = signalValueText(state.mobileSignalLevel),
                                    insideMargin = SandboxPreferenceInsideMargin,
                                    valueRange = 0f..4f,
                                    steps = 3,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0f, 1f, 2f, 3f, 4f),
                                )
                            } else {
                                Text(
                                    text = mobileDisabledSummary.orEmpty(),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                )
                            }
                        } else {
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
                                keyPoints = listOf(0f, 1f, 2f, 3f),
                            )
                        }

                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_sim_title),
                            iconRes = R.drawable.ic_material_symbol_sim_card,
                            options = simOptions,
                            selectedIndex = if (state.simPresent) 0 else 1,
                            onSelected = { onSimPresentChange(it == 0) },
                            maxWidth = 300.dp,
                            modifier = Modifier,
                        )
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
private fun PreviewStatusLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
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

@Composable
internal fun previewNetworkSummary(
    state: PreviewSandboxUiState,
): String {
    if (state.networkMode == PreviewNetworkMode.WIFI) {
        val wifi =
            when (state.wifiState) {
                PreviewWifiState.CONNECTED ->
                    stringResource(
                        R.string.home_preview_network_summary_wifi,
                        signalValueText(state.wifiSignalLevel),
                    )
                PreviewWifiState.NO_INTERNET ->
                    stringResource(
                        R.string.home_preview_network_summary_wifi_no_internet,
                        signalValueText(state.wifiSignalLevel),
                    )
                PreviewWifiState.HOTSPOT ->
                    stringResource(
                        R.string.home_preview_network_summary_hotspot,
                        signalValueText(state.wifiSignalLevel),
                    )
            }
        val withAirplane =
            if (state.airplaneMode) {
                stringResource(R.string.home_preview_network_summary_airplane_wifi, wifi)
            } else {
                wifi
            }
        return if (!state.simPresent) {
            stringResource(
                R.string.home_preview_network_summary_with_sim_state,
                withAirplane,
                stringResource(R.string.home_preview_sim_absent),
            )
        } else {
            withAirplane
        }
    }

    if (state.airplaneMode) {
        return if (!state.simPresent) {
            stringResource(
                R.string.home_preview_network_summary_with_sim_state,
                stringResource(R.string.home_preview_airplane_title),
                stringResource(R.string.home_preview_sim_absent),
            )
        } else {
            stringResource(R.string.home_preview_airplane_title)
        }
    }
    if (!state.simPresent) {
        return stringResource(R.string.home_preview_sim_absent)
    }

    val mobileType =
        when (state.mobileNetwork) {
            PreviewMobileNetwork.NONE -> stringResource(R.string.home_preview_network_none)
            PreviewMobileNetwork.TWO_G -> stringResource(R.string.home_preview_network_2g)
            PreviewMobileNetwork.EDGE -> stringResource(R.string.home_preview_network_edge)
            PreviewMobileNetwork.THREE_G -> stringResource(R.string.home_preview_network_3g)
            PreviewMobileNetwork.H_PLUS -> stringResource(R.string.home_preview_network_h_plus)
            PreviewMobileNetwork.FOUR_G -> stringResource(R.string.home_preview_network_4g)
            PreviewMobileNetwork.LTE -> stringResource(R.string.home_preview_network_lte)
            PreviewMobileNetwork.FIVE_G -> stringResource(R.string.home_preview_network_5g)
            PreviewMobileNetwork.FIVE_GA -> stringResource(R.string.home_preview_network_5ga)
        }
    return stringResource(
        R.string.home_preview_network_summary_mobile,
        mobileType,
        signalValueText(state.mobileSignalLevel),
    )
}

@Composable
internal fun previewBatterySummary(
    state: PreviewSandboxUiState,
): String {
    val mode =
        when (state.batteryMode) {
            PreviewBatteryMode.BALANCED ->
                stringResource(R.string.home_preview_battery_mode_balanced)
            PreviewBatteryMode.POWER_SAVE ->
                stringResource(R.string.home_preview_battery_mode_power_save)
            PreviewBatteryMode.PERFORMANCE ->
                stringResource(R.string.home_preview_battery_mode_performance)
            PreviewBatteryMode.SUPER_POWER_SAVE ->
                stringResource(R.string.home_preview_battery_mode_super_power_save)
        }
    val charging =
        when (state.chargingState) {
            PreviewChargingState.NOT_CHARGING ->
                stringResource(R.string.home_preview_charging_none)
            PreviewChargingState.CHARGING ->
                stringResource(R.string.home_preview_charging_normal)
            PreviewChargingState.SUPER_FAST_CHARGING ->
                stringResource(R.string.home_preview_charging_super_fast)
        }

    return stringResource(
        R.string.home_preview_battery_summary_format,
        state.batteryPercent,
        mode,
        charging,
    )
}

@Composable
private fun signalValueText(level: Int): String =
    if (level <= 0) {
        stringResource(R.string.home_preview_signal_none)
    } else {
        stringResource(R.string.home_preview_signal_level, level)
    }
