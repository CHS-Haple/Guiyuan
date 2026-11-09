package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot

@Composable
internal fun batterySchemeDisplayName(
    library: BatteryColorSchemeLibrary,
    key: String,
): String =
    BatteryBuiltInColorScheme.fromKey(key)?.let { batteryBuiltInName(it) }
        ?: library.customByKey(key)?.let { customSchemeName(it) }
        ?: batteryBuiltInName(BatteryBuiltInColorScheme.HYPEROS)

@Composable
internal fun batteryBuiltInName(scheme: BatteryBuiltInColorScheme): String =
    when (scheme) {
        BatteryBuiltInColorScheme.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatteryBuiltInColorScheme.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatteryBuiltInColorScheme.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
    }

@Composable
internal fun customSchemeName(scheme: BatteryCustomColorScheme): String =
    scheme.name.ifBlank {
        stringResource(R.string.battery_custom_scheme_default_name, scheme.id)
    }

@Composable
internal fun batterySourceLabel(source: BatteryColorSchemeSource): String =
    when (source) {
        BatteryColorSchemeSource.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatteryColorSchemeSource.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatteryColorSchemeSource.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
        BatteryColorSchemeSource.FOLLOW_SYSTEM ->
            stringResource(R.string.battery_color_follow_inversion)
        BatteryColorSchemeSource.CUSTOM ->
            stringResource(R.string.battery_color_source_custom)
    }

@StringRes
internal fun batteryColorSlotLabel(slot: BatteryColorSlot): Int =
    when (slot) {
        BatteryColorSlot.NORMAL -> R.string.battery_mode_normal
        BatteryColorSlot.POWER_SAVE -> R.string.battery_mode_power_save
        BatteryColorSlot.PERFORMANCE -> R.string.battery_mode_performance
        BatteryColorSlot.SUPER_POWER_SAVE -> R.string.battery_mode_super_power_save
        BatteryColorSlot.CHARGING -> R.string.battery_mode_charging
        BatteryColorSlot.LOW -> R.string.battery_mode_low
    }
