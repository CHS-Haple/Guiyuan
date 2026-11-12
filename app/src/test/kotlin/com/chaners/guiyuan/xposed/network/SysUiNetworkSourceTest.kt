package com.chaners.guiyuan.xposed.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiNetworkSourceTest {
    private companion object {
        const val HOME_CLASS =
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        const val MOBILE_SLOT = "mobile"
        const val MOBILE_RESOURCE = "mobile_combo"
    }

    @Test
    fun nativeActivityPresenceFollowsOriginalRootAndChildNotSceneIsShown() {
        // The original binder uses isVisible().value.first to gate the
        // mobile Home root and getInOutVisible() for the activity child.
        assertEquals(true, SysUiNetworkSource.nativeMobileActivityShown(true, true))
        assertEquals(false, SysUiNetworkSource.nativeMobileActivityShown(true, false))
        assertEquals(false, SysUiNetworkSource.nativeMobileActivityShown(false, true))
        assertEquals(false, SysUiNetworkSource.nativeMobileActivityShown(false, false))
        // No native binding authority means no invented visible state.
        assertNull(SysUiNetworkSource.nativeMobileActivityShown(null, true))
    }

    @Test
    fun nativeMobileFallbackRequiresVerifiedCurrentHomeRoot() {
        val eligible = { className: String, slot: String?, resourceEntry: String?,
                         attached: Boolean, directHomeChild: Boolean ->
            SysUiNetworkSource.isNativeHomeMobileRootIdentity(
                className = className,
                slot = slot,
                resourceEntry = resourceEntry,
                attached = attached,
                directHomeChild = directHomeChild,
            )
        }
        assertTrue(eligible(HOME_CLASS, MOBILE_SLOT, MOBILE_RESOURCE, true, true))

        // Identically named keyguard/QS/QS_FAKE views are not Home children.
        assertFalse(eligible(HOME_CLASS, MOBILE_SLOT, MOBILE_RESOURCE, true, false))
        assertFalse(eligible(HOME_CLASS, MOBILE_SLOT, MOBILE_RESOURCE, false, true))
        assertFalse(eligible(HOME_CLASS, "stacked_mobile", MOBILE_RESOURCE, true, true))
        assertFalse(eligible(HOME_CLASS, MOBILE_SLOT, "other_view", true, true))
        assertFalse(eligible("com.android.systemui.statusbar.pipeline.mobile.ui.view.Other",
            MOBILE_SLOT, MOBILE_RESOURCE, true, true))
    }

    @Test
    fun nativeOwnerIsRequiredEvenWhenBinderHasAValidId() {
        assertNull(
            SysUiNetworkSource.consistentHomeMobileSubscription(
                nativeId = null,
                binderObserved = true,
                binderId = 1,
            ),
        )
        assertNull(
            SysUiNetworkSource.consistentHomeMobileSubscription(
                nativeId = -1,
                binderObserved = false,
                binderId = null,
            ),
        )
    }

    @Test
    fun nativeOwnerCanRecoverMissingBinderButRejectsConflictingReceipts() {
        assertEquals(
            2,
            SysUiNetworkSource.consistentHomeMobileSubscription(
                nativeId = 2,
                binderObserved = false,
                binderId = null,
            ),
        )
        assertEquals(
            2,
            SysUiNetworkSource.consistentHomeMobileSubscription(
                nativeId = 2,
                binderObserved = true,
                binderId = 2,
            ),
        )
        assertNull(
            SysUiNetworkSource.consistentHomeMobileSubscription(
                nativeId = 2,
                binderObserved = true,
                binderId = 1,
            ),
        )
        // An invalid explicit Binder bind must block stale native ownership.
        assertNull(
            SysUiNetworkSource.consistentHomeMobileSubscription(
                nativeId = 2,
                binderObserved = true,
                binderId = null,
            ),
        )
    }

}
