package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.NativePresentationResolver
import com.chaners.guiyuan.xposed.StatusStateStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityPolicyTest {
    @Test
    fun systemUiWifiNoInternetWinsEvenWhenCellularIsDefault() {
        val result =
            ConnectivityPolicy.resolve(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(2),
                        internetValidated = false,
                    ),
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType =
                    NativePresentationResolver.NetworkType(
                        label = "5G",
                        enhanced = false,
                        source =
                            NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                    ),
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 2,
                internet = InternetState.NO_INTERNET,
                nativeResourceId = 1,
            ),
            result,
        )
    }

    @Test
    fun systemUiWifiLevelRemainsAuthoritativeAcrossDefaultTransportChanges() {
        val result =
            ConnectivityPolicy.resolve(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(0),
                        internetValidated = true,
                    ),
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

        assertEquals(
            CenterIndicator.Wifi(
                segments = 0,
                internet = InternetState.VALIDATED,
                nativeResourceId = 1,
            ),
            result,
        )
    }

    @Test
    fun connectivityOnlyFillsUnknownWifiInternetSemantics() {
        val result =
            ConnectivityPolicy.resolve(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(1),
                        internetValidated = null,
                    ),
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.WIFI,
                        validated = false,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType = null,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 1,
                internet = InternetState.NO_INTERNET,
                nativeResourceId = 1,
            ),
            result,
        )
    }

    @Test
    fun otherTransportDoesNotRenderWifiWhenSystemUiInternetSemanticsAreUnknown() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.OTHER,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val mobileType =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source =
                    NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )

        assertEquals(
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            ),
            ConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
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
    fun nativeMobileTypeAttributesStayIntactAcrossKnownTransports() {
        val native =
            NativePresentationResolver.NetworkType(
                label = "5GA",
                enhanced = true,
                source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                fontPath = "/system/fonts/native-type.ttf",
            )
        val connected =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.CELLULAR,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val expected =
            CenterIndicator.MobileType(
                label = "5GA",
                enhanced = true,
                internet = InternetState.VALIDATED,
                fontPath = "/system/fonts/native-type.ttf",
            )

        for (transport in listOf(
            SysUiConnectivitySource.Transport.CELLULAR,
            SysUiConnectivitySource.Transport.VPN,
            SysUiConnectivitySource.Transport.OTHER,
        )) {
            assertEquals(
                transport.toString(),
                expected,
                ConnectivityPolicy.resolve(
                    wifi = StatusStateStore.WifiState.Hidden,
                    airplaneMode = false,
                    connectivity = connected.copy(transport = transport),
                    mobileType = native,
                ),
            )
        }
    }

    @Test
    fun transportGatesDoNotInventMobileOrWifiPresentations() {
        val native =
            NativePresentationResolver.NetworkType(
                label = "LTE",
                enhanced = false,
                source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_SINGLE,
            )
        val connected =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.CELLULAR,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        for (transport in listOf(
            SysUiConnectivitySource.Transport.CELLULAR,
            SysUiConnectivitySource.Transport.VPN,
            SysUiConnectivitySource.Transport.OTHER,
        )) {
            assertEquals(
                CenterIndicator.Empty,
                ConnectivityPolicy.resolve(
                    wifi = StatusStateStore.WifiState.Hidden,
                    airplaneMode = false,
                    connectivity = connected.copy(
                        transport = transport,
                        mobileDataEnabled = false,
                    ),
                    mobileType = native,
                ),
            )
        }
        assertEquals(
            null,
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Unknown,
                airplaneMode = false,
                connectivity = connected.copy(
                    transport = SysUiConnectivitySource.Transport.VPN,
                ),
                mobileType = native,
            ),
        )
        assertEquals(
            CenterIndicator.Empty,
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity = connected.copy(
                    transport = SysUiConnectivitySource.Transport.OTHER,
                    mobileDataEnabled = null,
                ),
                mobileType = native,
            ),
        )
        assertEquals(
            null,
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity = connected.copy(
                    transport = SysUiConnectivitySource.Transport.WIFI,
                ),
                mobileType = native,
            ),
        )
        assertEquals(
            CenterIndicator.Empty,
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity = connected.copy(
                    transport = SysUiConnectivitySource.Transport.NONE,
                ),
                mobileType = native,
            ),
        )
    }

    @Test
    fun nativeWifiPrecedesAirplaneWhileUnknownConnectivityPreservesNativeType() {
        val native =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )
        assertEquals(
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.UNKNOWN,
            ),
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity = SysUiConnectivitySource.State.Unknown,
                mobileType = native,
            ),
        )
        assertEquals(
            CenterIndicator.Airplane,
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity = SysUiConnectivitySource.State.Unknown,
                mobileType = native,
            ),
        )
        assertEquals(
            CenterIndicator.Wifi(
                segments = 0,
                internet = InternetState.VALIDATED,
                nativeResourceId = 15,
            ),
            ConnectivityPolicy.resolve(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = 15,
                        signal = SignalStrength.Unavailable,
                        internetValidated = true,
                    ),
                airplaneMode = true,
                connectivity = SysUiConnectivitySource.State.Unknown,
                mobileType = native,
            ),
        )
    }

}
