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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The existing empty bottom margin is hit-test space; the bar and system gestures keep their geometry.
@Composable
internal fun Modifier.navTouchExtension(
    bottomPadding: Dp,
    horizontalPadding: Dp,
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
    val extension = (bottomPadding - bottomGestureInset - 4.dp).coerceIn(0.dp, 12.dp)
    if (extension == 0.dp) return this

    val select = rememberUpdatedState(onSelect)
    return this.pointerInput(bottomPadding, extension, horizontalPadding, itemCount, rtl) {
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
            val width = right - left
            if (width <= 0f) return@awaitEachGesture
            val relativeX = if (rtl) right - up.position.x else up.position.x - left
            val index = ((relativeX / width) * itemCount).toInt().coerceIn(0, itemCount - 1)
            up.consume()
            select.value(index)
        }
    }
}
