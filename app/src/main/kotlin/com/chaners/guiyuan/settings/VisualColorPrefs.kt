package com.chaners.guiyuan.settings

import android.content.SharedPreferences

internal fun SharedPreferences.Editor.putBatteryColors(settings: VisualCfg): SharedPreferences.Editor =
    putString(BATTERY_COLOR_PRESET_KEY, settings.batteryColorPreset.persistedValue)
        .applyBatteryColorModes(settings.batteryColorModes)
        .applyBatteryColorOverrides(settings.batteryColorOverrides)

private fun SharedPreferences.Editor.applyBatteryColorOverrides(
    overrides: BatteryColorOverrides,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        val key = batteryColorOverrideKey(slot)
        val color = overrides.colorFor(slot)
        if (color == null) remove(key)
        else putInt(key, color or 0xFF000000.toInt())
    }
    return this
}

private fun SharedPreferences.Editor.applyBatteryColorModes(
    modes: BatteryColorModes,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        putString(batteryColorModeKey(slot), modes.modeFor(slot).persistedValue)
    }
    return this
}
