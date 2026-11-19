package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_MAX
import com.chaners.guiyuan.settings.AIRPLANE_WEIGHT_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_DEFAULT
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.WIFI_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.WIFI_WEIGHT_MAX
import com.chaners.guiyuan.settings.WIFI_WEIGHT_MIN
import com.chaners.guiyuan.settings.mobileTypeScaleDefault
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

internal fun androidx.compose.foundation.lazy.LazyListScope.featureNetworkSection(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    weight: FeatureWeightUi,
    weightDialogOpen: Boolean,
    onOpenWeightDialog: () -> Unit,
) {
    Section(R.string.section_network) {
        if (search.matches(R.string.wifi_size)) {
            NetworkSizeSlider(
                value = visual.wifiScale,
                onValueChange = visualRepo::setWifiScale,
                row = NetworkSizeRow.WIFI,
                defaultValue = WIFI_SIZE_SCALE_DEFAULT,
                enabled = enabled,
            )
        }
        if (search.matches(R.string.wifi_weight)) {
            NetworkWeightSlider(
                value = visual.wifiWeight,
                onValueChange = visualRepo::setWifiWeight,
                row = NetworkWeightRow.WIFI,
                enabled = enabled,
            )
        }
        if (search.matches(R.string.airplane_size)) {
            NetworkSizeSlider(
                value = visual.airplaneScale,
                onValueChange = visualRepo::setAirplaneScale,
                row = NetworkSizeRow.AIRPLANE,
                defaultValue = AIRPLANE_SIZE_SCALE_DEFAULT,
                enabled = enabled,
            )
        }
        if (search.matches(R.string.airplane_weight)) {
            NetworkWeightSlider(
                value = visual.airplaneWeight,
                onValueChange = visualRepo::setAirplaneWeight,
                row = NetworkWeightRow.AIRPLANE,
                enabled = enabled,
            )
        }
        if (search.matches(R.string.no_sim_size)) {
            NetworkSizeSlider(
                value = visual.noSimScale,
                onValueChange = visualRepo::setNoSimScale,
                row = NetworkSizeRow.NO_SIM,
                defaultValue = NO_SIM_SIZE_SCALE_DEFAULT,
                enabled = enabled,
            )
        }
        if (search.matches(R.string.mobile_type_size)) {
            NetworkSizeSlider(
                value = visual.mobileTypeScale,
                onValueChange = visualRepo::setMobileTypeScale,
                row = NetworkSizeRow.MOBILE_TYPE,
                defaultValue = mobileTypeScaleDefault(visual.layout),
                enabled = enabled,
            )
        }
        if (search.matches(R.string.mobile_type_weight)) {
            SliderPreference(
                value = visual.mobileTypeWeight.toFloat().coerceIn(
                    weight.valueRange.start, weight.valueRange.endInclusive,
                ),
                onValueChange = { value ->
                    visualRepo.setMobileTypeWeight(value.roundToInt())
                },
                title = stringResource(R.string.mobile_type_weight),
                summary = weight.rangeText
                    ?: stringResource(
                        if (weight.pending) R.string.font_weight_pending
                        else R.string.font_weight_unverified,
                    ),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_5g, enabled, R.drawable.ic_material_symbol_line_weight) },
                valueText = stringResource(
                    R.string.integer_value,
                    visual.mobileTypeWeight.coerceIn(
                        weight.valueRange.start.roundToInt(),
                        weight.valueRange.endInclusive.roundToInt(),
                    ),
                ),
                valueRange = weight.valueRange,
                steps = 0,
                keyPoints = weight.points,
                magnetThreshold = weight.verifiedRange?.let(::weightSnapThreshold) ?: 0f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                holdDownState = weightDialogOpen,
                enabled = enabled && weight.verifiedRange != null,
                onClick = { onOpenWeightDialog() },
            )
        }
        if (search.matches(R.string.mobile_follow_battery_color)) {
            SwitchPreference(
                title = stringResource(R.string.mobile_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_signal_cellular_alt, enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.mobile_follow_battery_color_summary),
                checked = visual.mobileFollowsBatteryColor,
                enabled = enabled,
                onCheckedChange = visualRepo::setMobileFollowsBatteryColor,
            )
        }
        if (search.matches(R.string.center_follow_battery_color)) {
            SwitchPreference(
                title = stringResource(R.string.center_follow_battery_color),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_wifi, enabled, R.drawable.ic_material_symbol_colorize) },
                summary = stringResource(R.string.center_follow_battery_color_summary),
                checked = visual.centerFollowsBatteryColor,
                enabled = enabled,
                onCheckedChange = visualRepo::setCenterFollowsBatteryColor,
            )
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
