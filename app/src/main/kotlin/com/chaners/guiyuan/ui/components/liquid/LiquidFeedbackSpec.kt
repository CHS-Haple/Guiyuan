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

/** Symmetric press growth with directionally mirrored leading-edge extension. */
internal data class LiquidLensTransform(
    val x: Float,
    val y: Float,
    val translationX: Float,
    val translationY: Float,
)

internal fun liquidDirectionalLensTransform(
    widthPx: Float,
    heightPx: Float,
    growthPx: Float,
    pressProgress: Float,
    signedMotion: Float,
    vertical: Boolean,
): LiquidLensTransform {
    val width = widthPx.coerceAtLeast(1f)
    val height = heightPx.coerceAtLeast(1f)
    val growth = growthPx.coerceAtLeast(0f) * pressProgress.coerceIn(0f, 1f)
    val baseX = 1f + growth / width
    val baseY = 1f + growth / height
    // Both physical directions extend equally. Only the leading edge changes.
    val motion = kotlin.math.abs(signedMotion.coerceIn(-0.5f, 0.5f))
    val stretch = 1f + 0.65f * motion
    val squeeze = 1f - 0.18f * motion
    val direction = when {
        signedMotion > 0f -> 1f
        signedMotion < 0f -> -1f
        else -> 0f
    }
    // With centered transforms, half the added length offsets the layer so
    // the rear edge stays anchored in the pressed lens's original position.
    return if (vertical) {
        LiquidLensTransform(
            x = baseX * squeeze,
            y = baseY * stretch,
            translationX = 0f,
            translationY = direction * height * baseY * (stretch - 1f) / 2f,
        )
    } else {
        LiquidLensTransform(
            x = baseX * stretch,
            y = baseY * squeeze,
            translationX = direction * width * baseX * (stretch - 1f) / 2f,
            translationY = 0f,
        )
    }
}
