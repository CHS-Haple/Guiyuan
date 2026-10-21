// Adapted from MIUIX PressFeedback.kt (Apache-2.0).
// Diagnostic candidate: keep the hit pivot until its spring rotation returns to rest.
package com.chaners.guiyuan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.chaners.guiyuan.system.HomePressTrace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction

@Stable
internal data class RuntimeCardTilt(
    val tiltAmount: Float = 8f,
    val animationSpec: AnimationSpec<Float> = spring(0.6f, 400f),
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        val node = TiltNode(interactionSource, tiltAmount, animationSpec)
        HomePressTrace.record(
            "tilt.factory.create factory=${System.identityHashCode(this)} " +
                "source=${System.identityHashCode(interactionSource)} " +
                "node=${System.identityHashCode(node)}",
        )
        return node
    }

    private class TiltNode(
        val interactionSource: InteractionSource,
        val tiltAmount: Float,
        val animationSpec: AnimationSpec<Float>,
    ) : Modifier.Node(), LayoutModifierNode, PointerInputModifierNode {
        private var transformOrigin = TransformOrigin.Center
        private var targetX = 0f
        private var targetY = 0f
        private val animatedTiltX = Animatable(0f)
        private val animatedTiltY = Animatable(0f)
        private var isPressed = false
        private var isHoldDown = false
        private val nodeId = System.identityHashCode(this)

        private fun updateState() {
            val home = !isPressed && !isHoldDown
            val x = if (home) 0f else targetX
            val y = if (home) 0f else targetY
            HomePressTrace.record("tilt.target node=$nodeId home=$home x=${animatedTiltX.value} y=${animatedTiltY.value}")
            coroutineScope.launch {
                try {
                    animatedTiltX.animateTo(x, animationSpec)
                    if (home) HomePressTrace.record("tilt.x.rest node=$nodeId x=${animatedTiltX.value}")
                } catch (e: CancellationException) {
                    if (home) HomePressTrace.record("tilt.x.interrupted node=$nodeId x=${animatedTiltX.value}")
                    throw e
                }
            }
            coroutineScope.launch {
                try {
                    animatedTiltY.animateTo(y, animationSpec)
                    if (home) HomePressTrace.record("tilt.y.rest node=$nodeId y=${animatedTiltY.value}")
                } catch (e: CancellationException) {
                    if (home) HomePressTrace.record("tilt.y.interrupted node=$nodeId y=${animatedTiltY.value}")
                    throw e
                }
            }
            if (home) {
                coroutineScope.launch {
                    delay(80)
                    HomePressTrace.record("tilt.after80 node=$nodeId x=${animatedTiltX.value} y=${animatedTiltY.value}")
                }
            }
        }

        override fun onAttach() {
            HomePressTrace.record("tilt.attach node=$nodeId")
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction: Interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                        is HoldDownInteraction.HoldDown -> isHoldDown = true
                        is HoldDownInteraction.Release -> isHoldDown = false
                        else -> return@collect
                    }
                    updateState()
                }
            }
        }

        override fun onDetach() {
            HomePressTrace.record("tilt.detach node=$nodeId x=${animatedTiltX.value} y=${animatedTiltY.value}")
            if (kotlin.math.abs(animatedTiltX.value) > 0.5f) {
                val path = Throwable().stackTrace.drop(1).take(14)
                    .joinToString(">") { "${it.className.substringAfterLast('.') }.${it.methodName}" }
                HomePressTrace.record("tilt.detach.path node=$nodeId path=$path")
            }
        }

        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            if (pass != PointerEventPass.Main) return
            if (pointerEvent.type == PointerEventType.Press) {
                val offset = pointerEvent.changes.first().position
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (offset.x < bounds.width / 2f) 1f else 0f,
                    pivotFractionY = if (offset.y < bounds.height / 2f) 1f else 0f,
                )
                targetX = if (offset.y < bounds.height / 2f) tiltAmount else -tiltAmount
                targetY = if (offset.x < bounds.width / 2f) -tiltAmount else tiltAmount
                HomePressTrace.record(
                    "tilt.pointerDown side=${transformOrigin.pivotFractionX},${transformOrigin.pivotFractionY}",
                )
            }
        }

        override fun onCancelPointerInput() {
            HomePressTrace.record(
                "tilt.pointerCancel x=${animatedTiltX.value} y=${animatedTiltY.value} " +
                    "pivot=${transformOrigin.pivotFractionX},${transformOrigin.pivotFractionY}",
            )
            // MIUIX resets transformOrigin here. This test retains it while AnimateTo(0)
            // finishes; changing the pivot with a non-zero angle moves the visible card.
            targetX = 0f
            targetY = 0f
        }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    rotationX = animatedTiltX.value
                    rotationY = animatedTiltY.value
                    cameraDistance = 12 * density
                    this.transformOrigin = this@TiltNode.transformOrigin
                }
            }
        }
    }
}
