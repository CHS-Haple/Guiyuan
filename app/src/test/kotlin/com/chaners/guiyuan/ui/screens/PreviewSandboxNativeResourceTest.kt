package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSandboxNativeResourceTest {
    @Test
    fun noInternetWifiUsesExactHyperOsUnavailableFamily() {
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
    fun dualWifiPreviewUsesNativeSlaveWifiResourceFamily() {
        for (level in 0..3) {
            assertEquals(
                listOf("stat_sys_slave_wifi_signal_$level"),
                previewWifiResourceNames(PreviewWifiState.DUAL_WIFI, level),
            )
        }
        assertFalse(
            previewWifiResourceNames(PreviewWifiState.DUAL_WIFI, 3)
                .contains("stat_sys_wifi_signal_3"),
        )
    }

    @Test
    fun previewNetworkActivityUsesNativeTransportResourceFamilies() {
        val wifi = PreviewCenterSource.WIFI
        val mobile = PreviewCenterSource.MOBILE
        assertEquals(listOf("stat_sys_wifi_out"),
            previewNetworkActivityResourceNames(wifi, PreviewNetworkActivity.UPLOAD))
        assertEquals(listOf("stat_sys_wifi_in"),
            previewNetworkActivityResourceNames(wifi, PreviewNetworkActivity.DOWNLOAD))
        assertEquals(listOf("stat_sys_wifi_inout"),
            previewNetworkActivityResourceNames(wifi, PreviewNetworkActivity.BOTH))
        assertEquals(listOf("stat_sys_signal_out_left", "stat_sys_signal_out"),
            previewNetworkActivityResourceNames(mobile, PreviewNetworkActivity.UPLOAD))
        assertEquals(listOf("stat_sys_signal_in_left", "stat_sys_signal_in"),
            previewNetworkActivityResourceNames(mobile, PreviewNetworkActivity.DOWNLOAD))
        assertEquals(listOf("stat_sys_signal_inout_left", "stat_sys_signal_inout"),
            previewNetworkActivityResourceNames(mobile, PreviewNetworkActivity.BOTH))
        assertTrue(previewNetworkActivityResourceNames(wifi, PreviewNetworkActivity.IDLE).isEmpty())
        assertEquals(
            listOf("stat_sys_signal_data_left"),
            previewNetworkActivityResourceNames(mobile, PreviewNetworkActivity.IDLE),
        )
        assertTrue(previewNetworkActivityResourceNames(PreviewCenterSource.AIRPLANE,
            PreviewNetworkActivity.BOTH).isEmpty())
        assertEquals(PreviewNetworkActivity.BOTH, PreviewSandboxUiState().networkActivity)
    }

    @Test
    fun previewChargingUsesNativeHollowBatteryResourceFamilies() {
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
    fun wifiRemainsCenterAuthorityWhileAirplaneAndNoSimAreActive() {
        val state =
            PreviewSandboxUiState(
                simCards = PreviewSimCards.NONE,
                airplaneMode = true,
                networkMode = PreviewNetworkMode.WIFI,
            )

        assertEquals(PreviewCenterSource.WIFI, state.previewCenterSource())
    }

    @Test
    fun airplaneOverridesNoSimForMobileCenter() {
        val state =
            PreviewSandboxUiState(
                simCards = PreviewSimCards.NONE,
                airplaneMode = true,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertEquals(PreviewCenterSource.AIRPLANE, state.previewCenterSource())
    }

    @Test
    fun noSimOwnsMobileCenterWhenAirplaneIsOff() {
        val state =
            PreviewSandboxUiState(
                simCards = PreviewSimCards.NONE,
                airplaneMode = false,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertEquals(PreviewCenterSource.NO_SIM, state.previewCenterSource())
    }
}
