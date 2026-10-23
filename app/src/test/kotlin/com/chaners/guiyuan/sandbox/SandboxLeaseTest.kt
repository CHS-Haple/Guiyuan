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

    @Test fun dualPreviewSurvivesFirstLeaseBeforeAnyUpdates() {
        val selected = PreviewSandboxUiState(
            networkMode = PreviewNetworkMode.WIFI,
            dualMobile = true,
            simCards = PreviewSimCards.BOTH,
            mobileSignalLevel = 3,
            sim2SignalLevel = 1,
        )
        val received = SandboxLease(21, 10_000, 1_000_000, selected)
            .encode()
            .let(SandboxLease::decode)
        assertNotNull(received)
        assertTrue(received!!.state.dualMobile)
        assertEquals(com.chaners.guiyuan.xposed.DualMobileLevels(3, 1), received.state.dualLevels)
        assertEquals(PreviewNetworkMode.WIFI, received.state.networkMode)
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
        val malformedSignal = raw.split('|').toMutableList()
        malformedSignal[10] = "7" // SIM2 level, not the dual-SIM enabled flag.
        assertNull(SandboxLease.decode(malformedSignal.joinToString("|")))
        assertNull(SandboxLease.decode(raw.replace("|1|LTE|", "|x|LTE|")))
        assertNull(SandboxLease.decode(raw.replace("2|13|", "1|13|")))
        assertNull(SandboxLease.decode("x".repeat(401)))
    }
}
