package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.settings.FontWeightAxis
import com.chaners.guiyuan.xposed.NativeFontWeight
import com.chaners.guiyuan.xposed.NativeWeightMethod
import com.chaners.guiyuan.xposed.NativePresentationResolver
import com.chaners.guiyuan.xposed.StatusStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectivityMobileTransportTest {

    @Test
    fun mobileTypeKeepsVerifiedWeightCapabilityWithoutOemPath() {
        val source =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                weight = NativeFontWeight(FontWeightAxis(150f, 330f, 700f), NativeWeightMethod.VARIATION_SETTINGS),
            )
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity = SysUiConnectivitySource.State.Unknown,
                mobileType = source,
            ) as CenterIndicator.MobileType

        assertEquals(source.typeface, result.typeface)
        assertEquals(source.weight, result.weight)
    }

    @Test
    fun vpnDoesNotShowMobileTypeWhenMobileDataIsDisabled() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.VPN,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = false,
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
            CenterIndicator.Empty,
            result,
        )
    }

    @Test
    fun vpnCanStillUseCellularTypeWhenDataIsEnabledAndWifiIsAbsent() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.VPN,
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
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            ),
            result,
        )
    }

    @Test
    fun cellularWithDataDisabledDoesNotShowNetworkType() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.CELLULAR,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType =
                    NativePresentationResolver.NetworkType(
                        label = "5G",
                        enhanced = false,
                        source =
                            NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                    ),
            )

        assertEquals(CenterIndicator.Empty, result)
    }

    @Test
    fun vpnOnlyTransportWaitsForAuthoritativeWifiAbsence() {
        val mobileType =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source =
                    NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )
        val connectivity =
            SysUiConnectivitySource.State(
                known = true,
                transport = SysUiConnectivitySource.Transport.VPN,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = false,
            )

        assertNull(
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Unknown,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
            ),
        )
        assertNull(
            ConnectivityPolicy.resolve(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Unknown,
                        internetValidated = null,
                    ),
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
            ),
        )
    }

    @Test
    fun genericOtherTransportKeepsExistingMobileDataGate() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SysUiConnectivitySource.State(
                        known = true,
                        transport = SysUiConnectivitySource.Transport.OTHER,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = false,
                    ),
                mobileType =
                    NativePresentationResolver.NetworkType(
                        label = "5G",
                        enhanced = false,
                        source =
                            NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                    ),
            )

        assertEquals(CenterIndicator.Empty, result)
    }
}
