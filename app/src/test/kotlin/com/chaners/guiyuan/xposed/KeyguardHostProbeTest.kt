package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardHostProbeTest {
    @Test
    fun probeRunsOnlyForKeyguardSurface() {
        assertTrue(
            KeyguardHostProbe.shouldProbe(
                SceneSource.Surface.KEYGUARD,
            ),
        )
        assertFalse(
            KeyguardHostProbe.shouldProbe(
                SceneSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
        assertFalse(
            KeyguardHostProbe.shouldProbe(
                SceneSource.Surface.SHADE_LOCKED,
            ),
        )
    }

    @Test
    fun hostGuardAcceptsOnlyPinnedMiuiKeyguardHost() {
        assertTrue(
            KeyguardHostProbe.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertFalse(
            KeyguardHostProbe.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            ),
        )
    }

    @Test
    fun sampleFreezesOnlyAfterPositiveReadyTopology() {
        assertTrue(
            KeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertTrue(
            KeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            KeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 0,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            KeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = false,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            KeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 0,
            ),
        )
    }
}
