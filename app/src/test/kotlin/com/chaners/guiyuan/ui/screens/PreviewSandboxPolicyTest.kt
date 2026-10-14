package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.xposed.DualMobileLevels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSandboxPolicyTest {
    @Test
    fun dualSignalPreviewIsOptInAndPreservesBothLevels() {
        val basic = PreviewSandboxUiState()
        assertEquals(false, basic.dualMobile)
        val dual = basic.copy(dualMobile = true, mobileSignalLevel = 3, sim2SignalLevel = 1)
        assertEquals(true, dual.dualMobile)
        assertEquals(3, dual.mobileSignalLevel)
        assertEquals(1, dual.sim2SignalLevel)
    }


    @Test
    fun changingDualDataSimOnlyChangesCenterSourceNotPhysicalRows() {
        val sim1Data = PreviewSandboxUiState(
            networkMode = PreviewNetworkMode.MOBILE,
            dualMobile = true,
            mobileNetwork = PreviewMobileNetwork.FIVE_G,
            sim2MobileNetwork = PreviewMobileNetwork.FOUR_G,
            mobileSignalLevel = 4,
            sim2SignalLevel = 1,
        )
        val sim2Data = sim1Data.copy(dataSim = PreviewDataSim.SIM2)

        assertEquals(PreviewMobileNetwork.FIVE_G, sim1Data.dataMobileNetwork)
        assertEquals(PreviewMobileNetwork.FOUR_G, sim2Data.dataMobileNetwork)
        assertEquals(4, sim1Data.dataSignalLevel)
        assertEquals(1, sim2Data.dataSignalLevel)
        assertEquals(4, sim2Data.mobileSignalLevel)
        assertEquals(1, sim2Data.sim2SignalLevel)
        assertEquals(PreviewCenterSource.MOBILE, sim2Data.previewCenterSource())
        assertEquals(
            PreviewCenterSource.EMPTY,
            sim2Data.copy(sim2MobileNetwork = PreviewMobileNetwork.NONE)
                .previewCenterSource(),
        )

        val single = sim2Data.copy(dualMobile = false)
        assertEquals(PreviewMobileNetwork.FIVE_G, single.dataMobileNetwork)
        assertEquals(4, single.dataSignalLevel)
        assertEquals(PreviewCenterSource.MOBILE, single.previewCenterSource())
    }

    @Test
    fun dualCardPresenceUsesOnlyAvailablePhysicalSignals() {
        val both =
            PreviewSandboxUiState(
                networkMode = PreviewNetworkMode.MOBILE,
                dualMobile = true,
                mobileNetwork = PreviewMobileNetwork.FIVE_G,
                sim2MobileNetwork = PreviewMobileNetwork.FOUR_G,
                mobileSignalLevel = 4,
                sim2SignalLevel = 1,
            )
        assertEquals(0, PreviewSimCards.BOTH.ordinal)
        assertEquals(1, PreviewSimCards.SIM1_ONLY.ordinal)
        assertEquals(2, PreviewSimCards.SIM2_ONLY.ordinal)
        assertEquals(3, PreviewSimCards.NONE.ordinal)
        assertEquals(DualMobileLevels(4, 1), both.dualLevels)
        assertEquals(PreviewCenterSource.MOBILE, both.previewCenterSource())
        // The previous single-card switch is independent from dual insertion simulation.
        assertEquals(
            PreviewCenterSource.MOBILE,
            both.copy(simPresent = false).previewCenterSource(),
        )

        val sim1Only = both.copy(simCards = PreviewSimCards.SIM1_ONLY)
        assertEquals(null, sim1Only.dualLevels)
        assertEquals(4, sim1Only.displayedSignalLevel)
        assertEquals(PreviewMobileNetwork.FIVE_G, sim1Only.dataMobileNetwork)
        assertEquals(PreviewCenterSource.MOBILE, sim1Only.previewCenterSource())
        val missingDataSim = sim1Only.copy(dataSim = PreviewDataSim.SIM2)
        assertEquals(4, missingDataSim.displayedSignalLevel)
        assertEquals(PreviewMobileNetwork.NONE, missingDataSim.dataMobileNetwork)
        assertEquals(PreviewCenterSource.EMPTY, missingDataSim.previewCenterSource())

        val sim2Only = both.copy(simCards = PreviewSimCards.SIM2_ONLY)
        assertEquals(null, sim2Only.dualLevels)
        assertEquals(1, sim2Only.displayedSignalLevel)
        assertEquals(PreviewCenterSource.EMPTY, sim2Only.previewCenterSource())
        val sim2Data = sim2Only.copy(dataSim = PreviewDataSim.SIM2)
        assertEquals(PreviewMobileNetwork.FOUR_G, sim2Data.dataMobileNetwork)
        assertEquals(PreviewCenterSource.MOBILE, sim2Data.previewCenterSource())

        val neither = both.copy(simCards = PreviewSimCards.NONE)
        assertEquals(null, neither.dualLevels)
        assertFalse(neither.hasSim)
        assertFalse(neither.mobileControlsEnabled)
        assertEquals(PreviewCenterSource.NO_SIM, neither.previewCenterSource())
        assertEquals(
            PreviewCenterSource.WIFI,
            neither.copy(networkMode = PreviewNetworkMode.WIFI).previewCenterSource(),
        )

        // Sandbox-only card combinations never modify the saved single-card scenario.
        val single = neither.copy(dualMobile = false)
        assertTrue(single.sim1Active)
        assertFalse(single.sim2Active)
        assertTrue(single.mobileControlsEnabled)
        assertEquals(null, single.dualLevels)
        assertEquals(4, single.displayedSignalLevel)
        assertEquals(PreviewCenterSource.MOBILE, single.previewCenterSource())
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
            state.copy(simPresent = false).previewCenterSource(),
        )
        assertEquals(
            PreviewCenterSource.AIRPLANE,
            state.copy(airplaneMode = true).previewCenterSource(),
        )
        assertFalse(state.copy(simPresent = false).mobileControlsEnabled)
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
                simPresent = false,
                networkMode = PreviewNetworkMode.WIFI,
            )

        assertFalse(state.mobileOptionsVisible)
    }

    @Test
    fun wifiRemainsCenterAuthorityWhileAirplaneAndNoSimAreActive() {
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
    fun mobileSubordinateOptionsFoldWhenUnavailable() {
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
