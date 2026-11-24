package com.chaners.guiyuan.ui.glass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults

/**
 * Compatibility boundary around the *published* MIUIX GlassSegmentedTabRow.
 *
 * In pinned upstream a09d4f32, GlassTabRow.kt's pointer recognizer claims a
 * gesture using X movement alone, even if its actual intent is vertical page
 * scroll. This small topmost hit surface owns only direction arbitration and
 * index selection; MIUIX still owns the real tabs, selection animation,
 * materials, accessibility semantics and internal rendering.
 *
 * When the pinned MIUIX version implements X/Y intent arbitration, remove
 * the overlay below and delegate directly to GlassSegmentedTabRow. Call sites
 * do not need to change. No upstream source is forked.
 */
@Composable
internal fun ScrollAwareGlassSegmentedTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val select = rememberUpdatedState(onSelect)
    val selected = rememberUpdatedState(selectedIndex)
    val layout = SegmentedSlotGeometry(
        width = 0f,
        count = tabs.size,
        trackPadding = with(density) { GlassSegmentedTabRowDefaults.TrackPadding.toPx() },
        gap = with(density) { GlassSegmentedTabRowDefaults.TabGap.toPx() },
        rtl = direction == LayoutDirection.Rtl,
    )

    Box(modifier) {
        GlassSegmentedTabRow(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
        )
        // The overlay is the top hit target, leaving the published native
        // material and selection rendering untouched.
        Box(Modifier.matchParentSize().segmentedGestureGuard(layout, selected, select)) {}
    }
}

private fun Modifier.segmentedGestureGuard(
    layout: SegmentedSlotGeometry,
    selected: State<Int>,
    select: State<(Int) -> Unit>,
): Modifier = pointerInput(layout) {
    awaitEachGesture {
        trackSegmentedGesture(layout.copy(width = size.width.toFloat()), selected, select)
    }
}

private suspend fun AwaitPointerEventScope.trackSegmentedGesture(
    slots: SegmentedSlotGeometry,
    selected: State<Int>,
    select: State<(Int) -> Unit>,
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    var axis = SegmentedDragAxis.Pending
    var lastSelected = selected.value
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        if (change.isConsumed) break
        if (!change.pressed) {
            finishSegmentedSelection(change, axis, lastSelected, slots, select)
            break
        }
        if (axis == SegmentedDragAxis.Pending) {
            axis = segmentedDragAxis(
                change.position.x - down.position.x,
                change.position.y - down.position.y,
                viewConfiguration.touchSlop,
            )
        }
        if (axis == SegmentedDragAxis.Horizontal) {
            val next = slots.indexAt(change.position.x)
            if (next >= 0 && next != lastSelected) {
                select.value(next)
                lastSelected = next
            }
            change.consume()
        }
        // Vertical motion never consumes; the parent owns verticalScroll.
    }
}

private fun finishSegmentedSelection(
    change: PointerInputChange,
    axis: SegmentedDragAxis,
    lastSelected: Int,
    slots: SegmentedSlotGeometry,
    select: State<(Int) -> Unit>,
) {
    if (!change.changedToUp() || axis == SegmentedDragAxis.Vertical) return
    val next = slots.indexAt(change.position.x)
    if (next >= 0 && (axis == SegmentedDragAxis.Pending || next != lastSelected)) {
        select.value(next)
    }
    change.consume()
}

internal enum class SegmentedDragAxis { Pending, Horizontal, Vertical }

/** Classify once after platform slop, not on each event or using an app pixel threshold. */
internal fun segmentedDragAxis(dx: Float, dy: Float, touchSlop: Float): SegmentedDragAxis {
    val horizontal = abs(dx)
    val vertical = abs(dy)
    if (maxOf(horizontal, vertical) <= touchSlop) return SegmentedDragAxis.Pending
    return if (horizontal > vertical) SegmentedDragAxis.Horizontal else SegmentedDragAxis.Vertical
}

/** Own upstream public slot constants only; do not copy upstream animation/rendering logic. */
internal data class SegmentedSlotGeometry(
    val width: Float,
    val count: Int,
    val trackPadding: Float,
    val gap: Float,
    val rtl: Boolean,
) {
    fun indexAt(x: Float): Int {
        if (count <= 0) return -1
        val trackWidth = (width - trackPadding * 2).coerceAtLeast(0f)
        val slot = ((trackWidth - gap * (count - 1)) / count).coerceAtLeast(0f)
        if (slot <= 0f) return -1
        val logicalX = if (rtl) width - x else x
        return ((logicalX - trackPadding) / (slot + gap)).toInt().coerceIn(0, count - 1)
    }
}
