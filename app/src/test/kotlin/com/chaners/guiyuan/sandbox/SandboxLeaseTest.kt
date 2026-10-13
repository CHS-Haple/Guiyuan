package com.chaners.guiyuan.sandbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxLeaseTest {
    private val full =
        PreviewSandboxUiState(
            simPresent = false,
            airplaneMode = true,
            networkMode = PreviewNetworkMode.MOBILE,
            mobileNetwork = PreviewMobileNetwork.FIVE_GA,
            mobileSignalLevel = 2,
            wifiState = PreviewWifiState.HOTSPOT,
            wifiSignalLevel = 1,
            batteryPercent = 13,
            batteryMode = PreviewBatteryMode.POWER_SAVE,
            chargingState = PreviewChargingState.SUPER_FAST_CHARGING,
        )

    @Test
    fun stateRoundTripsWithoutChangingSandboxControls() {
        val lease = SandboxLease(13L, 10_000L, 1_000_000L, full)
        assertEquals(lease, SandboxLease.decode(lease.encode()))
    }

    @Test
    fun staleAndCrossBootFramesNeverRemainActive() {
        val lease = SandboxLease(13L, 10_000L, 1_000_000L, full)
        assertTrue(lease.valid(12_999L, 1_002_999L))
        assertFalse(lease.valid(13_000L, 1_002_000L))
        assertFalse(lease.valid(12_000L, 1_003_000L))
        assertFalse(lease.valid(5_000L, 1_000_500L))
        assertFalse(lease.valid(10_500L, 900_000L))
        assertFalse(lease.copy(id = 0).valid(10_500L, 1_000_500L))
    }

    @Test
    fun malformedAndOutOfRangeFramesFailClosed() {
        val valid = SandboxLease(13L, 10_000L, 1_000_000L, full).encode()
        assertNull(SandboxLease.decode(null))
        assertNull(SandboxLease.decode(""))
        assertNull(SandboxLease.decode(valid.replace("FIVE_GA", "UNRECOGNIZED")))
        assertNull(SandboxLease.decode(valid.replace("|13|POWER_SAVE|", "|200|POWER_SAVE|")))
        assertNull(SandboxLease.decode(valid.replace("|0|1|MOBILE|", "|X|1|MOBILE|")))
    }
}
