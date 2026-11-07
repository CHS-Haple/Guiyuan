package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.PresentationStore
import com.chaners.guiyuan.xposed.StatusStateStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityAirplanePolicyTest {
    @Test
    fun airplaneModeUsesNativeAirplaneCenterWhenWifiIsAbsent() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun airplaneModeUsesAirplaneCenterBeforeConnectivityIsKnown() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity = SysUiConnectivitySource.State.Unknown,
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun visibleWifiRemainsCenterPriorityDuringAirplaneMode() {
        val result =
            ConnectivityPolicy.resolve(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(2),
                        internetValidated = true,
                    ),
                airplaneMode = true,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 2,
                internet = InternetState.VALIDATED,
                nativeResourceId = 1,
            ),
            result,
        )
    }

    @Test
    fun noSimUsesNativeCenterWhenWifiIsAbsent() {
        val nativeNoSim =
            PresentationStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )

        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
                noSimIcon = nativeNoSim,
            )

        assertEquals(CenterIndicator.NoSim(nativeNoSim), result)
    }

    @Test
    fun airplaneRemainsHigherPriorityThanNoSim() {
        val nativeNoSim =
            PresentationStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )

        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
                noSimIcon = nativeNoSim,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }
}
