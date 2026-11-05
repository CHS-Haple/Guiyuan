package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.xposed.DualMobileLevels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSandboxPolicyTest {
    @Test
    fun simPresenceDrivesPhysicalRowsWithoutChangingSignalStyle() {
        val basic = PreviewSandboxUiState(networkMode = PreviewNetworkMode.MOBILE)
        assertEquals(PreviewSimCards.SIM1_ONLY, basic.simCards)
        assertEquals(null, basic.dualLevels)
        assertTrue(basic.sim1Active)
        assertFalse(basic.sim2Active)
        val both = basic.copy(simCards = PreviewSimCards.BOTH, mobileSignalLevel = 3, sim2SignalLevel = 1)
        assertEquals(DualMobileLevels(3, 1), both.dualLevels)
        assertEquals(null, both.copy(airplaneMode = true).dualLevels)
        assertEquals(PreviewCenterSource.AIRPLANE, both.copy(airplaneMode = true).previewCenterSource())
    }

    @Test
    fun dataSimChoiceOnlyAppliesWhenBothCardsArePresent() {
        val both = PreviewSandboxUiState(
            networkMode = PreviewNetworkMode.MOBILE,
            simCards = PreviewSimCards.BOTH,
            mobileNetwork = PreviewMobileNetwork.FIVE_G,
            sim2MobileNetwork = PreviewMobileNetwork.FOUR_G,
            mobileSignalLevel = 4,
            sim2SignalLevel = 1,
        )
        val sim2Data = both.copy(dataSim = PreviewDataSim.SIM2)
        assertEquals(PreviewMobileNetwork.FIVE_G, both.dataMobileNetwork)
        assertEquals(PreviewMobileNetwork.FOUR_G, sim2Data.dataMobileNetwork)
        assertEquals(4, both.dataSignalLevel)
        assertEquals(1, sim2Data.dataSignalLevel)
        assertEquals(DualMobileLevels(4, 1), sim2Data.dualLevels)

        val sim1Only = sim2Data.copy(simCards = PreviewSimCards.SIM1_ONLY)
        assertEquals(PreviewDataSim.SIM1, sim1Only.effectiveDataSim)
        assertEquals(PreviewMobileNetwork.FIVE_G, sim1Only.dataMobileNetwork)
        assertEquals(4, sim1Only.displayedSignalLevel)
        assertEquals(PreviewCenterSource.MOBILE, sim1Only.previewCenterSource())
        assertEquals(null, sim1Only.dualLevels)

        val sim2Only = both.copy(simCards = PreviewSimCards.SIM2_ONLY)
        assertEquals(PreviewDataSim.SIM2, sim2Only.effectiveDataSim)
        assertEquals(PreviewMobileNetwork.FOUR_G, sim2Only.dataMobileNetwork)
        assertEquals(1, sim2Only.displayedSignalLevel)
        assertEquals(PreviewCenterSource.MOBILE, sim2Only.previewCenterSource())
        assertEquals(null, sim2Only.dualLevels)

        val neither = both.copy(simCards = PreviewSimCards.NONE)
        assertEquals(null, neither.effectiveDataSim)
        assertFalse(neither.hasSim)
        assertFalse(neither.mobileControlsEnabled)
        assertEquals(PreviewCenterSource.NO_SIM, neither.previewCenterSource())
        assertEquals(PreviewCenterSource.WIFI,
            neither.copy(networkMode = PreviewNetworkMode.WIFI).previewCenterSource())
    }

    @Test
    fun networkTabsKeepSavedIndicesAndShowNoneFirst() {
        assertEquals(0, PreviewNetworkMode.MOBILE.ordinal)
        assertEquals(1, PreviewNetworkMode.WIFI.ordinal)
        assertEquals(2, PreviewNetworkMode.NONE.ordinal)
        assertEquals(
            listOf(PreviewNetworkMode.NONE, PreviewNetworkMode.MOBILE, PreviewNetworkMode.WIFI),
            previewNetworkTabs,
        )
    }

    @Test
    fun noNetworkStillShowsSimSignalWithoutDataConnection() {
        val state = PreviewSandboxUiState(networkMode = PreviewNetworkMode.NONE)
        assertEquals(PreviewCenterSource.EMPTY, state.previewCenterSource())
        assertTrue(state.mobileControlsEnabled)
        assertFalse(state.mobileOptionsVisible)
        assertTrue(state.copy(networkMode = PreviewNetworkMode.WIFI).mobileControlsEnabled)
        assertTrue(state.copy(networkMode = PreviewNetworkMode.MOBILE).mobileOptionsVisible)
    }

    @Test
    fun noNetworkRetainsAirplaneAndNoSimPriority() {
        val state = PreviewSandboxUiState(networkMode = PreviewNetworkMode.NONE)
        assertEquals(
            PreviewCenterSource.NO_SIM,
            state.copy(simCards = PreviewSimCards.NONE).previewCenterSource(),
        )
        assertEquals(
            PreviewCenterSource.AIRPLANE,
            state.copy(airplaneMode = true).previewCenterSource(),
        )
        assertFalse(state.copy(simCards = PreviewSimCards.NONE).mobileControlsEnabled)
        assertFalse(state.copy(airplaneMode = true).mobileControlsEnabled)
    }

    @Test
    fun mobileNetworkSandboxCoversCommonNativeLabelsWithoutChangingLegacyOrdinals() {
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
    fun noSimRemainsValidWithWifiSelected() {
        val state =
            PreviewSandboxUiState(
                simCards = PreviewSimCards.NONE,
                networkMode = PreviewNetworkMode.WIFI,
            )

        assertFalse(state.mobileOptionsVisible)
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

    @Test
    fun mobileSubordinateOptionsFoldWhenUnavailable() {
        val ready =
            PreviewSandboxUiState(
                simCards = PreviewSimCards.SIM1_ONLY,
                airplaneMode = false,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertTrue(ready.mobileOptionsVisible)
        assertFalse(ready.copy(simCards = PreviewSimCards.NONE).mobileOptionsVisible)
        assertFalse(ready.copy(airplaneMode = true).mobileOptionsVisible)
    }
}
