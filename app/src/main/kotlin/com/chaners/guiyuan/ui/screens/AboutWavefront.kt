package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// A soft pressure front. Rotation only nudges the center; it must not break the ring.
internal fun DrawScope.drawWave(
    color: Color,
    progress: Float,
    strength: Float,
    diameter: Float,
    phase: Float,
) {
    if (progress <= 0f || progress >= 1f) return

    val radius = waveRadius(progress, diameter)
    val feather = waveFeather(progress, diameter)
    val edge = radius + feather
    val front = center + waveDrift(progress, diameter, phase)
    val alpha = strength * sin((PI * progress).toFloat()).coerceAtLeast(0f)

    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            ((radius - feather) / edge) to Color.Transparent,
            ((radius - 0.55f * feather) / edge) to color.copy(alpha = alpha * 0.15f),
            (radius / edge) to color.copy(alpha = alpha * 0.52f),
            ((radius + 0.55f * feather) / edge) to color.copy(alpha = alpha * 0.19f),
            1f to Color.Transparent,
            center = front,
            radius = edge,
        ),
        radius = edge,
        center = front,
    )
}

// The slight drift remains circular and gives text the same wavefront crossing time.
private fun flowRadius(progress: Float, angle: Float, diameter: Float, phase: Float): Float {
    val radius = waveRadius(progress, diameter)
    val shift = diameter * 0.025f * progress
    return radius + shift * cos(angle - (phase - 0.55f * progress))
}

// Two-tone refraction stays inside the text strokes and follows the same displaced front.
internal fun Modifier.waveGlint(
    wave: Animatable<Float, AnimationVector1D>,
    distance: Float,
    diameter: Float,
    phase: Float,
    glint: Color,
    accent: Color,
): Modifier = drawWithContent {
    drawContent()
    if (wave.isRunning && distance > 0f) {
        val progress = wave.value
        drawGlint(progress, distance, diameter, phase, glint, accent, 0.95f)
        drawGlint((progress - 0.2f) / 0.8f, distance, diameter, phase, glint, accent, 0.50f)
    }
}

private fun DrawScope.drawGlint(
    progress: Float,
    distance: Float,
    diameter: Float,
    phase: Float,
    glint: Color,
    accent: Color,
    strength: Float,
) {
    if (progress <= 0f || progress >= 1f) return
    val radius = waveRadius(progress, diameter)
    val feather = waveFeather(progress, diameter)
    val edge = radius + feather
    // Use the ripple's moving circle, not an unrelated diagonal sweep.
    val drift = waveDrift(progress, diameter, phase)
    val front = Offset(size.width / 2f + drift.x, size.height / 2f - distance + drift.y)
    val intensity = strength * sin((PI * progress).toFloat()).coerceAtLeast(0f)
    drawRect(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            ((radius - feather) / edge) to Color.Transparent,
            ((radius - 0.55f * feather) / edge) to accent.copy(alpha = intensity * 0.22f),
            (radius / edge) to glint.copy(alpha = intensity),
            ((radius + 0.55f * feather) / edge) to accent.copy(alpha = intensity * 0.32f),
            1f to Color.Transparent,
            center = front,
            radius = edge,
        ),
        blendMode = BlendMode.SrcAtop,
    )
}

internal fun waveLift(progress: Float, distance: Float, diameter: Float, phase: Float): Float {
    if (distance <= 0f || diameter <= 0f) return 0f
    return maxOf(
        wavefrontLift(progress, distance, diameter, phase),
        wavefrontLift((progress - 0.2f) / 0.8f, distance, diameter, phase) * 0.6f,
    )
}

private fun wavefrontLift(progress: Float, distance: Float, diameter: Float, phase: Float): Float {
    if (progress <= 0f || progress >= 1f) return 0f
    val radius = flowRadius(progress, (PI / 2).toFloat(), diameter, phase)
    val feather = waveFeather(progress, diameter)
    val crossing = (1f - abs(distance - radius) / feather).coerceIn(0f, 1f)
    return crossing * sin((PI * progress).toFloat())
}

// Shared wavefront dimensions and drift keep the mark and text highlights aligned.
private fun waveRadius(progress: Float, diameter: Float): Float = diameter * (0.55f + 1.15f * progress)

private fun waveFeather(progress: Float, diameter: Float): Float = diameter * (0.20f + 0.07f * progress)

private fun waveDrift(progress: Float, diameter: Float, phase: Float): Offset {
    val turn = phase - 0.55f * progress
    val shift = diameter * 0.025f * progress
    return Offset(cos(turn) * shift, sin(turn) * shift)
}
