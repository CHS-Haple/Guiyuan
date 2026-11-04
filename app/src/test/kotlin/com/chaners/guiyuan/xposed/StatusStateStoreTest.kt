package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStateStoreTest {
    @Test
    fun nativeActivityStateFollowsItsTransport() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateWifi(StatusStateStore.WifiState.Visible(
            iconResId = 100, signal = com.chaners.guiyuan.xposed.network.SignalStrength.Level(3),
        ))
        StatusStateStore.updateWifiActivity(120)
        StatusStateStore.updateMobileActivity(1, 220)
        StatusStateStore.updateMobileActivity(2, 0)
        assertEquals(120, StatusStateStore.snapshot().wifiActivityResId)
        StatusStateStore.updateWifiActivity(null)
        assertNull(StatusStateStore.snapshot().wifiActivityResId)
        StatusStateStore.updateWifiActivity(120)
        assertEquals(120, StatusStateStore.snapshot().wifiActivityResId)
        assertEquals(220, StatusStateStore.snapshot().mobileActivityResIds[1])
        assertEquals(null, StatusStateStore.snapshot().mobileActivityResIds[2])
        StatusStateStore.updateMobileActivity(1, null)
        assertNull(StatusStateStore.snapshot().mobileActivityResIds[1])
        StatusStateStore.updateMobileActivity(1, 220)
        assertEquals(220, StatusStateStore.snapshot().mobileActivityResIds[1])

        StatusStateStore.updateWifi(StatusStateStore.WifiState.Hidden)
        assertEquals(null, StatusStateStore.snapshot().wifiActivityResId)
        StatusStateStore.updateMobileActivity(1, null)
        assertEquals(null, StatusStateStore.snapshot().mobileActivityResIds[1])
        StatusStateStore.restoreHotReloadState(null)
    }


    @Test
    fun newerActivityClearWinsOverOldHotReloadTransfer() {
        StatusStateStore.restoreHotReloadState(null)
        val wifi = StatusStateStore.WifiState.Visible(
            iconResId = 100, signal = SignalStrength.Level(3),
        )
        StatusStateStore.updateWifi(wifi)
        StatusStateStore.updateWifiActivity(120)
        StatusStateStore.updateMobileActivity(1, 220)
        StatusStateStore.updateMobileActivity(2, 230)
        val previous = StatusStateStore.exportHotReloadState()

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateWifi(wifi)
        // The new collectors have already observed no drawable. They should not
        // need a value change to override the outgoing generation's saved icons.
        assertNull(StatusStateStore.updateWifiActivity(null))
        assertNull(StatusStateStore.updateMobileActivity(1, null))
        StatusStateStore.updateMobileActivity(2, 240)
        val merged = StatusStateStore.restoreHotReloadState(previous)
        assertNull(merged.wifiActivityResId)
        assertNull(merged.mobileActivityResIds[1])
        assertEquals(240, merged.mobileActivityResIds[2])
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun missingFreshActivityObservationRetainsPreviousHotReloadState() {
        StatusStateStore.restoreHotReloadState(null)
        val wifi = StatusStateStore.WifiState.Visible(
            iconResId = 100, signal = SignalStrength.Level(3),
        )
        StatusStateStore.updateWifi(wifi)
        StatusStateStore.updateWifiActivity(120)
        StatusStateStore.updateMobileActivity(1, 220)
        val previous = StatusStateStore.exportHotReloadState()

        StatusStateStore.restoreHotReloadState(null)
        val restored = StatusStateStore.restoreHotReloadState(previous)
        assertEquals(120, restored.wifiActivityResId)
        assertEquals(220, restored.mobileActivityResIds[1])
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun hiddenWifiCannotRetainAnActivityTagAcrossReappearance() {
        StatusStateStore.restoreHotReloadState(null)
        val wifi = StatusStateStore.WifiState.Visible(
            iconResId = 100, signal = SignalStrength.Level(3),
        )
        StatusStateStore.updateWifi(wifi)
        StatusStateStore.updateWifiActivity(120)
        StatusStateStore.updateWifi(StatusStateStore.WifiState.Hidden)
        assertNull(StatusStateStore.updateWifiActivity(120))
        StatusStateStore.updateWifi(wifi)
        assertNull(StatusStateStore.snapshot().wifiActivityResId)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun freshAirplaneModeDoesNotRestoreOldMobileTraffic() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(false)
        StatusStateStore.updateMobileActivity(1, 220)
        val old = StatusStateStore.exportHotReloadState()

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        val restored = StatusStateStore.restoreHotReloadState(old)
        assertEquals(true, restored.airplaneMode)
        assertTrue(restored.mobileActivityResIds.isEmpty())
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun airplaneExitStartsFreshMobileRecoveryAndClearsCachedSignal() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateMobileSignal(
            subscriptionId = 4,
            signal = SignalStrength.Level(4),
        )

        val snapshot =
            StatusStateStore.updateAirplaneMode(false)
                ?: error("expected airplane transition")

        assertTrue(snapshot.mobileRecoveryPending)
        assertTrue(snapshot.mobile[4]?.signal is SignalStrength.Unknown)
    }

    @Test
    fun unavailableSignalDoesNotFinishAirplaneRecovery() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        StatusStateStore.updateMobileSignal(
            subscriptionId = 4,
            signal = SignalStrength.Unavailable,
        )

        val completed =
            StatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = 4,
                mobileTypeReady = true,
                mobileDataEnabled = true,
            )

        assertNull(completed)
        assertTrue(StatusStateStore.snapshot().mobileRecoveryPending)
    }

    @Test
    fun batterySnapshotRetainsNativeSemanticStateAndColor() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(
                percent = 61,
                charging = false,
                semanticState = BatterySemanticState.PERFORMANCE,
                systemSemanticColor = 0xff3482ff.toInt(),
            ),
        )

        val restored =
            StatusStateStore.snapshot().battery
                ?: error("expected battery state")

        assertEquals(61, restored.percent)
        assertFalse(restored.charging)
        assertEquals(BatterySemanticState.PERFORMANCE, restored.semanticState)
        assertEquals(0xff3482ff.toInt(), restored.systemSemanticColor)
    }

    @Test
    fun invalidChargingIconDoesNotClearCurrentIcon() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(
                percent = 80,
                charging = true,
                chargingIconResId = 7,
            ),
        )

        assertNull(StatusStateStore.updateBatteryChargingIcon(0))
        assertEquals(7, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun chargingSampleWithoutIconKeepsCommittedGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )

        assertNull(
            StatusStateStore.updateBattery(
                StatusStateStore.BatteryState(percent = 80, charging = true),
            ),
        )
        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun freshGlyphReplacesPreviousResource() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 7),
        )
        StatusStateStore.updateBatteryChargingIcon(42)

        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun freshBatterySampleReplacesCachedGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 7),
        )
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )

        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun chargingExitClearsGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = false),
        )

        assertNull(StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun chargingStateRefreshKeepsLastGlyph() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 80, charging = true, chargingIconResId = 42),
        )
        StatusStateStore.updateBattery(
            StatusStateStore.BatteryState(percent = 81, charging = true),
        )

        assertEquals(81, StatusStateStore.snapshot().battery?.percent)
        assertEquals(42, StatusStateStore.snapshot().battery?.chargingIconResId)
    }

    @Test
    fun freshSignalAndMobileTypeFinishAirplaneRecovery() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        StatusStateStore.updateMobileSignal(
            subscriptionId = 4,
            signal = SignalStrength.Level(3),
        )

        val completed =
            StatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = 4,
                mobileTypeReady = true,
                mobileDataEnabled = true,
            ) ?: error("expected recovery completion")

        assertFalse(completed.mobileRecoveryPending)
    }
}
