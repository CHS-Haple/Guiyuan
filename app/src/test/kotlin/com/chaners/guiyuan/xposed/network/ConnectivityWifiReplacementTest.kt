package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.StatusStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectivityWifiReplacementTest {
    @Test
    fun unknownConnectivityDoesNotRenderWifiWhenSystemUiInternetSemanticsAreUnknown() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity = SysUiConnectivitySource.State.Unknown

        assertNull(
            ConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = null,
            ),
        )
        assertEquals(
            false,
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = connectivity,
            ),
        )
    }

    @Test
    fun systemUiWifiLevelsRemainFourDistinctVisualStates() {
        val connectivity =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val segments =
            (0..3).map { level ->
                val result =
                    ConnectivityPolicy.resolve(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = level + 1,
                                signal = SignalStrength.Level(level),
                                internetValidated = true,
                            ),
                        airplaneMode = false,
                        connectivity = connectivity,
                        mobileType = null,
                    )
                (result as CenterIndicator.Wifi).segments
            }

        assertEquals(listOf(0, 1, 2, 3), segments)
    }

    @Test
    fun unavailableWifiSignalWithoutNativeResourceDoesNotMasqueradeAsLevelZero() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = null,
                signal = SignalStrength.Unavailable,
                internetValidated = true,
            )
        val result =
            ConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType = null,
            )

        assertEquals(CenterIndicator.Empty, result)
        assertEquals(
            false,
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            ),
        )
    }

    @Test
    fun nativeWifiReplacementIsReadyWhenSystemUiProvidesInternetSemantics() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = false,
            )

        val ready =
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            )

        assertEquals(true, ready)
    }

    @Test
    fun nativeWifiReplacementUsesConnectivityOnlyWhenWifiIsDefault() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(1),
                internetValidated = null,
            )
        val wifiDefault =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val cellularDefault =
            wifiDefault.copy(
                transport = SysUiConnectivitySource.Transport.CELLULAR,
            )

        assertEquals(
            true,
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = wifiDefault,
            ),
        )
        assertEquals(
            false,
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = cellularDefault,
            ),
        )
    }

    @Test
    fun unknownWifiSignalWithoutNativeResourceNeverClaimsReplacement() {
        val ready =
            ConnectivityPolicy.wifiReplacementReady(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = null,
                        signal = SignalStrength.Unknown,
                        internetValidated = true,
                    ),
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            )

        assertEquals(false, ready)
    }
    @Test
    fun nativeWifiVariantCanRenderWithoutParsedSignalLevel() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 99,
                signal = SignalStrength.Unknown,
                internetValidated = false,
            )
        val connectivity =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.WIFI,
                validated = false,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val result =
            ConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = null,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 0,
                internet = InternetState.NO_INTERNET,
                nativeResourceId = 99,
            ),
            result,
        )
        assertEquals(
            true,
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = connectivity,
            ),
        )
    }
}
