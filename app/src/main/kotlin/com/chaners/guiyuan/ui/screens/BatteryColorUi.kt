package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
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
        border = BorderStroke(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
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
        border = BorderStroke(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
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
