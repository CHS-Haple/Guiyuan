package com.chaners.guiyuan.sandbox

import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewDataSim
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewSimCards
import com.chaners.guiyuan.ui.screens.PreviewWifiState
import org.junit.Assert.*
import org.junit.Test

class SandboxLeaseTest {
    private val state = PreviewSandboxUiState(
        simPresent = false,
        airplaneMode = true,
        networkMode = PreviewNetworkMode.MOBILE,
        mobileNetwork = PreviewMobileNetwork.FIVE_GA,
        mobileSignalLevel = 2,
        dualMobile = true,
        sim2SignalLevel = 1,
        sim2MobileNetwork = PreviewMobileNetwork.LTE,
        dataSim = PreviewDataSim.SIM2,
        simCards = PreviewSimCards.SIM2_ONLY,
        wifiState = PreviewWifiState.HOTSPOT,
        wifiSignalLevel = 1,
        batteryPercent = 13,
        batteryMode = PreviewBatteryMode.POWER_SAVE,
        chargingState = PreviewChargingState.SUPER_FAST_CHARGING,
    )
    private val lease = SandboxLease(13, 10_000, 1_000_000, state)

    @Test fun fullStateRoundTrips() {
        assertEquals(lease, SandboxLease.decode(lease.encode()))
    }

    @Test fun expiryAndClockResetRejectFrames() {
        assertTrue(lease.valid(12_999, 1_002_999))
        assertFalse(lease.valid(13_000, 1_002_000))
        assertFalse(lease.valid(12_000, 1_003_000))
        assertFalse(lease.valid(5_000, 1_000_500))
        assertFalse(lease.valid(10_500, 900_000))
        assertFalse(lease.copy(id = 0).valid(10_500, 1_000_500))
    }

    @Test fun invalidFieldsNeverReachSystemUi() {
        val raw = lease.encode()
        assertNull(SandboxLease.decode(null))
        assertNull(SandboxLease.decode(""))
        assertNull(SandboxLease.decode(raw.replace("FIVE_GA", "FAKE_5GA")))
        assertNull(SandboxLease.decode(raw.replace("|13|POWER_SAVE|", "|101|POWER_SAVE|")))
        assertNull(SandboxLease.decode(raw.replace("|2|1|LTE|", "|2|7|LTE|")))
        assertNull(SandboxLease.decode(raw.replace("|1|LTE|", "|x|LTE|")))
        assertNull(SandboxLease.decode(raw.replace("2|13|", "1|13|")))
        assertNull(SandboxLease.decode("x".repeat(401)))
    }
}
