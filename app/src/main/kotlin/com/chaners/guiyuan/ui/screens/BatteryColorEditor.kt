package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatterySchemeSource
import com.chaners.guiyuan.settings.CustomBatteryScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import com.chaners.guiyuan.settings.limitBatteryCustomSchemeNameInput
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.color.space.Hsv
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
internal fun BatteryCustomModeEditor(
    custom: CustomBatteryScheme,
    slot: BatteryColorSlot,
    onSourceChange: (BatterySchemeSource) -> Unit,
    onColorChange: (Int) -> Unit,
) {
    val entry = custom.entries.entryFor(slot)
    val resolved = batterySchemeEntryColor(entry, slot)
    val sourceOptions = BatterySchemeSource.entries
    val sourceLabels =
        listOf(
            batterySourceLabel(BatterySchemeSource.HYPEROS),
            batterySourceLabel(BatterySchemeSource.IOS),
            batterySourceLabel(BatterySchemeSource.LOW_SATURATION),
            batterySourceLabel(BatterySchemeSource.FOLLOW_SYSTEM),
            batterySourceLabel(BatterySchemeSource.CUSTOM),
        )
    val selectedSourceIndex = sourceOptions.indexOf(entry.source).coerceAtLeast(0)
    val seed = batteryColorEditorSeed(entry, slot)
    val visuallyInactive = entry.source == BatterySchemeSource.FOLLOW_SYSTEM

    var editingColor by remember(custom.id, slot, entry.source, seed) {
        mutableStateOf(seed?.or(0xFF000000.toInt()))
    }
    var hexText by remember(custom.id, slot, editingColor) {
        mutableStateOf(
            editingColor
                ?.let(::batteryColorHex)
                ?.removePrefix("#")
                .orEmpty(),
        )
    }
    val rgb = editingColor?.let(::batteryColorRgb)
    var redText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb?.first?.toString().orEmpty())
    }
    var greenText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb?.second?.toString().orEmpty())
    }
    var blueText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb?.third?.toString().orEmpty())
    }

    fun applyColor(color: Int) {
        val opaque = color or 0xFF000000.toInt()
        editingColor = opaque
        hexText = batteryColorHex(opaque).removePrefix("#")
        val value = batteryColorRgb(opaque)
        redText = value.first.toString()
        greenText = value.second.toString()
        blueText = value.third.toString()
        // setCustomColor is the single copy-on-write path: an actual edit promotes this slot
        // to CUSTOM while simply browsing FOLLOW_SYSTEM keeps the runtime source untouched.
        onColorChange(opaque)
    }

    val hsv = editingColor?.let { Color(it).toHsv() }
    val currentValueText =
        when {
            resolved != null -> batteryColorHex(resolved)
            entry.source == BatterySchemeSource.CUSTOM ->
                stringResource(R.string.battery_color_custom_unset)
            else -> stringResource(R.string.battery_color_follow_inversion)
        }
    val inactiveTitleColor =
        if (visuallyInactive) {
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        } else {
            MiuixTheme.colorScheme.onBackgroundVariant
        }
    val inactiveFieldColors =
        if (visuallyInactive) {
            TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.disabledSecondaryVariant,
                labelColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                borderColor = MiuixTheme.colorScheme.disabledPrimary,
            )
        } else {
            TextFieldDefaults.textFieldColors()
        }
    val inactiveFieldTextStyle =
        MiuixTheme.textStyles.main.copy(
            color =
                if (visuallyInactive) {
                    MiuixTheme.colorScheme.disabledOnSecondaryVariant
                } else {
                    MiuixTheme.colorScheme.onBackground
                },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            OverlayDropdownPreference(
            items = sourceLabels,
            selectedIndex = selectedSourceIndex,
            title = stringResource(R.string.battery_color_source),
            summary = currentValueText,
            startAction = {
                if (resolved == null) {
                    BatteryColorMosaic(size = 24.dp)
                } else {
                    BatteryColorDot(
                        color = resolved,
                        size = 24.dp,
                    )
                }
            },
            onSelectedIndexChange = { index ->
                sourceOptions.getOrNull(index)?.let(onSourceChange)
            },
        )
        }

        BatterySheetSmallTitle(
            text = stringResource(R.string.battery_color_common),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            COMMON_BATTERY_COLORS.chunked(5).forEachIndexed { index, colors ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == 0) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    colors.forEach { color ->
                        BatteryCommonColorButton(
                            color = color,
                            selected =
                                entry.source == BatterySchemeSource.CUSTOM &&
                                    editingColor == color,
                            visuallyInactive = visuallyInactive,
                            onClick = { applyColor(color) },
                        )
                    }
                }
            }
        }

        BatterySheetSmallTitle(
            text = stringResource(R.string.battery_color_full_adjustment),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (hsv != null) {
                BatteryHsvAdjustmentRow(
                    title = stringResource(R.string.battery_color_hue),
                    valueText = "${hsv.h.roundToInt()}°",
                    visuallyInactive = visuallyInactive,
                ) {
                    HsvHueSlider(
                        currentHue = hsv.h,
                        onHueChanged = { fraction ->
                            applyColor(
                                Hsv(fraction * 360f, hsv.s, hsv.v).toColor().toArgb(),
                            )
                        },
                    )
                }
                BatteryHsvAdjustmentRow(
                    title = stringResource(R.string.battery_color_saturation),
                    valueText = "${hsv.s.roundToInt()}%",
                    visuallyInactive = visuallyInactive,
                ) {
                    HsvSaturationSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        onSaturationChanged = { saturation ->
                            applyColor(
                                Hsv(hsv.h, saturation * 100f, hsv.v).toColor().toArgb(),
                            )
                        },
                    )
                }
                BatteryHsvAdjustmentRow(
                    title = stringResource(R.string.battery_color_brightness),
                    valueText = "${hsv.v.roundToInt()}%",
                    visuallyInactive = visuallyInactive,
                ) {
                    HsvValueSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        currentValue = hsv.v / 100f,
                        onValueChanged = { value ->
                            applyColor(
                                Hsv(hsv.h, hsv.s, value * 100f).toColor().toArgb(),
                            )
                        },
                    )
                }
            } else {
                BasicComponent(
                    title = stringResource(R.string.battery_color_no_fixed_color),
                    summary = stringResource(R.string.battery_color_no_fixed_color_summary),
                    enabled = false,
                )
            }
        }

        BatterySheetSmallTitle(
            text = stringResource(R.string.battery_color_precise_input),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            TextField(
                value = hexText,
                onValueChange = { raw ->
                    val normalized =
                        raw.removePrefix("#")
                            .uppercase()
                            .filter { it.isDigit() || it in 'A'..'F' }
                    if (normalized.length <= 6) {
                        hexText = normalized
                        batteryColorFromHex(normalized)?.let(::applyColor)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.battery_color_hex),
                singleLine = true,
                colors = inactiveFieldColors,
                textStyle = inactiveFieldTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = redText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            redText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "R",
                    singleLine = true,
                    colors = inactiveFieldColors,
                    textStyle = inactiveFieldTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = greenText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            greenText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "G",
                    singleLine = true,
                    colors = inactiveFieldColors,
                    textStyle = inactiveFieldTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = blueText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            blueText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "B",
                    singleLine = true,
                    colors = inactiveFieldColors,
                    textStyle = inactiveFieldTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
    }
}

@Composable
private fun BatteryHsvAdjustmentRow(
    title: String,
    valueText: String,
    visuallyInactive: Boolean,
    slider: @Composable () -> Unit,
) {
    BasicComponent(
        title = title,
        enabled = !visuallyInactive,
        endActions = {
            Text(
                text = valueText,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color =
                    if (visuallyInactive) {
                        MiuixTheme.colorScheme.disabledOnSecondaryVariant
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantActions
                    },
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        bottomAction = slider,
    )
}

@Composable
internal fun BatteryCreateSchemeDialog(
    show: Boolean,
    nextId: Int?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val defaultName =
        nextId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }.orEmpty()
    var name by remember(show, nextId) { mutableStateOf(defaultName) }

    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_new),
        show = show && nextId != null,
        onDismissRequest = onDismiss,
    ) {
        Column {
            TextField(
                value = name,
                onValueChange = { name = limitBatteryCustomSchemeNameInput(it) },
                label = stringResource(R.string.battery_custom_scheme_name),
                singleLine = true,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.battery_custom_scheme_create),
                    modifier = Modifier.weight(1f),
                    enabled = name.trim().isNotEmpty(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        onCreate(name.trim())
                    },
                )
            }
        }
    }
}

@Composable
internal fun BatteryRenameSchemeDialog(
    scheme: CustomBatteryScheme?,
    onDismiss: () -> Unit,
    onRename: (Int, String) -> Unit,
) {
    val currentName = scheme?.let { customSchemeName(it) }.orEmpty()
    var name by remember(scheme?.id, currentName) { mutableStateOf(currentName) }
    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_rename),
        show = scheme != null,
        onDismissRequest = onDismiss,
    ) {
        if (scheme != null) {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = limitBatteryCustomSchemeNameInput(it) },
                    label = stringResource(R.string.battery_custom_scheme_name),
                    singleLine = true,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.confirm),
                        modifier = Modifier.weight(1f),
                        enabled =
                            name.trim().isNotEmpty() &&
                                name.trim() != currentName,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            onRename(scheme.id, name.trim())
                        },
                    )
                }
            }
        }
    }
}
