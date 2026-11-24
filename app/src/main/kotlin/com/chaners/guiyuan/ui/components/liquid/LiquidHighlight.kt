// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.material.RuntimeShader
import top.yukonga.miuix.kmp.material.asBrush
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported

// Adapted from the accepted MIUIX Liquid example's InteractiveHighlight.
// Use its material shader API and the same radial-gradient fallback, without a second shader.
internal class LiquidHighlight(
    private val scope: CoroutineScope,
    private val position: (Size, Offset) -> Offset = { _, offset -> offset },
) {
    private val pressSpec = spring(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)
    private val pressAnim = Animatable(0f, 0.001f)
    private val positionAnim = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var startPosition = Offset.Zero

    private val spotShader: RuntimeShader? =
        if (isRuntimeShaderSupported()) RuntimeShader(SPOT_SHADER) else null

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressAnim.value
        if (progress > 0f) {
            drawRect(
                color = Color.White.copy(alpha = 0.06f * progress),
                blendMode = BlendMode.Plus,
            )
            val rawPosition = position(size, positionAnim.value)
            val center = Offset(
                rawPosition.x.coerceIn(0f, size.width),
                rawPosition.y.coerceIn(0f, size.height),
            )
            val radius = (size.minDimension * 1.2f).coerceAtLeast(1f)
            val spotColor = Color.White.copy(alpha = 0.12f * progress)
            val brush = if (spotShader != null) {
                spotShader.setColorUniform("color", spotColor)
                spotShader.setFloatUniform("radius", radius)
                spotShader.setFloatUniform("position", center.x, center.y)
                spotShader.asBrush()
            } else {
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to spotColor,
                        0.5f to spotColor,
                        1.0f to Color.White.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius,
                )
            }
            drawRect(brush = brush, blendMode = BlendMode.Plus)
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(scope) {
        inspectLiquidDrag(
            onStart = { down ->
                startPosition = down.position
                scope.launch {
                    launch { pressAnim.animateTo(1f, pressSpec) }
                    launch { positionAnim.snapTo(startPosition) }
                }
            },
            onEnd = { release() },
            onCancel = { release() },
        ) { change, _ ->
            scope.launch { positionAnim.snapTo(change.position) }
        }
    }

    private fun release() {
        scope.launch {
            launch { pressAnim.animateTo(0f, pressSpec) }
            launch { positionAnim.animateTo(startPosition, positionSpec) }
        }
    }
}

private const val SPOT_SHADER = """
    layout(color) uniform half4 color;
    uniform float radius;
    uniform float2 position;

    half4 main(float2 coord) {
        float dist = distance(coord, position);
        float intensity = smoothstep(radius, radius * 0.5, dist);
        return color * intensity;
    }"""
