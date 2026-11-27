package com.chaners.guiyuan.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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

/** Keep the offset callback stable for the lifetime of the page's existing scroll owner. */
@Composable
internal fun rememberPageOverscrollCallback(behavior: ScrollBehavior): (Float) -> Unit =
    remember(behavior) { { offset -> behavior.state.overscrollOffset = offset } }

/** Shared MIUIX modifier order; callers retain their own refresh and haptic eligibility. */
internal fun Modifier.pageScrollEffects(
    behavior: ScrollBehavior,
    edgeHaptic: Boolean,
    glassOverscroll: Boolean,
    onOverscrollChanged: (Float) -> Unit,
): Modifier = then(if (edgeHaptic) Modifier.scrollEndHaptic() else Modifier)
    .then(if (glassOverscroll) Modifier.overScrollVertical(onOffsetChanged = onOverscrollChanged) else Modifier)
    .nestedScroll(behavior.nestedScrollConnection)
