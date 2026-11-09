package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
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
    fun defaultNetworkLossDoesNotOverrideNativeActivityVisibility() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()

        val nativeStillVisible = RenderModel.from(observed, lost, 1)
        assertTrue(nativeStillVisible?.centerIndicator is CenterIndicator.Empty)
        assertEquals(220, nativeStillVisible?.networkActivityResId)

        // Changing Android's default-route data flag is not a native GONE.
        assertEquals(
            220,
            RenderModel.from(observed, vpnHandoffPresentation(mobileDataEnabled = false), 1)
                ?.networkActivityResId,
        )
    }

    @Test
    fun nativeMobileActivityVisibilityOutranksTemporaryWifiStateWhenCenterIsEmpty() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()
        for (wifi in listOf(
            StatusStateStore.WifiState.Unknown,
            StatusStateStore.WifiState.Visible(
                iconResId = 12,
                signal = SignalStrength.Level(3),
            ),
        )) {
            val nativeVisible = RenderModel.from(observed.copy(wifi = wifi), lost, 1)
            assertTrue(nativeVisible?.centerIndicator is CenterIndicator.Empty)
            assertEquals(220, nativeVisible?.networkActivityResId)

            val nativeHidden = RenderModel.from(
                observed.copy(wifi = wifi, mobileActivityShownIds = emptySet()),
                lost,
                1,
            )
            assertNull(nativeHidden?.networkActivityResId)
        }
    }

    @Test
    fun nativeDrawableWithoutVisibilityConfirmationRemainsHidden() {
        val observed = vpnHandoffActivity().copy(
            mobileActivityShownIds = emptySet(),
            mobileActivityVisibilityObservedIds = emptySet(),
        )
        val lost = vpnHandoffPresentation()
        // Resource can arrive before the independent native visibility collector.
        // Never treat its presence as an implicit native VISIBLE.
        assertNull(RenderModel.from(observed, lost, 1)?.networkActivityResId)
        val confirmed = observed.copy(
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
        )
        assertEquals(220, RenderModel.from(confirmed, lost, 1)?.networkActivityResId)
    }

    @Test
    fun nativeVisibilityBeforeResourceWaitsForDrawableThenDisplays() {
        val observed = vpnHandoffActivity().copy(mobileActivityResIds = emptyMap())
        val lost = vpnHandoffPresentation()
        // Native VISIBLE alone cannot manufacture a drawable.
        assertNull(RenderModel.from(observed, lost, 1)?.networkActivityResId)
        // The subsequent native drawable must be accepted without waiting for
        // a second visibility emission from the independent collector.
        val drawableArrived = observed.copy(mobileActivityResIds = mapOf(1 to 220))
        assertEquals(220, RenderModel.from(drawableArrived, lost, 1)?.networkActivityResId)
    }

    @Test
    fun nativeVisibilityAndResourceAbsenceIndependentlyHideActivity() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()

        // The visibility collector can hide the View before a resource event.
        val nativeGone = RenderModel.from(
            observed.copy(mobileActivityShownIds = emptySet()),
            lost,
            1,
        )
        assertNull(nativeGone?.networkActivityResId)

        // A missing native drawable is independently sufficient to hide it.
        assertNull(
            RenderModel.from(
                observed.copy(mobileActivityResIds = emptyMap()), lost, 1,
            )?.networkActivityResId,
        )
    }

    @Test
    fun airplaneAndUnavailableSignalSuppressMobileActivity() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()

        assertNull(
            RenderModel.from(observed.copy(airplaneMode = true), lost, 1)
                ?.networkActivityResId,
        )
        assertNull(
            RenderModel.from(
                observed.copy(mobile = mapOf(
                    1 to StatusStateStore.MobileState(signal = SignalStrength.Unavailable),
                )),
                lost,
                1,
            )?.networkActivityResId,
        )
    }

    private fun vpnHandoffActivity(): StatusStateStore.Snapshot =
        StatusStateStore.Snapshot(
            battery = StatusStateStore.BatteryState(percent = 81, charging = false),
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                1 to StatusStateStore.MobileState(signal = SignalStrength.Level(3)),
            ),
            mobileActivityResIds = mapOf(1 to 220),
            mobileActivityObservedIds = setOf(1),
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
            airplaneMode = false,
        )

    private fun vpnHandoffPresentation(mobileDataEnabled: Boolean = true): PresentationStore.Snapshot =
        presentation(
            connectivity = connectivity(
                transport = SysUiConnectivitySource.Transport.NONE,
                validated = false,
                mobileDataEnabled = mobileDataEnabled,
            ),
            networkType = mobileType("5G"),
        )

}
