package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT
import com.chaners.guiyuan.settings.BOTH_SIDES_BOTTOM_INFO_SCALE_MAX
import com.chaners.guiyuan.settings.BOTH_SIDES_BOTTOM_INFO_SCALE_MIN
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MAX
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MIN
import com.chaners.guiyuan.settings.SignalDotArrangement
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
internal fun ColumnScope.featureLayoutBothSides(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    bothSidesActivitySearch: Boolean,
    bothSidesChargingSearch: Boolean,
) {
    val bothSidesArrangement = visual.dualSignal &&
        visual.dualRowDotArrangement == SignalDotArrangement.BOTH_SIDES
    if (search.matches(R.string.both_sides_activity_title) ||
        (bothSidesActivitySearch && !visual.bothSidesShowActivity)) {
        SwitchPreference(
            title = stringResource(R.string.both_sides_activity_title),
            summary = stringResource(
                if (bothSidesArrangement) R.string.both_sides_activity_summary
                else R.string.both_sides_activity_unavailable_summary,
            ),
            checked = visual.bothSidesShowActivity,
            enabled = enabled && bothSidesArrangement,
            startAction = {
                SemanticLeadingIcon(R.drawable.ic_guiyuan_network_activity_triangles, enabled && bothSidesArrangement)
            },
            onCheckedChange = visualRepo::setBothSidesShowActivity,
        )
    }
    AnimatedPreferenceGroup(visible = bothSidesArrangement) {
        featureBothSidesActivityOptions(search, visual, visualRepo, enabled)
        featureBothSidesChargingOptions(search, visual, visualRepo, enabled, bothSidesChargingSearch)
    }
}

@Composable
private fun ColumnScope.featureBothSidesActivityOptions(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
    AnimatedPreferenceGroup(visible = visual.bothSidesShowActivity) {
        featureBothSidesIdleActivity(search, visual, visualRepo, enabled)
        featureBothSidesActivityScale(search, visual, visualRepo, enabled)
        featureBothSidesActivityVerticalOffset(search, visual, visualRepo, enabled)
    }
}

@Composable
private fun ColumnScope.featureBothSidesIdleActivity(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
        if (search.matches(R.string.both_sides_idle_activity_title)) {
            SwitchPreference(
                title = stringResource(R.string.both_sides_idle_activity_title),
                summary = stringResource(R.string.both_sides_idle_activity_summary),
                checked = visual.bothSidesHideIdleActivity,
                enabled = enabled,
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_guiyuan_network_activity_triangles,
                        enabled,
                        R.drawable.ic_material_symbol_visibility_off,
                    )
                },
                onCheckedChange = visualRepo::setBothSidesHideIdleActivity,
            )
        }
}

@Composable
private fun ColumnScope.featureBothSidesActivityScale(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
        if (search.matches(R.string.both_sides_activity_size)) {
            SliderPreference(
                value = visual.bothSidesActivityScale,
                onValueChange = visualRepo::setBothSidesActivityScale,
                title = stringResource(R.string.both_sides_activity_size),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_guiyuan_network_activity_triangles,
                        enabled,
                        R.drawable.ic_material_symbol_open_in_full,
                    )
                },
                valueText = stringResource(
                    R.string.percent_value,
                    (visual.bothSidesActivityScale * 100f).roundToInt(),
                ),
                valueRange = BOTH_SIDES_BOTTOM_INFO_SCALE_MIN..BOTH_SIDES_BOTTOM_INFO_SCALE_MAX,
                steps = 9,
                showKeyPoints = true,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT),
                magnetThreshold = 0.035f,
                enabled = enabled,
            )
        }
}

@Composable
private fun ColumnScope.featureBothSidesActivityVerticalOffset(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
        if (search.matches(R.string.both_sides_activity_vertical_offset)) {
            SliderPreference(
                value = visual.bothSidesActivityOffsetY,
                onValueChange = visualRepo::setBothSidesActivityOffsetY,
                title = stringResource(R.string.both_sides_activity_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_guiyuan_network_activity_triangles,
                        enabled,
                        R.drawable.ic_material_symbol_height_w500,
                    )
                },
                valueText = stringResource(
                    R.string.battery_top_offset_value,
                    visual.bothSidesActivityOffsetY.roundToInt(),
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

@Composable
private fun ColumnScope.featureBothSidesChargingOptions(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    bothSidesChargingSearch: Boolean,
) {
    if (search.matches(R.string.both_sides_charging_title) ||
        (bothSidesChargingSearch && !visual.bothSidesBottomCharging)) {
        SwitchPreference(
            title = stringResource(R.string.both_sides_charging_title),
            summary = stringResource(R.string.both_sides_charging_summary),
            checked = visual.bothSidesBottomCharging,
            enabled = enabled && visual.showChargingIcon,
            startAction = {
                SemanticLeadingIcon(R.drawable.ic_material_symbol_bolt, enabled)
            },
            onCheckedChange = visualRepo::setBothSidesBottomCharging,
        )
    }
    AnimatedPreferenceGroup(visible = visual.bothSidesBottomCharging && visual.showChargingIcon) {
        if (search.matches(R.string.both_sides_charging_vertical_offset)) {
            SliderPreference(
                value = visual.bothSidesChargingOffsetY,
                onValueChange = visualRepo::setBothSidesChargingOffsetY,
                title = stringResource(R.string.both_sides_charging_vertical_offset),
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_bolt,
                        enabled,
                        R.drawable.ic_material_symbol_height_w500,
                )
            },
            valueText = stringResource(
                R.string.battery_top_offset_value,
                visual.bothSidesChargingOffsetY.roundToInt(),
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
