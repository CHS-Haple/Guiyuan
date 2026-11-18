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

/** Same X deformation as the portrait lens renderer, including its velocity stretch. */
internal fun liquidRenderedLensScaleX(scaleX: Float, velocity: Float): Float =
    scaleX / (1f - (velocity / 10f * 0.75f).coerceIn(-0.2f, 0.2f))

/**
 * The portrait lens and panel are siblings. The action follows whichever
 * visible logical-end edge reaches farther, without adding their growth twice.
 */
internal fun liquidPortraitActionReachPx(
    panelReachPx: Float,
    lensWidthPx: Float,
    lensScaleX: Float,
    lensDistanceFromEndPx: Float,
    endInsetPx: Float,
): Float = maxOf(
    0f,
    panelReachPx,
    (lensScaleX - 1f) * lensWidthPx / 2f - lensDistanceFromEndPx - endInsetPx,
)

/**
 * The Rail lens lives inside the scaled panel. Reserve its visible end under
 * the same press transformation, including the original X-spring overtravel.
 */
internal fun liquidRailActionReachPx(
    panelWidthPx: Float,
    panelHeightPx: Float,
    panelGrowthPx: Float,
    lensGrowthPx: Float,
    endInsetPx: Float,
): Float {
    val panelScale = liquidPanelPressScale(panelWidthPx, panelHeightPx, panelGrowthPx, 1f)
    val lensOverhang = lensGrowthPx * (1f + LiquidFeedbackSpec.LensXSpringOvershootAllowance) / 2f
    return (panelWidthPx * (panelScale - 1f) / 2f +
        panelScale * (lensOverhang - endInsetPx)).coerceAtLeast(0f)
}

/** Portrait-width panel press growth, kept bounded by the long side of a wide rail. */
internal fun liquidPanelPressScale(
    widthPx: Float,
    heightPx: Float,
    growthPx: Float,
    progress: Float,
): Float = 1f + growthPx * progress.coerceIn(0f, 1f) /
    maxOf(widthPx, heightPx).coerceAtLeast(1f)
