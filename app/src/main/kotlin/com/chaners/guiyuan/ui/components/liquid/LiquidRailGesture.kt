package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Map a pointer to the interval between destination centres, retaining partial drags. */
internal fun liquidRailPointerTarget(
    y: Float,
    itemHeight: Float,
    inset: Float,
    count: Int,
): Float {
    if (count <= 0 || itemHeight <= 0f) return 0f
    return ((y - inset) / itemHeight - 0.5f).fastCoerceIn(0f, (count - 1).toFloat())
}

/**
 * Vertical Liquid layout adapted from the accepted horizontal B1842 optics.
 *
 * This is Guiyuan-owned geometry (MIUIX has no published LiquidNavigationRail).
 * It deliberately reuses LiquidDragMotion, LiquidHighlight, theme color, tilt,
 * lens/edge stroke and the recorded background, not OS4 Glass material.
 */
internal data class RailGesture(
    val motion: LiquidDragMotion,
    val bounceOffset: State<Float>,
    val selectedIndex: State<Int>,
    val highlight: LiquidHighlight,
    val itemHeightPx: Float,
    val onSelect: (Int) -> Unit,
)

@Composable
internal fun rememberRailGesture(
    selectedIndex: () -> Int,
    onSelected: (Int) -> Unit,
    count: Int,
): RailGesture {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { LiquidRailSpec.ItemHeight.toPx() }
    val railHeightPx = with(density) { LiquidRailSpec.height(count).toPx() }
    val insetPx = with(density) { 4.dp.toPx() }
    val scope = rememberCoroutineScope()
    val currentIndex = remember(count) {
        mutableIntStateOf(selectedIndex().coerceIn(0, count - 1))
    }
    val onSelectedUpdated by rememberUpdatedState(onSelected)
    val bounce = remember { Animatable(0f) }
    val bounceOffset = remember(railHeightPx, insetPx) {
        derivedStateOf {
            val fraction = (bounce.value / railHeightPx).fastCoerceIn(-1f, 1f)
            insetPx * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }
    val motion = remember(scope, count, itemHeightPx) {
        LiquidDragMotion(
            scope = scope,
            initialValue = selectedIndex().toFloat(),
            range = 0f..(count - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position -> position.y in 0f..railHeightPx },
            onStarted = { position ->
                updateValue(liquidRailPointerTarget(position.y, itemHeightPx, insetPx, count))
            },
            onStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, count - 1)
                if (currentIndex.intValue != target) {
                    currentIndex.intValue = target
                    onSelectedUpdated(target)
                }
                updateValue(target.toFloat())
                scope.launch { bounce.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onCancelled = {
                updateValue(currentIndex.intValue.toFloat())
                scope.launch { bounce.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, delta ->
                if (delta.y != 0f) {
                    updateValue((targetValue + delta.y / itemHeightPx)
                        .fastCoerceIn(0f, (count - 1).toFloat()))
                    scope.launch { bounce.snapTo(bounce.value + delta.y) }
                }
            },
        )
    }
    MirrorRailPagerSelection(selectedIndex, currentIndex, motion, count)
    val highlight = rememberRailHighlight(scope, motion, itemHeightPx, bounceOffset)
    return RailGesture(motion, bounceOffset, currentIndex, highlight, itemHeightPx, onSelectedUpdated)
}

@Composable
private fun MirrorRailPagerSelection(
    selectedIndex: () -> Int,
    currentIndex: MutableIntState,
    motion: LiquidDragMotion,
    count: Int,
) {
    LaunchedEffect(selectedIndex, motion) {
        snapshotFlow { selectedIndex() }.collectLatest { index ->
            val target = index.coerceIn(0, count - 1)
            if (currentIndex.intValue != target) {
                currentIndex.intValue = target
                motion.animateToValue(target.toFloat())
            }
        }
    }
}

@Composable
private fun rememberRailHighlight(
    scope: CoroutineScope,
    motion: LiquidDragMotion,
    itemHeightPx: Float,
    bounceOffset: State<Float>,
): LiquidHighlight = remember(scope, motion, itemHeightPx, bounceOffset) {
    LiquidHighlight(scope) { size, _ ->
        Offset(size.width / 2f, (motion.value + 0.5f) * itemHeightPx + bounceOffset.value)
    }
}
