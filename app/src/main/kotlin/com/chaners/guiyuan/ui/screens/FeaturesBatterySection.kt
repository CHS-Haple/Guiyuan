package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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

internal data class BatteryFeatureSnapshot(
    val visual: VisualCfg,
    val cfg: FeatureCfg,
    val library: BatteryColorSchemeLibrary,
    val weight: FeatureWeightUi,
)

internal data class BatteryFeatureRepositories(
    val visual: VisualRepo,
    val feature: FeatureRepo,
)

internal data class BatteryFeatureDialogs(
    val colorOpen: Boolean,
    val onOpenColor: () -> Unit,
    val weightOpen: Boolean,
    val onOpenWeight: () -> Unit,
)

internal fun androidx.compose.foundation.lazy.LazyListScope.featureBatterySection(
    search: PageSearch,
    state: BatteryFeatureSnapshot,
    repositories: BatteryFeatureRepositories,
    dialogs: BatteryFeatureDialogs,
    bothSidesChargingSearch: Boolean,
) {
    Section(R.string.section_battery) {
        batteryColorAndThreshold(search, state, repositories, dialogs)
        batteryTopReadout(search, state, repositories, dialogs)
        batteryChargingIcon(search, state, repositories, bothSidesChargingSearch)
    }
}

@Composable
private fun ColumnScope.batteryColorAndThreshold(
    search: PageSearch,
    state: BatteryFeatureSnapshot,
    repositories: BatteryFeatureRepositories,
    dialogs: BatteryFeatureDialogs,
) {
    val featureCfg = state.cfg
    val featureRepo = repositories.feature
    val schemeLibrary = state.library
    val colorSheetOpen = dialogs.colorOpen
    val onOpenColorSheet = dialogs.onOpenColor

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
    state: BatteryFeatureSnapshot,
    repositories: BatteryFeatureRepositories,
    dialogs: BatteryFeatureDialogs,
) {
    val visual = state.visual
    val visualRepo = repositories.visual
    val enabled = state.cfg.enabled
    val weight = state.weight
    val weightDialogOpen = dialogs.weightOpen
    val onOpenWeightDialog = dialogs.onOpenWeight

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
            BatteryTopScaleSlider(
                row = BatteryTopScaleRow.TEXT,
                value = textUiScale,
                defaultValue = topTextUiScaleDefault(visual.layout),
                enabled = enabled,
                onValueChange = visualRepo::setTopTextScale,
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
    state: BatteryFeatureSnapshot,
    repositories: BatteryFeatureRepositories,
    bothSidesChargingSearch: Boolean,
) {
    val visual = state.visual
    val visualRepo = repositories.visual
    val enabled = state.cfg.enabled

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
            BatteryTopScaleSlider(
                row = BatteryTopScaleRow.CHARGING_ICON,
                value = chargingIconUiScale,
                defaultValue = topChargingIconUiScaleDefault(visual.layout),
                enabled = enabled,
                onValueChange = visualRepo::setTopChargingIconScale,
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

private enum class BatteryTopScaleRow(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val summaryRes: Int?,
    val range: ClosedFloatingPointRange<Float>,
) {
    TEXT(
        R.string.battery_top_text_size,
        R.drawable.ic_material_symbol_battery_5_bar,
        null,
        BATTERY_TOP_TEXT_UI_SCALE_MIN..BATTERY_TOP_TEXT_UI_SCALE_MAX,
    ),
    CHARGING_ICON(
        R.string.battery_top_charging_icon_size,
        R.drawable.ic_material_symbol_bolt,
        R.string.battery_charging_size_summary,
        BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN..BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
    ),
}

@Composable
private fun BatteryTopScaleSlider(
    row: BatteryTopScaleRow,
    value: Float,
    defaultValue: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        title = stringResource(row.titleRes),
        summary = row.summaryRes?.let { stringResource(it) },
        startAction = {
            SemanticLeadingIcon(row.iconRes, enabled, R.drawable.ic_material_symbol_open_in_full)
        },
        valueText = stringResource(
            R.string.battery_top_scale_value,
            (value * 100f).roundToInt(),
        ),
        valueRange = row.range,
        steps = 23,
        showKeyPoints = true,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        keyPoints = listOf(defaultValue),
        magnetThreshold = 0.035f,
        enabled = enabled,
    )
}
