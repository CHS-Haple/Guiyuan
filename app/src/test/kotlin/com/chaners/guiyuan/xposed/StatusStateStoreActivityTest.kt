package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStateStoreActivityTest {
    @Test
    fun sortedMobileUpdatesRetainEarlierSnapshots() {
        StatusStateStore.restoreHotReloadState(null)
        val signal = SignalStrength.Level(3)
        StatusStateStore.updateMobileSignal(9, signal)
        StatusStateStore.updateMobileActivity(9, 209)
        val first = StatusStateStore.snapshot()

        StatusStateStore.updateMobileSignal(2, signal)
        StatusStateStore.updateMobileSignal(5, signal)
        StatusStateStore.updateMobileActivity(2, 202)
        StatusStateStore.updateMobileActivity(5, 205)
        val current = StatusStateStore.snapshot()

        assertEquals(listOf(2, 5, 9), current.mobile.keys.toList())
        assertEquals(listOf(2, 5, 9), current.mobileActivityResIds.keys.toList())
        assertEquals(listOf(9), first.mobile.keys.toList())
        assertEquals(mapOf(9 to 209), first.mobileActivityResIds)

        assertNull(StatusStateStore.updateMobileSignal(9, signal))
        assertNull(StatusStateStore.updateMobileActivity(9, 209))
        assertTrue(current === StatusStateStore.snapshot())

        val removed = StatusStateStore.updateMobileActivity(5, null)
            ?: error("resource removal must publish")
        assertEquals(listOf(2, 9), removed.mobileActivityResIds.keys.toList())
        assertEquals(205, current.mobileActivityResIds[5])
        StatusStateStore.restoreHotReloadState(null)
    }

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
        val shown = StatusStateStore.updateMobileActivityVisibility(1, true)
            ?: error("initial native visibility must be observed")
        assertTrue(1 in shown.mobileActivityShownIds)
        assertTrue(1 in shown.mobileActivityVisibilityObservedIds)
        assertNull(StatusStateStore.updateMobileActivityVisibility(1, true))
        assertTrue(shown === StatusStateStore.snapshot())

        val gone = StatusStateStore.updateMobileActivityVisibility(1, false)
            ?: error("native GONE must hide the activity")
        assertEquals(220, gone.mobileActivityResIds[1])
        assertTrue(1 !in gone.mobileActivityShownIds)
        assertTrue(1 in gone.mobileActivityVisibilityObservedIds)

        // A resource collector may emit after the visibility collector.
        // The native hidden state remains authoritative in the model.
        StatusStateStore.updateMobileActivity(1, 221)
        val laterResource = StatusStateStore.snapshot()
        assertEquals(221, laterResource.mobileActivityResIds[1])
        assertTrue(1 !in laterResource.mobileActivityShownIds)

        val restored = StatusStateStore.updateMobileActivityVisibility(1, true)
            ?: error("native VISIBLE must restore activity")
        assertTrue(1 in restored.mobileActivityShownIds)
        assertEquals(221, restored.mobileActivityResIds[1])
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun nativeResourceAbsenceClearsCacheWithoutForgingVisibility() {
        StatusStateStore.restoreHotReloadState(null)
        val shown = StatusStateStore.updateMobileActivityResource(1, 220, true)
            ?: error("resource collector must publish its native receipt")
        assertEquals(220, shown.mobileActivityResIds[1])
        assertTrue(1 in shown.mobileActivityShownIds)

        // A visibility-only callback with no tag must retain the last drawable.
        assertNull(StatusStateStore.updateMobileActivityVisibility(1, true))
        assertEquals(220, StatusStateStore.snapshot().mobileActivityResIds[1])

        // Only the original resource collector's explicit absence clears it.
        val cleared = StatusStateStore.updateMobileActivityResource(1, null, true)
            ?: error("resource absence must be committed")
        assertNull(cleared.mobileActivityResIds[1])
        assertTrue(1 in cleared.mobileActivityShownIds)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun firstNativeHiddenReceiptIsRetainedWithoutPublishingRepeatSnapshots() {
        StatusStateStore.restoreHotReloadState(null)
        val first = StatusStateStore.updateMobileActivityVisibility(3, false)
            ?: error("first native hidden receipt must be committed")
        assertTrue(3 in first.mobileActivityVisibilityObservedIds)
        assertTrue(3 !in first.mobileActivityShownIds)
        assertNull(StatusStateStore.updateMobileActivityVisibility(3, false))
        assertTrue(first === StatusStateStore.snapshot())
        assertNull(StatusStateStore.updateMobileActivityVisibility(-1, true))
        assertTrue(first === StatusStateStore.snapshot())
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun nativeVisibilityNoOpRetainsTheAppliedChildDrawable() {
        StatusStateStore.restoreHotReloadState(null)
        StatusStateStore.updateMobileActivity(1, 220)
        val shown = StatusStateStore.updateMobileActivityVisibility(1, true)
            ?: error("initial native visibility is required")
        assertEquals(220, shown.mobileActivityResIds[1])

        // Native visibility flow can temporarily hide the root without
        // writing a new child visibility or drawable.
        val rootHidden = StatusStateStore.updateMobileActivityVisibility(1, false)
            ?: error("root hidden must change effective visibility")
        assertEquals(220, rootHidden.mobileActivityResIds[1])
        assertTrue(1 !in rootHidden.mobileActivityShownIds)

        // On root return the OEM child may remain VISIBLE; no resource emit
        // is required to display the same native drawable again.
        val visibleAgain = StatusStateStore.updateMobileActivityVisibility(1, true)
            ?: error("root reappearance must restore effective visibility")
        assertEquals(220, visibleAgain.mobileActivityResIds[1])
        assertTrue(1 in visibleAgain.mobileActivityShownIds)
        StatusStateStore.restoreHotReloadState(null)
    }

    @Test
    fun nativeVisibilityBeforeDrawableKeepsTheVisibleOwnerState() {
        StatusStateStore.restoreHotReloadState(null)

        // A native VISIBLE event can precede the independent drawable collector.
        val visible = StatusStateStore.updateMobileActivityVisibility(1, true)
            ?: error("first native visibility must be committed")
        assertTrue(1 in visible.mobileActivityShownIds)
        assertTrue(1 in visible.mobileActivityVisibilityObservedIds)
        assertNull(visible.mobileActivityResIds[1])

        val resource = StatusStateStore.updateMobileActivity(1, 220)
            ?: error("late native resource must be committed")
        assertTrue(1 in resource.mobileActivityShownIds)
        assertEquals(220, resource.mobileActivityResIds[1])

        StatusStateStore.restoreHotReloadState(null)
    }}
