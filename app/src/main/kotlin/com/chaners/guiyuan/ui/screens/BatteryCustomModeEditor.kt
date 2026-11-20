package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import com.chaners.guiyuan.settings.opaqueArgb
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.color.space.Hsv
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

private data class ColorWheelState(
    val editing: MutableState<Int?>,
    val hue: MutableFloatState,
    val saturation: MutableFloatState,
)

private data class ColorChannelState(
    val hex: MutableState<String>,
    val red: MutableState<String>,
    val green: MutableState<String>,
    val blue: MutableState<String>,
)

private data class ColorEditorState(
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

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BatteryColorHsvPanel(
    state: ColorEditorState,
    onColorChange: (Int) -> Unit,
) {
    val hsv = state.wheel.editing.value?.let { Color(it).toHsv() }
    BatterySheetSmallTitle(
        text = stringResource(R.string.battery_color_full_adjustment),
        textColor = MiuixTheme.colorScheme.onBackgroundVariant,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp,
            ),
        ) {
            BatteryColorField(
                color = state.wheel.editing.value?.let(::Color),
                hue = state.wheel.hue.floatValue,
                saturation = state.wheel.saturation.floatValue,
                onColorChanged = { color, hue ->
                    state.applyColor(color.toArgb(), hue, onColorChange)
                },
            )
        }
        if (hsv != null) {
            BatteryColorHueRow(state, onColorChange)
            BatteryColorSaturationRow(state, hsv, onColorChange)
            BatteryColorBrightnessRow(state, hsv, onColorChange)
        } else {
            BasicComponent(
                title = stringResource(R.string.battery_color_no_fixed_color),
                summary = stringResource(R.string.battery_color_no_fixed_color_summary),
                enabled = false,
            )
        }
    }
}

@Composable
private fun BatteryColorHueRow(
    state: ColorEditorState,
    onColorChange: (Int) -> Unit,
) {
    BatteryHsvAdjustmentRow(
        title = stringResource(R.string.battery_color_hue),
        valueText = "${state.wheel.hue.floatValue.roundToInt()}°",
    ) {
        HsvHueSlider(
            currentHue = state.wheel.hue.floatValue,
            onHueChanged = { fraction ->
                val hue = fraction * 360f
                val current = state.wheel.editing.value?.let { Color(it).toHsv() }
                state.applyColor(
                    Hsv(hue, state.wheel.saturation.floatValue, current?.v ?: 100f).toColor().toArgb(),
                    hue,
                    onColorChange,
                )
            },
        )
    }
}

@Composable
private fun BatteryColorSaturationRow(
    state: ColorEditorState,
    hsv: Hsv,
    onColorChange: (Int) -> Unit,
) {
    BatteryHsvAdjustmentRow(
        title = stringResource(R.string.battery_color_saturation),
        valueText = "${state.wheel.saturation.floatValue.roundToInt()}%",
    ) {
        HsvSaturationSlider(
            currentHue = state.wheel.hue.floatValue,
            currentSaturation = state.wheel.saturation.floatValue / 100f,
            onSaturationChanged = { saturation ->
                state.wheel.saturation.floatValue = saturation * 100f
                state.applyColor(
                    Hsv(state.wheel.hue.floatValue, saturation * 100f, hsv.v).toColor().toArgb(),
                    onColorChange = onColorChange,
                )
            },
        )
    }
}

@Composable
private fun BatteryColorBrightnessRow(
    state: ColorEditorState,
    hsv: Hsv,
    onColorChange: (Int) -> Unit,
) {
    BatteryHsvAdjustmentRow(
        title = stringResource(R.string.battery_color_brightness),
        valueText = "${hsv.v.roundToInt()}%",
    ) {
        HsvValueSlider(
            currentHue = state.wheel.hue.floatValue,
            currentSaturation = state.wheel.saturation.floatValue / 100f,
            currentValue = hsv.v / 100f,
            onValueChanged = { value ->
                state.applyColor(
                    Hsv(state.wheel.hue.floatValue, state.wheel.saturation.floatValue, value * 100f).toColor().toArgb(),
                    onColorChange = onColorChange,
                )
            },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BatteryColorPreciseFields(
    state: ColorEditorState,
    onColorChange: (Int) -> Unit,
) {
    BatterySheetSmallTitle(
        text = stringResource(R.string.battery_color_precise_input),
        textColor = MiuixTheme.colorScheme.onBackgroundVariant,
    )
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        BatteryHexField(state, onColorChange)
        BatteryRgbFields(state, onColorChange)
    }
}

@Composable
private fun BatteryHexField(state: ColorEditorState, onColorChange: (Int) -> Unit) {
    TextField(
        value = state.channels.hex.value,
        onValueChange = { raw ->
            val normalized = raw.removePrefix("#")
                .uppercase()
                .filter { it.isDigit() || it in 'A'..'F' }
            if (normalized.length <= 6) {
                state.channels.hex.value = normalized
                batteryColorFromHex(normalized)?.let { state.applyColor(it, onColorChange = onColorChange) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.battery_color_hex),
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(),
        textStyle = MiuixTheme.textStyles.main,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
}

@Composable
private fun BatteryRgbFields(state: ColorEditorState, onColorChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BatteryRgbChannelField(value = state.channels.red.value, label = "R") { text ->
            state.channels.red.value = text
            batteryColorFromRgb(
                state.channels.red.value, state.channels.green.value, state.channels.blue.value,
            )?.let { state.applyColor(it, onColorChange = onColorChange) }
        }
        BatteryRgbChannelField(value = state.channels.green.value, label = "G") { text ->
            state.channels.green.value = text
            batteryColorFromRgb(
                state.channels.red.value, state.channels.green.value, state.channels.blue.value,
            )?.let { state.applyColor(it, onColorChange = onColorChange) }
        }
        BatteryRgbChannelField(value = state.channels.blue.value, label = "B") { text ->
            state.channels.blue.value = text
            batteryColorFromRgb(
                state.channels.red.value, state.channels.green.value, state.channels.blue.value,
            )?.let { state.applyColor(it, onColorChange = onColorChange) }
        }
    }
}

@Composable
private fun RowScope.BatteryRgbChannelField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = { raw ->
            if (raw.length <= 3 && raw.all(Char::isDigit)) onValueChange(raw)
        },
        modifier = Modifier.weight(1f),
        label = label,
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(),
        textStyle = MiuixTheme.textStyles.main,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun BatteryHsvAdjustmentRow(
    title: String,
    valueText: String,
    slider: @Composable () -> Unit,
) {
    BasicComponent(
        title = title,
        endActions = {
            Text(
                text = valueText,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        bottomAction = slider,
    )
}
