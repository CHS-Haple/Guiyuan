package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
import com.chaners.guiyuan.settings.BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT
import com.chaners.guiyuan.settings.BOTH_SIDES_BOTTOM_INFO_SCALE_MAX
import com.chaners.guiyuan.settings.BOTH_SIDES_BOTTOM_INFO_SCALE_MIN
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MAX
import com.chaners.guiyuan.settings.NETWORK_TOP_OFFSET_MIN
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_DEFAULT
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MAX
import com.chaners.guiyuan.settings.OUTER_WEIGHT_SCALE_MIN
import com.chaners.guiyuan.settings.SignalDotArrangement
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.settings.topOffsetYUi
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

internal fun LazyListScope.featureLayoutSection(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    bothSidesChargingSearch: Boolean,
) {
    val searching = search.filtering
    val bothSidesActivitySearch = searching && search.any(
        R.string.both_sides_activity_title,
        R.string.both_sides_idle_activity_title,
        R.string.both_sides_activity_size,
        R.string.both_sides_activity_vertical_offset,
    )
    val bothSidesSearch = bothSidesActivitySearch || bothSidesChargingSearch
    Section(R.string.section_layout_size) {
        featureLayoutChoices(search, visual, visualRepo, enabled, bothSidesSearch)
        featureLayoutSignalAdjustments(search, visual, visualRepo, enabled)
        featureLayoutBothSides(search, visual, visualRepo, enabled, bothSidesActivitySearch, bothSidesChargingSearch)
        featureLayoutOffsetsAndScale(search, visual, visualRepo, enabled)
    }
}

@Composable
private fun ColumnScope.featureLayoutChoices(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
    bothSidesSearch: Boolean,
) {
    // Display order is deliberately independent of the persisted ContentLayout enum.
    val layoutChoices = listOf(
        ContentLayout.NETWORK_CENTER to stringResource(R.string.content_layout_network_center),
        ContentLayout.MOBILE_CENTER to stringResource(R.string.content_layout_mobile_center),
        ContentLayout.BATTERY_CENTER to stringResource(R.string.content_layout_battery_center),
    )
    val signalDotArrangements = listOf(
        stringResource(R.string.signal_dot_arrangement_curved),
        stringResource(R.string.signal_dot_arrangement_horizontal),
    )
    val dualSignalDotArrangements = signalDotArrangements +
        stringResource(R.string.signal_dot_arrangement_both_sides)
    if (search.matches(R.string.content_layout_title) ||
        (searching && visual.layout != ContentLayout.BATTERY_CENTER &&
            search.any(R.string.wifi_vertical_offset, R.string.mobile_type_vertical_offset)) ||
        (searching && visual.layout == ContentLayout.BATTERY_CENTER &&
            search.matches(R.string.top_info_vertical_offset)) ||
        (searching && visual.centeredMobileSignal &&
            search.any(R.string.curved_signal_dot_vertical_offset,
                R.string.horizontal_signal_dot_vertical_offset))) {
        UiDropdownPreference(
            items = layoutChoices.map { it.second },
            selectedIndex = layoutChoices.indexOfFirst { it.first == visual.layout },
            title = stringResource(R.string.content_layout_title),
            startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_dashboard, enabled) },
            summary = stringResource(
                when (visual.layout) {
                    ContentLayout.NETWORK_CENTER -> R.string.content_layout_network_center_summary
                    ContentLayout.BATTERY_CENTER -> R.string.content_layout_battery_center_summary
                    ContentLayout.MOBILE_CENTER -> R.string.content_layout_mobile_center_summary
                },
            ),
            showValue = true,
            enabled = enabled,
            onSelectedIndexChange = { index ->
                layoutChoices.getOrNull(index)?.first?.let(visualRepo::setLayout)
            },
        )
    }
    if (search.matches(R.string.dual_signal_title) ||
        (searching && !visual.dualSignal &&
            (search.any(R.string.dual_row_signal_dot_arrangement_title,
                R.string.curved_signal_dot_vertical_offset,
                R.string.horizontal_signal_dot_vertical_offset) || bothSidesSearch))) {
        SwitchPreference(
            title = stringResource(R.string.dual_signal_title),
            startAction = {
                SemanticLeadingIcon(
                    R.drawable.ic_material_symbol_sim_cards,
                    enabled,
                )
            },
            summary = stringResource(R.string.dual_signal_summary),
            checked = visual.dualSignal,
            enabled = enabled,
            onCheckedChange = visualRepo::setDualSignal,
        )
    }
    if (search.matches(R.string.single_row_signal_dot_arrangement_title)) {
        UiDropdownPreference(
            title = stringResource(R.string.single_row_signal_dot_arrangement_title),
            summary = stringResource(R.string.outer_signal_dots_only_summary),
            items = signalDotArrangements,
            selectedIndex = visual.singleRowDotArrangement.ordinal,
            showValue = true,
            enabled = enabled,
            startAction = {
                SemanticLeadingIcon(
                    R.drawable.ic_material_symbol_sim_card,
                    enabled,
                    R.drawable.ic_material_symbol_layers,
                )
            },
            onSelectedIndexChange = { index ->
                SignalDotArrangement.entries.getOrNull(index)?.let(visualRepo::setSingleRowDotArrangement)
            },
        )
    }
    AnimatedPreferenceGroup(visible = visual.dualSignal) {
        if (search.matches(R.string.dual_row_signal_dot_arrangement_title) ||
            (bothSidesSearch && visual.dualRowDotArrangement != SignalDotArrangement.BOTH_SIDES)) {
            UiDropdownPreference(
                title = stringResource(R.string.dual_row_signal_dot_arrangement_title),
                summary = stringResource(R.string.outer_signal_dots_only_summary),
                items = dualSignalDotArrangements,
                selectedIndex = visual.dualRowDotArrangement.ordinal,
                showValue = true,
                enabled = enabled,
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_material_symbol_sim_cards,
                        enabled,
                        R.drawable.ic_material_symbol_layers,
                    )
                },
                onSelectedIndexChange = { index ->
                    SignalDotArrangement.entries.getOrNull(index)?.let(visualRepo::setDualRowDotArrangement)
                },
            )
        }
    }
}

