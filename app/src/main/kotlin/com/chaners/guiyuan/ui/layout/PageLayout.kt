package com.chaners.guiyuan.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun pageContentPadding(
    innerPadding: PaddingValues,
    outerBottomPadding: Dp? = null,
    extraBottom: Dp,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val start = innerPadding.calculateStartPadding(layoutDirection)
    val top = innerPadding.calculateTopPadding()
    val end = innerPadding.calculateEndPadding(layoutDirection)
    val bottom = (outerBottomPadding ?: innerPadding.calculateBottomPadding()) + extraBottom

    return remember(start, top, end, bottom) {
        PaddingValues(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )
    }
}

// Scaffold derives the content inset from measured TopAppBar height, which
// decreases by heightOffset. Cancel only that offset for a stable scroll track.
@Composable
internal fun pageScrollBarTrackPadding(
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
): PaddingValues {
    val offset = with(LocalDensity.current) { scrollBehavior.state.heightOffset.toDp() }
    val top = (contentPadding.calculateTopPadding() - offset).coerceAtLeast(0.dp)
    val bottom = contentPadding.calculateBottomPadding()
    return remember(top, bottom) { PaddingValues(top = top, bottom = bottom) }
}
