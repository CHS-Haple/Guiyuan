package com.chaners.guiyuan.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

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
