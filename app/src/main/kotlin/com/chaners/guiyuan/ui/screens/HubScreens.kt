package com.chaners.guiyuan.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassSearch
import com.chaners.guiyuan.GyApplication
import com.chaners.guiyuan.settings.SettingsEntry
import com.chaners.guiyuan.settings.SettingsEntryCfg
import com.chaners.guiyuan.settings.SettingsEntryPrefs
import com.chaners.guiyuan.settings.SettingsEntryIcon
import com.chaners.guiyuan.settings.AppLang
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MIN
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_MIN
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_MAX
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.NativeFontAxis
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_DEFAULT
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MAX
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MIN
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
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.DualDotStyle
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
import com.chaners.guiyuan.settings.topOffsetYUi
import com.chaners.guiyuan.settings.mobileTypeScaleDefault
import com.chaners.guiyuan.ui.components.MiuixBlurredTopBar
import com.chaners.guiyuan.ui.components.rememberTopBarBackdrop
import com.chaners.guiyuan.ui.components.topBarBackdropSource
import com.chaners.guiyuan.ui.layout.pageContentPadding
import com.chaners.guiyuan.ui.navigation.AppRoute
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SliderDefaults
import com.chaners.guiyuan.ui.components.UiDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

@Composable
internal fun FeaturesScreen(
    bottomContentPadding: Dp,
    visible: Boolean,
    snackbarState: SnackbarHostState,
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
    val verifiedWeightRange = (fontAxis as? NativeFontAxis.State.Variable)?.axis?.let { axis ->
        ceil(axis.min.toDouble()).toInt()..floor(axis.max.toDouble()).toInt()
    }?.takeIf { it.last > it.first }
    val mobileWeightRange =
        (verifiedWeightRange?.first ?: 200).toFloat()..
            (verifiedWeightRange?.last ?: 1000).toFloat()
    val mobileWeightPoints = remember(verifiedWeightRange) {
        verifiedWeightRange?.let(::weightSnapPoints)
    }
    val mobileWeightRangeText = verifiedWeightRange?.let { range ->
        stringResource(R.string.font_weight_range, range.first, range.last)
    }
    val verifiedBatteryRange = (batteryAxis as? NativeFontAxis.State.Variable)?.axis?.let { axis ->
        ceil(axis.min.toDouble()).toInt()..floor(axis.max.toDouble()).toInt()
    }?.takeIf { it.last > it.first }
    val batteryWeightRange =
        (verifiedBatteryRange?.first ?: BATTERY_TOP_TEXT_WEIGHT_MIN).toFloat()..
            (verifiedBatteryRange?.last ?: BATTERY_TOP_TEXT_WEIGHT_MAX).toFloat()
    val batteryWeightPoints = remember(verifiedBatteryRange) {
        verifiedBatteryRange?.let(::weightSnapPoints)
    }
    val batteryWeightRangeText = verifiedBatteryRange?.let { range ->
        stringResource(R.string.font_weight_range, range.first, range.last)
    }
    val schemeRepo =
        remember(context.applicationContext) {
            BatterySchemeRepo(context.applicationContext)
        }
    val schemeLibrary by
        schemeRepo.library.collectAsState(
            initial = schemeRepo.current(),
        )
    val layoutOptions =
        listOf(
            stringResource(R.string.content_layout_network_center),
            stringResource(R.string.content_layout_battery_center),
        )
    val dualDotStyles = listOf(
        stringResource(R.string.dual_dot_style_default),
        stringResource(R.string.dual_dot_style_duo),
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

    val search = rememberPageSearch()
    LaunchedEffect(visible) {
        if (!visible) search.close()
    }
    val searching = search.filtering
    val displayMatches = search.any(*FeatureSearchIds.display)
    val layoutMatches = search.any(*FeatureSearchIds.layout)
    val networkMatches = search.any(*FeatureSearchIds.network)
    val batteryMatches = search.any(*FeatureSearchIds.battery)
    val managementMatches = search.any(*FeatureSearchIds.management)

    val topInfoVerticalOffsetUi = topOffsetYUi(visual.topOffsetY)
    val textUiScale = topTextUiScale(visual.topTextScale)
    val chargingIconUiScale = topChargingIconUiScale(visual.topChargingIconScale)
    val renderSetting: @Composable (Int) -> Unit = { id ->
        when (id) {
            R.string.keyguard_combined_status_title -> SwitchPreference(
                title = stringResource(R.string.keyguard_combined_status_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_lock, featureCfg.enabled) },
                summary = stringResource(R.string.keyguard_combined_status_summary),
                checked = featureCfg.keyguard,
                enabled = featureCfg.enabled,
                onCheckedChange = featureRepo::setKeyguard,
            )
            R.string.aod_combined_status_title -> SwitchPreference(
                title = stringResource(R.string.aod_combined_status_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bedtime, featureCfg.enabled) },
                summary = stringResource(R.string.aod_combined_status_summary),
                checked = featureCfg.aod,
                enabled = featureCfg.enabled,
                onCheckedChange = featureRepo::setAod,
            )
            R.string.control_center_tint_transition -> SwitchPreference(
                title = stringResource(R.string.control_center_tint_transition),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_contrast, featureCfg.enabled) },
                summary = stringResource(R.string.control_center_tint_transition_summary),
                checked = visual.ccTintTransition,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setCcTintTransition,
            )
            R.string.battery_fill_follow_retract -> SwitchPreference(
                title = stringResource(R.string.battery_fill_follow_retract),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_donut_large, featureCfg.enabled) },
                summary = stringResource(R.string.battery_fill_follow_retract_summary),
                checked = visual.fillFollowsRetract,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setFillFollowsRetract,
            )
            R.string.content_layout_title -> UiDropdownPreference(
                items = layoutOptions,
                selectedIndex = visual.layout.ordinal,
                title = stringResource(R.string.content_layout_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_dashboard, featureCfg.enabled) },
                summary = stringResource(R.string.content_layout_summary),
                showValue = true,
                enabled = featureCfg.enabled,
                onSelectedIndexChange = { index ->
                    ContentLayout.entries
                        .getOrNull(index)
                        ?.let(visualRepo::setLayout)
                },
            )
            R.string.dual_signal_title -> SwitchPreference(
                title = stringResource(R.string.dual_signal_title),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_sim_cards,
                        featureCfg.enabled,
                    )
                },
                summary = stringResource(R.string.dual_signal_summary),
                checked = visual.dualSignal,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setDualSignal,
            )
            R.string.dual_dot_style_title -> UiDropdownPreference(
                title = stringResource(R.string.dual_dot_style_title),
                items = dualDotStyles,
                selectedIndex = visual.dualDotStyle.ordinal,
                showValue = true,
                enabled = featureCfg.enabled && (visual.dualSignal),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_layers, featureCfg.enabled) },
                onSelectedIndexChange = { index ->
                    DualDotStyle.entries.getOrNull(index)?.let(visualRepo::setDualDotStyle)
                },
            )
            R.string.dual_mobile_vertical_offset -> SliderPreference(
                value = visual.dualMobileOffsetY,
                onValueChange = visualRepo::setDualMobileOffsetY,
                title = stringResource(R.string.dual_mobile_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_signal_cellular_alt,
                        featureCfg.enabled,
                        R.drawable.ic_material_symbol_height_w500,
                    )
                },
                valueText = stringResource(
                    R.string.battery_top_offset_value,
                    visual.dualMobileOffsetY.roundToInt(),
                ),
                valueRange = NETWORK_TOP_OFFSET_MIN..NETWORK_TOP_OFFSET_MAX,
                steps = 19,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(0f),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled && (visual.dualSignal),
            )
            R.string.combined_size -> SliderPreference(
                value = visual.combinedScale,
                onValueChange = visualRepo::setCombinedScale,
                title = stringResource(R.string.combined_size),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_donut_large, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.combinedScale * 100f).roundToInt(),
                    ),
                valueRange = COMBINED_SCALE_MIN..COMBINED_SCALE_MAX,
                steps = 7,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(COMBINED_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.outer_weight -> SliderPreference(
                value = visual.outerWeightScale,
                onValueChange = visualRepo::setOuterWeightScale,
                title = stringResource(R.string.outer_weight),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_donut_large, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                summary = stringResource(R.string.outer_weight_summary),
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.outerWeightScale * 100f).roundToInt(),
                    ),
                valueRange = OUTER_WEIGHT_SCALE_MIN..OUTER_WEIGHT_SCALE_MAX,
                steps = 11,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(OUTER_WEIGHT_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.wifi_vertical_offset -> SliderPreference(
                value = visual.wifiOffsetY,
                onValueChange = visualRepo::setWifiOffsetY,
                title = stringResource(R.string.wifi_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_wifi,
                        featureCfg.enabled,
                        R.drawable.ic_material_symbol_height_w500,
                    )
                },
                valueText = stringResource(R.string.battery_top_offset_value, visual.wifiOffsetY.roundToInt()),
                valueRange = NETWORK_TOP_OFFSET_MIN..NETWORK_TOP_OFFSET_MAX,
                steps = 19,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(0f),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled && (visual.layout == ContentLayout.BATTERY_CENTER),
            )
            R.string.mobile_type_vertical_offset -> SliderPreference(
                value = visual.mobileTypeOffsetY,
                onValueChange = visualRepo::setMobileTypeOffsetY,
                title = stringResource(R.string.mobile_type_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_5g,
                        featureCfg.enabled,
                        R.drawable.ic_material_symbol_height_w500,
                    )
                },
                valueText = stringResource(R.string.battery_top_offset_value, visual.mobileTypeOffsetY.roundToInt()),
                valueRange = NETWORK_TOP_OFFSET_MIN..NETWORK_TOP_OFFSET_MAX,
                steps = 19,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(0f),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled && (visual.layout == ContentLayout.BATTERY_CENTER),
            )
            R.string.top_info_vertical_offset -> SliderPreference(
                value = topInfoVerticalOffsetUi,
                onValueChange = visualRepo::setTopOffsetY,
                title = stringResource(R.string.top_info_vertical_offset),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_height, featureCfg.enabled) },
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
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(0f),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled && (visual.layout != ContentLayout.BATTERY_CENTER),
            )
            R.string.wifi_size -> SliderPreference(
                value = visual.wifiScale,
                onValueChange = visualRepo::setWifiScale,
                title = stringResource(R.string.wifi_size),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_wifi, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.wifiScale * 100f).roundToInt(),
                    ),
                valueRange = WIFI_SIZE_SCALE_MIN..WIFI_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(WIFI_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.wifi_weight -> SliderPreference(
                value = visual.wifiWeight.toFloat(),
                onValueChange = { visualRepo.setWifiWeight(it.roundToInt()) },
                title = stringResource(R.string.wifi_weight),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_wifi, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                valueText = stringResource(R.string.percent_value, visual.wifiWeight),
                valueRange = WIFI_WEIGHT_MIN.toFloat()..WIFI_WEIGHT_MAX.toFloat(),
                steps = 13,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(WIFI_WEIGHT_DEFAULT.toFloat()),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.airplane_size -> SliderPreference(
                value = visual.airplaneScale,
                onValueChange = visualRepo::setAirplaneScale,
                title = stringResource(R.string.airplane_size),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_airplanemode_active, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.airplaneScale * 100f).roundToInt(),
                    ),
                valueRange = AIRPLANE_SIZE_SCALE_MIN..AIRPLANE_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(AIRPLANE_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.airplane_weight -> SliderPreference(
                value = visual.airplaneWeight.toFloat(),
                onValueChange = { visualRepo.setAirplaneWeight(it.roundToInt()) },
                title = stringResource(R.string.airplane_weight),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_airplanemode_active, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                valueText = stringResource(R.string.percent_value, visual.airplaneWeight),
                valueRange = AIRPLANE_WEIGHT_MIN.toFloat()..AIRPLANE_WEIGHT_MAX.toFloat(),
                steps = 13,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(AIRPLANE_WEIGHT_DEFAULT.toFloat()),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.no_sim_size -> SliderPreference(
                value = visual.noSimScale,
                onValueChange = visualRepo::setNoSimScale,
                title = stringResource(R.string.no_sim_size),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_sim_card, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.noSimScale * 100f).roundToInt(),
                    ),
                valueRange = NO_SIM_SIZE_SCALE_MIN..NO_SIM_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(NO_SIM_SIZE_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.mobile_type_size -> SliderPreference(
                value = visual.mobileTypeScale,
                onValueChange = visualRepo::setMobileTypeScale,
                title = stringResource(R.string.mobile_type_size),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_5g, featureCfg.enabled, R.drawable.ic_material_symbol_open_in_full) },
                valueText =
                    stringResource(
                        R.string.percent_value,
                        (visual.mobileTypeScale * 100f).roundToInt(),
                    ),
                valueRange = MOBILE_TYPE_SIZE_SCALE_MIN..MOBILE_TYPE_SIZE_SCALE_MAX,
                steps = 16,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints =
                    listOf(
                        mobileTypeScaleDefault(
                            visual.layout,
                        ),
                    ),
                magnetThreshold = 0.035f,
                enabled = featureCfg.enabled,
            )
            R.string.mobile_type_weight -> SliderPreference(
                value = visual.mobileTypeWeight.toFloat().coerceIn(
                    mobileWeightRange.start, mobileWeightRange.endInclusive,
                ),
                onValueChange = { value ->
                    visualRepo.setMobileTypeWeight(value.roundToInt())
                },
                title = stringResource(R.string.mobile_type_weight),
                summary = mobileWeightRangeText
                    ?: stringResource(
                        if (fontAxis == NativeFontAxis.State.Pending) R.string.font_weight_pending
                        else R.string.font_weight_unverified,
                    ),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_5g, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                valueText = stringResource(
                    R.string.integer_value,
                    visual.mobileTypeWeight.coerceIn(
                        mobileWeightRange.start.roundToInt(),
                        mobileWeightRange.endInclusive.roundToInt(),
                    ),
                ),
                valueRange = mobileWeightRange,
                steps = 0,
                keyPoints = mobileWeightPoints,
                magnetThreshold = verifiedWeightRange?.let(::weightSnapThreshold) ?: 0f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                holdDownState = showWeightDialog,
                enabled = featureCfg.enabled && verifiedWeightRange != null,
                onClick = { showWeightDialog = true },
            )
            R.string.mobile_follow_battery_color -> SwitchPreference(
                title = stringResource(R.string.mobile_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.mobile_follow_battery_color_summary),
                checked = visual.mobileFollowsBatteryColor,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setMobileFollowsBatteryColor,
            )
            R.string.center_follow_battery_color -> SwitchPreference(
                title = stringResource(R.string.center_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_wifi, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.center_follow_battery_color_summary),
                checked = visual.centerFollowsBatteryColor,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setCenterFollowsBatteryColor,
            )
            R.string.battery_colors -> BatteryColorPreference(
                library = schemeLibrary,
                enabled = featureCfg.enabled,
                holdDownState = showBatteryColorSheet,
                onClick = {
                    showBatteryColorSheet = true
                },
            )
            R.string.low_battery_threshold -> SliderPreference(
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
            R.string.battery_top_readout -> SwitchPreference(
                title = stringResource(R.string.battery_top_readout),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled) },
                summary = stringResource(R.string.battery_top_readout_summary),
                checked = visual.showTopReadout,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setTopReadout,
            )
            R.string.battery_top_text_size -> SliderPreference(
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
                enabled = featureCfg.enabled && (visual.showTopReadout),
            )
            R.string.battery_top_text_weight -> SliderPreference(
                value = visual.topTextWeight.toFloat().coerceIn(
                    batteryWeightRange.start, batteryWeightRange.endInclusive,
                ),
                onValueChange = { value ->
                    visualRepo.setTopTextWeight(value.roundToInt())
                },
                title = stringResource(R.string.battery_top_text_weight),
                summary = batteryWeightRangeText
                    ?: stringResource(
                        if (batteryAxis == NativeFontAxis.State.Pending) R.string.font_weight_pending
                        else R.string.font_weight_unverified,
                    ),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled, R.drawable.ic_material_symbol_line_weight) },
                valueText = stringResource(
                    R.string.integer_value,
                    visual.topTextWeight.coerceIn(
                        batteryWeightRange.start.roundToInt(),
                        batteryWeightRange.endInclusive.roundToInt(),
                    ),
                ),
                valueRange = batteryWeightRange,
                steps = 0,
                keyPoints = batteryWeightPoints,
                magnetThreshold = verifiedBatteryRange?.let(::weightSnapThreshold) ?: 0f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                holdDownState = showBatteryWeightDialog,
                enabled = featureCfg.enabled && (visual.showTopReadout) && verifiedBatteryRange != null,
                onClick = { showBatteryWeightDialog = true },
            )
            R.string.battery_text_follow_battery_color -> SwitchPreference(
                title = stringResource(R.string.battery_text_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.battery_text_follow_battery_color_summary),
                checked = visual.topTextFollowsBatteryColor,
                enabled = featureCfg.enabled && (visual.showTopReadout),
                onCheckedChange = visualRepo::setTopTextFollowsBatteryColor,
            )
            R.string.battery_charging_icon -> SwitchPreference(
                title = stringResource(R.string.battery_charging_icon),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, featureCfg.enabled) },
                summary = stringResource(R.string.battery_charging_icon_summary),
                checked = visual.showTopChargingIcon,
                enabled = featureCfg.enabled,
                onCheckedChange = visualRepo::setTopChargingIcon,
            )
            R.string.battery_top_charging_icon_size -> SliderPreference(
                value = chargingIconUiScale,
                onValueChange = visualRepo::setTopChargingIconScale,
                title = stringResource(R.string.battery_top_charging_icon_size),
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
                enabled = featureCfg.enabled && (visual.showTopChargingIcon),
            )
            R.string.charging_icon_follow_battery_color -> SwitchPreference(
                title = stringResource(R.string.charging_icon_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, featureCfg.enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.charging_icon_follow_battery_color_summary),
                checked = visual.topChargingIconFollowsBatteryColor,
                enabled = featureCfg.enabled && (visual.showTopChargingIcon),
                onCheckedChange =
                    visualRepo::setTopChargingIconFollowsBatteryColor,
            )
            R.string.config_backup_title -> BasicComponent(
                title = stringResource(R.string.config_backup_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_save) },
                summary = stringResource(R.string.config_backup_summary),
                enabled = !ioBusy,
                onClick = { createDocument.launch(backupFiles.fileName()) },
            )
            R.string.config_restore_title -> BasicComponent(
                title = stringResource(R.string.config_restore_title),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_file_open) },
                summary = stringResource(R.string.config_restore_summary),
                enabled = !ioBusy,
                onClick = { openDocument.launch(arrayOf("application/json", "text/plain")) },
            )
            R.string.restore_feature_defaults -> BasicComponent(
                title = stringResource(R.string.restore_feature_defaults),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_settings_backup_restore) },
                summary = stringResource(R.string.restore_feature_defaults_summary),
                onClick = { showResetDialog = true },
            )
            else -> Unit
        }
    }
    val results = if (search.filtering) {
        buildList {
            for (id in search.results()) {
                val required = when (id) {
                    R.string.dual_dot_style_title, R.string.dual_mobile_vertical_offset ->
                        R.string.dual_signal_title
                    R.string.battery_top_text_size, R.string.battery_top_text_weight,
                    R.string.battery_text_follow_battery_color -> R.string.battery_top_readout
                    R.string.battery_top_charging_icon_size, R.string.charging_icon_follow_battery_color ->
                        R.string.battery_charging_icon
                    R.string.wifi_vertical_offset, R.string.mobile_type_vertical_offset,
                    R.string.top_info_vertical_offset -> R.string.content_layout_title
                    else -> null
                }
                if (required != null && required !in this) add(required)
                if (id !in this) add(id)
            }
        }
    } else emptyList()

    HubPage(
        title = stringResource(R.string.features_title),
        bottomContentPadding = bottomContentPadding,
        search = search,
        overlay = {
            pendingImport?.let { selected ->
                UiOverlayDialog(
                    title = stringResource(R.string.config_restore_title),
                    summary = stringResource(R.string.config_restore_confirm),
                    show = true,
                    onDismissRequest = { if (!ioBusy) pendingImport = null },
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        UiDialogButton(
                            text = stringResource(R.string.cancel),
                            modifier = Modifier.weight(1f),
                            enabled = !ioBusy,
                            onClick = { pendingImport = null },
                        )
                        Spacer(Modifier.width(20.dp))
                        UiDialogButton(
                            text = stringResource(R.string.restore),
                            modifier = Modifier.weight(1f),
                            enabled = !ioBusy,
                            role = GlassDialogButtonRole.Primary,
                            onClick = {
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
                            },
                        )
                    }
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
                show = showWeightDialog && verifiedWeightRange != null,
                title = stringResource(R.string.mobile_type_weight),
                summary = mobileWeightRangeText.orEmpty(),
                value = visual.mobileTypeWeight.coerceIn(
                    mobileWeightRange.start.roundToInt(),
                    mobileWeightRange.endInclusive.roundToInt(),
                ),
                range = verifiedWeightRange ?: 200..1000,
                onDismiss = { showWeightDialog = false },
                onConfirm = { value ->
                    showWeightDialog = false
                    verifiedWeightRange?.let { range ->
                        visualRepo.setMobileTypeWeight(value.coerceIn(range))
                    }
                },
            )
            IntegerInputDialog(
                show = showBatteryWeightDialog && verifiedBatteryRange != null,
                title = stringResource(R.string.battery_top_text_weight),
                summary = batteryWeightRangeText.orEmpty(),
                value = visual.topTextWeight.coerceIn(
                    batteryWeightRange.start.roundToInt(),
                    batteryWeightRange.endInclusive.roundToInt(),
                ),
                range = verifiedBatteryRange ?: BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
                onDismiss = { showBatteryWeightDialog = false },
                onConfirm = { value ->
                    showBatteryWeightDialog = false
                    verifiedBatteryRange?.let { range ->
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
                    Spacer(Modifier.width(20.dp))
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
        if (search.filtering) {
            results.forEachIndexed { index, id ->
                item(key = id) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        renderSetting(id)
                        if (index < results.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        } else {
        if (displayMatches) {
            Section(R.string.section_display_transition) {
                if (search.matches(R.string.keyguard_combined_status_title)) {
                    renderSetting(R.string.keyguard_combined_status_title)
                }
                if (search.matches(R.string.aod_combined_status_title)) {
                    renderSetting(R.string.aod_combined_status_title)
                }
                if (search.matches(R.string.control_center_tint_transition)) {
                    renderSetting(R.string.control_center_tint_transition)
                }
                if (search.matches(R.string.battery_fill_follow_retract)) {
                    renderSetting(R.string.battery_fill_follow_retract)
                }
            }
        }
        if (layoutMatches) {
            Section(R.string.section_layout_size) {
                if (search.matches(R.string.content_layout_title) ||
                    (searching && visual.layout != ContentLayout.BATTERY_CENTER &&
                        search.any(R.string.wifi_vertical_offset, R.string.mobile_type_vertical_offset)) ||
                    (searching && visual.layout == ContentLayout.BATTERY_CENTER &&
                        search.matches(R.string.top_info_vertical_offset))) {
                    renderSetting(R.string.content_layout_title)
                }
                if (search.matches(R.string.dual_signal_title) ||
                    (searching && !visual.dualSignal &&
                        search.any(R.string.dual_dot_style_title, R.string.dual_mobile_vertical_offset))) {
                    renderSetting(R.string.dual_signal_title)
                }
                AnimatedPreferenceGroup(visible = visual.dualSignal) {
                    if (search.matches(R.string.dual_dot_style_title)) {
                        renderSetting(R.string.dual_dot_style_title)
                    }
                    if (search.matches(R.string.dual_mobile_vertical_offset)) {
                        renderSetting(R.string.dual_mobile_vertical_offset)
                    }
                }
                if (search.matches(R.string.combined_size)) {
                    renderSetting(R.string.combined_size)
                }
                if (search.matches(R.string.outer_weight)) {
                    renderSetting(R.string.outer_weight)
                }
                if (visual.layout == ContentLayout.BATTERY_CENTER) {
                    if (search.matches(R.string.wifi_vertical_offset)) {
                        renderSetting(R.string.wifi_vertical_offset)
                    }
                    if (search.matches(R.string.mobile_type_vertical_offset)) {
                        renderSetting(R.string.mobile_type_vertical_offset)
                    }
                } else {
                    if (search.matches(R.string.top_info_vertical_offset)) {
                        renderSetting(R.string.top_info_vertical_offset)
                    }
                }
            }
        }
        if (networkMatches) {
            Section(R.string.section_network) {
                if (search.matches(R.string.wifi_size)) {
                    renderSetting(R.string.wifi_size)
                }
                if (search.matches(R.string.wifi_weight)) {
                    renderSetting(R.string.wifi_weight)
                }
                if (search.matches(R.string.airplane_size)) {
                    renderSetting(R.string.airplane_size)
                }
                if (search.matches(R.string.airplane_weight)) {
                    renderSetting(R.string.airplane_weight)
                }
                if (search.matches(R.string.no_sim_size)) {
                    renderSetting(R.string.no_sim_size)
                }
                if (search.matches(R.string.mobile_type_size)) {
                    renderSetting(R.string.mobile_type_size)
                }
                if (search.matches(R.string.mobile_type_weight)) {
                    renderSetting(R.string.mobile_type_weight)
                }
                if (search.matches(R.string.mobile_follow_battery_color)) {
                    renderSetting(R.string.mobile_follow_battery_color)
                }
                if (search.matches(R.string.center_follow_battery_color)) {
                    renderSetting(R.string.center_follow_battery_color)
                }
            }
        }
        if (batteryMatches) {
            Section(R.string.section_battery) {
                if (search.matches(R.string.battery_colors)) {
                renderSetting(R.string.battery_colors)
                }
                if (search.matches(R.string.low_battery_threshold)) {
                    renderSetting(R.string.low_battery_threshold)
                }
                if (search.matches(R.string.battery_top_readout) ||
                    (searching && !visual.showTopReadout &&
                        search.any(R.string.battery_top_text_size, R.string.battery_top_text_weight,
                            R.string.battery_text_follow_battery_color))) {
                    renderSetting(R.string.battery_top_readout)
                }
                AnimatedPreferenceGroup(visible = visual.showTopReadout) {
                    if (search.matches(R.string.battery_top_text_size)) {
                        renderSetting(R.string.battery_top_text_size)
                    }
                    if (search.matches(R.string.battery_top_text_weight)) {
                        renderSetting(R.string.battery_top_text_weight)
                    }
                    if (search.matches(R.string.battery_text_follow_battery_color)) {
                        renderSetting(R.string.battery_text_follow_battery_color)
                    }
                }

                if (search.matches(R.string.battery_charging_icon) ||
                    (searching && !visual.showTopChargingIcon &&
                        search.any(R.string.battery_top_charging_icon_size,
                            R.string.charging_icon_follow_battery_color))) {
                    renderSetting(R.string.battery_charging_icon)
                }
                AnimatedPreferenceGroup(visible = visual.showTopChargingIcon) {
                    if (search.matches(R.string.battery_top_charging_icon_size)) {
                        renderSetting(R.string.battery_top_charging_icon_size)
                    }
                    if (search.matches(R.string.charging_icon_follow_battery_color)) {
                        renderSetting(R.string.charging_icon_follow_battery_color)
                    }
                }
            }
        }
        if (managementMatches) {
            Section(R.string.section_management) {
                if (search.matches(R.string.config_backup_title)) {
                    renderSetting(R.string.config_backup_title)
                }
                if (search.matches(R.string.config_restore_title)) {
                    renderSetting(R.string.config_restore_title)
                }
                if (search.matches(R.string.restore_feature_defaults)) {
                    renderSetting(R.string.restore_feature_defaults)
                }
            }
        }
        }
        if (search.filtering && results.isEmpty()) {
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

@Composable
internal fun SettingsHubScreen(
    bottomContentPadding: Dp,
    lang: AppLang,
    iconHidden: Boolean,
    swipeBackEnabled: Boolean,
    runtimeBusy: Boolean,
    onRestart: () -> Unit,
    onLangChange: (AppLang) -> Unit,
    onIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val ctx = LocalContext.current
    val app = remember(ctx.applicationContext) {
        ctx.applicationContext as GyApplication
    }
    val entryFlow = remember(ctx.applicationContext) {
        SettingsEntryPrefs.observe(ctx.applicationContext)
    }
    val entryCfg by entryFlow.collectAsState(
        initial = SettingsEntryCfg(
            entry = SettingsEntryPrefs.get(ctx),
            icon = SettingsEntryPrefs.icon(ctx),
        ),
    )
    val entry = entryCfg.entry
    val entryIcon = entryCfg.icon
    var pending by remember { mutableStateOf<SettingsEntry?>(null) }
    var scopeApplying by remember { mutableStateOf<SettingsEntry?>(null) }
    var requestFailed by remember { mutableStateOf(false) }
    val entryOptions = listOf(
        stringResource(R.string.settings_entry_disabled),
        stringResource(R.string.settings_entry_top),
        stringResource(R.string.settings_entry_middle),
        stringResource(R.string.settings_entry_bottom),
    )
    val iconOptions = listOf(
        stringResource(R.string.settings_entry_icon_rounded),
        stringResource(R.string.settings_entry_icon_hollow),
    )
    fun saveEntryIcon(value: SettingsEntryIcon) {
        if (!SettingsEntryPrefs.setIcon(ctx, value)) requestFailed = true
    }
    fun saveEntry(value: SettingsEntry) {
        if (!SettingsEntryPrefs.set(ctx, value)) requestFailed = true
    }

    val languageOptions = listOf(
        stringResource(R.string.language_system),
        stringResource(R.string.language_english),
        stringResource(R.string.language_simplified_chinese),
    )
    HubPage(
        title = stringResource(R.string.settings_title),
        bottomContentPadding = bottomContentPadding,
        overlay = {
            UiOverlayDialog(
                title = stringResource(R.string.settings_entry_scope_title),
                summary = stringResource(R.string.settings_entry_scope_summary),
                show = pending != null,
                onDismissRequest = { pending = null },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UiDialogButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = { pending = null },
                    )
                    Spacer(Modifier.width(20.dp))
                    UiDialogButton(
                        text = stringResource(R.string.confirm),
                        modifier = Modifier.weight(1f),
                        role = GlassDialogButtonRole.Primary,
                        enabled = !runtimeBusy && scopeApplying == null,
                        onClick = {
                            val target = pending
                            pending = null
                            if (target != null && scopeApplying == null) {
                                val previous = SettingsEntryPrefs.get(ctx)
                                scopeApplying = target
                                val accepted = app.requestSettingsScope { approved ->
                                    if (scopeApplying == target) {
                                        scopeApplying = null
                                        if (approved) {
                                            // A newer selection takes precedence over this grant.
                                            if (SettingsEntryPrefs.get(ctx) == previous) saveEntry(target)
                                        } else {
                                            requestFailed = true
                                        }
                                    }
                                }
                                if (!accepted) {
                                    scopeApplying = null
                                    requestFailed = true
                                }
                            }
                        },
                    )
                }
            }
            UiOverlayDialog(
                title = stringResource(R.string.settings_entry_failed_title),
                summary = stringResource(R.string.settings_entry_failed_summary),
                show = requestFailed,
                onDismissRequest = { requestFailed = false },
            ) {
                UiDialogButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { requestFailed = false },
                )
            }
        },
    ) {
        Section(R.string.section_appearance_interaction) {
            ArrowPreference(
                title = stringResource(R.string.appearance_title),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_display_settings,
                    )
                },
                summary = stringResource(R.string.appearance_summary),
                onClick = { onNavigate(AppRoute.Appearance) },
            )
            SwitchPreference(
                title = stringResource(R.string.swipe_back),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_swipe,
                    )
                },
                summary = stringResource(R.string.swipe_back_summary),
                checked = swipeBackEnabled,
                onCheckedChange = onSwipeBackChange,
            )
        }
        Section(R.string.section_app) {
            UiDropdownPreference(
                items = languageOptions,
                selectedIndex = lang.ordinal,
                title = stringResource(R.string.language_title),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_language,
                    )
                },
                summary = stringResource(R.string.language_summary),
                showValue = true,
                onSelectedIndexChange = { index ->
                    AppLang.entries.getOrNull(index)?.let(onLangChange)
                },
            )
            SwitchPreference(
                title = stringResource(R.string.hide_launcher_icon),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_visibility_off,
                    )
                },
                summary = stringResource(R.string.hide_launcher_icon_summary),
                checked = iconHidden,
                onCheckedChange = onIconHiddenChange,
            )
            UiDropdownPreference(
                items = entryOptions,
                selectedIndex = entry.ordinal,
                title = stringResource(R.string.settings_entry_title),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_settings_applications,
                        enabled = scopeApplying == null && !runtimeBusy,
                    )
                },
                enabled = scopeApplying == null && !runtimeBusy,
                summary = stringResource(
                    if (entry == SettingsEntry.Disabled) R.string.settings_entry_summary
                    else R.string.settings_entry_restart_hint,
                ),
                showValue = true,
                onSelectedIndexChange = { index ->
                    SettingsEntry.entries.getOrNull(index)?.let { selected ->
                        if (selected != entry) {
                            if (selected == SettingsEntry.Disabled || app.settingsInScope()) {
                                saveEntry(selected)
                            } else {
                                pending = selected
                            }
                        }
                    }
                },
            )
            if (entry != SettingsEntry.Disabled) {
                UiDropdownPreference(
                    items = iconOptions,
                    selectedIndex = entryIcon.ordinal,
                    title = stringResource(R.string.settings_entry_icon_title),
                    startAction = {
                        SemanticLeadingIcon(R.drawable.ic_material_symbol_filter_none)
                    },
                    summary = stringResource(R.string.settings_entry_icon_summary),
                    showValue = true,
                    onSelectedIndexChange = { index ->
                        SettingsEntryIcon.entries.getOrNull(index)?.let(::saveEntryIcon)
                    },
                )
            }
        }
        Section(R.string.section_diagnostics_maintenance) {
            ArrowPreference(
                title = stringResource(R.string.about_title),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_info,
                    )
                },
                summary = stringResource(R.string.about_summary),
                onClick = { onNavigate(AppRoute.About) },
            )
            ArrowPreference(
                title = stringResource(R.string.diagnostics_title),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_troubleshoot_outlined,
                    )
                },
                summary = stringResource(R.string.diagnostics_summary),
                onClick = { onNavigate(AppRoute.Diagnostics) },
            )
            BasicComponent(
                title = stringResource(R.string.restart_scope),
                startAction = {
                    SemanticLeadingIcon(
                        iconRes = R.drawable.ic_material_symbol_restart_alt,
                        enabled = !runtimeBusy && scopeApplying == null,
                    )
                },
                summary = stringResource(R.string.restart_scope_summary),
                enabled = !runtimeBusy && scopeApplying == null,
                onClick = onRestart,
            )

        }
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
    bottomContentPadding: Dp,
    search: PageSearch? = null,
    overlay: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    // Browsing and filtered results must not overwrite each other's scroll position.
    val browsingState = rememberLazyListState()
    val resultsState = rememberLazyListState()
    val filtering = search?.filtering == true
    val listState = if (filtering) resultsState else browsingState
    val canScroll = listState.canScrollBackward || listState.canScrollForward
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()
    val keyboardController = LocalSoftwareKeyboardController.current
    val os4 = LocalUiStyle.current == UiStyle.OS4
    LaunchedEffect(filtering, search?.query) {
        if (filtering) {
            if (os4) resultsState.requestScrollToItem(0)
            else resultsState.scrollToItem(0)
        }
    }
    val searchMotion = rememberOs4SearchMotion(os4 && search?.active == true)
    val searchReturn = 1f - searchMotion.progress.coerceIn(0f, 1f)
    val os3SearchActionAlpha by animateFloatAsState(
        targetValue = if (!os4 && search?.active == true) 0f else 1f,
        animationSpec = tween(150),
        label = "os3SearchAction",
    )
    // Only actual content overlap changes the surface; search geometry has its own clock.
    val searchMaterial = animateFloatAsState(
        targetValue = if (os4 && listState.canScrollBackward) 1f else 0f,
        animationSpec = GlassMotion.topBarButtonFloat(),
        label = "os4SearchMaterial",
    )
    val searchAction: @Composable RowScope.() -> Unit = {
        if (search != null) {
            val label = stringResource(R.string.feature_search)
            TooltipBox(text = label) {
                Box(Modifier.graphicsLayer {
                    alpha = if (os4) searchReturn else os3SearchActionAlpha
                }) {
                    if (os4) {
                        GlassIconButton(
                            onClick = search::open,
                            enabled = !search.active,
                        ) {
                            Icon(Os4GlassSearch, contentDescription = label)
                        }
                    } else IconButton(
                        onClick = search::open,
                        enabled = !search.active,
                    ) {
                        Icon(MiuixIcons.Search, contentDescription = label)
                    }
                }
            }
        }
    }
    val searchContent: @Composable () -> Unit = {
        if (search != null) {
            if (os4) {
                if (searchMotion.mounted) {
                    Os4SearchMode(
                        motion = searchMotion,
                        expanded = search.active,
                        query = search.query,
                        backdrop = topBarBackdrop,
                        materialProgress = searchMaterial,
                        onQueryChange = search::update,
                        onSearch = { keyboardController?.hide() },
                        onClose = search::close,
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = search.active,
                    modifier = Modifier.fillMaxWidth(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SearchBar(
                        modifier = Modifier.fillMaxWidth(),
                        inputField = {
                            InputField(
                                query = search.query,
                                onQueryChange = search::update,
                                onSearch = { keyboardController?.hide() },
                                expanded = search.active,
                                onExpandedChange = { if (!it) search.close() },
                                label = stringResource(R.string.feature_search),
                            )
                        },
                        expanded = search.active,
                        onExpandedChange = { if (!it) search.close() },
                        outsideEndAction = {
                            Text(
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 16.dp)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = search::close,
                                    ),
                                text = stringResource(R.string.cancel),
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                            )
                        },
                        content = {},
                    )
                }
            }
        }
    }
    CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
    Scaffold(
        topBar = {
            if (os4) {
                Box(Modifier.fillMaxWidth()) {
                    GlassTopAppBar(
                        title = title,
                        isContentScrolled = listState.canScrollBackward,
                        backdrop = topBarBackdrop,
                        scrollBehavior = scrollBehavior,
                        titleAlpha = { searchReturn },
                        actions = searchAction,
                        unclippedActions = search != null,
                    )
                    if (search != null) {
                        // An overlaid search row keeps the bar's measured height stable.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding)
                                .padding(top = 4.dp),
                        ) {
                            searchContent()
                        }
                    }
                }
            } else {
                MiuixBlurredTopBar(
                    backdrop = topBarBackdrop,
                    scrollBehavior = scrollBehavior,
                ) { barColor ->
                    TopAppBar(
                        title = title,
                        color = barColor,
                        scrollBehavior = scrollBehavior,
                        actions = searchAction,
                        bottomContent = searchContent,
                    )
                }
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
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = pageContentPadding(
                    innerPadding = paddingValues,
                    outerBottomPadding = bottomContentPadding,
                    extraBottom = 12.dp,
                ),
                content = content,
            )
        }

        overlay()
    }
    }
}

internal fun weightSnapPoints(range: IntRange): List<Float> {
    if (range.isEmpty()) return emptyList()
    val first = ((range.first + 49) / 50) * 50
    return if (first > range.last) emptyList() else (first..range.last step 50).map(Int::toFloat)
}

// MIUIX's magnet threshold is normalized; shrink it when 50-point anchors get dense.
internal fun weightSnapThreshold(range: IntRange): Float =
    minOf(0.02f, 12.5f / (range.last - range.first).coerceAtLeast(1))
