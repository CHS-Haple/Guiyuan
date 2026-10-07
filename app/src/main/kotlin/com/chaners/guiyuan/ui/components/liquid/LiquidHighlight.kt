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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Adapted from AndroidLiquidGlass catalog's InteractiveHighlight (Apache-2.0).
internal class LiquidHighlight(
    private val scope: CoroutineScope,
    private val position: (Size, Offset) -> Offset = { _, offset -> offset },
) {
    private val pressSpec = spring(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)
    private val pressAnim = Animatable(0f, 0.001f)
    private val positionAnim =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero

    private val shader =
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}""",
            )
        } else {
            null
        }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressAnim.value
            if (progress > 0f) {
                val runtimeShader = shader
                if (runtimeShader != null) {
                    drawRect(
                        Color.White.copy(alpha = 0.08f * progress),
                        blendMode = BlendMode.Plus,
                    )
                    runtimeShader.apply {
                        val highlightPos = position(size, positionAnim.value)
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(alpha = 0.15f * progress))
                        setFloatUniform("radius", size.minDimension * 1.5f)
                        setFloatUniform(
                            "position",
                            highlightPos.x.fastCoerceIn(0f, size.width),
                            highlightPos.y.fastCoerceIn(0f, size.height),
                        )
                    }
                    drawRect(
                        ShaderBrush(runtimeShader.asComposeShader()),
                        blendMode = BlendMode.Plus,
                    )
                } else {
                    drawRect(
                        Color.White.copy(alpha = 0.25f * progress),
                        blendMode = BlendMode.Plus,
                    )
                }
            }
            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(scope) {
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
