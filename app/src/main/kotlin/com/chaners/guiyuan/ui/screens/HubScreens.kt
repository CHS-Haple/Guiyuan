package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_MIN
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_DEFAULT
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MAX
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MIN
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.BatteryColorSchemeLibraryRepository
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.topChargingIconUiScale
import com.chaners.guiyuan.settings.topChargingIconUiScaleDefault
import com.chaners.guiyuan.settings.topTextUiScale
import com.chaners.guiyuan.settings.topTextUiScaleDefault
import com.chaners.guiyuan.settings.topOffsetYUi
import com.chaners.guiyuan.settings.mobileTypeScaleDefault
import com.chaners.guiyuan.system.SysUiScope
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.navigation.AppRoute
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
internal fun FeaturesScreen(
    bottomContentPadding: Dp,
    onNavigate: (AppRoute) -> Unit,
) {
    val context = LocalContext.current
    val featureRepo =
        remember(context.applicationContext) {
            FeatureRepo(context.applicationContext)
        }
    val featureCfg by
        featureRepo.settings.collectAsState(
            initial = featureRepo.current(),
        )
    val visualRepo =
        remember(context.applicationContext) {
            VisualRepo(context.applicationContext)
        }
    val visual by
        visualRepo.settings.collectAsState(
            initial = visualRepo.current(),
        )
    val batteryColorSchemeRepository =
        remember(context.applicationContext) {
            BatteryColorSchemeLibraryRepository(context.applicationContext)
        }
    val batteryColorSchemeLibrary by
        batteryColorSchemeRepository.library.collectAsState(
            initial = batteryColorSchemeRepository.current(),
        )
    val layoutOptions =
        listOf(
            stringResource(R.string.content_layout_network_center),
            stringResource(R.string.content_layout_battery_center),
        )
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryColorSheet by rememberSaveable { mutableStateOf(false) }

    HubPage(
        title = stringResource(R.string.features_title),
        sectionTitle = stringResource(R.string.section_global),
        bottomContentPadding = bottomContentPadding,
        secondarySectionTitle = stringResource(R.string.section_network),
        secondaryContent = {
            SliderPreference(
                value = visual.wifiScale,
                onValueChange = visualRepo::setWifiScale,
                title = stringResource(R.string.wifi_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.wifiScale * 100f).roundToInt(),
                    ),
                valueRange = WIFI_SIZE_SCALE_MIN..WIFI_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints = listOf(WIFI_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            SliderPreference(
                value = visual.airplaneScale,
                onValueChange = visualRepo::setAirplaneScale,
                title = stringResource(R.string.airplane_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.airplaneScale * 100f).roundToInt(),
                    ),
                valueRange = AIRPLANE_SIZE_SCALE_MIN..AIRPLANE_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints = listOf(AIRPLANE_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            SliderPreference(
                value = visual.noSimScale,
                onValueChange = visualRepo::setNoSimScale,
                title = stringResource(R.string.no_sim_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.noSimScale * 100f).roundToInt(),
                    ),
                valueRange = NO_SIM_SIZE_SCALE_MIN..NO_SIM_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints = listOf(NO_SIM_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            SliderPreference(
                value = visual.mobileTypeScale,
                onValueChange = visualRepo::setMobileTypeScale,
                title = stringResource(R.string.mobile_type_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.mobileTypeScale * 100f).roundToInt(),
                    ),
                valueRange = MOBILE_TYPE_SIZE_SCALE_MIN..MOBILE_TYPE_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints =
                    listOf(
                        mobileTypeScaleDefault(
                            visual.layout,
                        ),
                    ),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            SliderPreference(
                value = visual.mobileTypeWeight.toFloat(),
                onValueChange = { value ->
                    visualRepo.setMobileTypeWeight(value.roundToInt())
                },
                title = stringResource(R.string.mobile_type_weight),
                valueText =
                    stringResource(
                        R.string.integer_value,
                        visual.mobileTypeWeight,
                    ),
                valueRange =
                    MOBILE_TYPE_WEIGHT_MIN.toFloat()..
                        MOBILE_TYPE_WEIGHT_MAX.toFloat(),
                steps = 19,
                showKeyPoints = true,
                keyPoints = listOf(MOBILE_TYPE_WEIGHT_DEFAULT.toFloat()),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            SwitchPreference(
                title = stringResource(R.string.mobile_follow_battery_color),
                summary = stringResource(R.string.mobile_follow_battery_color_summary),
                checked = visual.mobileFollowsBatteryColor,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setMobileFollowsBatteryColor,
            )
            SwitchPreference(
                title = stringResource(R.string.center_follow_battery_color),
                summary = stringResource(R.string.center_follow_battery_color_summary),
                checked = visual.centerFollowsBatteryColor,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setCenterFollowsBatteryColor,
            )
        },
        tertiarySectionTitle = stringResource(R.string.section_battery),
        tertiaryContent = {
            BatteryColorPreference(
                library = batteryColorSchemeLibrary,
                enabled = featureCfg.enabled,
                holdDownState = showBatteryColorSheet,
                onClick = {
                    showBatteryColorSheet = true
                },
            )
            SwitchPreference(
                title = stringResource(R.string.battery_fill_follow_retract),
                summary = stringResource(R.string.battery_fill_follow_retract_summary),
                checked = visual.fillFollowsRetract,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setFillFollowsRetract,
            )
            SwitchPreference(
                title = stringResource(R.string.battery_top_readout),
                summary = stringResource(R.string.battery_top_readout_summary),
                checked = visual.showTopReadout,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setTopReadout,
            )
            AnimatedPreferenceGroup(visible = visual.showTopReadout) {
                val textUiScale =
                    topTextUiScale(
                        visual.topTextScale,
                    )
                SliderPreference(
                    value = textUiScale,
                    onValueChange = visualRepo::setTopTextScale,
                    title = stringResource(R.string.battery_top_text_size),
                    valueText =
                        stringResource(
                            R.string.battery_top_scale_value,
                            (textUiScale * 100f).roundToInt(),
                        ),
                    valueRange =
                        BATTERY_TOP_TEXT_UI_SCALE_MIN..
                            BATTERY_TOP_TEXT_UI_SCALE_MAX,
                    steps = 23,
                    showKeyPoints = true,
                    keyPoints =
                        listOf(
                            topTextUiScaleDefault(
                                visual.layout,
                            ),
                        ),
                    magnetThreshold = 0.035f,
                    enabled = featureCfg.enabled,
                )
                SliderPreference(
                    value = visual.topTextWeight.toFloat(),
                    onValueChange = { value ->
                        visualRepo.setTopTextWeight(value.roundToInt())
                    },
                    title = stringResource(R.string.battery_top_text_weight),
                    valueText =
                        stringResource(
                            R.string.battery_top_weight_value,
                            visual.topTextWeight,
                        ),
                    valueRange =
                        BATTERY_TOP_TEXT_WEIGHT_MIN.toFloat()..
                            BATTERY_TOP_TEXT_WEIGHT_MAX.toFloat(),
                    steps = 19,
                    showKeyPoints = true,
                    keyPoints = listOf(900f),
                    magnetThreshold = 0.035f,
                    enabled = featureCfg.enabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.battery_text_follow_battery_color),
                    summary = stringResource(R.string.battery_text_follow_battery_color_summary),
                    checked = visual.topTextFollowsBatteryColor,
                    enabled = featureCfg.enabled,
                    onCheckedChange = visualRepo::setTopTextFollowsBatteryColor,
                )
            }

            SwitchPreference(
                title = stringResource(R.string.battery_charging_icon),
                summary = stringResource(R.string.battery_charging_icon_summary),
                checked = visual.showTopChargingIcon,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setTopChargingIcon,
            )
            AnimatedPreferenceGroup(visible = visual.showTopChargingIcon) {
                val chargingIconUiScale =
                    topChargingIconUiScale(
                        visual.topChargingIconScale,
                    )
                SliderPreference(
                    value = chargingIconUiScale,
                    onValueChange = visualRepo::setTopChargingIconScale,
                    title = stringResource(R.string.battery_top_charging_icon_size),
                    valueText =
                        stringResource(
                            R.string.battery_top_scale_value,
                            (chargingIconUiScale * 100f).roundToInt(),
                        ),
                    valueRange =
                        BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN..
                            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
                    steps = 23,
                    showKeyPoints = true,
                    keyPoints =
                        listOf(
                            topChargingIconUiScaleDefault(
                                visual.layout,
                            ),
                        ),
                    magnetThreshold = 0.035f,
                    enabled = featureCfg.enabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.charging_icon_follow_battery_color),
                    summary = stringResource(R.string.charging_icon_follow_battery_color_summary),
                    checked = visual.topChargingIconFollowsBatteryColor,
                    enabled = featureCfg.enabled,
                    onCheckedChange =
                        visualRepo::setTopChargingIconFollowsBatteryColor,
                )
            }

        },
        quaternarySectionTitle = stringResource(R.string.section_management),
        quaternaryContent = {
            BasicComponent(
                title = stringResource(R.string.restore_feature_defaults),
                summary = stringResource(R.string.restore_feature_defaults_summary),
                onClick = { showResetDialog = true },
            )
        },
        overlay = {
            BatteryColorBottomSheet(
                show = showBatteryColorSheet,
                library = batteryColorSchemeLibrary,
                repository = batteryColorSchemeRepository,
                onDismiss = {
                    showBatteryColorSheet = false
                },
            )
            OverlayDialog(
                title = stringResource(R.string.restore_feature_defaults),
                summary = stringResource(R.string.restore_feature_defaults_dialog_summary),
                show = showResetDialog,
                onDismissRequest = { showResetDialog = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = { showResetDialog = false },
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.restore),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            showResetDialog = false
                            featureRepo.reset()
                            visualRepo.resetToDefaults()
                        },
                    )
                }
            }
        },
    ) {
        SwitchPreference(
            title = stringResource(R.string.keyguard_combined_status_title),
            summary = stringResource(R.string.keyguard_combined_status_summary),
            checked = featureCfg.keyguard,
            enabled = featureCfg.enabled,
            onCheckedChange = featureRepo::setKeyguard,
        )
        SwitchPreference(
            title = stringResource(R.string.aod_combined_status_title),
            summary = stringResource(R.string.aod_combined_status_summary),
            checked = featureCfg.aod,
            enabled = featureCfg.enabled,
            onCheckedChange = featureRepo::setAod,
        )
        OverlayDropdownPreference(
            items = layoutOptions,
            selectedIndex = visual.layout.ordinal,
            title = stringResource(R.string.content_layout_title),
            summary = stringResource(R.string.content_layout_summary),
            showValue = true,
            enabled = featureCfg.enabled,
            onSelectedIndexChange = { index ->
                ContentLayout.entries
                    .getOrNull(index)
                    ?.let(visualRepo::setLayout)
            },
        )
        val topInfoVerticalOffsetUi =
            topOffsetYUi(
                visual.topOffsetY,
            )
        SliderPreference(
            value = topInfoVerticalOffsetUi,
            onValueChange = visualRepo::setTopOffsetY,
            title = stringResource(R.string.top_info_vertical_offset),
            valueText =
                stringResource(
                    R.string.battery_top_offset_value,
                    topInfoVerticalOffsetUi.roundToInt(),
                ),
            valueRange =
                BATTERY_TOP_VERTICAL_OFFSET_UI_MIN..
                    BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
            steps = 19,
            showKeyPoints = true,
            keyPoints = listOf(0f),
            magnetThreshold = 0.035f,
            enabled = featureCfg.enabled,
        )
        SliderPreference(
            value = visual.combinedScale,
            onValueChange = visualRepo::setCombinedScale,
            title = stringResource(R.string.combined_size),
            valueText =
                stringResource(
                    R.string.percent_value,
                    (visual.combinedScale * 100f).roundToInt(),
                ),
            valueRange = COMBINED_SCALE_MIN..COMBINED_SCALE_MAX,
            steps = 7,
            showKeyPoints = true,
            keyPoints = listOf(COMBINED_SCALE_DEFAULT),
            magnetThreshold = 0.035f,
            enabled = featureCfg.enabled,
        )
        SliderPreference(
            value = visual.outerWeightScale,
            onValueChange = visualRepo::setOuterWeightScale,
            title = stringResource(R.string.outer_weight),
            summary = stringResource(R.string.outer_weight_summary),
            valueText =
                stringResource(
                    R.string.percent_value,
                    (visual.outerWeightScale * 100f).roundToInt(),
                ),
            valueRange = OUTER_WEIGHT_SCALE_MIN..OUTER_WEIGHT_SCALE_MAX,
            steps = 11,
            showKeyPoints = true,
            keyPoints = listOf(OUTER_WEIGHT_SCALE_DEFAULT),
            magnetThreshold = 0.035f,
            enabled = featureCfg.enabled,
        )
        SwitchPreference(
            title = stringResource(R.string.control_center_tint_transition),
            summary = stringResource(R.string.control_center_tint_transition_summary),
            checked = visual.ccTintTransition,
            enabled = featureCfg.enabled,
            onCheckedChange = visualRepo::setCcTintTransition,
        )
    }
}

