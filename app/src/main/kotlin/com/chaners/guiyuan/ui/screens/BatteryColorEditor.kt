package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import com.chaners.guiyuan.settings.limitCustomSchemeName
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
    custom: BatteryCustomColorScheme,
    slot: BatteryColorSlot,
    onSourceChange: (BatteryColorSchemeSource) -> Unit,
    onColorChange: (Int) -> Unit,
) {
    val entry = custom.entries.entryFor(slot)
    val resolved = batterySchemeEntryColor(entry, slot)
    val sourceOptions = BatteryColorSchemeSource.entries
    val sourceLabels =
        listOf(
            batterySourceLabel(BatteryColorSchemeSource.HYPEROS),
            batterySourceLabel(BatteryColorSchemeSource.IOS),
            batterySourceLabel(BatteryColorSchemeSource.LOW_SATURATION),
            batterySourceLabel(BatteryColorSchemeSource.FOLLOW_SYSTEM),
            batterySourceLabel(BatteryColorSchemeSource.CUSTOM),
        )
    val selectedSourceIndex = sourceOptions.indexOf(entry.source).coerceAtLeast(0)
    val seed = batteryColorEditorSeed(entry, slot)
    val visuallyInactive = entry.source == BatteryColorSchemeSource.FOLLOW_SYSTEM

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

    fun showColor(color: Int) {
        val opaque = color or 0xFF000000.toInt()
        editingColor = opaque
        hexText = batteryColorHex(opaque).removePrefix("#")
        val value = batteryColorRgb(opaque)
        redText = value.first.toString()
        greenText = value.second.toString()
        blueText = value.third.toString()
    }

    fun applyColor(color: Int) {
        showColor(color)
        // The repository remains the single writer; choosing a color switches the slot to CUSTOM.
        onColorChange(color or 0xFF000000.toInt())
    }
    var showPrecise by remember(custom.id, slot) { mutableStateOf(false) }

    val hsv = editingColor?.let { Color(it).toHsv() }
    val currentValueText =
        when {
            resolved != null -> batteryColorHex(resolved)
            entry.source == BatteryColorSchemeSource.CUSTOM ->
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
            text = stringResource(R.string.battery_color_visual_picker),
            textColor = inactiveTitleColor,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(16.dp),
        ) {
            BatteryColorGrid(
                color = editingColor?.let(::Color) ?: Color(0xFF3482FF),
                onPreview = { showColor(it.toArgb()) },
                onSelect = { applyColor(it.toArgb()) },
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
                                entry.source == BatteryColorSchemeSource.CUSTOM &&
                                    editingColor == color,
                            visuallyInactive = visuallyInactive,
                            onClick = { applyColor(color) },
                        )
                    }
                }
            }
        }

        TextButton(
            text =
                stringResource(
                    if (showPrecise) R.string.battery_color_hide_precision
                    else R.string.battery_color_show_precision
                ),
            onClick = { showPrecise = !showPrecise },
        )
        if (showPrecise) {
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
}


// The battery renderer stores opaque colors. The pinned MIUIX ColorPalette currently
// always shows an alpha slider, so this small RGB-only grid uses MIUIX cards around it.
@Composable
private fun BatteryColorGrid(
    color: Color,
    onPreview: (Color) -> Unit,
    onSelect: (Color) -> Unit,
) {
    val rows = 7
    val columns = 13 // 12 hue columns and one grayscale column
    val palette = remember {
        List(rows) { row ->
            List(columns) { column ->
                if (column == 12) {
                    Color.hsv(0f, 0f, (1f - row * 0.14f).coerceAtLeast(0.16f))
                } else {
                    val saturation = (0.25f + row.coerceAtMost(4) * 0.1875f).coerceAtMost(1f)
                    val value = when (row) {
                        5 -> 0.73f
                        6 -> 0.48f
                        else -> 1f
                    }
                    Color.hsv(column * 30f, saturation, value)
                }
            }
        }
    }
    val preview by rememberUpdatedState(onPreview)
    val select by rememberUpdatedState(onSelect)
    val selected = remember(color) {
        var closest = 0 to 0
        var distance = Float.MAX_VALUE
        palette.forEachIndexed { row, cells ->
            cells.forEachIndexed { column, sample ->
                val d =
                    (sample.red - color.red) * (sample.red - color.red) +
                    (sample.green - color.green) * (sample.green - color.green) +
                    (sample.blue - color.blue) * (sample.blue - color.blue)
                if (d < distance) {
                    distance = d
                    closest = row to column
                }
            }
        }
        closest
    }
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .pointerInput(palette) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastCell: Pair<Int, Int>? = null
                        fun preview(position: Offset) {
                            val column = (position.x / size.width * columns).toInt().coerceIn(0, columns - 1)
                            val row = (position.y / size.height * rows).toInt().coerceIn(0, rows - 1)
                            if (lastCell != (row to column)) {
                                lastCell = row to column
                                preview(palette[row][column])
                            }
                        }
                        preview(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            preview(change.position)
                            change.consume()
                        }
                        lastCell?.let { (row, column) -> select(palette[row][column]) }
                    }
                },
    ) {
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows
        val gap = 2.dp.toPx()
        palette.forEachIndexed { row, cells ->
            cells.forEachIndexed { column, sample ->
                drawRoundRect(
                    color = sample,
                    topLeft = Offset(column * cellWidth + gap, row * cellHeight + gap),
                    size = Size(cellWidth - gap * 2f, cellHeight - gap * 2f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            }
        }
        val center =
            Offset((selected.second + 0.5f) * cellWidth, (selected.first + 0.5f) * cellHeight)
        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 12.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
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
                onValueChange = { name = limitCustomSchemeName(it) },
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
    scheme: BatteryCustomColorScheme?,
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
                    onValueChange = { name = limitCustomSchemeName(it) },
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
