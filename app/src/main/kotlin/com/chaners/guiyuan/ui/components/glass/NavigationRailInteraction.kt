// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.components.glass

import top.yukonga.miuix.kmp.glass.GlassMotion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure touch-axis classification shared by both rail orientations. */
internal enum class RailGestureDecision { Pending, Drag, Cancel }

internal fun railGestureDecision(
    movementX: Float,
    movementY: Float,
    touchSlop: Float,
    horizontal: Boolean,
): RailGestureDecision {
    if (abs(movementY) > touchSlop && abs(movementY) > abs(movementX)) {
        return RailGestureDecision.Cancel
    }
    if (abs(movementX) > touchSlop) {
        return if (horizontal) RailGestureDecision.Drag else RailGestureDecision.Cancel
    }
    return RailGestureDecision.Pending
}

/** Liquid keeps the native pointer owner, but follows X collapsed and Y expanded. */
internal fun liquidRailGestureDecision(
    movementX: Float,
    movementY: Float,
    touchSlop: Float,
    horizontal: Boolean,
): RailGestureDecision {
    if (horizontal) return railGestureDecision(movementX, movementY, touchSlop, true)
    if (abs(movementX) > touchSlop && abs(movementX) > abs(movementY)) return RailGestureDecision.Cancel
    if (abs(movementY) > touchSlop && abs(movementY) > abs(movementX)) return RailGestureDecision.Drag
    return RailGestureDecision.Pending
}

/** The indicator travels continuously between the existing measured rows. */
internal fun liquidRailPreviewAt(y: Float, centres: List<Float>): Float {
    if (centres.isEmpty() || y <= centres.first()) return 0f
    for (i in 0 until centres.lastIndex) {
        if (y <= centres[i + 1]) {
            val distance = (centres[i + 1] - centres[i]).coerceAtLeast(1f)
            return i + ((y - centres[i]) / distance).coerceIn(0f, 1f)
        }
    }
    return centres.lastIndex.toFloat()
}

/** Pointer deltas must map through the active slot's logical width, including RTL. */
internal fun railLogicalDelta(
    physicalDeltaX: Float,
    slotScale: Float,
    direction: LayoutDirection,
): Float = (if (direction == LayoutDirection.Rtl) -physicalDeltaX else physicalDeltaX) * slotScale

/** Measured in the scrolling content's logical coordinates; updated only by layout. */
internal class NavigationRailSlot {
    var left = 0f
    var top = 0f
    var width = 0f
    var height = 0f
}

/** One press and one pair of indicator springs for the entire panel/capsule lifetime. */
internal class NavigationRailInteraction(
    widths: List<Float>,
    val contentStart: Float,
    val endPadding: Float,
    selectedIndex: Int,
    private val scope: CoroutineScope,
) {
    var widths: List<Float> = widths
        private set
    val slots = List(widths.size) { NavigationRailSlot() }
    val rowInteractions = List(widths.size) { MutableInteractionSource() }
    private val starts = FloatArray(widths.size + 1).also { starts ->
        starts[0] = contentStart
        for (index in widths.indices) starts[index + 1] = starts[index] + widths[index]
    }
    val width: Float get() = starts.last() + endPadding
    val left = Animatable(starts[selectedIndex])
    val right = Animatable(starts[selectedIndex + 1])

    fun updateWidths(updated: List<Float>) {
        require(updated.size == slots.size)
        if (this.widths === updated) return
        this.widths = updated
        for (index in updated.indices) starts[index + 1] = starts[index] + updated[index]
    }

    var pressedIndex by mutableIntStateOf(-1)
        private set
    var releaseGeneration by mutableIntStateOf(0)
        private set
    var previewRow by mutableFloatStateOf(selectedIndex.toFloat())
        private set

    fun indexAt(x: Float, y: Float): Int {
        var nearest = -1
        var distance = Float.POSITIVE_INFINITY
        for (index in slots.indices) {
            val slot = slots[index]
            if (x < slot.left || x >= slot.left + slot.width || y < slot.top || y >= slot.top + slot.height) continue
            // Flying rows can cross. Prefer the visible slot centre, never the hidden icon column.
            val dx = (x - slot.left - slot.width / 2f) / slot.width.coerceAtLeast(1f)
            val dy = (y - slot.top - slot.height / 2f) / slot.height.coerceAtLeast(1f)
            val candidate = dx * dx + dy * dy
            if (candidate < distance) {
                nearest = index
                distance = candidate
            }
        }
        return nearest
    }

    fun positionAt(index: Int, x: Float): Float {
        val slot = slots[index]
        return starts[index] + ((x - slot.left) / slot.width.coerceAtLeast(1f)).coerceIn(0f, 1f) * widths[index]
    }

    fun indexAt(position: Float): Int = navigationSlotAt(position - contentStart, widths).toInt().coerceIn(widths.indices)

    fun press(index: Int) {
        pressedIndex = index
        previewRow = index.toFloat()
        settle(index)
    }

    fun release() {
        pressedIndex = -1
        releaseGeneration++
    }

    /** Smooth Y-axis destination preview inside the existing rail pointer state. */
    fun dragExpanded(y: Float) {
        if (slots.any { it.height <= 0f }) return
        val centres = slots.map { it.top + it.height / 2f }
        val destination = liquidRailPreviewAt(y, centres)
        previewRow = destination
        pressedIndex = destination.roundToInt().coerceIn(slots.indices)
    }

    fun settle(index: Int) {
        animate(starts[index], starts[index + 1])
    }

    fun drag(position: Float, grabOffset: Float, delta: Float, previousIndex: Int) {
        val index = indexAt(position)
        val chipWidth = navigationDragWidth(position - contentStart, widths)
        val travelStart = contentStart - endPadding
        val target = navigationDragTarget(
            left = position - grabOffset,
            width = chipWidth,
            minLeft = travelStart,
            maxRight = width,
            delta = delta,
            changedItem = index != previousIndex,
        )
        pressedIndex = index
        animate(target.left, target.right, target.leftSpring, target.rightSpring)
    }

    fun start(index: Int): Float = starts[index]

    private fun animate(
        start: Float,
        end: Float,
        leftSpring: SpringSpec<Float> = GlassMotion.edgeSpring(start <= left.value),
        rightSpring: SpringSpec<Float> = GlassMotion.edgeSpring(end > right.value),
    ) {
        if (left.targetValue != start) scope.launch { left.animateTo(start, leftSpring) }
        if (right.targetValue != end) scope.launch { right.animateTo(end, rightSpring) }
    }
}

