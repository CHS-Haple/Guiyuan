package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiKeyguardHostProbeTest {
    @Test
    fun probeRunsOnlyForKeyguardSurface() {
        assertTrue(
            SysUiKeyguardHostProbe.shouldProbe(
                SysUiSceneSource.Surface.KEYGUARD,
            ),
        )
        assertFalse(
            SysUiKeyguardHostProbe.shouldProbe(
                SysUiSceneSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
        assertFalse(
            SysUiKeyguardHostProbe.shouldProbe(
                SysUiSceneSource.Surface.SHADE_LOCKED,
            ),
        )
    }

    @Test
    fun hostGuardAcceptsOnlyPinnedMiuiKeyguardHost() {
        assertTrue(
            SysUiKeyguardHostProbe.isHostClassName(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertFalse(
            SysUiKeyguardHostProbe.isHostClassName(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            ),
        )
    }

    @Test
    fun sampleFreezesOnlyAfterPositiveReadyTopology() {
        assertTrue(
            SysUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertTrue(
            SysUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            SysUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 0,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            SysUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = false,
                batteryCarrierWidthPx = 105,
            ),
        )
        assertFalse(
            SysUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 0,
            ),
        )
    }
}
