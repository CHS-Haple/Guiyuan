package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardHostProbeTest {
    @Test
    fun probeRunsOnlyForKeyguardSurface() {
        assertTrue(
            SystemUiKeyguardHostProbe.shouldProbe(
                SysUiSceneSource.Surface.KEYGUARD,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldProbe(
                SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldProbe(
                SysUiSceneSource.Surface.SHADE_LOCKED,
            ),
        )
    }

    @Test
    fun hostGuardAcceptsOnlyPinnedMiuiKeyguardHost() {
        assertTrue(
            SystemUiKeyguardHostProbe.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            ),
        )
    }

    @Test
    fun sampleFreezesOnlyAfterPositiveReadyTopology() {
        assertTrue(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertTrue(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 0,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = false,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 0,
            ),
        )
    }
}
