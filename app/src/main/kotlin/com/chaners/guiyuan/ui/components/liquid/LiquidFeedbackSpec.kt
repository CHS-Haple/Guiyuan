package com.chaners.guiyuan.ui.components.liquid

/** Portrait and wide rails share the same physical Liquid press targets. */
internal object LiquidFeedbackSpec {
    const val PressedLensScale = 78f / 56f
    const val PressedItemScale = 1.2f
}

/** Portrait-width panel press growth, kept bounded by the long side of a wide rail. */
internal fun liquidPanelPressScale(
    widthPx: Float,
    heightPx: Float,
    growthPx: Float,
    progress: Float,
): Float = 1f + growthPx * progress.coerceIn(0f, 1f) /
    maxOf(widthPx, heightPx).coerceAtLeast(1f)

/** Scale response to the signed spring velocity, along the active navigation axis. */
internal data class LiquidLensScales(val x: Float, val y: Float)

internal fun liquidLensScales(
    widthPx: Float,
    heightPx: Float,
    growthPx: Float,
    pressProgress: Float,
    signedVelocity: Float,
    vertical: Boolean,
): LiquidLensScales {
    val growth = growthPx * pressProgress.coerceIn(0f, 1f)
    val baseX = 1f + growth / widthPx.coerceAtLeast(1f)
    val baseY = 1f + growth / heightPx.coerceAtLeast(1f)
    // Portrait's motion law: stretch with travel and compress across it.
    // The expanded rail rotates these same axes without changing its layout.
    val along = (signedVelocity * 0.75f).coerceIn(-0.2f, 0.2f)
    val across = (signedVelocity * 0.25f).coerceIn(-0.2f, 0.2f)
    return if (vertical) {
        LiquidLensScales(baseX * (1f - across), baseY / (1f - along))
    } else {
        LiquidLensScales(baseX / (1f - along), baseY * (1f - across))
    }
}
