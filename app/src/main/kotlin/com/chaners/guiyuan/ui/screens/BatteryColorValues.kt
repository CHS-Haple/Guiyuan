package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeEntry
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import com.chaners.guiyuan.settings.opaqueArgb

// Foreground following is a custom color choice, not a separate preset style.
internal val BATTERY_COLOR_STYLE_OPTIONS =
    listOf(
        BatteryColorSchemeSource.HYPEROS,
        BatteryColorSchemeSource.IOS,
        BatteryColorSchemeSource.LOW_SATURATION,
        BatteryColorSchemeSource.CUSTOM,
    )

internal fun batteryColorStyleSource(source: BatteryColorSchemeSource): BatteryColorSchemeSource =
    if (source == BatteryColorSchemeSource.FOLLOW_SYSTEM) BatteryColorSchemeSource.CUSTOM else source

internal fun schemePageForKey(
    library: BatteryColorSchemeLibrary,
    key: String,
): BatterySchemePage? =
    BatteryBuiltInColorScheme.fromKey(key)?.let { BatterySchemePage.BuiltIn(it) }
        ?: library.customByKey(key)?.let { BatterySchemePage.Custom(it) }

internal fun batteryColorEditorSeed(
    entry: BatteryColorSchemeEntry,
    slot: BatteryColorSlot,
): Int? =
    batterySchemeEntryColor(entry, slot)

internal fun batteryColorHex(color: Int): String =
    "#%06X".format(color and 0x00FFFFFF)

internal fun batteryColorFromHex(value: String): Int? {
    val normalized = value.removePrefix("#")
    if (normalized.length != 6) return null
    return normalized.toLongOrNull(16)
        ?.toInt()
        ?.opaqueArgb()
}

internal fun batteryColorRgb(color: Int): Triple<Int, Int, Int> =
    Triple(
        (color shr 16) and 0xFF,
        (color shr 8) and 0xFF,
        color and 0xFF,
    )

internal fun batteryColorFromRgb(
    red: String,
    green: String,
    blue: String,
): Int? {
    val r = red.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val g = green.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val b = blue.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    return ((r shl 16) or (g shl 8) or b).opaqueArgb()
}
