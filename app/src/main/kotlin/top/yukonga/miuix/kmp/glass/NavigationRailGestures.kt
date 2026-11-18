// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection

/** Shared native-rail gesture owner; layout and indicator springs live in NavigationRailInteraction. */
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
