package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardHostResolverTest {
    @Test
    fun steadyKeyguardAcceptsLockedStates() {
        assertTrue(KeyguardHostResolver.isSteadyKeyguardSurface(SceneSource.Surface.KEYGUARD))
        assertTrue(KeyguardHostResolver.isSteadyKeyguardSurface(SceneSource.Surface.SHADE_LOCKED))
        assertFalse(KeyguardHostResolver.isSteadyKeyguardSurface(SceneSource.Surface.UNLOCKED_STATUS_BAR))
        assertFalse(KeyguardHostResolver.isSteadyKeyguardSurface(SceneSource.Surface.UNKNOWN))
    }

    @Test
    fun resolverAcceptsPinnedHost() {
        assertTrue(KeyguardHostResolver.isKeyguardHostClassName("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"))
        assertFalse(KeyguardHostResolver.isKeyguardHostClassName("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"))
    }
    @Test
    fun keyguardAlphaUsesLocalIconLayer() {
        assertTrue(
            KeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 1f,
            ) == 1f,
        )
        assertTrue(
            KeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 0.005f,
            ) == 0.005f,
        )
        assertTrue(
            KeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = false,
                alpha = 1f,
            ) == 0f,
        )
        assertTrue(
            KeyguardHostResolver.resolveStatusIconsPresentationAlpha(
                visible = true,
                alpha = 2f,
            ) == 1f,
        )
    }

}
