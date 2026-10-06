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
import com.chaners.guiyuan.settings.AppLanguage
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
import com.chaners.guiyuan.settings.FeatureSettingsRepo
import com.chaners.guiyuan.settings.VisualSettingsRepo
import com.chaners.guiyuan.settings.batteryTopChargingIconUiScale
import com.chaners.guiyuan.settings.batteryTopChargingIconUiScaleDefault
import com.chaners.guiyuan.settings.batteryTopTextUiScale
import com.chaners.guiyuan.settings.batteryTopTextUiScaleDefault
import com.chaners.guiyuan.settings.batteryTopVerticalOffsetUi
import com.chaners.guiyuan.settings.mobileTypeSizeScaleDefault
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
    val featureRepository =
        remember(context.applicationContext) {
            FeatureSettingsRepo(context.applicationContext)
        }
    val featureSettings by
        featureRepository.settings.collectAsState(
            initial = featureRepository.current(),
        )
    val visualRepository =
        remember(context.applicationContext) {
            VisualSettingsRepo(context.applicationContext)
        }
    val visualSettings by
        visualRepository.settings.collectAsState(
            initial = visualRepository.current(),
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
                value = visualSettings.wifiSizeScale,
                onValueChange = visualRepository::setWifiSizeScale,
                title = stringResource(R.string.wifi_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visualSettings.wifiSizeScale * 100f).roundToInt(),
                    ),
                valueRange = WIFI_SIZE_SCALE_MIN..WIFI_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints = listOf(WIFI_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureSettings.enabled,
            )
            SliderPreference(
                value = visualSettings.airplaneSizeScale,
                onValueChange = visualRepository::setAirplaneSizeScale,
                title = stringResource(R.string.airplane_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visualSettings.airplaneSizeScale * 100f).roundToInt(),
                    ),
                valueRange = AIRPLANE_SIZE_SCALE_MIN..AIRPLANE_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints = listOf(AIRPLANE_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureSettings.enabled,
            )
            SliderPreference(
                value = visualSettings.noSimSizeScale,
                onValueChange = visualRepository::setNoSimSizeScale,
                title = stringResource(R.string.no_sim_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visualSettings.noSimSizeScale * 100f).roundToInt(),
                    ),
                valueRange = NO_SIM_SIZE_SCALE_MIN..NO_SIM_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints = listOf(NO_SIM_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureSettings.enabled,
            )
            SliderPreference(
                value = visualSettings.mobileTypeSizeScale,
                onValueChange = visualRepository::setMobileTypeSizeScale,
                title = stringResource(R.string.mobile_type_size),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visualSettings.mobileTypeSizeScale * 100f).roundToInt(),
                    ),
                valueRange = MOBILE_TYPE_SIZE_SCALE_MIN..MOBILE_TYPE_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                keyPoints =
                    listOf(
                        mobileTypeSizeScaleDefault(
                            visualSettings.contentLayout,
                        ),
                    ),
                magnetThreshold = 0.035f,
                enabled = featureSettings.enabled,
            )
            SliderPreference(
                value = visualSettings.mobileTypeWeight.toFloat(),
                onValueChange = { value ->
                    visualRepository.setMobileTypeWeight(value.roundToInt())
                },
                title = stringResource(R.string.mobile_type_weight),
                valueText =
                    stringResource(
                        R.string.integer_value,
                        visualSettings.mobileTypeWeight,
                    ),
                valueRange =
                    MOBILE_TYPE_WEIGHT_MIN.toFloat()..
                        MOBILE_TYPE_WEIGHT_MAX.toFloat(),
                steps = 19,
                showKeyPoints = true,
                keyPoints = listOf(MOBILE_TYPE_WEIGHT_DEFAULT.toFloat()),
                magnetThreshold = 0.035f,
                enabled = featureSettings.enabled,
            )
            SwitchPreference(
                title = stringResource(R.string.mobile_follow_battery_color),
                summary = stringResource(R.string.mobile_follow_battery_color_summary),
                checked = visualSettings.mobileFollowsBatteryColor,
                enabled = featureSettings.enabled,
                onCheckedChange = visualRepository::setMobileFollowsBatteryColor,
            )
            SwitchPreference(
                title = stringResource(R.string.center_follow_battery_color),
                summary = stringResource(R.string.center_follow_battery_color_summary),
                checked = visualSettings.centerFollowsBatteryColor,
                enabled = featureSettings.enabled,
                onCheckedChange = visualRepository::setCenterFollowsBatteryColor,
            )
        },
        tertiarySectionTitle = stringResource(R.string.section_battery),
        tertiaryContent = {
            BatteryColorPreference(
                library = batteryColorSchemeLibrary,
                enabled = featureSettings.enabled,
                holdDownState = showBatteryColorSheet,
                onClick = {
                    showBatteryColorSheet = true
                },
            )
            SwitchPreference(
                title = stringResource(R.string.battery_fill_follow_retract),
                summary = stringResource(R.string.battery_fill_follow_retract_summary),
                checked = visualSettings.batteryFillFollowsRetractEndpoint,
                enabled = featureSettings.enabled,
                onCheckedChange = visualRepository::setBatteryFillFollowsRetractEndpoint,
            )
            SwitchPreference(
                title = stringResource(R.string.battery_top_readout),
                summary = stringResource(R.string.battery_top_readout_summary),
                checked = visualSettings.batteryTopReadoutEnabled,
                enabled = featureSettings.enabled,
                onCheckedChange = visualRepository::setBatteryTopReadoutEnabled,
            )
            AnimatedPreferenceGroup(visible = visualSettings.batteryTopReadoutEnabled) {
                val textUiScale =
                    batteryTopTextUiScale(
                        visualSettings.batteryTopTextScale,
                    )
                SliderPreference(
                    value = textUiScale,
                    onValueChange = visualRepository::setBatteryTopTextScale,
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
                            batteryTopTextUiScaleDefault(
                                visualSettings.contentLayout,
                            ),
                        ),
                    magnetThreshold = 0.035f,
                    enabled = featureSettings.enabled,
                )
                SliderPreference(
                    value = visualSettings.batteryTopTextWeight.toFloat(),
                    onValueChange = { value ->
                        visualRepository.setBatteryTopTextWeight(value.roundToInt())
                    },
                    title = stringResource(R.string.battery_top_text_weight),
                    valueText =
                        stringResource(
                            R.string.battery_top_weight_value,
                            visualSettings.batteryTopTextWeight,
                        ),
                    valueRange =
                        BATTERY_TOP_TEXT_WEIGHT_MIN.toFloat()..
                            BATTERY_TOP_TEXT_WEIGHT_MAX.toFloat(),
                    steps = 19,
                    showKeyPoints = true,
                    keyPoints = listOf(900f),
                    magnetThreshold = 0.035f,
                    enabled = featureSettings.enabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.battery_text_follow_battery_color),
                    summary = stringResource(R.string.battery_text_follow_battery_color_summary),
                    checked = visualSettings.batteryTopTextFollowsBatteryColor,
                    enabled = featureSettings.enabled,
                    onCheckedChange = visualRepository::setBatteryTopTextFollowsBatteryColor,
                )
            }

            SwitchPreference(
                title = stringResource(R.string.battery_charging_icon),
                summary = stringResource(R.string.battery_charging_icon_summary),
                checked = visualSettings.batteryTopChargingIconEnabled,
                enabled = featureSettings.enabled,
                onCheckedChange = visualRepository::setBatteryTopChargingIconEnabled,
            )
            AnimatedPreferenceGroup(visible = visualSettings.batteryTopChargingIconEnabled) {
                val chargingIconUiScale =
                    batteryTopChargingIconUiScale(
                        visualSettings.batteryTopChargingIconScale,
                    )
                SliderPreference(
                    value = chargingIconUiScale,
                    onValueChange = visualRepository::setBatteryTopChargingIconScale,
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
                            batteryTopChargingIconUiScaleDefault(
                                visualSettings.contentLayout,
                            ),
                        ),
                    magnetThreshold = 0.035f,
                    enabled = featureSettings.enabled,
                )
                SwitchPreference(
                    title = stringResource(R.string.charging_icon_follow_battery_color),
                    summary = stringResource(R.string.charging_icon_follow_battery_color_summary),
                    checked = visualSettings.batteryTopChargingIconFollowsBatteryColor,
                    enabled = featureSettings.enabled,
                    onCheckedChange =
                        visualRepository::setBatteryTopChargingIconFollowsBatteryColor,
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
                            featureRepository.resetToDefaults()
                            visualRepository.resetToDefaults()
                        },
                    )
                }
            }
        },
    ) {
        SwitchPreference(
            title = stringResource(R.string.keyguard_combined_status_title),
            summary = stringResource(R.string.keyguard_combined_status_summary),
            checked = featureSettings.keyguardEnabled,
            enabled = featureSettings.enabled,
            onCheckedChange = featureRepository::setKeyguardEnabled,
        )
        SwitchPreference(
            title = stringResource(R.string.aod_combined_status_title),
            summary = stringResource(R.string.aod_combined_status_summary),
            checked = featureSettings.aodEnabled,
            enabled = featureSettings.enabled,
            onCheckedChange = featureRepository::setAodEnabled,
        )
        OverlayDropdownPreference(
            items = layoutOptions,
            selectedIndex = visualSettings.contentLayout.ordinal,
            title = stringResource(R.string.content_layout_title),
            summary = stringResource(R.string.content_layout_summary),
            showValue = true,
            enabled = featureSettings.enabled,
            onSelectedIndexChange = { index ->
                ContentLayout.entries
                    .getOrNull(index)
                    ?.let(visualRepository::setContentLayout)
            },
        )
        val topInfoVerticalOffsetUi =
            batteryTopVerticalOffsetUi(
                visualSettings.batteryTopVerticalOffset,
            )
        SliderPreference(
            value = topInfoVerticalOffsetUi,
            onValueChange = visualRepository::setBatteryTopVerticalOffset,
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
            enabled = featureSettings.enabled,
        )
        SliderPreference(
            value = visualSettings.combinedScale,
            onValueChange = visualRepository::setCombinedScale,
            title = stringResource(R.string.combined_size),
            valueText =
                stringResource(
                    R.string.percent_value,
                    (visualSettings.combinedScale * 100f).roundToInt(),
                ),
            valueRange = COMBINED_SCALE_MIN..COMBINED_SCALE_MAX,
            steps = 7,
            showKeyPoints = true,
            keyPoints = listOf(COMBINED_SCALE_DEFAULT),
            magnetThreshold = 0.035f,
            enabled = featureSettings.enabled,
        )
        SliderPreference(
            value = visualSettings.outerWeightScale,
            onValueChange = visualRepository::setOuterWeightScale,
            title = stringResource(R.string.outer_weight),
            summary = stringResource(R.string.outer_weight_summary),
            valueText =
                stringResource(
                    R.string.percent_value,
                    (visualSettings.outerWeightScale * 100f).roundToInt(),
                ),
            valueRange = OUTER_WEIGHT_SCALE_MIN..OUTER_WEIGHT_SCALE_MAX,
            steps = 11,
            showKeyPoints = true,
            keyPoints = listOf(OUTER_WEIGHT_SCALE_DEFAULT),
            magnetThreshold = 0.035f,
            enabled = featureSettings.enabled,
        )
        SwitchPreference(
            title = stringResource(R.string.control_center_tint_transition),
            summary = stringResource(R.string.control_center_tint_transition_summary),
            checked = visualSettings.controlCenterTintTransitionEnabled,
            enabled = featureSettings.enabled,
            onCheckedChange = visualRepository::setControlCenterTintTransitionEnabled,
        )
    }
}

@Composable
internal fun SettingsHubScreen(
    bottomContentPadding: Dp,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    swipeBackEnabled: Boolean,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
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
                selectedIndex = appLanguage.ordinal,
                title = stringResource(R.string.language_title),
                summary = stringResource(R.string.language_summary),
                showValue = true,
                onSelectedIndexChange = { index ->
                    AppLanguage.entries.getOrNull(index)?.let(onAppLanguageChange)
                },
            )
            SwitchPreference(
                title = stringResource(R.string.hide_launcher_icon),
                summary = stringResource(R.string.hide_launcher_icon_summary),
                checked = launcherIconHidden,
                onCheckedChange = onLauncherIconHiddenChange,
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
            onCheckedChange = onSwipeBackEnabledChange,
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