/** The captured state references are remembered by the caller; no gesture is restarted on a morph. */
private class RailGestureControl(
    val interaction: NavigationRailInteraction,
    val layoutDirection: LayoutDirection,
    val expanded: State<Boolean>,
    val headerBottom: State<() -> Float>,
    val onSelect: State<(Int) -> Unit>,
    val liquid: Boolean,
)

/** One touch's mutable coordinates; the global indicator still belongs to NavigationRailInteraction. */
private class RailGestureRun(
    val initial: Int,
    var position: Float,
    val grabOffset: Float,
    val horizontal: Boolean,
    val source: MutableInteractionSource,
) {
    var current = initial
    var movementX = 0f
    var movementY = 0f
    var dragging = false
    var committed = false
    var rowPress: PressInteraction.Press? = null
}

/** A stable recognizer: morph frames and the expanded target never replace an in-flight press. */
internal fun Modifier.navigationRailGestures(
    interaction: NavigationRailInteraction,
    layoutDirection: LayoutDirection,
    expanded: State<Boolean>,
    headerBottom: State<() -> Float>,
    onSelect: State<(Int) -> Unit>,
    liquid: Boolean = false,
): Modifier = pointerInput(interaction, layoutDirection, liquid) {
    val control = RailGestureControl(interaction, layoutDirection, expanded, headerBottom, onSelect, liquid)
    awaitEachGesture { performRailGesture(control, viewConfiguration.touchSlop) }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.performRailGesture(
    control: RailGestureControl,
    touchSlop: Float,
) {
    val down = awaitFirstDown()
    fun logicalX(position: Offset): Float =
        if (control.layoutDirection == LayoutDirection.Rtl) size.width - position.x else position.x
    if (down.position.y < control.headerBottom.value()) return
    val initial = control.interaction.indexAt(logicalX(down.position), down.position.y)
    if (initial < 0) return
    val horizontal = !control.expanded.value
    val position = control.interaction.positionAt(initial, logicalX(down.position))
    val run = RailGestureRun(
        initial, position, position - control.interaction.start(initial),
        horizontal, control.interaction.rowInteractions[initial],
    )
    val liquidExpanded = control.liquid && !horizontal
    run.rowPress = if (horizontal || liquidExpanded) null else PressInteraction.Press(down.position)
    if (horizontal || liquidExpanded) control.interaction.press(initial)
    else run.source.tryEmit(checkNotNull(run.rowPress))
    try {
        trackRailPointer(control, down, run, touchSlop)
    } finally {
        if (horizontal || liquidExpanded) {
            control.interaction.release()
        } else {
            run.rowPress?.let { press ->
                run.source.tryEmit(if (run.committed) PressInteraction.Release(press) else PressInteraction.Cancel(press))
            }
        }
    }
}

/** The original upstream pointer policy, isolated from press lifecycle and gesture setup. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.trackRailPointer(
    control: RailGestureControl,
    down: androidx.compose.ui.input.pointer.PointerInputChange,
    run: RailGestureRun,
    touchSlop: Float,
) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        if (change.isConsumed) break
        val physicalDelta = change.position - change.previousPosition
        val dragScale = control.interaction.widths[run.current] /
            control.interaction.slots[run.current].width.coerceAtLeast(1f)
        val delta = railLogicalDelta(physicalDelta.x, dragScale, control.layoutDirection)
        run.movementX += physicalDelta.x
        run.movementY += physicalDelta.y
        run.position += delta
        if (!change.pressed) {
            if (change.changedToUp()) {
                control.onSelect.value(
                    when {
                        run.horizontal -> control.interaction.indexAt(run.position)
                        control.liquid && run.dragging -> run.current
                        else -> run.initial
                    },
                )
                change.consume()
                run.committed = true
            }
            break
        }
        if (!run.dragging) {
            val decision = if (control.liquid) {
                liquidRailGestureDecision(run.movementX, run.movementY, touchSlop, run.horizontal)
            } else railGestureDecision(run.movementX, run.movementY, touchSlop, run.horizontal)
            when (decision) {
                RailGestureDecision.Cancel -> break
                RailGestureDecision.Drag -> run.dragging = true
                RailGestureDecision.Pending -> Unit
            }
        }
        if (run.dragging) {
            if (control.liquid && !run.horizontal) {
                control.interaction.dragExpanded(change.position.y)
                run.current = control.interaction.pressedIndex
            } else {
                control.interaction.drag(run.position, run.grabOffset, delta, run.current)
                run.current = control.interaction.indexAt(run.position)
            }
            change.consume()
        } else {
            val final = awaitPointerEvent(PointerEventPass.Final)
            if (final.changes.any { it.id == down.id && it.isConsumed }) break
        }
    }
}
