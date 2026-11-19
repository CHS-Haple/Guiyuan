package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MIN
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_MIN
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_MAX
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.NativeFontAxis
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.WIFI_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.WIFI_WEIGHT_MIN
import com.chaners.guiyuan.settings.WIFI_WEIGHT_MAX
import com.chaners.guiyuan.settings.BatterySchemeRepo
import com.chaners.guiyuan.settings.ConfigBackup
import com.chaners.guiyuan.settings.ConfigBackupFiles
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_MIN
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_MAX
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.topChargingIconUiScale
import com.chaners.guiyuan.settings.topChargingIconUiScaleDefault
import com.chaners.guiyuan.settings.topTextUiScale
import com.chaners.guiyuan.settings.topTextUiScaleDefault
import com.chaners.guiyuan.settings.mobileTypeScaleDefault
import com.chaners.guiyuan.ui.navigation.AppRoute
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SliderDefaults
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

@Composable
internal fun FeaturesScreen(
    bottomContentPadding: Dp,
    visible: Boolean,
    snackbarState: SnackbarHostState,
    search: PageSearch,
    searchInNavigationBar: Boolean,
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
    val fontAxis by
        remember(context.applicationContext) {
            NativeFontAxis.observe(context.applicationContext)
        }.collectAsState(initial = NativeFontAxis.State.Pending)
    val batteryAxis by
        remember(context.applicationContext) {
            NativeFontAxis.observeBattery(context.applicationContext)
        }.collectAsState(initial = NativeFontAxis.State.Pending)
    val mobileWeight = featureWeightUi(fontAxis, 200..1000)
    val batteryWeight = featureWeightUi(
        batteryAxis,
        BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
    )
    val schemeRepo =
        remember(context.applicationContext) {
            BatterySchemeRepo(context.applicationContext)
        }
    val schemeLibrary by
        schemeRepo.library.collectAsState(
            initial = schemeRepo.current(),
        )
    val backupFiles = remember(context.applicationContext) {
        ConfigBackupFiles(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<ConfigBackup?>(null) }
    var ioBusy by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.config_backup_success)
    val restoredMsg = stringResource(R.string.config_restore_success)
    val saveError = stringResource(R.string.config_backup_failed)
    val restoreError = stringResource(R.string.config_restore_failed)
    val invalidBackup = stringResource(R.string.config_restore_invalid)
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null && !ioBusy) {
            ioBusy = true
            scope.launch {
                val result = runCatching { backupFiles.export(uri) }
                ioBusy = false
                snackbarState.showSnackbar(if (result.isSuccess) savedMsg else saveError)
            }
        }
    }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && !ioBusy) {
            ioBusy = true
            scope.launch {
                val result = runCatching { backupFiles.read(uri) }
                ioBusy = false
                if (result.isSuccess) {
                    pendingImport = result.getOrThrow()
                } else {
                    snackbarState.showSnackbar(invalidBackup)
                }
            }
        }
    }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryColorSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (!visible) search.close()
    }
    val searching = search.filtering
    val bothSidesChargingSearch = searching && search.any(
        R.string.both_sides_charging_title,
        R.string.both_sides_charging_vertical_offset,
    )
    val displayMatches = search.anyFrom(FeatureSearchIds.display)
    val layoutMatches = search.anyFrom(FeatureSearchIds.layout)
    val networkMatches = search.anyFrom(FeatureSearchIds.network)
    val batteryMatches = search.anyFrom(FeatureSearchIds.battery) ||
        (bothSidesChargingSearch && !visual.showChargingIcon)
    val managementMatches = search.anyFrom(FeatureSearchIds.management)

    HubPage(
        title = stringResource(R.string.features_title),
        bottomContentPadding = bottomContentPadding,
        search = search,
        searchInNavigationBar = searchInNavigationBar,
        overlay = {
            val selectedImport = pendingImport
            UiOverlayDialog(
                title = stringResource(R.string.config_restore_title),
                summary = stringResource(R.string.config_restore_confirm),
                show = selectedImport != null,
                onDismissRequest = { if (!ioBusy) pendingImport = null },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    UiDialogButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        enabled = !ioBusy,
                        onClick = { pendingImport = null },
                    )
                    Spacer(Modifier.width(dialogActionGap()))
                    UiDialogButton(
                        text = stringResource(R.string.restore),
                        modifier = Modifier.weight(1f),
                        enabled = !ioBusy,
                        role = GlassDialogButtonRole.Primary,
                        onClick = {
                            val selected = selectedImport
                            if (selected != null && !ioBusy) {
                                ioBusy = true
                                scope.launch {
                                    val result = runCatching { backupFiles.restore(selected) }
                                    pendingImport = null
                                    ioBusy = false
                                    snackbarState.showSnackbar(
                                        if (result.getOrNull() == true) restoredMsg
                                        else restoreError,
                                    )
                                }
                            }
                        },
                    )
                }
            }
            BatteryColorBottomSheet(
                show = showBatteryColorSheet,
                library = schemeLibrary,
                repository = schemeRepo,
                onDismiss = {
                    showBatteryColorSheet = false
                },
            )
            IntegerInputDialog(
                show = showWeightDialog && mobileWeight.verifiedRange != null,
                title = stringResource(R.string.mobile_type_weight),
                summary = mobileWeight.rangeText.orEmpty(),
                value = visual.mobileTypeWeight.coerceIn(
                    mobileWeight.valueRange.start.roundToInt(),
                    mobileWeight.valueRange.endInclusive.roundToInt(),
                ),
                range = mobileWeight.verifiedRange ?: 200..1000,
                onDismiss = { showWeightDialog = false },
                onConfirm = { value ->
                    showWeightDialog = false
                    mobileWeight.verifiedRange?.let { range ->
                        visualRepo.setMobileTypeWeight(value.coerceIn(range))
                    }
                },
            )
            IntegerInputDialog(
                show = showBatteryWeightDialog && batteryWeight.verifiedRange != null,
                title = stringResource(R.string.battery_top_text_weight),
                summary = batteryWeight.rangeText.orEmpty(),
                value = visual.topTextWeight.coerceIn(
                    batteryWeight.valueRange.start.roundToInt(),
                    batteryWeight.valueRange.endInclusive.roundToInt(),
                ),
                range = batteryWeight.verifiedRange ?: BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
                onDismiss = { showBatteryWeightDialog = false },
                onConfirm = { value ->
                    showBatteryWeightDialog = false
                    batteryWeight.verifiedRange?.let { range ->
                        visualRepo.setTopTextWeight(value.coerceIn(range))
                    }
                },
            )
            UiOverlayDialog(
                title = stringResource(R.string.restore_feature_defaults),
                summary = stringResource(R.string.restore_feature_defaults_dialog_summary),
                show = showResetDialog,
                onDismissRequest = { showResetDialog = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UiDialogButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = { showResetDialog = false },
                    )
                    Spacer(Modifier.width(dialogActionGap()))
                    UiDialogButton(
                        text = stringResource(R.string.restore),
                        modifier = Modifier.weight(1f),
                        role = GlassDialogButtonRole.Primary,
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
        if (displayMatches) {
            Section(R.string.section_display_transition) {
                if (search.matches(R.string.keyguard_combined_status_title)) {
                    SwitchPreference(
                        title = stringResource(R.string.keyguard_combined_status_title),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_lock, featureCfg.enabled) },
                        summary = stringResource(R.string.keyguard_combined_status_summary),
                        checked = featureCfg.keyguard,
                        enabled = featureCfg.enabled,
                        onCheckedChange = featureRepo::setKeyguard,
                    )
                }
                if (search.matches(R.string.aod_combined_status_title)) {
                    SwitchPreference(
                        title = stringResource(R.string.aod_combined_status_title),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bedtime, featureCfg.enabled) },
                        summary = stringResource(R.string.aod_combined_status_summary),
                        checked = featureCfg.aod,
                        enabled = featureCfg.enabled,
                        onCheckedChange = featureRepo::setAod,
                    )
                }
                if (search.matches(R.string.control_center_tint_transition)) {
                    SwitchPreference(
                        title = stringResource(R.string.control_center_tint_transition),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_contrast, featureCfg.enabled) },
                        summary = stringResource(R.string.control_center_tint_transition_summary),
                        checked = visual.ccTintTransition,
                        enabled = featureCfg.enabled,
                        onCheckedChange = visualRepo::setCcTintTransition,
                    )
                }
                if (search.matches(R.string.battery_fill_follow_retract)) {
                    SwitchPreference(
                        title = stringResource(R.string.battery_fill_follow_retract),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_donut_large, featureCfg.enabled) },
                        summary = stringResource(R.string.battery_fill_follow_retract_summary),
                        checked = visual.fillFollowsRetract,
                        enabled = featureCfg.enabled,
                        onCheckedChange = visualRepo::setFillFollowsRetract,
                    )
                }
            }
        }
        if (layoutMatches) {
            featureLayoutSection(
                search = search,
                visual = visual,
                visualRepo = visualRepo,
                enabled = featureCfg.enabled,
                bothSidesChargingSearch = bothSidesChargingSearch,
            )
        }
        if (networkMatches) {
            Section(R.string.section_network) {
                if (search.matches(R.string.wifi_size)) {
                    NetworkSizeSlider(
                        value = visual.wifiScale,
                        onValueChange = visualRepo::setWifiScale,
                        row = NetworkSizeRow.WIFI,
                        defaultValue = WIFI_SIZE_SCALE_DEFAULT,
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.wifi_weight)) {
                    NetworkWeightSlider(
                        value = visual.wifiWeight,
                        onValueChange = visualRepo::setWifiWeight,
                        row = NetworkWeightRow.WIFI,
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.airplane_size)) {
                    NetworkSizeSlider(
                        value = visual.airplaneScale,
                        onValueChange = visualRepo::setAirplaneScale,
                        row = NetworkSizeRow.AIRPLANE,
                        defaultValue = AIRPLANE_SIZE_SCALE_DEFAULT,
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.airplane_weight)) {
                    NetworkWeightSlider(
                        value = visual.airplaneWeight,
                        onValueChange = visualRepo::setAirplaneWeight,
                        row = NetworkWeightRow.AIRPLANE,
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.no_sim_size)) {
                    NetworkSizeSlider(
                        value = visual.noSimScale,
                        onValueChange = visualRepo::setNoSimScale,
                        row = NetworkSizeRow.NO_SIM,
                        defaultValue = NO_SIM_SIZE_SCALE_DEFAULT,
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.mobile_type_size)) {
                    NetworkSizeSlider(
                        value = visual.mobileTypeScale,
                        onValueChange = visualRepo::setMobileTypeScale,
                        row = NetworkSizeRow.MOBILE_TYPE,
                        defaultValue = mobileTypeScaleDefault(visual.layout),
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.mobile_type_weight)) {
                    SliderPreference(
                        value = visual.mobileTypeWeight.toFloat().coerceIn(
                            mobileWeight.valueRange.start, mobileWeight.valueRange.endInclusive,
                        ),
                        onValueChange = { value ->
                            visualRepo.setMobileTypeWeight(value.roundToInt())
                        },
                        title = stringResource(R.string.mobile_type_weight),
                        summary = mobileWeight.rangeText
                            ?: stringResource(
                                if (mobileWeight.pending) R.string.font_weight_pending
                                else R.string.font_weight_unverified,
                            ),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_5g, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                        valueText = stringResource(
                            R.string.integer_value,
                            visual.mobileTypeWeight.coerceIn(
                                mobileWeight.valueRange.start.roundToInt(),
                                mobileWeight.valueRange.endInclusive.roundToInt(),
                            ),
                        ),
                        valueRange = mobileWeight.valueRange,
                        steps = 0,
                        keyPoints = mobileWeight.points,
                        magnetThreshold = mobileWeight.verifiedRange?.let(::weightSnapThreshold) ?: 0f,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        holdDownState = showWeightDialog,
                        enabled = featureCfg.enabled && mobileWeight.verifiedRange != null,
                        onClick = { showWeightDialog = true },
                    )
                }
                if (search.matches(R.string.mobile_follow_battery_color)) {
                    SwitchPreference(
                        title = stringResource(R.string.mobile_follow_battery_color),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                        summary = stringResource(R.string.mobile_follow_battery_color_summary),
                        checked = visual.mobileFollowsBatteryColor,
                        enabled = featureCfg.enabled,
                        onCheckedChange = visualRepo::setMobileFollowsBatteryColor,
                    )
                }
                if (search.matches(R.string.center_follow_battery_color)) {
                    SwitchPreference(
                        title = stringResource(R.string.center_follow_battery_color),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_wifi, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                        summary = stringResource(R.string.center_follow_battery_color_summary),
                        checked = visual.centerFollowsBatteryColor,
                        enabled = featureCfg.enabled,
                        onCheckedChange = visualRepo::setCenterFollowsBatteryColor,
                    )
                }
            }
        }
        if (batteryMatches) {
            Section(R.string.section_battery) {
                if (search.matches(R.string.battery_colors)) {
                BatteryColorPreference(
                    library = schemeLibrary,
                    enabled = featureCfg.enabled,
                    holdDownState = showBatteryColorSheet,
                    onClick = {
                        showBatteryColorSheet = true
                    },
                )
                }
                if (search.matches(R.string.low_battery_threshold)) {
                    SliderPreference(
                        value = featureCfg.lowBatteryThreshold.toFloat(),
                        onValueChange = { featureRepo.setLowBatteryThreshold(it.roundToInt()) },
                        title = stringResource(R.string.low_battery_threshold),
                        summary = stringResource(R.string.low_battery_threshold_summary),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_alert, featureCfg.enabled) },
                        valueText = stringResource(R.string.percent_value, featureCfg.lowBatteryThreshold),
                        valueRange = LOW_BATTERY_THRESHOLD_MIN.toFloat()..LOW_BATTERY_THRESHOLD_MAX.toFloat(),
                        steps = LOW_BATTERY_THRESHOLD_MAX - LOW_BATTERY_THRESHOLD_MIN - 1,
                        showKeyPoints = true,
                        keyPoints = listOf(LOW_BATTERY_THRESHOLD_DEFAULT.toFloat()),
                        magnetThreshold = 0f,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        enabled = featureCfg.enabled,
                    )
                }
                if (search.matches(R.string.battery_top_readout) ||
                    (searching && !visual.showTopReadout &&
                        search.any(R.string.battery_top_text_size, R.string.battery_top_text_weight,
                            R.string.battery_text_follow_battery_color))) {
                    SwitchPreference(
                        title = stringResource(R.string.battery_top_readout),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled) },
                        summary = stringResource(R.string.battery_top_readout_summary),
                        checked = visual.showTopReadout,
                        enabled = featureCfg.enabled,
                        onCheckedChange = visualRepo::setTopReadout,
                    )
                }
                AnimatedPreferenceGroup(visible = visual.showTopReadout) {
                    val textUiScale =
                        topTextUiScale(
                            visual.topTextScale,
                        )
                    if (search.matches(R.string.battery_top_text_size)) {
                        SliderPreference(
                            value = textUiScale,
                            onValueChange = visualRepo::setTopTextScale,
                            title = stringResource(R.string.battery_top_text_size),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
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
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            keyPoints =
                                listOf(
                                    topTextUiScaleDefault(
                                        visual.layout,
                                    ),
                                ),
                            magnetThreshold = 0.035f,
                            enabled = featureCfg.enabled,
                        )
                    }
                    if (search.matches(R.string.battery_top_text_weight)) {
                        SliderPreference(
                            value = visual.topTextWeight.toFloat().coerceIn(
                                batteryWeight.valueRange.start, batteryWeight.valueRange.endInclusive,
                            ),
                            onValueChange = { value ->
                                visualRepo.setTopTextWeight(value.roundToInt())
                            },
                            title = stringResource(R.string.battery_top_text_weight),
                            summary = batteryWeight.rangeText
                                ?: stringResource(
                                    if (batteryWeight.pending) R.string.font_weight_pending
                                    else R.string.font_weight_unverified,
                                ),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                            valueText = stringResource(
                                R.string.integer_value,
                                visual.topTextWeight.coerceIn(
                                    batteryWeight.valueRange.start.roundToInt(),
                                    batteryWeight.valueRange.endInclusive.roundToInt(),
                                ),
                            ),
                            valueRange = batteryWeight.valueRange,
                            steps = 0,
                            keyPoints = batteryWeight.points,
                            magnetThreshold = batteryWeight.verifiedRange?.let(::weightSnapThreshold) ?: 0f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            holdDownState = showBatteryWeightDialog,
                            enabled = featureCfg.enabled && batteryWeight.verifiedRange != null,
                            onClick = { showBatteryWeightDialog = true },
                        )
                    }
                    if (search.matches(R.string.battery_text_follow_battery_color)) {
                        SwitchPreference(
                            title = stringResource(R.string.battery_text_follow_battery_color),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                            summary = stringResource(R.string.battery_text_follow_battery_color_summary),
                            checked = visual.topTextFollowsBatteryColor,
                            enabled = featureCfg.enabled,
                            onCheckedChange = visualRepo::setTopTextFollowsBatteryColor,
                        )
                    }
                }

                if (search.matches(R.string.battery_charging_icon) ||
                    (searching && !visual.showChargingIcon &&
                        (search.any(R.string.battery_top_charging_icon_size,
                            R.string.charging_icon_follow_battery_color) || bothSidesChargingSearch))) {
                    SwitchPreference(
                        title = stringResource(R.string.battery_charging_icon),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, featureCfg.enabled) },
                        summary = stringResource(R.string.battery_charging_icon_summary),
                        checked = visual.showChargingIcon,
                        enabled = featureCfg.enabled,
                        onCheckedChange = visualRepo::setChargingIcon,
                    )
                }
                AnimatedPreferenceGroup(
                    visible = visual.showChargingIcon,
                ) {
                    val chargingIconUiScale =
                        topChargingIconUiScale(
                            visual.topChargingIconScale,
                        )
                    if (search.matches(R.string.battery_top_charging_icon_size)) {
                        SliderPreference(
                            value = chargingIconUiScale,
                            onValueChange = visualRepo::setTopChargingIconScale,
                            title = stringResource(R.string.battery_top_charging_icon_size),
                            summary = stringResource(R.string.battery_charging_size_summary),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
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
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            keyPoints =
                                listOf(
                                    topChargingIconUiScaleDefault(
                                        visual.layout,
                                    ),
                                ),
                            magnetThreshold = 0.035f,
                            enabled = featureCfg.enabled,
                        )
                    }
                    if (search.matches(R.string.charging_icon_follow_battery_color)) {
                        SwitchPreference(
                            title = stringResource(R.string.charging_icon_follow_battery_color),
                            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                            summary = stringResource(R.string.charging_icon_follow_battery_color_summary),
                            checked = visual.topChargingIconFollowsBatteryColor,
                            enabled = featureCfg.enabled,
                            onCheckedChange =
                                visualRepo::setTopChargingIconFollowsBatteryColor,
                        )
                    }
                }
            }
        }
        if (managementMatches) {
            Section(R.string.section_management) {
                if (search.matches(R.string.config_backup_title)) {
                    BasicComponent(
                        title = stringResource(R.string.config_backup_title),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_save) },
                        summary = stringResource(R.string.config_backup_summary),
                        enabled = !ioBusy,
                        onClick = { createDocument.launch(backupFiles.fileName()) },
                    )
                }
                if (search.matches(R.string.config_restore_title)) {
                    BasicComponent(
                        title = stringResource(R.string.config_restore_title),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_file_open) },
                        summary = stringResource(R.string.config_restore_summary),
                        enabled = !ioBusy,
                        onClick = { openDocument.launch(arrayOf("application/json", "text/plain")) },
                    )
                }
                if (search.matches(R.string.restore_feature_defaults)) {
                    BasicComponent(
                        title = stringResource(R.string.restore_feature_defaults),
                        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_settings_backup_restore) },
                        summary = stringResource(R.string.restore_feature_defaults_summary),
                        onClick = { showResetDialog = true },
                    )
                }
            }
        }
        if (search.filtering &&
            !displayMatches && !layoutMatches && !networkMatches &&
            !batteryMatches && !managementMatches
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.feature_search_empty),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
        }
    }
}

