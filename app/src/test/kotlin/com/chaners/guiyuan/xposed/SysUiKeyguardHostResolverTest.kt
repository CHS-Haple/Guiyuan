package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiKeyguardHostResolverTest {
    @Test
    fun steadyKeyguardAcceptsKeyguardAndShadeLockedOnly() {
        assertTrue(SysUiKeyguardHostResolver.isSteadyKeyguardSurface(SysUiSceneSource.Surface.KEYGUARD))
        assertTrue(SysUiKeyguardHostResolver.isSteadyKeyguardSurface(SysUiSceneSource.Surface.SHADE_LOCKED))
        assertFalse(SysUiKeyguardHostResolver.isSteadyKeyguardSurface(SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertFalse(SysUiKeyguardHostResolver.isSteadyKeyguardSurface(SysUiSceneSource.Surface.UNKNOWN))
    }

    @Test
    fun resolverAcceptsOnlyPinnedKeyguardHostClass() {
        assertTrue(SysUiKeyguardHostResolver.isKeyguardHostClassName("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"))
        assertFalse(SysUiKeyguardHostResolver.isKeyguardHostClassName("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"))
    }
    @Test
    fun keyguardStatusIconsAlphaUsesOnlyLocalNativeStatusIconLayer() {
        assertTrue(
            SysUiKeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 1f,
            ) == 1f,
        )
        assertTrue(
            SysUiKeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 0.005f,
            ) == 0.005f,
        )
        assertTrue(
            SysUiKeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = false,
                alpha = 1f,
            ) == 0f,
        )
        assertTrue(
            SysUiKeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 2f,
            ) == 1f,
        )
    }

}