@Composable
internal fun SettingsHubScreen(
    bottomContentPadding: Dp,
    lang: AppLang,
    iconHidden: Boolean,
    swipeBackEnabled: Boolean,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val languageOptions = listOf(
        stringResource(R.string.language_system),
        stringResource(R.string.language_english),
        stringResource(R.string.language_simplified_chinese),
    )
    val scope = rememberCoroutineScope()
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showRestartFailure by rememberSaveable { mutableStateOf(false) }
    var restartInProgress by rememberSaveable { mutableStateOf(false) }
    var restartAfterDialogDismiss by remember { mutableStateOf(false) }

    HubPage(
        title = stringResource(R.string.settings_title),
        sectionTitle = stringResource(R.string.section_appearance_interaction),
        bottomContentPadding = bottomContentPadding,
        secondarySectionTitle = stringResource(R.string.section_app),
        secondaryContent = {
            OverlayDropdownPreference(
                items = languageOptions,
                selectedIndex = lang.ordinal,
                title = stringResource(R.string.language_title),
                summary = stringResource(R.string.language_summary),
                showValue = true,
                onSelectedIndexChange = { index ->
                    AppLang.entries.getOrNull(index)?.let(onLangChange)
                },
            )
            SwitchPreference(
                title = stringResource(R.string.hide_launcher_icon),
                summary = stringResource(R.string.hide_launcher_icon_summary),
                checked = iconHidden,
                onCheckedChange = onIconHiddenChange,
            )
        },
        tertiarySectionTitle = stringResource(R.string.section_diagnostics_maintenance),
        tertiaryContent = {
            ArrowPreference(
                title = stringResource(R.string.about_title),
                summary = stringResource(R.string.about_summary),
                onClick = { onNavigate(AppRoute.About) },
            )
            ArrowPreference(
                title = stringResource(R.string.diagnostics_title),
                summary = stringResource(R.string.diagnostics_summary),
                onClick = { onNavigate(AppRoute.Diagnostics) },
            )
            BasicComponent(
                title = stringResource(R.string.restart_scope),
                summary = stringResource(R.string.restart_scope_summary),
                enabled = !restartInProgress,
                onClick = { showRestartDialog = true },
            )
        },
        overlay = {
            OverlayDialog(
                title = stringResource(R.string.restart_scope),
                summary = stringResource(R.string.restart_scope_dialog_summary),
                show = showRestartDialog,
                onDismissRequest = {
                    restartAfterDialogDismiss = false
                    showRestartDialog = false
                },
                onDismissFinished = {
                    if (restartAfterDialogDismiss && !restartInProgress) {
                        restartAfterDialogDismiss = false
                        restartInProgress = true
                        scope.launch {
                            val success = SysUiScope.restart()
                            restartInProgress = false
                            if (!success) {
                                showRestartFailure = true
                            }
                        }
                    }
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            restartAfterDialogDismiss = false
                            showRestartDialog = false
                        },
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.restart),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            restartAfterDialogDismiss = true
                            showRestartDialog = false
                        },
                    )
                }
            }

            OverlayDialog(
                title = stringResource(R.string.restart_scope_failed),
                summary = stringResource(R.string.restart_scope_failed_summary),
                show = showRestartFailure,
                onDismissRequest = { showRestartFailure = false },
            ) {
                TextButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { showRestartFailure = false },
                )
            }
        },
    ) {
        ArrowPreference(
            title = stringResource(R.string.appearance_title),
            summary = stringResource(R.string.appearance_summary),
            onClick = { onNavigate(AppRoute.Appearance) },
        )
        SwitchPreference(
            title = stringResource(R.string.swipe_back),
            summary = stringResource(R.string.swipe_back_summary),
            checked = swipeBackEnabled,
            onCheckedChange = onSwipeBackChange,
        )
    }
}

