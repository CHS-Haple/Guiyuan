package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.ui.components.StatusPreview
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.ui.components.PreviewSummary
import com.chaners.guiyuan.ui.components.signalValueText
import com.chaners.guiyuan.ui.components.MiuixTopBarBackdrop
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class SandboxHint {
    BATTERY,
    DUAL_SIGNAL,
    BATTERY_MODE,
    CHARGING,
    NETWORK,
    WIFI_STATE,
    WIFI_SIGNAL,
    MOBILE_NETWORK,
    SIM2_NETWORK,
    MOBILE_SIGNAL,
    SIM2_SIGNAL,
    SIM,
    DATA_SIM,
    AIRPLANE,
    PARASITE,
}

@Composable
internal fun PreviewSandboxScreen(
    state: PreviewSandboxUiState,
    parasiteActive: Boolean,
    onParasiteChange: (Boolean) -> Unit,
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
    val featureRepo = remember(context.applicationContext) {
        FeatureRepo(context.applicationContext)
    }
    val feature by featureRepo.settings.collectAsState(initial = featureRepo.current())
    val renderModel = remember(state, feature.lowBatteryThreshold, resourceResolver, LocalConfiguration.current) {
        state.toRenderModel(resourceResolver, lowBatteryThreshold = feature.lowBatteryThreshold)
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

    val listState = rememberLazyListState()
    val showDock by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val preview = info.visibleItemsInfo.firstOrNull { it.key == "fullPreview" }
            if (preview != null) {
                preview.offset + preview.size <= info.viewportStartOffset
            } else {
                listState.firstVisibleItemIndex > 0
            }
        }
    }
    var previewExpanded by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(SandboxHint.BATTERY) }
    BackHandler(enabled = previewExpanded) { previewExpanded = false }
    LaunchedEffect(showDock) {
        if (!showDock) previewExpanded = false
    }

    val hintLabel = when (hint) {
        SandboxHint.BATTERY -> stringResource(R.string.home_preview_battery_level_title)
        SandboxHint.DUAL_SIGNAL -> stringResource(R.string.home_preview_dual_mobile_title)
        SandboxHint.BATTERY_MODE -> stringResource(R.string.home_preview_battery_mode_title)
        SandboxHint.CHARGING -> stringResource(R.string.home_preview_charging_state_title)
        SandboxHint.NETWORK -> stringResource(R.string.home_preview_network_mode_title)
        SandboxHint.WIFI_STATE -> stringResource(R.string.home_preview_wifi_state_title)
        SandboxHint.WIFI_SIGNAL -> stringResource(R.string.home_preview_wifi_signal_title)
        SandboxHint.MOBILE_NETWORK -> stringResource(
            if (state.dualMobile) R.string.home_preview_sim1_network_title
            else R.string.home_preview_mobile_network_title,
        )
        SandboxHint.SIM2_NETWORK -> stringResource(R.string.home_preview_sim2_network_title)
        SandboxHint.MOBILE_SIGNAL -> stringResource(
            if (state.dualMobile) R.string.home_preview_sim1_signal_title
            else R.string.home_preview_mobile_signal_title,
        )
        SandboxHint.SIM2_SIGNAL -> stringResource(R.string.home_preview_sim2_signal_title)
        SandboxHint.SIM -> stringResource(R.string.home_preview_sim_title)
        SandboxHint.DATA_SIM -> stringResource(R.string.home_preview_data_sim_title)
        SandboxHint.AIRPLANE -> stringResource(R.string.home_preview_airplane_title)
        SandboxHint.PARASITE -> stringResource(R.string.home_preview_parasite_title)
    }
    val onText = stringResource(R.string.home_preview_state_on)
    val offText = stringResource(R.string.home_preview_state_off)
    val hintValue = when (hint) {
        SandboxHint.BATTERY -> stringResource(R.string.home_preview_battery_percent, state.batteryPercent)
        SandboxHint.DUAL_SIGNAL -> if (state.dualMobile) onText else offText
        SandboxHint.BATTERY_MODE -> batteryModeOptions[state.batteryMode.ordinal]
        SandboxHint.CHARGING -> chargingOptions[state.chargingState.ordinal]
        SandboxHint.NETWORK -> networkModeOptions[previewNetworkTabs.indexOf(state.networkMode)]
        SandboxHint.WIFI_STATE -> wifiOptions[state.wifiState.ordinal]
        SandboxHint.WIFI_SIGNAL -> signalValueText(state.wifiSignalLevel)
        SandboxHint.MOBILE_NETWORK -> mobileNetworkChoices.firstOrNull {
            it.first == state.mobileNetwork
        }?.second ?: stringResource(R.string.home_preview_network_mode_mobile)
        SandboxHint.SIM2_NETWORK -> mobileNetworkChoices.firstOrNull {
            it.first == state.sim2MobileNetwork
        }?.second ?: stringResource(R.string.home_preview_network_mode_mobile)
        SandboxHint.MOBILE_SIGNAL -> signalValueText(state.mobileSignalLevel)
        SandboxHint.SIM2_SIGNAL -> signalValueText(state.sim2SignalLevel)
        SandboxHint.SIM -> if (state.dualMobile) {
            dualSimOptions[state.simCards.ordinal]
        } else {
            simOptions[if (state.simPresent) 0 else 1]
        }
        SandboxHint.DATA_SIM -> dataSimOptions[state.dataSim.ordinal]
        SandboxHint.AIRPLANE -> if (state.airplaneMode) onText else offText
        SandboxHint.PARASITE -> if (parasiteActive) onText else offText
    }
    val hintIcon = when (hint) {
        SandboxHint.BATTERY, SandboxHint.BATTERY_MODE -> R.drawable.ic_material_symbol_battery_5_bar
        SandboxHint.CHARGING -> R.drawable.ic_material_symbol_bolt
        SandboxHint.DUAL_SIGNAL -> R.drawable.ic_material_symbol_sim_cards
        SandboxHint.SIM, SandboxHint.DATA_SIM -> R.drawable.ic_material_symbol_sim_card
        SandboxHint.WIFI_STATE, SandboxHint.WIFI_SIGNAL -> R.drawable.ic_material_symbol_wifi
        SandboxHint.AIRPLANE -> R.drawable.ic_material_symbol_airplanemode_active
        SandboxHint.PARASITE -> R.drawable.ic_material_symbol_bacteriophage
        else -> R.drawable.ic_material_symbol_signal_cellular_alt
    }
    val hintDetail = when (hint) {
        SandboxHint.BATTERY_MODE -> R.drawable.ic_material_symbol_tune
        SandboxHint.WIFI_STATE, SandboxHint.DATA_SIM -> R.drawable.ic_material_symbol_swap_horiz
        SandboxHint.MOBILE_NETWORK, SandboxHint.SIM2_NETWORK -> R.drawable.ic_material_symbol_category_w500
        else -> null
    }
    val hintText = "$hintLabel $hintValue"
    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.home_preview_sandbox_title),
                color = Color.Transparent,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TooltipBox(text = stringResource(R.string.back)) {
                        IconButton(onClick = { if (previewExpanded) previewExpanded = false else onBack() }) {
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
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize(),
        ) {
            // The scroll viewport and floating preview share the same Scaffold top inset.
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .topBarBackdropSource(topBarBackdrop),
                contentPadding =
                    pageContentPadding(
                        innerPadding = PaddingValues(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = paddingValues.calculateBottomPadding(),
                        ),
                        extraBottom = 12.dp,
                    ),
            ) {
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
                            style = MiuixTheme.textStyles.body1,
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
                                hint = SandboxHint.PARASITE
                                onParasiteChange(enabled)
                            },
                            title = stringResource(R.string.home_preview_parasite_title),
                            summary = stringResource(R.string.home_preview_parasite_summary),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bacteriophage) },
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                        )
                    }
                }

                item {
                    SmallTitle(stringResource(R.string.home_preview_section_network))
                }

                item {
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    ) {
                        TabRowWithContour(
                            tabs = networkModeOptions,
                            selectedTabIndex = previewNetworkTabs.indexOf(state.networkMode),
                            onTabSelected = { index ->
                                previewNetworkTabs.getOrNull(index)?.let { mode ->
                                    hint = SandboxHint.NETWORK
                                    onNetworkModeChange(mode)
                                }
                            },
                            modifier =
                                Modifier
                                    .widthIn(max = 300.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .padding(horizontal = 6.dp)
                                    .padding(top = 8.dp, bottom = 6.dp),
                        )

                        SwitchPreference(
                            title = stringResource(R.string.home_preview_dual_mobile_title),
                            summary = stringResource(R.string.home_preview_dual_mobile_summary),
                            checked = state.dualMobile,
                            onCheckedChange = { enabled ->
                                hint = SandboxHint.DUAL_SIGNAL
                                onDualMobileChange(enabled)
                            },
                            startAction = {
                                SemanticLeadingIcon(R.drawable.ic_material_symbol_sim_cards)
                            },
                            insideMargin = SandboxPreferenceInsideMargin,
                        )
                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_sim_title),
                            iconRes = R.drawable.ic_material_symbol_sim_card,
                            options = if (state.dualMobile) dualSimOptions else simOptions,
                            selectedIndex =
                                if (state.dualMobile) state.simCards.ordinal
                                else if (state.simPresent) 0 else 1,
                            onSelected = { index ->
                                hint = SandboxHint.SIM
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
                                        detailRes = R.drawable.ic_material_symbol_swap_horiz_w500,
                                    )
                                },
                                items = dataSimOptions,
                                selectedIndex = state.dataSim.ordinal,
                                onSelectedIndexChange = { index ->
                                    PreviewDataSim.entries.getOrNull(index)?.let { selected ->
                                        hint = SandboxHint.DATA_SIM
                                        onDataSimChange(selected)
                                    }
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
                                    hint = SandboxHint.WIFI_STATE
                                    onWifiStateChange(PreviewWifiState.entries[index])
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            SliderPreference(
                                value = state.wifiSignalLevel.toFloat(),
                                onValueChange = { value ->
                                    hint = SandboxHint.WIFI_SIGNAL
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
                                                detailRes = R.drawable.ic_material_symbol_category_w500,
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
                                                ?.let { network ->
                                                    hint = SandboxHint.MOBILE_NETWORK
                                                    onMobileNetworkChange(network)
                                                }
                                        },
                                        insideMargin = SandboxPreferenceInsideMargin,
                                        maxHeight = 360.dp,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                SliderPreference(
                                    value = state.mobileSignalLevel.toFloat(),
                                    onValueChange = { value ->
                                        hint = SandboxHint.MOBILE_SIGNAL
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
                                                detailRes = R.drawable.ic_material_symbol_category_w500,
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
                                                ?.let { network ->
                                                    hint = SandboxHint.SIM2_NETWORK
                                                    onSim2NetworkChange(network)
                                                }
                                        },
                                        insideMargin = SandboxPreferenceInsideMargin,
                                        maxHeight = 360.dp,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                SliderPreference(
                                    value = state.sim2SignalLevel.toFloat(),
                                    onValueChange = { value ->
                                        hint = SandboxHint.SIM2_SIGNAL
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
                            onCheckedChange = { enabled ->
                                hint = SandboxHint.AIRPLANE
                                onAirplaneModeChange(enabled)
                            },
                            title = stringResource(R.string.home_preview_airplane_title),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_airplanemode_active) },
                            summary = stringResource(R.string.home_preview_airplane_summary),
                            insideMargin = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 18.dp),
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
                                hint = SandboxHint.BATTERY
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
                                hint = SandboxHint.BATTERY_MODE
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
                                hint = SandboxHint.CHARGING
                                onChargingStateChange(PreviewChargingState.entries[index])
                            },
                            modifier = Modifier,
                        )
                    }
                }
            }

            // The dock animates outside the LazyColumn's layout and joins the top bar surface.
            // A single backdrop spans the app bar and drawer without changing LazyColumn measurements.
            Box(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(1f),
            ) {
                MiuixTopBarBackdrop(
                    backdrop = topBarBackdrop,
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.matchParentSize(),
                )
                Column {
                    Spacer(Modifier.height(paddingValues.calculateTopPadding()))
                    AnimatedVisibility(
                        visible = showDock,
                        modifier = Modifier.fillMaxWidth(),
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(240),
                ) + fadeIn(animationSpec = tween(170)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(240),
                ) + fadeOut(animationSpec = tween(160)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AnimatedContent(
                                targetState = previewExpanded,
                                contentAlignment = Alignment.TopStart,
                                transitionSpec = {
                                    (fadeIn(tween(170)) togetherWith fadeOut(tween(120)))
                                        .using(SizeTransform(clip = true) { _, _ -> tween(280) })
                                },
                                label = "sandboxPreviewDrawer",
                            ) { expanded ->
                                if (expanded) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        SandboxFullPreview(
                                            state = state,
                                            model = renderModel,
                                            visual = visual,
                                            showDescription = false,
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 44.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        SemanticLeadingIcon(hintIcon, detailRes = hintDetail)
                                        Text(
                                            text = hintText,
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        ScaledSandboxPreview(
                                            model = renderModel,
                                            visual = visual,
                                            scale = 0.30f,
                                        )
                                    }
                                }
                            }
                            val label = stringResource(
                                if (previewExpanded) R.string.home_preview_collapse
                                else R.string.home_preview_expand,
                            )
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                TooltipBox(text = label) {
                                    IconButton(onClick = { previewExpanded = !previewExpanded }) {
                                        Icon(
                                            if (previewExpanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                                            contentDescription = label,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
                }
            }
        }

    }
}

@Composable
private fun SandboxFullPreview(
    state: PreviewSandboxUiState,
    model: RenderModel,
    visual: VisualCfg,
    showDescription: Boolean = true,
) {
    if (showDescription) {
        Text(
            text = stringResource(R.string.home_preview_sandbox_summary),
            modifier = Modifier.padding(top = 2.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showDescription) 8.dp else 0.dp)
            .height(192.dp),
        contentAlignment = Alignment.Center,
    ) {
        StatusPreview(
            model = model,
            visual = visual,
            modifier = Modifier
                .width(SandboxPreviewWidth)
                .height(SandboxPreviewHeight),
        )
    }
    PreviewSummary(state, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ScaledSandboxPreview(
    model: RenderModel,
    visual: VisualCfg,
    scale: Float,
) {
    val density = LocalDensity.current
    val width = with(density) { SandboxPreviewWidth.roundToPx() }
    val height = with(density) { SandboxPreviewHeight.roundToPx() }

    // Keep the renderer's logical viewport unchanged; only the composed layer shrinks.
    Layout(
        content = {
            StatusPreview(
                model = model,
                visual = visual,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            )
        },
        modifier = Modifier.clipToBounds(),
    ) { children, constraints ->
        val preview = children.single().measure(Constraints.fixed(width, height))
        layout(
            (width * scale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth),
            (height * scale).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
            preview.place(0, 0)
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

private val SandboxPreviewWidth = 120.dp
private val SandboxPreviewHeight = 184.dp

private val SandboxPreferenceInsideMargin =
    PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    )

