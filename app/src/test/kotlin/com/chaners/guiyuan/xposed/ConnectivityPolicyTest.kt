package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectivityPolicyTest {
    @Test
    fun sysUiNoInternetWins() {
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.CELLULAR,
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
    fun sysUiWifiLevelStaysAuthoritative() {
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.CELLULAR,
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
    fun connectivityFillsUnknownWifiOnly() {
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.WIFI,
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
    fun otherTransportDoesNotInventWifi() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity =
            ConnectivitySource.State(
                known = true,
                transport = ConnectivitySource.Transport.OTHER,
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
    fun airplaneUsesNativeCenter() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun airplaneCenterBeforeConnectivity() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity = ConnectivitySource.State.Unknown,
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun wifiKeepsCenterInAirplane() {
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.WIFI,
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.NONE,
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
                noSimIcon = nativeNoSim,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun vpnDoesNotHideAuthoritativeMobileType() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.VPN,
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
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            ),
            result,
        )
    }

    @Test
    fun vpnWaitsForWifiAbsence() {
        val mobileType =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source =
                    NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )
        val connectivity =
            ConnectivitySource.State(
                known = true,
                transport = ConnectivitySource.Transport.VPN,
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
    fun otherTransportKeepsMobileGate() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = StatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.OTHER,
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

    @Test
    fun unknownConnectivityDoesNotInventWifi() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity = ConnectivitySource.State.Unknown

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
    fun sysUiWifiKeepsFourLevels() {
        val connectivity =
            ConnectivitySource.State(
                known = true,
                transport = ConnectivitySource.Transport.WIFI,
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
    fun missingWifiResourceIsNotLevelZero() {
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.CELLULAR,
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            ),
        )
    }

    @Test
    fun wifiReplacementReadyWithSysUiState() {
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
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            )

        assertEquals(true, ready)
    }

    @Test
    fun wifiReplacementUsesDefaultConnectivity() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(1),
                internetValidated = null,
            )
        val wifiDefault =
            ConnectivitySource.State(
                known = true,
                transport = ConnectivitySource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val cellularDefault =
            wifiDefault.copy(
                transport = ConnectivitySource.Transport.CELLULAR,
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
    fun unknownWifiNeverClaimsReplacement() {
        val ready =
            ConnectivityPolicy.wifiReplacementReady(
                wifi =
                    StatusStateStore.WifiState.Visible(
                        iconResId = null,
                        signal = SignalStrength.Unknown,
                        internetValidated = true,
                    ),
                connectivity =
                    ConnectivitySource.State(
                        known = true,
                        transport = ConnectivitySource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            )

        assertEquals(false, ready)
    }
    @Test
    fun nativeWifiRendersWithoutParsedLevel() {
        val wifi =
            StatusStateStore.WifiState.Visible(
                iconResId = 99,
                signal = SignalStrength.Unknown,
                internetValidated = false,
            )
        val connectivity =
            ConnectivitySource.State(
                known = true,
                transport = ConnectivitySource.Transport.WIFI,
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
