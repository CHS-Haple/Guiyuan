package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysUiKeyguardHostProbeTest {
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
