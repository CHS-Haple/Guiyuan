package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
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
import kotlin.math.roundToInt

@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.BatteryColorHsvPanel(
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
internal fun androidx.compose.foundation.layout.ColumnScope.BatteryColorPreciseFields(
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
