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
    fun airplaneExitStartsFreshMobileRecoveryAndClearsCachedSignal() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateMobile(
            StatusStateStore.MobileIconUpdate(
                subscriptionId = 4,
                kind = StatusStateStore.MobileIconKind.SIGNAL,
                resourceId = 1,
                signal = SignalStrength.Level(4),
            ),
        )

        val snapshot =
            StatusStateStore.updateAirplaneMode(false)
                ?: error("expected airplane transition")

        assertTrue(snapshot.mobileRecoveryPending)
        assertTrue(snapshot.mobile[4]?.signal is SignalStrength.Unknown)
        assertNull(snapshot.mobile[4]?.signalResId)
    }

    @Test
    fun unavailableSignalDoesNotFinishAirplaneRecovery() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        StatusStateStore.updateMobile(
            StatusStateStore.MobileIconUpdate(
                subscriptionId = 4,
                kind = StatusStateStore.MobileIconKind.SIGNAL,
                resourceId = 2,
                signal = SignalStrength.Unavailable,
            ),
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
    fun freshSignalAndMobileTypeFinishAirplaneRecovery() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        StatusStateStore.updateMobile(
            StatusStateStore.MobileIconUpdate(
                subscriptionId = 4,
                kind = StatusStateStore.MobileIconKind.SIGNAL,
                resourceId = 3,
                signal = SignalStrength.Level(3),
            ),
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
