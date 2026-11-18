package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.ui.unit.dp

/** Portrait and wide rails share the same physical Liquid press targets. */
internal object LiquidFeedbackSpec {
    const val PressedLensScale = 78f / 56f
    const val PressedItemScale = 1.2f
    val PressedPanelGrowth = 16.dp
    // The Rail lens's total X/Y press growth, shared by optics and hit-free action spacing.
    val PressedLensGrowth = 22.dp
}

/** Portrait-width panel press growth, kept bounded by the long side of a wide rail. */
internal fun liquidPanelPressScale(
    widthPx: Float,
    heightPx: Float,
    growthPx: Float,
    progress: Float,
): Float = 1f + growthPx * progress.coerceIn(0f, 1f) /
    maxOf(widthPx, heightPx).coerceAtLeast(1f)
