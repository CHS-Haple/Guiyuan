package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MAX
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MIN
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_DEFAULT
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MAX
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MIN
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.topOffsetYUi
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference

@Composable
internal fun ColumnScope.featureLayoutOffsetsAndScale(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
    featureBatteryCenteredOffsets(search, visual, visualRepo, enabled)
    featureRelativePositionOffsets(search, visual, visualRepo, enabled)
    featureLayoutSizeAndWeight(search, visual, visualRepo, enabled)
}

@Composable
private fun ColumnScope.featureBatteryCenteredOffsets(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
    AnimatedPreferenceGroup(visible = visual.layout == ContentLayout.BATTERY_CENTER) {
        if (search.matches(R.string.wifi_vertical_offset)) {
            SliderPreference(
                value = visual.wifiOffsetY,
                onValueChange = visualRepo::setWifiOffsetY,
                title = stringResource(R.string.wifi_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_wifi,
                        enabled,
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
                enabled = enabled,
            )
        }
        if (search.matches(R.string.mobile_type_vertical_offset)) {
            SliderPreference(
                value = visual.mobileTypeOffsetY,
                onValueChange = visualRepo::setMobileTypeOffsetY,
                title = stringResource(R.string.mobile_type_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_5g,
                        enabled,
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
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ColumnScope.featureRelativePositionOffsets(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
    AnimatedPreferenceGroup(visible = visual.layout != ContentLayout.BATTERY_CENTER) {
        val topInfoVerticalOffsetUi =
            topOffsetYUi(
                visual.topOffsetY,
            )
        if (search.matches(R.string.top_info_vertical_offset)) {
            SliderPreference(
                value = topInfoVerticalOffsetUi,
                onValueChange = visualRepo::setTopOffsetY,
                title = stringResource(R.string.top_info_vertical_offset),
                startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_height, enabled) },
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
                enabled = enabled,
            )
        }
    }
    AnimatedPreferenceGroup(visible = visual.centeredMobileSignal) {
        if (search.matches(R.string.mobile_type_bottom_offset)) {
            SliderPreference(
                value = visual.mobileTypeBottomOffsetY,
                onValueChange = visualRepo::setMobileTypeBottomOffsetY,
                title = stringResource(R.string.mobile_type_bottom_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_5g,
                        enabled,
                        R.drawable.ic_material_symbol_height_w500,
                    )
                },
                valueText = stringResource(
                    R.string.battery_top_offset_value,
                    visual.mobileTypeBottomOffsetY.roundToInt(),
                ),
                valueRange = NETWORK_TOP_OFFSET_MIN..NETWORK_TOP_OFFSET_MAX,
                steps = 19,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(0f),
                magnetThreshold = 0.035f,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ColumnScope.featureLayoutSizeAndWeight(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
    if (search.matches(R.string.combined_size)) {
        SliderPreference(
            value = visual.combinedScale,
            onValueChange = visualRepo::setCombinedScale,
            title = stringResource(R.string.combined_size),
            startAction = { SemanticLeadingIcon(R.drawable.ic_battery_ring_outline, enabled, R.drawable.ic_material_symbol_open_in_full) },
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
            enabled = enabled,
        )
    }
    if (search.matches(R.string.outer_weight)) {
        SliderPreference(
            value = visual.outerWeightScale,
            onValueChange = visualRepo::setOuterWeightScale,
            title = stringResource(R.string.outer_weight),
            startAction = { SemanticLeadingIcon(R.drawable.ic_battery_ring_outline, enabled, R.drawable.ic_material_symbol_line_weight) },
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
            enabled = enabled,
        )
    }
}
