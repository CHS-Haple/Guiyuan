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
}
