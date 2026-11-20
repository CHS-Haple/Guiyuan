package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BatterySchemeIndicatorRail(
    pages: List<BatterySchemePage>,
    pagerState: PagerState,
    targetPage: Int,
    onPageSelect: (Int) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        BatteryPagerIndicator(
            pages = pages,
            pagerState = pagerState,
            targetPage = targetPage,
            onPageSelect = onPageSelect,
        )
    }
}

private data class BatteryIndicatorGesture(
    val centers: FloatArray,
    val pressed: androidx.compose.runtime.MutableIntState,
    val navigate: androidx.compose.runtime.State<(Int) -> Unit>,
    val switchMargin: Float,
)

private data class BatteryIndicatorLayout(
    val pager: PagerState,
    val targetPage: Int,
    val gap: Dp,
    val spring: androidx.compose.animation.core.SpringSpec<Dp>,
)

@Composable
private fun BatteryPagerIndicator(
    pages: List<BatterySchemePage>,
    pagerState: PagerState,
    targetPage: Int,
    onPageSelect: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val navigate = rememberUpdatedState(onPageSelect)
    val centers = remember(pages.size) { FloatArray(pages.size) }
    val pressedPage = remember { mutableIntStateOf(-1) }
    val dotSpring = remember {
        spring<Dp>(stiffness = Spring.StiffnessMedium, dampingRatio = 0.85f)
    }
    val gap by animateDpAsState(
        targetValue = if (pressedPage.intValue >= 0) 10.dp else 8.dp,
        animationSpec = dotSpring,
        label = "battery-scheme-gap",
    )
    val gesture = BatteryIndicatorGesture(
        centers = centers,
        pressed = pressedPage,
        navigate = navigate,
        switchMargin = with(LocalDensity.current) { 3.dp.toPx() },
    )
    val layout = BatteryIndicatorLayout(pagerState, targetPage, gap, dotSpring)
    Row(
        modifier = Modifier.batteryIndicatorGestures(gesture, haptic),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryIndicatorDots(pages, layout, gesture)
    }
}

private fun Modifier.batteryIndicatorGestures(
    gesture: BatteryIndicatorGesture,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
): Modifier = pointerInput(gesture.centers.size, haptic) {
    awaitEachGesture {
        fun nearest(x: Float): Int =
            gesture.centers.indices.minByOrNull { abs(x - gesture.centers[it]) } ?: -1

        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        var selected = nearest(down.position.x)
        if (selected >= 0) {
            gesture.pressed.intValue = selected
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            down.consume()
            gesture.navigate.value(selected)
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val next = nearest(change.position.x)
                    if (next >= 0 && next != selected &&
                        abs(change.position.x - gesture.centers[next]) + gesture.switchMargin <
                        abs(change.position.x - gesture.centers[selected])
                    ) {
                        selected = next
                        gesture.pressed.intValue = next
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        gesture.navigate.value(next)
                    }
                    event.changes.forEach { it.consume() }
                    if (!change.pressed) break
                }
            } finally {
                gesture.pressed.intValue = -1
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BatteryIndicatorDots(
    pages: List<BatterySchemePage>,
    layout: BatteryIndicatorLayout,
    gesture: BatteryIndicatorGesture,
) {
    val currentPage = layout.pager.currentPage
    val pagePosition =
        (currentPage + layout.pager.currentPageOffsetFraction).coerceIn(0f, pages.lastIndex.toFloat())
    val scrolling = layout.pager.isScrollInProgress
    val pressed = gesture.pressed.intValue
    val highlightedPage = pressed.takeIf { it >= 0 } ?: layout.targetPage.takeIf { it >= 0 }
    pages.forEachIndexed { index, page ->
        val name = when (page) {
            is BatterySchemePage.BuiltIn -> batteryBuiltInName(page.scheme)
            is BatterySchemePage.Custom -> customSchemeName(page.scheme)
            BatterySchemePage.Add -> stringResource(R.string.battery_custom_scheme_add_page_title)
        }
        val isCurrent = index == currentPage
        val progress =
            if (highlightedPage != null) {
                if (index == highlightedPage) 1f else 0f
            } else {
                (1f - abs(index - pagePosition)).coerceIn(0f, 1f)
            }
        val width by animateDpAsState(
            targetValue = 8.dp + (if (pressed == index) 16.dp else 13.dp) * progress,
            animationSpec = if (scrolling && highlightedPage == null) snap() else layout.spring,
            label = "battery-scheme-indicator",
        )
        Box(
            modifier = Modifier
                .width(width + layout.gap).height(28.dp)
                .onGloballyPositioned {
                    gesture.centers[index] = it.positionInParent().x + it.size.width / 2f
                }
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClickLabel = stringResource(R.string.battery_scheme_go_to, name),
                    onClick = { gesture.navigate.value(index) },
                )
                .semantics {
                    contentDescription = name
                    selected = isCurrent
                },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.width(width).height(8.dp),
                shape = CircleShape,
                color = lerp(
                    MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                    MiuixTheme.colorScheme.primary,
                    progress,
                ),
            ) {}
        }
    }
}
