package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.ui.components.StatusPreview
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.xposed.RenderModel
import com.chaners.guiyuan.ui.components.PreviewSummary
import com.chaners.guiyuan.ui.components.signalValueText
import com.chaners.guiyuan.ui.layout.pageContentPadding
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PreviewSandboxScreen(
    state: PreviewSandboxUiState,
    parasiteActive: Boolean,
    onParasiteChange: (Boolean, PreviewSandboxUiState) -> Unit,
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

    // The dock is an overlay; compare the preview against the list's native content inset.
    val listState = rememberLazyListState()
    val showDock by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val preview = info.visibleItemsInfo.firstOrNull { it.key == "fullPreview" }
            when {
                info.totalItemsCount == 0 -> false
                preview != null -> preview.offset + preview.size <= info.beforeContentPadding
                else -> listState.firstVisibleItemIndex > 0
            }
        }
    }
    val scope = rememberCoroutineScope()

    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()
    val os4 = LocalUiStyle.current == UiStyle.OS4
    // Match MIUIX's 52dp compact title center plus its 4dp bottom title clearance.
    val dockPadding = if (os4) {
        val inset = (TopAppBarDefaults.CollapsedHeight - TopAppBarDefaults.SmallTopAppBarCenterHeight) / 2
        PaddingValues(top = inset, bottom = inset + TopAppBarDefaults.LargeTitleBottomPadding)
    } else {
        PaddingValues(vertical = 6.dp)
    }
    var dockHeightPx by remember { mutableIntStateOf(0) }
    var nativeBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val dockHeight = with(density) { dockHeightPx.toDp() }
    val backAction: @Composable () -> Unit = {
        TooltipBox(text = stringResource(R.string.back)) {
            if (os4) {
                GlassIconButton(onClick = onBack) {
                    Icon(Os4GlassBack, contentDescription = stringResource(R.string.back))
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                }
            }
        }
    }
    val barActions: @Composable RowScope.() -> Unit = {
        val title = stringResource(
            if (dark) R.string.home_preview_switch_light
            else R.string.home_preview_switch_dark,
        )
        TooltipBox(text = title) {
            if (os4) {
                GlassIconButton(onClick = onToggleTheme) {
                    Image(
                        painter = painterResource(if (dark) R.drawable.ic_material_symbol_light_mode else R.drawable.ic_material_symbol_dark_mode),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                    )
                }
            } else IconButton(onClick = onToggleTheme) {
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
    }
    val dock: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = showDock,
            modifier = Modifier.onSizeChanged { dockHeightPx = it.height },
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dockPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TopAppBarDefaults.TitlePadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.home_preview_live_title),
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SandboxDockIcon(renderModel, visual)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (os4) GlassTopAppBarDefaults.HorizontalPadding else TopAppBarDefaults.ActionIconPadding),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    val backTop = stringResource(R.string.diagnostics_scroll_top)
                    TooltipBox(text = backTop) {
                        if (os4) {
                            GlassIconButton(
                                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                                backdrop = topBarBackdrop,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_material_symbol_vertical_align_top),
                                    contentDescription = backTop,
                                )
                            }
                        } else IconButton(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_material_symbol_vertical_align_top),
                                contentDescription = backTop,
                            )
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
        Scaffold(
            topBar = {
              Box(Modifier.onSizeChanged { nativeBarHeightPx = it.height }) {
                if (os4) {
                    GlassTopAppBar(
                        isContentScrolled = listState.canScrollBackward,
                        title = stringResource(R.string.home_preview_sandbox_title),
                        backdrop = topBarBackdrop,
                        scrollBehavior = scrollBehavior,
                        bandOverhang = maxOf(GlassTopAppBarDefaults.BandOverhang, dockHeight),
                        navigationIcon = backAction,
                        actions = barActions,
                        unclippedNavigationIcon = true,
                        unclippedActions = true,
                    )
                } else {
                    MiuixBlurredTopBar(
                        backdrop = topBarBackdrop,
                        scrollBehavior = scrollBehavior,
                        fadeExtension = dockHeight,
                    ) { barColor ->
                        SmallTopAppBar(
                            title = stringResource(R.string.home_preview_sandbox_title),
                            color = barColor,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = backAction,
                            actions = barActions,
                        )
                    }
                }
              }
            },
            // Retain MIUIX's one native popup host; it draws above this dock.
            popupHost = {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth().padding(
                            top = with(density) { nativeBarHeightPx.toDp() },
                        ),
                    ) {
                        dock()
                    }
                    MiuixPopupHost()
                }
            },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .topBarBackdropSource(topBarBackdrop)
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    pageContentPadding(
                        innerPadding = paddingValues,
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
                                onParasiteChange(enabled, state)
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
                    UiTabRow(
                        tabs = networkModeOptions,
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
                        SwitchPreference(
                            title = stringResource(R.string.home_preview_dual_mobile_title),
                            summary = stringResource(R.string.home_preview_dual_mobile_summary),
                            checked = state.dualMobile,
                            onCheckedChange = { enabled ->
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
                                if (state.dualMobile) {
                                    PreviewSimCards.entries.getOrNull(index)?.let(onSimCardsChange)
                                } else {
                                    onSimPresentChange(index == 0)
                                }
                            },
                            maxWidth = 300.dp,
                        )
                        if (state.mobileOptionsVisible && state.dualMobile) {
                            UiDropdownPreference(
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
                                    UiDropdownPreference(
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
                                    UiDropdownPreference(
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

// Draw into the actual compact View rather than resampling a full-sized GPU layer.
@Composable
private fun SandboxDockIcon(
    model: RenderModel,
    visual: VisualCfg,
) {
    val compactHeight = TopAppBarDefaults.SmallTopAppBarCenterHeight
    val compactWidth = compactHeight * (SandboxPreviewWidth.value / SandboxPreviewHeight.value)
    StatusPreview(
        model = model,
        visual = visual,
        modifier = Modifier.size(compactWidth, compactHeight),
        referenceSize = DpSize(SandboxPreviewWidth, SandboxPreviewHeight),
    )
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
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val glass = LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()
        val requiredWidth = if (glass) {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val selectedStyle = MiuixTheme.textStyles.subtitle
            val idleStyle = MiuixTheme.textStyles.body2
            remember(options, measurer, density, layoutDirection, selectedStyle, idleStyle) {
                val widest = options.maxOfOrNull { option ->
                    val text = AnnotatedString(option)
                    maxOf(
                        measurer.measure(text, style = selectedStyle, softWrap = false, maxLines = 1).size.width,
                        measurer.measure(text, style = idleStyle, softWrap = false, maxLines = 1).size.width,
                    )
                } ?: 0
                val spacing = with(density) {
                    (
                        GlassSegmentedTabRowDefaults.TrackPadding * 2 +
                            GlassSegmentedTabRowDefaults.TabPaddingHorizontal * (options.size * 2) +
                            GlassSegmentedTabRowDefaults.TabGap * (options.size - 1).coerceAtLeast(0) +
                            SandboxPreferenceInsideMargin.calculateStartPadding(layoutDirection) +
                            SandboxPreferenceInsideMargin.calculateEndPadding(layoutDirection)
                        ).roundToPx()
                }
                widest * options.size + spacing
            }
        } else {
            0
        }

        if (glass && requiredWidth > constraints.maxWidth) {
            // Equal-width segments cannot fit these labels; show the full options in a popup.
            UiDropdownPreference(
                items = options,
                selectedIndex = selectedIndex,
                title = title,
                startAction = { SemanticLeadingIcon(iconRes, detailRes = detailRes) },
                onSelectedIndexChange = onSelected,
                modifier = Modifier.fillMaxWidth(),
                insideMargin = SandboxPreferenceInsideMargin,
            )
        } else {
            BasicComponent(
                title = title,
                startAction = { SemanticLeadingIcon(iconRes, detailRes = detailRes) },
                modifier = Modifier.fillMaxWidth(),
                insideMargin = SandboxPreferenceInsideMargin,
                bottomAction = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        UiTabRow(
                            tabs = options,
                            selectedTabIndex = selectedIndex,
                            onTabSelected = onSelected,
                            modifier = if (glass) Modifier.fillMaxWidth() else Modifier.widthIn(max = maxWidth).fillMaxWidth(),
                        )
                    }
                },
            )
        }
    }
}

private val SandboxPreviewWidth = 120.dp
private val SandboxPreviewHeight = 184.dp

private val SandboxPreferenceInsideMargin =
    PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    )


@Composable
private fun UiTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    inCard: Boolean = true,
) {
    if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
        // A tab inside the scrolling page must not sample the page's own
        // backdrop: that recorder also contains this tab. Record only its base.
        val surface = if (inCard) MiuixTheme.colorScheme.surfaceContainer else MiuixTheme.colorScheme.background
        val backdrop = rememberLayerBackdrop { drawRect(surface) }
        Box(modifier = modifier) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop))
            GlassSegmentedTabRow(
                tabs = tabs,
                selectedIndex = selectedTabIndex,
                onSelect = onTabSelected,
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        TabRowWithContour(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
            colors = TabRowDefaults.tabRowColors(
                backgroundColor = if (inCard) MiuixTheme.colorScheme.surface else MiuixTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}

