package com.chaners.guiyuan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.NativeFontAxis
import kotlin.math.ceil
import kotlin.math.floor

internal data class FeatureWeightUi(
    val verifiedRange: IntRange?,
    val valueRange: ClosedFloatingPointRange<Float>,
    val points: List<Float>?,
    val rangeText: String?,
    val pending: Boolean,
)

@Composable
internal fun featureWeightUi(axis: NativeFontAxis.State, fallbackRange: IntRange): FeatureWeightUi {
    val verified = axis.verifiedWeightRange()
    val valueRange = (verified?.first ?: fallbackRange.first).toFloat()..
        (verified?.last ?: fallbackRange.last).toFloat()
    val points = remember(verified) { verified?.let(::weightSnapPoints) }
    val rangeText = verified?.let { range ->
        stringResource(R.string.font_weight_range, range.first, range.last)
    }
    return FeatureWeightUi(
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
