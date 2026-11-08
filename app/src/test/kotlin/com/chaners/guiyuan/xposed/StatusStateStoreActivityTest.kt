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
    fun nativeVisibilityWinsOverLaterResourceEventsWithoutExtraListeners() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateMobileActivity(1, 220)
        val shown = StatusStateStore.updateMobileActivityVisibility(1, 220, true)
            ?: error("initial native visibility must be observed")
        assertTrue(1 in shown.mobileActivityShownIds)
        assertTrue(1 in shown.mobileActivityVisibilityObservedIds)
        assertNull(StatusStateStore.updateMobileActivityVisibility(1, 220, true))
        assertTrue(shown === StatusStateStore.snapshot())

        val gone = StatusStateStore.updateMobileActivityVisibility(1, null, false)
            ?: error("native GONE must clear the activity")
        assertNull(gone.mobileActivityResIds[1])
        assertTrue(1 !in gone.mobileActivityShownIds)
        assertTrue(1 in gone.mobileActivityVisibilityObservedIds)

        // A resource collector may emit after the visibility collector.
        // The native hidden state remains authoritative in the model.
        StatusStateStore.updateMobileActivity(1, 221)
        val laterResource = StatusStateStore.snapshot()
        assertEquals(221, laterResource.mobileActivityResIds[1])
        assertTrue(1 !in laterResource.mobileActivityShownIds)

        val restored = StatusStateStore.updateMobileActivityVisibility(1, 221, true)
            ?: error("native VISIBLE must restore activity")
        assertTrue(1 in restored.mobileActivityShownIds)
        assertEquals(221, restored.mobileActivityResIds[1])
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun nativeVisibilityNoOpRetainsTheAppliedChildDrawable() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateMobileActivity(1, 220)
        val shown = StatusStateStore.updateMobileActivityVisibility(1, 220, true)
            ?: error("initial native visibility is required")
        assertEquals(220, shown.mobileActivityResIds[1])

        // Native visibility flow can temporarily hide the root without
        // writing a new child visibility or drawable.
        val rootHidden = StatusStateStore.updateMobileActivityVisibility(1, 220, false)
            ?: error("root hidden must change effective visibility")
        assertEquals(220, rootHidden.mobileActivityResIds[1])
        assertTrue(1 !in rootHidden.mobileActivityShownIds)

        // On root return the OEM child may remain VISIBLE; no resource emit
        // is required to display the same native drawable again.
        val visibleAgain = StatusStateStore.updateMobileActivityVisibility(1, 220, true)
            ?: error("root reappearance must restore effective visibility")
        assertEquals(220, visibleAgain.mobileActivityResIds[1])
        assertTrue(1 in visibleAgain.mobileActivityShownIds)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun nativeHiddenStateSurvivesHotReloadUntilNewVisibilityObservation() {
        val saved = StatusStateStore.Snapshot(
            mobileActivityResIds = mapOf(1 to 220),
            mobileActivityShownIds = emptySet(),
            mobileActivityVisibilityObservedIds = setOf(1),
        )
        // A newly installed resource collector has emitted, but its separate
        // visibility collector has not yet supplied the first native state.
        val fresh = StatusStateStore.Snapshot(
            mobileActivityResIds = mapOf(1 to 221),
            mobileActivityObservedIds = setOf(1),
        )
        val merged = StatusStateStore.mergeHotReloadSnapshots(saved, fresh)
        assertEquals(221, merged.mobileActivityResIds[1])
        assertTrue(1 in merged.mobileActivityVisibilityObservedIds)
        assertTrue(1 !in merged.mobileActivityShownIds)
    }

    @Test
    fun freshNativeVisibilityOverridesSavedStateDuringHotReload() {
        val saved = StatusStateStore.Snapshot(
            mobileActivityResIds = mapOf(1 to 220),
            mobileActivityVisibilityObservedIds = setOf(1),
        )
        val fresh = StatusStateStore.Snapshot(
            mobileActivityResIds = mapOf(1 to 221),
            mobileActivityObservedIds = setOf(1),
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
        )
        val merged = StatusStateStore.mergeHotReloadSnapshots(saved, fresh)
        assertEquals(221, merged.mobileActivityResIds[1])
        assertTrue(1 in merged.mobileActivityShownIds)
        assertTrue(1 in merged.mobileActivityVisibilityObservedIds)

        val airplane = StatusStateStore.mergeHotReloadSnapshots(
            merged,
            StatusStateStore.Snapshot(airplaneMode = true),
        )
        assertTrue(airplane.mobileActivityShownIds.isEmpty())
        assertTrue(airplane.mobileActivityResIds.isEmpty())
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
