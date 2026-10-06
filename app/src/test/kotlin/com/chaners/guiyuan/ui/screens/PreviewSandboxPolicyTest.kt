package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSandboxPolicyTest {
    @Test
    fun mobileSandboxKeepsLegacyOrdinals() {
        assertEquals(0, PreviewMobileNetwork.NONE.ordinal)
        assertEquals(1, PreviewMobileNetwork.FOUR_G.ordinal)
        assertEquals(2, PreviewMobileNetwork.FIVE_G.ordinal)
        assertEquals(3, PreviewMobileNetwork.FIVE_GA.ordinal)

        assertEquals("", PreviewMobileNetwork.NONE.systemLabel)
        assertEquals("2G", PreviewMobileNetwork.TWO_G.systemLabel)
        assertEquals("E", PreviewMobileNetwork.EDGE.systemLabel)
        assertEquals("3G", PreviewMobileNetwork.THREE_G.systemLabel)
        assertEquals("H+", PreviewMobileNetwork.H_PLUS.systemLabel)
        assertEquals("4G", PreviewMobileNetwork.FOUR_G.systemLabel)
        assertEquals("LTE", PreviewMobileNetwork.LTE.systemLabel)
        assertEquals("5G", PreviewMobileNetwork.FIVE_G.systemLabel)
        assertEquals("5G-A", PreviewMobileNetwork.FIVE_GA.systemLabel)
    }

    @Test
    fun noInternetWifiUsesNativeFamily() {
        val names = previewWifiResourceNames(
            state = PreviewWifiState.NO_INTERNET,
            level = 3,
        )

        assertEquals(
            listOf("stat_sys_wifi_signal_unavailable_3"),
            names,
        )
        assertFalse(names.contains("stat_sys_wifi_signal_3"))
    }


    @Test
    fun previewChargingUsesNativeBattery() {
        assertEquals(
            listOf(
                "hollow_battery_meter_charging",
                "tiny_battery_charging",
            ),
            previewChargingResourceNames(PreviewChargingState.CHARGING),
        )
        assertEquals(
            listOf(
                "hollow_battery_meter_quick_charging",
                "tiny_battery_quick_charging",
            ),
            previewChargingResourceNames(PreviewChargingState.SUPER_FAST_CHARGING),
        )
        assertTrue(
            previewChargingResourceNames(PreviewChargingState.NOT_CHARGING).isEmpty(),
        )
    }

    @Test
    fun noSimRemainsValidWithWifiSelected() {
        val state =
            PreviewSandboxUiState(
                simPresent = false,
                networkMode = PreviewNetworkMode.WIFI,
            )

        assertEquals(PreviewNetworkMode.WIFI, state.networkMode)
        assertFalse(state.mobileOptionsVisible)
    }

    @Test
    fun wifiKeepsCenterAuthority() {
        val state =
            PreviewSandboxUiState(
                simPresent = false,
                airplaneMode = true,
                networkMode = PreviewNetworkMode.WIFI,
            )

        assertEquals(PreviewCenterSource.WIFI, state.previewCenterSource())
    }

    @Test
    fun airplaneOverridesNoSimForMobileCenter() {
        val state =
            PreviewSandboxUiState(
                simPresent = false,
                airplaneMode = true,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertEquals(PreviewCenterSource.AIRPLANE, state.previewCenterSource())
    }

    @Test
    fun noSimOwnsMobileCenterWhenAirplaneIsOff() {
        val state =
            PreviewSandboxUiState(
                simPresent = false,
                airplaneMode = false,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertEquals(PreviewCenterSource.NO_SIM, state.previewCenterSource())
    }

    @Test
    fun mobileOptionsFoldWhenUnavailable() {
        val ready =
            PreviewSandboxUiState(
                simPresent = true,
                airplaneMode = false,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertTrue(ready.mobileOptionsVisible)
        assertFalse(ready.copy(simPresent = false).mobileOptionsVisible)
        assertFalse(ready.copy(airplaneMode = true).mobileOptionsVisible)
    }
}
