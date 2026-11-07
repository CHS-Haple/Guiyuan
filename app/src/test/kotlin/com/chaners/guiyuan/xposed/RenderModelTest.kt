package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.connectivity
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.presentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.mobileType

class RenderModelTest {
    @Test
    fun nativeLowColorAndThresholdReclassificationAreConsistent() {
        val native = StatusStateStore.BatteryState(
            percent = 24,
            charging = false,
            semanticState = BatterySemanticState.NORMAL,
            systemSemanticColor = null,
            lowSystemColor = 0xFFFF5500.toInt(),
        )
        fun model(battery: StatusStateStore.BatteryState, threshold: Int) =
            RenderModel.from(
                // Airplane provides a complete native center; the test only varies battery semantics.
                snapshot = StatusStateStore.Snapshot(battery = battery, airplaneMode = true),
                presentation = PresentationStore.Snapshot(),
                defaultDataSubscriptionId = -1,
                lowBatteryThreshold = threshold,
            )

        val early = model(native, 30)
        assertEquals(BatterySemanticState.LOW, early?.batterySemanticState)
        assertEquals(native.lowSystemColor, early?.batterySystemSemanticColor)
        val normal = model(native, 20)
        assertEquals(BatterySemanticState.NORMAL, normal?.batterySemanticState)
        assertEquals(null, normal?.batterySystemSemanticColor)

        val nativeLow = native.copy(
            percent = 12,
            semanticState = BatterySemanticState.LOW,
            systemSemanticColor = 0xFFFF5500.toInt(),
        )
        val suppressed = model(nativeLow, 5)
        assertEquals(BatterySemanticState.NORMAL, suppressed?.batterySemanticState)
        assertEquals(null, suppressed?.batterySystemSemanticColor)
        val retained = model(nativeLow, 20)
        assertEquals(BatterySemanticState.LOW, retained?.batterySemanticState)
        assertEquals(nativeLow.lowSystemColor, retained?.batterySystemSemanticColor)
    }

    @Test
    fun nativeActivityDrawableFollowsOnlyTheSelectedTransport() {
        val observed = StatusStateStore.Snapshot(
            battery = StatusStateStore.BatteryState(percent = 81, charging = true),
            wifi = StatusStateStore.WifiState.Visible(
                iconResId = 11,
                signal = SignalStrength.Level(3),
            ),
            wifiActivityResId = 120,
            mobile = mapOf(
                1 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
            ),
            mobileActivityResIds = mapOf(1 to 220),
            airplaneMode = false,
        )
        val wifiPresentation = presentation(
            connectivity = connectivity(SysUiConnectivitySource.Transport.WIFI, true),
            networkType = mobileType("5G"),
        )
        val wifiModel = RenderModel.from(observed, wifiPresentation, 1)
        assertTrue(wifiModel?.centerIndicator is CenterIndicator.Wifi)
        assertEquals(120, wifiModel?.networkActivityResId)

        val noWifiActivity = RenderModel.from(
            observed.copy(wifiActivityResId = null),
            wifiPresentation,
            1,
        )
        assertNull(noWifiActivity?.networkActivityResId)

        val mobileModel = RenderModel.from(
            observed.copy(
                wifi = StatusStateStore.WifiState.Hidden,
                wifiActivityResId = 120,
            ),
            presentation(
                connectivity = connectivity(SysUiConnectivitySource.Transport.CELLULAR, true),
                networkType = mobileType("5G"),
            ),
            1,
        )
        assertTrue(mobileModel?.centerIndicator is CenterIndicator.MobileType)
        assertEquals(220, mobileModel?.networkActivityResId)
    }

    @Test
    fun aggregateActivityReadsSoleVisibleNativeRootEvenWhenDataSimIsSim2() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        ).copy(mobileActivityResIds = mapOf(14 to 220))

        val aggregated = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        assertTrue(aggregated?.centerIndicator is CenterIndicator.MobileType)
        assertEquals(3, aggregated?.effectiveDataSubscriptionId)
        assertEquals(220, aggregated?.networkActivityResId)

        // A native hide event must clear rather than retain the previous icon.
        val idle = RenderModel.from(
            state.copy(mobileActivityResIds = emptyMap()),
            native,
            3,
            dualSignalEnabled = true,
        )
        assertNull(idle?.networkActivityResId)
    }

    @Test
    fun separateAndUnverifiedMobileLayoutsDoNotBorrowAnotherSimsActivity() {
        val aggregated = dualPresentation(mapOf(0 to 14, 1 to 3))
        val native = aggregated.mobilePresentation!!
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        ).copy(mobileActivityResIds = mapOf(14 to 220, 3 to 330))

        val aggregate = RenderModel.from(state, aggregated, 3, dualSignalEnabled = true)
        assertEquals(220, aggregate?.networkActivityResId)

        val separate = RenderModel.from(
            state,
            aggregated.copy(mobilePresentation = native.copy(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                visibleRoots = 2,
            )),
            3,
            dualSignalEnabled = true,
        )
        assertEquals(330, separate?.networkActivityResId)

        val notAuthoritative = RenderModel.from(
            state,
            aggregated.copy(mobilePresentation = native.copy(activeSubsAuthoritative = false)),
            3,
            dualSignalEnabled = true,
        )
        assertEquals(330, notAuthoritative?.networkActivityResId)
    }
}
