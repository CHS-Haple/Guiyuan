package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

// Adapted from AndroidLiquidGlass catalog's DampedDragAnimation (Apache-2.0).
internal class LiquidDragMotion(
    private val scope: CoroutineScope,
    initialValue: Float,
    private val range: ClosedRange<Float>,
    visibilityThreshold: Float,
    initialScale: Float,
    private val pressedScale: Float,
    private val onStopped: LiquidDragMotion.() -> Unit,
    private val onDrag: LiquidDragMotion.(IntSize, Offset) -> Unit,
) {
    private val valueSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocitySpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressSpec = spring(1f, 1000f, 0.001f)
    private val scaleXSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnim = Animatable(initialValue, visibilityThreshold)
    private val velocityAnim = Animatable(0f, 5f)
    private val pressAnim = Animatable(0f, 0.001f)
    private val scaleXAnim = Animatable(initialScale, 0.001f)
    private val scaleYAnim = Animatable(initialScale, 0.001f)
    private val mutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnim.value
    val targetValue: Float get() = valueAnim.targetValue
    val pressProgress: Float get() = pressAnim.value
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value
    val velocity: Float get() = velocityAnim.value

    val modifier: Modifier =
        Modifier.pointerInput(Unit) {
            inspectLiquidDrag(
                onStart = {
                    press()
                },
                onEnd = {
                    onStopped()
                    release()
                },
                onCancel = {
                    onStopped()
                    release()
                },
            ) { _, dragAmount ->
                onDrag(size, dragAmount)
            }
        }

    fun press() {
        velocityTracker.resetTracking()
        scope.launch {
            launch { pressAnim.animateTo(1f, pressSpec) }
            launch { scaleXAnim.animateTo(pressedScale, scaleXSpec) }
            launch { scaleYAnim.animateTo(pressedScale, scaleYSpec) }
        }
    }

    fun release() {
        scope.launch {
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = (range.endInclusive - range.start) * 0.025f
                snapshotFlow { valueAnim.value }
                    .filter { abs(it - valueAnim.targetValue) < threshold }
                    .first()
            }
            launch { pressAnim.animateTo(0f, pressSpec) }
            launch { scaleXAnim.animateTo(1f, scaleXSpec) }
            launch { scaleYAnim.animateTo(1f, scaleYSpec) }
        }
    }

    fun updateValue(value: Float) {
        val target = value.coerceIn(range)
        scope.launch {
            valueAnim.animateTo(target, valueSpec) {
                updateVelocity()
            }
        }
    }

    fun animateToValue(value: Float) {
        scope.launch {
            mutex.mutate {
                press()
                val target = value.coerceIn(range)
                launch { valueAnim.animateTo(target, valueSpec) }
                if (velocity != 0f) {
                    launch { velocityAnim.animateTo(0f, velocitySpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            android.os.SystemClock.uptimeMillis(),
            Offset(value, 0f),
        )
        val width = range.endInclusive - range.start
        val target = if (width == 0f) 0f else velocityTracker.calculateVelocity().x / width
        scope.launch { velocityAnim.animateTo(target, velocitySpec) }
    }
}
