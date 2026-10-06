package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BuiltInBatteryScheme
import com.chaners.guiyuan.settings.BatterySchemeEntry
import com.chaners.guiyuan.settings.BatterySchemeLibrary
import com.chaners.guiyuan.settings.BatterySchemeSource
import com.chaners.guiyuan.settings.CustomBatteryScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.drawCheckerboard
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BatterySheetSmallTitle(
    text: String,
    textColor: Color? = null,
) {
    SmallTitle(
        text = text,
        textColor = textColor ?: MiuixTheme.colorScheme.onBackgroundVariant,
    )
}

@Composable
internal fun BatterySchemePreviewStrip(
    page: BatterySchemePage?,
    size: Dp = 12.dp,
    spacing: Dp = 6.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BATTERY_COLOR_PREVIEW_SLOTS.forEach { slot ->
            val color =
                when (page) {
                    is BatterySchemePage.BuiltIn ->
                        batteryBuiltInColor(page.scheme, slot)
                    is BatterySchemePage.Custom ->
                        batterySchemeEntryColor(page.scheme.entries.entryFor(slot), slot)
                    else -> null
                }
            if (color == null) {
                BatteryColorMosaic(size = size)
            } else {
                BatteryColorDot(
                    color = color,
                    size = size,
                )
            }
        }
    }
}

@Composable
private fun batteryCommonColorName(color: Int): String =
    stringResource(
        when (color) {
            0xFFFF3B30.toInt() -> R.string.battery_common_color_red
            0xFFFF9500.toInt() -> R.string.battery_common_color_orange
            0xFFFFCC00.toInt() -> R.string.battery_common_color_yellow
            0xFF34C759.toInt() -> R.string.battery_common_color_green
            0xFF32ADE6.toInt() -> R.string.battery_common_color_light_blue
            0xFF007AFF.toInt() -> R.string.battery_common_color_blue
            0xFF5856D6.toInt() -> R.string.battery_common_color_indigo
            0xFFAF52DE.toInt() -> R.string.battery_common_color_purple
            0xFFFF2D55.toInt() -> R.string.battery_common_color_pink
            else -> R.string.battery_common_color_gray
        },
    )

@Composable
internal fun BatteryCommonColorButton(
    color: Int,
    selected: Boolean,
    visuallyInactive: Boolean,
    onClick: () -> Unit,
) {
    TooltipBox(text = batteryCommonColorName(color)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(3.dp, Color.White),
                        shadowElevation = 2.dp,
                    ) {}
                }
                BatteryColorDot(
                    color = color,
                    size = 28.dp,
                    visuallyInactive = visuallyInactive,
                )
            }
        }
    }
}

private const val BATTERY_SWATCH_BORDER_RATIO = 1f / 14f
private const val BATTERY_SWATCH_BORDER_ALPHA = 0.26f

@Composable
private fun batterySwatchBorder(size: Dp): BorderStroke =
    BorderStroke(
        width = (size.value * BATTERY_SWATCH_BORDER_RATIO).dp,
        color = MiuixTheme.colorScheme.onSurface.copy(alpha = BATTERY_SWATCH_BORDER_ALPHA),
    )

@Composable
internal fun BatteryColorDot(
    color: Int,
    size: Dp = 20.dp,
    visuallyInactive: Boolean = false,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color =
            if (visuallyInactive) {
                Color(color).copy(alpha = 0.45f)
            } else {
                Color(color)
            },
        border =
            if (visuallyInactive) {
                BorderStroke(
                    width = (size.value * BATTERY_SWATCH_BORDER_RATIO).dp,
                    color = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                )
            } else {
                batterySwatchBorder(size)
            },
    ) {}
}

@Composable
internal fun BatteryColorMosaic(
    size: Dp = 20.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Color.Transparent,
        border = batterySwatchBorder(size),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawCheckerboard(
                        cellSizeDp = 3.dp,
                        lightColor = MiuixTheme.colorScheme.surfaceContainer,
                        darkColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    ),
        )
    }
}

@Composable
internal fun batterySchemeDisplayName(
    library: BatterySchemeLibrary,
    key: String,
): String =
    BuiltInBatteryScheme.fromKey(key)?.let { batteryBuiltInName(it) }
        ?: library.customByKey(key)?.let { customSchemeName(it) }
        ?: batteryBuiltInName(BuiltInBatteryScheme.HYPEROS)

@Composable
internal fun batteryBuiltInName(scheme: BuiltInBatteryScheme): String =
    when (scheme) {
        BuiltInBatteryScheme.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BuiltInBatteryScheme.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BuiltInBatteryScheme.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
    }

@Composable
internal fun customSchemeName(scheme: CustomBatteryScheme): String =
    scheme.name.ifBlank {
        stringResource(R.string.battery_custom_scheme_default_name, scheme.id)
    }

@Composable
internal fun batterySourceLabel(source: BatterySchemeSource): String =
    when (source) {
        BatterySchemeSource.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatterySchemeSource.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatterySchemeSource.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
        BatterySchemeSource.FOLLOW_SYSTEM ->
            stringResource(R.string.battery_color_follow_inversion)
        BatterySchemeSource.CUSTOM ->
            stringResource(R.string.battery_color_source_custom)
    }

@Composable
private fun batterySourceValue(
    entry: BatterySchemeEntry,
    slot: BatteryColorSlot,
): String =
    batterySchemeEntryColor(entry, slot)?.let(::batteryColorHex)
        ?: stringResource(R.string.battery_color_follow_inversion)

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

internal fun schemePageForKey(
    library: BatterySchemeLibrary,
    key: String,
): BatterySchemePage? =
    BuiltInBatteryScheme.fromKey(key)?.let { BatterySchemePage.BuiltIn(it) }
        ?: library.customByKey(key)?.let { BatterySchemePage.Custom(it) }

internal fun batteryColorEditorSeed(
    entry: BatterySchemeEntry,
    slot: BatteryColorSlot,
): Int? =
    batterySchemeEntryColor(entry, slot)
        ?: entry.customColor

internal fun batteryColorHex(color: Int): String =
    "#%06X".format(color and 0x00FFFFFF)

internal fun batteryColorFromHex(value: String): Int? {
    val normalized = value.removePrefix("#")
    if (normalized.length != 6) return null
    return normalized.toLongOrNull(16)
        ?.toInt()
        ?.or(0xFF000000.toInt())
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
    return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
}