@Composable
private fun ColumnScope.featureLayoutSignalAdjustments(
    search: PageSearch,
    visual: VisualCfg,
    visualRepo: VisualRepo,
    enabled: Boolean,
) {
    val lowerOpeningOptions = listOf(
        stringResource(R.string.battery_lower_opening_follow),
        stringResource(R.string.battery_lower_opening_fixed),
    )
    val activeDotArrangement =
        if (visual.dualSignal) visual.dualRowDotArrangement else visual.singleRowDotArrangement
    listOf(
        SignalDotArrangement.CURVED to R.string.curved_signal_dot_vertical_offset,
        SignalDotArrangement.HORIZONTAL to R.string.horizontal_signal_dot_vertical_offset,
    ).forEach { (style, dotOffsetTitle) ->
        AnimatedPreferenceGroup(visible = activeDotArrangement == style) {
            if (search.matches(dotOffsetTitle)) {
                SliderPreference(
                    value = visual.activeSignalDotOffsetY,
                    onValueChange = visualRepo::setSignalDotOffsetY,
                    title = stringResource(dotOffsetTitle),
                    summary = stringResource(R.string.outer_signal_dots_only_summary),
                    startAction = {
                        SemanticLeadingIcon(
                            R.drawable.ic_material_symbol_signal_cellular_alt,
                            enabled,
                            R.drawable.ic_material_symbol_height_w500,
                        )
                    },
                    valueText = stringResource(
                        R.string.battery_top_offset_value,
                        visual.activeSignalDotOffsetY.roundToInt(),
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
    AnimatedPreferenceGroup(visible = activeDotArrangement != SignalDotArrangement.BOTH_SIDES) {
        if (search.matches(R.string.battery_lower_opening_title)) {
            UiDropdownPreference(
                title = stringResource(R.string.battery_lower_opening_title),
                summary = stringResource(R.string.battery_lower_opening_summary),
                items = lowerOpeningOptions,
                selectedIndex = if (visual.fixedLowerOpening) 1 else 0,
                showValue = true,
                enabled = enabled,
                startAction = {
                    SemanticLeadingIcon(
                        R.drawable.ic_battery_ring_opening,
                        enabled,
                    )
                },
                onSelectedIndexChange = { index ->
                    if (index in lowerOpeningOptions.indices) {
                        visualRepo.setFixedLowerOpening(index == 1)
                    }
                },
            )
        }
    }
}

@Composable
private fun ColumnScope.featureLayoutBothSides(
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
        AnimatedPreferenceGroup(visible = visual.bothSidesShowActivity) {
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
}

@Composable
private fun ColumnScope.featureLayoutOffsetsAndScale(
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
