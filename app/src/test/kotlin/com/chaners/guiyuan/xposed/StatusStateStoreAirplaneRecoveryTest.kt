package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStateStoreAirplaneRecoveryTest {
    @Test
    fun freshAirplaneModeDoesNotRestoreOldMobileTraffic() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(false)
        StatusStateStore.updateMobileActivity(1, 220)
        val old = StatusStateStore.snapshot()

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        val restored = StatusStateStore.mergeHotReloadSnapshots(old, StatusStateStore.snapshot())
        assertEquals(true, restored.airplaneMode)
        assertTrue(restored.mobileActivityResIds.isEmpty())
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun freshMobileRecoveryPendingSurvivesHotReloadMerge() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(false)
        val saved = StatusStateStore.snapshot()

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        val fresh = StatusStateStore.snapshot()
        assertTrue(fresh.mobileRecoveryPending)

        val merged = StatusStateStore.mergeHotReloadSnapshots(saved, fresh)
        assertEquals(false, merged.airplaneMode)
        assertTrue(merged.mobileRecoveryPending)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun freshAirplaneModeCancelsTransferredMobileRecovery() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        val saved = StatusStateStore.snapshot()
        assertTrue(saved.mobileRecoveryPending)

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        val fresh = StatusStateStore.snapshot()
        assertFalse(fresh.mobileRecoveryPending)

        val merged = StatusStateStore.mergeHotReloadSnapshots(saved, fresh)
        assertEquals(true, merged.airplaneMode)
        assertFalse(merged.mobileRecoveryPending)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun unobservedFreshAirplaneModeRetainsSavedRecovery() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        StatusStateStore.updateAirplaneMode(false)
        val saved = StatusStateStore.snapshot()
        assertTrue(saved.mobileRecoveryPending)

        StatusStateStore.restoreHotReloadState(null)
        val merged =
            StatusStateStore.mergeHotReloadSnapshots(saved, StatusStateStore.snapshot())
        assertEquals(false, merged.airplaneMode)
        assertTrue(merged.mobileRecoveryPending)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun transferredAirplaneModeMasksActivityWhenFreshAirplaneIsUnobserved() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateAirplaneMode(true)
        val saved = StatusStateStore.snapshot()
        assertTrue(saved.mobileActivityResIds.isEmpty())

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateMobileActivity(4, 220)
        val fresh = StatusStateStore.snapshot()
        assertNull(fresh.airplaneMode)
        assertEquals(220, fresh.mobileActivityResIds[4])

        val merged = StatusStateStore.mergeHotReloadSnapshots(saved, fresh)
        assertEquals(true, merged.airplaneMode)
        assertTrue(merged.mobileActivityResIds.isEmpty())
        assertFalse(merged.mobileRecoveryPending)
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
        StatusStateStore.updateMobileSignal(
            subscriptionId = 2,
            signal = SignalStrength.Level(2),
        )
        val beforeExit = StatusStateStore.snapshot()

        val snapshot =
            StatusStateStore.updateAirplaneMode(false)
                ?: error("expected airplane transition")

        assertTrue(snapshot.mobileRecoveryPending)
        assertEquals(listOf(2, 4), snapshot.mobile.keys.toList())
        assertTrue(snapshot.mobile[4]?.signal is SignalStrength.Unknown)
        assertTrue(snapshot.mobile[2]?.signal is SignalStrength.Unknown)
        assertTrue(beforeExit.mobile[4]?.signal is SignalStrength.Level)
        assertTrue(beforeExit.mobile[2]?.signal is SignalStrength.Level)
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
