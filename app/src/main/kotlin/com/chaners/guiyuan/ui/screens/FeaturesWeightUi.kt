package com.chaners.guiyuan.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.NativeFontAxis
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.preference.SliderPreference

internal data class FeatureWeightUi(
    val row: NativeWeightRow,
    val enabled: Boolean,
    val verifiedRange: IntRange?,
    val valueRange: ClosedFloatingPointRange<Float>,
    val points: List<Float>?,
    val rangeText: String?,
    val pending: Boolean,
)

@Composable
internal fun featureWeightUi(axis: NativeFontAxis.State, fallbackRange: IntRange, row: NativeWeightRow, enabled: Boolean): FeatureWeightUi {
    val verified = axis.verifiedWeightRange()
    val valueRange = (verified?.first ?: fallbackRange.first).toFloat()..
        (verified?.last ?: fallbackRange.last).toFloat()
    val points = remember(verified) { verified?.let(::weightSnapPoints) }
    val rangeText = verified?.let { range ->
        stringResource(R.string.font_weight_range, range.first, range.last)
    }
    return FeatureWeightUi(
        row = row,
        enabled = enabled,
        verifiedRange = verified,
        valueRange = valueRange,
        points = points,
        rangeText = rangeText,
        pending = axis == NativeFontAxis.State.Pending,
    )
}

internal fun NativeFontAxis.State.verifiedWeightRange(): IntRange? =
    (this as? NativeFontAxis.State.Variable)?.axis?.let { axis ->
        ceil(axis.min.toDouble()).toInt()..floor(axis.max.toDouble()).toInt()
    }?.takeIf { it.last > it.first }

internal fun weightSnapPoints(range: IntRange): List<Float> {
    if (range.isEmpty()) return emptyList()
    val first = ((range.first + 49) / 50) * 50
    return if (first > range.last) emptyList() else (first..range.last step 50).map(Int::toFloat)
}

// MIUIX's magnet threshold is normalized; shrink it when 50-point anchors get dense.
internal fun weightSnapThreshold(range: IntRange): Float =
    minOf(0.02f, 12.5f / (range.last - range.first).coerceAtLeast(1))

internal enum class NativeWeightRow(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    BATTERY(R.string.battery_top_text_weight, R.drawable.ic_material_symbol_battery_5_bar),
    MOBILE_TYPE(R.string.mobile_type_weight, R.drawable.ic_material_symbol_5g),
}

@Composable
internal fun NativeWeightSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    weight: FeatureWeightUi,
    dialogOpen: Boolean,
    onOpenDialog: () -> Unit,
) {
    SliderPreference(
        value = value.toFloat().coerceIn(
            weight.valueRange.start, weight.valueRange.endInclusive,
        ),
        onValueChange = { onValueChange(it.roundToInt()) },
        title = stringResource(weight.row.titleRes),
        summary = weight.rangeText ?: stringResource(
            if (weight.pending) R.string.font_weight_pending else R.string.font_weight_unverified,
        ),
        startAction = {
            SemanticLeadingIcon(weight.row.iconRes, weight.enabled, R.drawable.ic_material_symbol_line_weight)
        },
        valueText = stringResource(
            R.string.integer_value,
            value.coerceIn(
                weight.valueRange.start.roundToInt(),
                weight.valueRange.endInclusive.roundToInt(),
            ),
        ),
        valueRange = weight.valueRange,
        steps = 0,
        keyPoints = weight.points,
        magnetThreshold = weight.verifiedRange?.let(::weightSnapThreshold) ?: 0f,
        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        holdDownState = dialogOpen,
        enabled = weight.enabled && weight.verifiedRange != null,
        onClick = onOpenDialog,
    )
}
