package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_UI_SCALE_MIN
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_MAX
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_MIN
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.topChargingIconUiScale
import com.chaners.guiyuan.settings.topChargingIconUiScaleDefault
import com.chaners.guiyuan.settings.topTextUiScale
import com.chaners.guiyuan.settings.topTextUiScaleDefault
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

internal fun androidx.compose.foundation.lazy.LazyListScope.featureBatterySection(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    featureCfg: FeatureCfg,
    featureRepo: FeatureRepo,
    schemeLibrary: BatteryColorSchemeLibrary,
    weight: FeatureWeightUi,
    colorSheetOpen: Boolean,
    onOpenColorSheet: () -> Unit,
    weightDialogOpen: Boolean,
    onOpenWeightDialog: () -> Unit,
    bothSidesChargingSearch: Boolean,
) {
    Section(R.string.section_battery) {
        batteryColorAndThreshold(search, featureCfg, featureRepo, schemeLibrary, colorSheetOpen, onOpenColorSheet)
        batteryTopReadout(search, visual, visualRepo, featureCfg.enabled, weight, weightDialogOpen, onOpenWeightDialog)
        batteryChargingIcon(search, visual, visualRepo, featureCfg.enabled, bothSidesChargingSearch)
    }
}

@Composable
private fun ColumnScope.batteryColorAndThreshold(
    search: PageSearch,
    featureCfg: FeatureCfg,
    featureRepo: FeatureRepo,
    schemeLibrary: BatteryColorSchemeLibrary,
    colorSheetOpen: Boolean,
    onOpenColorSheet: () -> Unit,
) {
    if (search.matches(R.string.battery_colors)) {
    BatteryColorPreference(
        library = schemeLibrary,
        enabled = featureCfg.enabled,
        holdDownState = colorSheetOpen,
        onClick = {
            onOpenColorSheet()
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
}

@Composable
private fun ColumnScope.batteryTopReadout(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    weight: FeatureWeightUi,
    weightDialogOpen: Boolean,
    onOpenWeightDialog: () -> Unit,
) {
    if (search.matches(R.string.battery_top_readout) ||
        (search.filtering && !visual.showTopReadout &&
            search.any(R.string.battery_top_text_size, R.string.battery_top_text_weight,
                R.string.battery_text_follow_battery_color))) {
        SwitchPreference(
            title = stringResource(R.string.battery_top_readout),
            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, enabled) },
            summary = stringResource(R.string.battery_top_readout_summary),
            checked = visual.showTopReadout,
            enabled = enabled,
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
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, enabled, R.drawable.ic_material_symbol_open_in_full) },
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
                enabled = enabled,
            )
        }
        if (search.matches(R.string.battery_top_text_weight)) {
            NativeWeightSlider(
                value = visual.topTextWeight,
                onValueChange = visualRepo::setTopTextWeight,
                weight = weight,
                dialogOpen = weightDialogOpen,
                onOpenDialog = onOpenWeightDialog,
            )
        }
        if (search.matches(R.string.battery_text_follow_battery_color)) {
            SwitchPreference(
                title = stringResource(R.string.battery_text_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_battery_5_bar, enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.battery_text_follow_battery_color_summary),
                checked = visual.topTextFollowsBatteryColor,
                enabled = enabled,
                onCheckedChange = visualRepo::setTopTextFollowsBatteryColor,
            )
        }
    }
}

@Composable
private fun ColumnScope.batteryChargingIcon(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    bothSidesChargingSearch: Boolean,
) {
    if (search.matches(R.string.battery_charging_icon) ||
        (search.filtering && !visual.showChargingIcon &&
            (search.any(R.string.battery_top_charging_icon_size,
                R.string.charging_icon_follow_battery_color) || bothSidesChargingSearch))) {
        SwitchPreference(
            title = stringResource(R.string.battery_charging_icon),
            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, enabled) },
            summary = stringResource(R.string.battery_charging_icon_summary),
            checked = visual.showChargingIcon,
            enabled = enabled,
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
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, enabled, R.drawable.ic_material_symbol_open_in_full) },
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
                enabled = enabled,
            )
        }
        if (search.matches(R.string.charging_icon_follow_battery_color)) {
            SwitchPreference(
                title = stringResource(R.string.charging_icon_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.charging_icon_follow_battery_color_summary),
                checked = visual.topChargingIconFollowsBatteryColor,
                enabled = enabled,
                onCheckedChange =
                    visualRepo::setTopChargingIconFollowsBatteryColor,
            )
        }
    }
}
