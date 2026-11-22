package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiKeyguardHostResolverTest {
    @Test
    fun steadyKeyguardAcceptsKeyguardAndShadeLockedOnly() {
        assertTrue(SysUiKeyguardHostResolver.isSteadySurface(SysUiSceneSource.Surface.KEYGUARD))
        assertTrue(SysUiKeyguardHostResolver.isSteadySurface(SysUiSceneSource.Surface.SHADE_LOCKED))
        assertFalse(SysUiKeyguardHostResolver.isSteadySurface(SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertFalse(SysUiKeyguardHostResolver.isSteadySurface(SysUiSceneSource.Surface.UNKNOWN))
    }

    @Test
    fun keyguardStatusIconsAlphaUsesOnlyLocalNativeStatusIconLayer() {
        assertTrue(
            SysUiKeyguardNativeSignals.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 1f,
            ) == 1f,
        )
        assertTrue(
            SysUiKeyguardNativeSignals.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 0.005f,
            ) == 0.005f,
        )
        assertTrue(
            SysUiKeyguardNativeSignals.resolveStatusIconsPresentationAlpha(
                visible = false,
                alpha = 1f,
            ) == 0f,
        )
        assertTrue(
            SysUiKeyguardNativeSignals.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 2f,
            ) == 1f,
        )
    }

}
