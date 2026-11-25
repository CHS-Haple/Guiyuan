package com.chaners.guiyuan.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun pageContentPadding(
    innerPadding: PaddingValues,
    outerBottomPadding: Dp? = null,
    extraBottom: Dp,
): PaddingValues = remember(innerPadding, outerBottomPadding, extraBottom) {
    // Follow MIUIX Scaffold's live padding contract: read during layout, not composition.
    object : PaddingValues {
        override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
            innerPadding.calculateLeftPadding(layoutDirection)

        override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
            innerPadding.calculateRightPadding(layoutDirection)

        override fun calculateTopPadding(): Dp = innerPadding.calculateTopPadding()

        override fun calculateBottomPadding(): Dp =
            (outerBottomPadding ?: innerPadding.calculateBottomPadding()) + extraBottom
    }
}

// Scaffold derives the content inset from measured TopAppBar height, which
// decreases by heightOffset. Cancel only that offset for a stable scroll track.
@Composable
internal fun pageScrollBarTrackPadding(
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
): PaddingValues {
    val density = LocalDensity.current
    return remember(contentPadding, scrollBehavior, density) {
        object : PaddingValues {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp

            override fun calculateTopPadding(): Dp {
                val offset = with(density) { scrollBehavior.state.heightOffset.toDp() }
                return (contentPadding.calculateTopPadding() - offset).coerceAtLeast(0.dp)
            }

            override fun calculateBottomPadding(): Dp = contentPadding.calculateBottomPadding()
        }
    }
}
