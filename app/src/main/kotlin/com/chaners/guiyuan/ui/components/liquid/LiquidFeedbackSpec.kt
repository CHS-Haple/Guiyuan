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

/** The portrait Liquid lens deformation law, rotated only for vertical rails. */
internal data class LiquidLensScales(val x: Float, val y: Float)

internal fun liquidVelocityScales(
    baseX: Float,
    baseY: Float,
    signedVelocity: Float,
    vertical: Boolean,
): LiquidLensScales {
    // Exactly the coefficients used by LiquidNavBar's original layerBlock.
    val velocity = signedVelocity / 10f
    val along = (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
    val across = (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
    return if (vertical) {
        LiquidLensScales(baseX * (1f - across), baseY / (1f - along))
    } else {
        LiquidLensScales(baseX / (1f - along), baseY * (1f - across))
    }
}
