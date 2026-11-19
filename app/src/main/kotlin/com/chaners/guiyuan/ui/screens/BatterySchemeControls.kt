package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BatterySchemeSettingsCard(
    builtIn: BatteryBuiltInColorScheme?,
    custom: BatteryCustomColorScheme?,
    canCreateCustom: Boolean,
    onSlotClick: (BatteryColorSlot) -> Unit,
    onSettingsCardMeasured: (Int) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    onSettingsCardMeasured(size.height)
                },
    ) {
        BatteryColorSlot.entries.forEach { slot ->
            val entry = custom?.entries?.entryFor(slot)
            val color =
                when {
                    builtIn != null -> batteryBuiltInColor(builtIn, slot)
                    entry != null -> batterySchemeEntryColor(entry, slot)
                    else -> null
                }
            val followsSystem =
                color == null ||
                    (builtIn == null && entry?.source == BatteryColorSchemeSource.FOLLOW_SYSTEM)
            BatteryModeSettingItem(
                slot = slot,
                color = color,
                followsSystem = followsSystem,
                enabled = custom != null || canCreateCustom,
                onClick = { onSlotClick(slot) },
            )
        }
    }
}

@Composable
internal fun BatterySchemeHeader(
    name: String,
    previewPage: BatterySchemePage?,
    endAction: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = IconButtonDefaults.MinHeight),
    ) {
        Text(
            text = name,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IconButtonDefaults.MinWidth)
                    .align(Alignment.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        endAction?.let { action ->
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                action()
            }
        }
    }
    Spacer(Modifier.height(BATTERY_SCHEME_VERTICAL_GAP))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BatterySchemePreviewStrip(
            page = previewPage,
            size = 28.dp,
            spacing = 10.dp,
        )
    }
}

@Composable
internal fun BatterySchemeActionArea(
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(BATTERY_SCHEME_VERTICAL_GAP))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
    Spacer(Modifier.height(BATTERY_SCHEME_VERTICAL_GAP))
}

@Composable
private fun BatteryModeSettingItem(
    slot: BatteryColorSlot,
    color: Int?,
    followsSystem: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val summary =
        if (followsSystem || color == null) {
            stringResource(R.string.battery_color_follow_inversion)
        } else {
            batteryColorHex(color)
        }
    ArrowPreference(
        title = stringResource(batteryColorSlotLabel(slot)),
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        endActions = {
            if (followsSystem || color == null) {
                BatteryColorMosaic(size = 24.dp)
            } else {
                BatteryColorDot(
                    color = color,
                    size = 24.dp,
                )
            }
        },
    )
}
