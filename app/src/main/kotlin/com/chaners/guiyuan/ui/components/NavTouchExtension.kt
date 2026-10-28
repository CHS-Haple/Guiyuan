package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The existing empty bottom margin is hit-test space; the bar and system gestures keep their geometry.
@Composable
internal fun Modifier.navTouchExtension(
    bottomPadding: Dp,
    horizontalPadding: Dp,
    contentPadding: Dp,
    itemCount: Int,
    rtl: Boolean,
    onSelect: (Int) -> Unit,
): Modifier {
    if (itemCount <= 0) return this

    val bottomGestureInset = maxOf(
        WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding(),
        WindowInsets.mandatorySystemGestures.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding(),
    )
    val extension = navTouchExtensionHeight(bottomPadding, bottomGestureInset)
    if (extension == 0.dp) return this

    val select = rememberUpdatedState(onSelect)
    return this.pointerInput(bottomPadding, extension, horizontalPadding, contentPadding, itemCount, rtl) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val left = horizontalPadding.toPx()
            val right = size.width - left
            val top = size.height - bottomPadding.toPx()
            val bottom = top + extension.toPx()
            val start = down.position
            if (start.x < left || start.x >= right || start.y < top || start.y >= bottom) {
                return@awaitEachGesture
            }

            val up = awaitShortTapUp(down) ?: return@awaitEachGesture
            if (up.position.x < left || up.position.x >= right ||
                up.position.y < top || up.position.y >= bottom
            ) return@awaitEachGesture
            val index = navTouchIndex(
                x = up.position.x,
                left = left,
                right = right,
                contentPadding = contentPadding.toPx(),
                count = itemCount,
                rtl = rtl,
            ) ?: return@awaitEachGesture
            up.consume()
            select.value(index)
        }
    }
}

internal fun navTouchExtensionHeight(bottomPadding: Dp, gestureInset: Dp): Dp =
    (bottomPadding - gestureInset - 4.dp).coerceIn(0.dp, 12.dp)

internal fun navTouchIndex(
    x: Float,
    left: Float,
    right: Float,
    contentPadding: Float,
    count: Int,
    rtl: Boolean,
): Int? {
    val width = right - left - contentPadding * 2f
    if (count <= 0 || x < left || x >= right || width <= 0f) return null
    val relative = if (rtl) right - contentPadding - x else x - left - contentPadding
    return (relative / width * count).toInt().coerceIn(0, count - 1)
}

/**
 * MIUIX's published floating bar positions each destination at its measured width.
 * Use those bounds instead of dividing the host width into equal slots.
 */
@Composable
internal fun Modifier.measuredNavTouchExtension(
    hostBounds: Rect?,
    barBounds: Rect?,
    itemBounds: List<Rect?>,
    onSelect: (Int) -> Unit,
): Modifier {
    if (hostBounds == null || barBounds == null || itemBounds.isEmpty() ||
        itemBounds.any { it == null }
    ) return this

    val gestureInset = maxOf(
        WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding(),
        WindowInsets.mandatorySystemGestures.only(WindowInsetsSides.Bottom)
            .asPaddingValues().calculateBottomPadding(),
    )
    val density = LocalDensity.current
    val gesturePx = with(density) { gestureInset.toPx() }
    val extensionPx = with(density) { 12.dp.toPx() }
    val safetyPx = with(density) { 4.dp.toPx() }
    val select = rememberUpdatedState(onSelect)
    val measuredItems = itemBounds.filterNotNull()
    return this.pointerInput(hostBounds, barBounds, measuredItems, gesturePx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position + hostBounds.topLeft
            val index = measuredNavTouchTarget(
                start.x, start.y, hostBounds, barBounds, measuredItems,
                gesturePx, extensionPx, safetyPx,
            ) ?: return@awaitEachGesture

            val up = awaitShortTapUp(down) ?: return@awaitEachGesture
            val end = up.position + hostBounds.topLeft
            if (measuredNavTouchTarget(
                    end.x, end.y, hostBounds, barBounds, measuredItems,
                    gesturePx, extensionPx, safetyPx,
                ) != index
            ) return@awaitEachGesture

            up.consume()
            select.value(index)
        }
    }
}

internal fun measuredNavTouchTarget(
    x: Float,
    y: Float,
    host: Rect,
    bar: Rect,
    items: List<Rect>,
    gestureInset: Float,
    extension: Float,
    safety: Float,
): Int? {
    val bottom = minOf(bar.bottom + extension, host.bottom - gestureInset - safety)
    if (items.isEmpty() || x < bar.left || x >= bar.right ||
        y < bar.bottom || y >= bottom
    ) return null
    return items.indices.firstOrNull { x >= items[it].left && x < items[it].right }
}

/** Accept one short stationary tap. A long hold, drag excursion or second finger cancels. */
private suspend fun AwaitPointerEventScope.awaitShortTapUp(down: PointerInputChange): PointerInputChange? =
    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        var up: PointerInputChange? = null
        while (up == null) {
            val event = awaitPointerEvent()
            if (event.changes.any { it.id != down.id && it.pressed }) return@withTimeoutOrNull null
            val change = event.changes.firstOrNull { it.id == down.id }
                ?: return@withTimeoutOrNull null
            if (change.isConsumed ||
                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            ) return@withTimeoutOrNull null
            if (!change.pressed) up = change
        }
        up
    }
