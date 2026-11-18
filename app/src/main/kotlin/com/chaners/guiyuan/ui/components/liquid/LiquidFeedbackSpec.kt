package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.ui.unit.dp

/** Portrait and wide rails share the same physical Liquid press targets. */
internal object LiquidFeedbackSpec {
    const val PressedLensScale = 78f / 56f
    const val PressedItemScale = 1.2f
    val PressedPanelGrowth = 16.dp
    // This target growth is 56dp * (78/56 - 1) in the original Liquid lens.
    val PressedLensGrowth = 22.dp
    // The 0.6-damped X spring's first overshoot is about 9.5% of its travel.
    const val LensXSpringOvershootAllowance = 0.10f
}

/** Plan the maximum expected press clearance once; never chase the rendered lens spring. */
internal fun liquidActionPressReachPx(
    panelGrowthPx: Float,
    lensGrowthPx: Float,
    endInsetPx: Float,
): Float = (panelGrowthPx / 2f +
    lensGrowthPx * (1f + LiquidFeedbackSpec.LensXSpringOvershootAllowance) / 2f -
    endInsetPx).coerceAtLeast(0f)

/** Portrait-width panel press growth, kept bounded by the long side of a wide rail. */
internal fun liquidPanelPressScale(
    widthPx: Float,
    heightPx: Float,
    growthPx: Float,
    progress: Float,
): Float = 1f + growthPx * progress.coerceIn(0f, 1f) /
    maxOf(widthPx, heightPx).coerceAtLeast(1f)