private enum class NetworkSizeRow(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val range: ClosedFloatingPointRange<Float>,
) {
    WIFI(R.string.wifi_size, R.drawable.ic_material_symbol_wifi, WIFI_SIZE_SCALE_MIN..WIFI_SIZE_SCALE_MAX),
    AIRPLANE(R.string.airplane_size, R.drawable.ic_material_symbol_airplanemode_active, AIRPLANE_SIZE_SCALE_MIN..AIRPLANE_SIZE_SCALE_MAX),
    NO_SIM(R.string.no_sim_size, R.drawable.ic_material_symbol_sim_card, NO_SIM_SIZE_SCALE_MIN..NO_SIM_SIZE_SCALE_MAX),
    MOBILE_TYPE(R.string.mobile_type_size, R.drawable.ic_material_symbol_5g, MOBILE_TYPE_SIZE_SCALE_MIN..MOBILE_TYPE_SIZE_SCALE_MAX),
}

private enum class NetworkWeightRow(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val range: IntRange,
    val defaultValue: Int,
) {
    WIFI(R.string.wifi_weight, R.drawable.ic_material_symbol_wifi, WIFI_WEIGHT_MIN..WIFI_WEIGHT_MAX, WIFI_WEIGHT_DEFAULT),
    AIRPLANE(R.string.airplane_weight, R.drawable.ic_material_symbol_airplanemode_active, AIRPLANE_WEIGHT_MIN..AIRPLANE_WEIGHT_MAX, AIRPLANE_WEIGHT_DEFAULT),
}

@Composable
private fun NetworkSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    row: NetworkSizeRow,
    defaultValue: Float,
    enabled: Boolean,
) {
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        title = stringResource(row.titleRes),
        startAction = {
            SemanticLeadingIcon(row.iconRes, enabled, R.drawable.ic_material_symbol_open_in_full)
        },
        valueText = stringResource(R.string.percent_value, (value * 100f).roundToInt()),
        valueRange = row.range,
        steps = 16,
        showKeyPoints = true,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        keyPoints = listOf(defaultValue),
        magnetThreshold = 0.035f,
        enabled = enabled,
    )
}

@Composable
private fun NetworkWeightSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    row: NetworkWeightRow,
    enabled: Boolean,
) {
    SliderPreference(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        title = stringResource(row.titleRes),
        startAction = {
            SemanticLeadingIcon(row.iconRes, enabled, R.drawable.ic_material_symbol_line_weight)
        },
        valueText = stringResource(R.string.percent_value, value),
        valueRange = row.range.first.toFloat()..row.range.last.toFloat(),
        steps = 13,
        showKeyPoints = true,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        keyPoints = listOf(row.defaultValue.toFloat()),
        magnetThreshold = 0.035f,
        enabled = enabled,
    )
}
