package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
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

            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
            if (up.isConsumed || (up.position - start).getDistance() > viewConfiguration.touchSlop ||
                up.position.x < left || up.position.x >= right ||
                up.position.y < top || up.position.y >= bottom
            ) {
                return@awaitEachGesture
            }
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
