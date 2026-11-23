package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSandboxStatePersistenceTest {
    @Test
    fun defaultValuesRoundTripThroughSaveablePayload() {
        val initial = PreviewSandboxUiState()
        assertEquals(14, encodePreviewSelection(initial).size)
        assertEquals(initial, decodePreviewSelection(encodePreviewSelection(initial)))
    }

    @Test
    fun allEditedFieldsRoundTripWithoutChangingOrdinalsOrSettings() {
        val selected = PreviewSandboxUiState(
            airplaneMode = true,
            networkMode = PreviewNetworkMode.MOBILE,
            mobileNetwork = PreviewMobileNetwork.H_PLUS,
            mobileSignalLevel = 0,
            sim2SignalLevel = 1,
            sim2MobileNetwork = PreviewMobileNetwork.FIVE_GA,
            dataSim = PreviewDataSim.SIM2,
            simCards = PreviewSimCards.SIM2_ONLY,
            wifiState = PreviewWifiState.DUAL_WIFI,
            wifiSignalLevel = 2,
            networkActivity = PreviewNetworkActivity.IDLE,
            batteryPercent = 31,
            batteryMode = PreviewBatteryMode.SUPER_POWER_SAVE,
            chargingState = PreviewChargingState.SUPER_FAST_CHARGING,
        )
        assertEquals(selected, decodePreviewSelection(encodePreviewSelection(selected)))
    }

    @Test
    fun malformedOrIncompleteSavedSnapshotDoesNotRestore() {
        assertNull(decodePreviewSelection(emptyList()))
        assertNull(decodePreviewSelection(listOf(true, 1)))
        val incorrectType = encodePreviewSelection(PreviewSandboxUiState()).toMutableList()
        incorrectType[8] = "UNKNOWN"
        assertNull(decodePreviewSelection(incorrectType))
    }

    @Test
    fun futureOrdinalIsClampedLikeOriginalRememberSaveableRestoration() {
        val fields = encodePreviewSelection(PreviewSandboxUiState()).toMutableList()
        fields[1] = Int.MAX_VALUE
        fields[2] = -20
        val restored = requireNotNull(decodePreviewSelection(fields))
        assertEquals(PreviewNetworkMode.entries.last(), restored.networkMode)
        assertEquals(PreviewMobileNetwork.entries.first(), restored.mobileNetwork)
    }

    @Test
    fun sim1OnlyWithoutMobileTypeAffectsDisplayButNotSavedSelection() {
        val selection = PreviewSandboxUiState(
            networkMode = PreviewNetworkMode.MOBILE,
            simCards = PreviewSimCards.SIM1_ONLY,
            mobileNetwork = PreviewMobileNetwork.NONE,
        )
        assertEquals(PreviewNetworkMode.NONE, selection.previewForPresentation().networkMode)
        assertEquals(PreviewNetworkMode.MOBILE, selection.networkMode)
        assertEquals(selection, decodePreviewSelection(encodePreviewSelection(selection)))
        assertTrue(selection.previewForPresentation() !== selection)
        assertEquals(PreviewNetworkMode.MOBILE, selection.copy(simCards = PreviewSimCards.BOTH).previewForPresentation().networkMode)
    }

    @Test
    fun selectingMobileRestoresDefaultTypeInSingleStateWrite() {
        val selection = PreviewSandboxUiState(
            networkMode = PreviewNetworkMode.WIFI,
            mobileNetwork = PreviewMobileNetwork.NONE,
        )
        val mobile = selection.selectPreviewNetworkMode(PreviewNetworkMode.MOBILE)
        assertEquals(PreviewNetworkMode.MOBILE, mobile.networkMode)
        assertEquals(PreviewMobileNetwork.FIVE_G, mobile.mobileNetwork)
        assertEquals(selection.mobileNetwork, selection.selectPreviewNetworkMode(PreviewNetworkMode.WIFI).mobileNetwork)
    }
}