@Composable
internal fun AnimatedPreferenceGroup(
    visible: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun HubPage(
    title: String,
    sectionTitle: String,
    bottomContentPadding: Dp,
    secondarySectionTitle: String? = null,
    secondaryContent: (@Composable ColumnScope.() -> Unit)? = null,
    tertiarySectionTitle: String? = null,
    tertiaryContent: (@Composable ColumnScope.() -> Unit)? = null,
    quaternarySectionTitle: String? = null,
    quaternaryContent: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                TopAppBar(
                    title = title,
                    color = barColor,
                    scrollBehavior = scrollBehavior,
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
                contentPadding = pageContentPadding(
                    innerPadding = paddingValues,
                    outerBottomPadding = bottomContentPadding,
                    extraBottom = 12.dp,
                ),
            ) {
                item {
                    SmallTitle(sectionTitle)
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        content = content,
                    )
                }

                if (secondarySectionTitle != null && secondaryContent != null) {
                    item {
                        SmallTitle(secondarySectionTitle)
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            content = secondaryContent,
                        )
                    }
                }

                if (tertiarySectionTitle != null && tertiaryContent != null) {
                    item {
                        SmallTitle(tertiarySectionTitle)
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            content = tertiaryContent,
                        )
                    }
                }

                if (quaternarySectionTitle != null && quaternaryContent != null) {
                    item {
                        SmallTitle(quaternarySectionTitle)
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            content = quaternaryContent,
                        )
                    }
                }
            }
        }

        overlay()
    }
}
