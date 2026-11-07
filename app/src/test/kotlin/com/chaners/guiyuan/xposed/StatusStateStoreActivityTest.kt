package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStateStoreActivityTest {
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
    fun repeatedNativeActivityObservationsAvoidReplacingUnchangedSnapshots() {
        StatusStateStore.restoreHotReloadState(null)
        // The first empty reading must still mark the source as observed.
        assertNull(StatusStateStore.updateWifiActivity(null))
        assertNull(StatusStateStore.updateMobileActivity(1, null))
        val observed = StatusStateStore.snapshot()
        assertTrue(observed.wifiActivityObserved)
        assertTrue(1 in observed.mobileActivityObservedIds)

        assertNull(StatusStateStore.updateWifiActivity(0))
        assertNull(StatusStateStore.updateMobileActivity(1, 0))
        assertTrue(observed === StatusStateStore.snapshot())

        // Actual activity transitions still publish snapshots; repeats do not.
        val wifi = StatusStateStore.updateWifiActivity(120) ?: error("expected wifi change")
        val mobile = StatusStateStore.updateMobileActivity(1, 220)
            ?: error("expected mobile change")
        assertEquals(120, wifi.wifiActivityResId)
        assertEquals(220, mobile.mobileActivityResIds[1])
        assertNull(StatusStateStore.updateWifiActivity(120))
        assertNull(StatusStateStore.updateMobileActivity(1, 220))
        assertTrue(mobile === StatusStateStore.snapshot())
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
        val previous = StatusStateStore.snapshot()

        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateWifi(wifi)
        // The new collectors have already observed no drawable. They should not
        // need a value change to override the outgoing generation's saved icons.
        assertNull(StatusStateStore.updateWifiActivity(null))
        assertNull(StatusStateStore.updateMobileActivity(1, null))
        StatusStateStore.updateMobileActivity(2, 240)
        val merged = StatusStateStore.mergeHotReloadSnapshots(previous, StatusStateStore.snapshot())
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
        val previous = StatusStateStore.snapshot()

        StatusStateStore.restoreHotReloadState(null)
        val restored = StatusStateStore.mergeHotReloadSnapshots(previous, StatusStateStore.snapshot())
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
}
