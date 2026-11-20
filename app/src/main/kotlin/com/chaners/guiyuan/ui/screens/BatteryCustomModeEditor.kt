package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import com.chaners.guiyuan.settings.opaqueArgb
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

internal data class ColorWheelState(
    val editing: MutableState<Int?>,
    val hue: MutableFloatState,
    val saturation: MutableFloatState,
)

internal data class ColorChannelState(
    val hex: MutableState<String>,
    val red: MutableState<String>,
    val green: MutableState<String>,
    val blue: MutableState<String>,
)

internal data class ColorEditorState(
    val wheel: ColorWheelState,
    val channels: ColorChannelState,
) {
    fun showColor(color: Int, hue: Float? = null) {
        val opaque = color.opaqueArgb()
        wheel.editing.value = opaque
        val chosenHsv = Color(opaque).toHsv()
        if (chosenHsv.v > 0.01f) wheel.saturation.floatValue = chosenHsv.s
        if (hue != null) {
            wheel.hue.floatValue = hue
        } else if (chosenHsv.v > 0.01f && chosenHsv.s > 0.01f) {
            wheel.hue.floatValue = chosenHsv.h
        }
        channels.hex.value = batteryColorHex(opaque).removePrefix("#")
        val value = batteryColorRgb(opaque)
        channels.red.value = value.first.toString()
        channels.green.value = value.second.toString()
        channels.blue.value = value.third.toString()
    }

    fun applyColor(color: Int, hue: Float? = null, onColorChange: (Int) -> Unit) {
        showColor(color, hue)
        // The repository stays the sole writer; choosing a color selects CUSTOM.
        onColorChange(color.opaqueArgb())
    }
}

@Composable
internal fun BatteryCustomModeEditor(
    custom: BatteryCustomColorScheme,
    slot: BatteryColorSlot,
    onSourceChange: (BatteryColorSchemeSource) -> Unit,
    onColorChange: (Int) -> Unit,
) {
    val state = rememberColorEditorState(custom, slot)
    val scrollState = rememberScrollState()
    val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canScroll) Modifier.scrollEndHaptic() else Modifier)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        BatteryColorSourceSelector(custom, slot, state, onSourceChange)
        BatteryColorCommonGrid(custom, slot, state, onColorChange)
        BatteryColorHsvPanel(state, onColorChange)
        BatteryColorPreciseFields(state, onColorChange)
    }
}

@Composable
private fun rememberColorEditorState(
    custom: BatteryCustomColorScheme,
    slot: BatteryColorSlot,
): ColorEditorState {
    val entry = custom.entries.entryFor(slot)
    val seed = batteryColorEditorSeed(entry, slot)
    val editing = remember(custom.id, slot, entry.source, seed) {
        mutableStateOf(seed?.opaqueArgb())
    }
    val hue = remember(custom.id, slot) {
        mutableFloatStateOf(
            seed?.let { Color(it).toHsv().h } ?: Color(0xFF3482FF).toHsv().h,
        )
    }
    val saturation = remember(custom.id, slot) {
        mutableFloatStateOf(
            seed?.let { Color(it).toHsv() }
                ?.takeIf { it.v > 0.01f }
                ?.s ?: 100f,
        )
    }
    val hex = remember(custom.id, slot, editing.value) {
        mutableStateOf(editing.value?.let(::batteryColorHex)?.removePrefix("#").orEmpty())
    }
    val rgb = editing.value?.let(::batteryColorRgb)
    val red = remember(custom.id, slot, editing.value) {
        mutableStateOf(rgb?.first?.toString().orEmpty())
    }
    val green = remember(custom.id, slot, editing.value) {
        mutableStateOf(rgb?.second?.toString().orEmpty())
    }
    val blue = remember(custom.id, slot, editing.value) {
        mutableStateOf(rgb?.third?.toString().orEmpty())
    }
    return ColorEditorState(
        wheel = ColorWheelState(editing, hue, saturation),
        channels = ColorChannelState(hex, red, green, blue),
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BatteryColorSourceSelector(
    custom: BatteryCustomColorScheme,
    slot: BatteryColorSlot,
    state: ColorEditorState,
    onSourceChange: (BatteryColorSchemeSource) -> Unit,
) {
    val entry = custom.entries.entryFor(slot)
    val resolved = batterySchemeEntryColor(entry, slot)
    val seed = batteryColorEditorSeed(entry, slot)
    val sourceOptions = BatteryColorSchemeSource.entries
    val sourceLabels = listOf(
        batterySourceLabel(BatteryColorSchemeSource.HYPEROS),
        batterySourceLabel(BatteryColorSchemeSource.IOS),
        batterySourceLabel(BatteryColorSchemeSource.LOW_SATURATION),
        batterySourceLabel(BatteryColorSchemeSource.FOLLOW_SYSTEM),
        batterySourceLabel(BatteryColorSchemeSource.CUSTOM),
    )
    val selectedSourceIndex = sourceOptions.indexOf(entry.source).coerceAtLeast(0)
    val currentValueText = when {
        resolved != null -> batteryColorHex(resolved)
        entry.source == BatteryColorSchemeSource.CUSTOM ->
            stringResource(R.string.battery_color_custom_unset)
        else -> stringResource(R.string.battery_color_follow_inversion)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        UiDropdownPreference(
            items = sourceLabels,
            selectedIndex = selectedSourceIndex,
            title = stringResource(R.string.battery_color_source),
            summary = currentValueText,
            startAction = {
                val shown = if (entry.source == BatteryColorSchemeSource.CUSTOM ||
                    state.wheel.editing.value != seed
                ) state.wheel.editing.value ?: resolved else resolved
                if (shown == null) BatteryColorMosaic(size = 24.dp)
                else BatteryColorDot(color = shown, size = 24.dp)
            },
            onSelectedIndexChange = { index ->
                sourceOptions.getOrNull(index)?.let { source ->
                    val nextColor = batteryColorEditorSeed(entry.copy(source = source), slot)
                    if (nextColor == null) state.wheel.editing.value = null
                    else state.showColor(nextColor)
                    onSourceChange(source)
                }
            },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BatteryColorCommonGrid(
    custom: BatteryCustomColorScheme,
    slot: BatteryColorSlot,
    state: ColorEditorState,
    onColorChange: (Int) -> Unit,
) {
    val rows = remember { COMMON_BATTERY_COLORS.chunked(5) }
    val source = custom.entries.entryFor(slot).source
    BatterySheetSmallTitle(
        text = stringResource(R.string.battery_color_common),
        textColor = MiuixTheme.colorScheme.onBackgroundVariant,
    )
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        rows.forEachIndexed { index, colors ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = if (index == 0) 12.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                colors.forEach { color ->
                    BatteryCommonColorButton(
                        color = color,
                        selected = source == BatteryColorSchemeSource.CUSTOM && state.wheel.editing.value == color,
                        visuallyInactive = false,
                        onClick = { state.applyColor(color, onColorChange = onColorChange) },
                    )
                }
            }
        }
    }
}
