package com.chaners.guiyuan.xposed.network

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiNetworkSourceTest {
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
        val homeClass =
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        assertTrue(eligible(homeClass, "mobile", "mobile_combo", true, true))

        // Identically named keyguard/QS/QS_FAKE views are not Home children.
        assertFalse(eligible(homeClass, "mobile", "mobile_combo", true, false))
        assertFalse(eligible(homeClass, "mobile", "mobile_combo", false, true))
        assertFalse(eligible(homeClass, "stacked_mobile", "mobile_combo", true, true))
        assertFalse(eligible(homeClass, "mobile", "other_view", true, true))
        assertFalse(eligible("com.android.systemui.statusbar.pipeline.mobile.ui.view.Other", "mobile",
            "mobile_combo", true, true))
    }

    @Test
    fun fatalReflectionCauseIsNotReportedAsMissingNativeState() {
        val fatal = OutOfMemoryError("simulated")
        val actual = runCatching {
            SysUiNetworkSource.rethrowFatal(InvocationTargetException(fatal))
        }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun threadDeathPropagatesWithoutWrapping() {
        val fatal = ThreadDeath()
        val actual = runCatching { SysUiNetworkSource.rethrowFatal(fatal) }.exceptionOrNull()
        assertSame(fatal, actual)
    }

    @Test
    fun ordinaryReflectionFailureRemainsRecoverable() {
        SysUiNetworkSource.rethrowFatal(InvocationTargetException(NoSuchMethodException("optional")))
    }
}
