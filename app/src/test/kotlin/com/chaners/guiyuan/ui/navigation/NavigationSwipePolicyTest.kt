package com.chaners.guiyuan.ui.navigation

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

class NavigationSwipePolicyTest {
    @Test fun disabledSwipeBackNeverAcceptsEitherLayoutDirection() {
        assertEquals(NavSwipeDirection.None, appSwipeDismissDirection(false, LayoutDirection.Ltr))
        assertEquals(NavSwipeDirection.None, appSwipeDismissDirection(false, LayoutDirection.Rtl))
    }

    @Test fun enabledSwipeBackFollowsLayoutDirection() {
        assertEquals(NavSwipeDirection.LeftToRight, appSwipeDismissDirection(true, LayoutDirection.Ltr))
        assertEquals(NavSwipeDirection.RightToLeft, appSwipeDismissDirection(true, LayoutDirection.Rtl))
    }
}
