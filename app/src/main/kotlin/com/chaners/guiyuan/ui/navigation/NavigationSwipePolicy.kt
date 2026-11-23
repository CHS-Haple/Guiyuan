package com.chaners.guiyuan.ui.navigation

import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

/**
 * Shared gesture direction for the main stack and onboarding stages.
 * The user's swipe-back preference gates the same logical leading-edge gesture.
 */
internal fun appSwipeDismissDirection(enabled: Boolean, direction: LayoutDirection): NavSwipeDirection =
    when {
        !enabled -> NavSwipeDirection.None
        direction == LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        else -> NavSwipeDirection.RightToLeft
    }
