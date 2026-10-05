package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectivityPolicyTest {
    @Test
    fun systemUiWifiNoInternetWinsEvenWhenCellularIsDefault() {
        val result =
            ConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(2),
                        internetValidated = false,
                    ),
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
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
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(0),
                        internetValidated = true,
                    ),
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
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
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(1),
                        internetValidated = null,
                    ),
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
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
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.OTHER,
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
    fun airplaneModeUsesNativeAirplaneCenterWhenWifiIsAbsent() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.NONE,
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
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity = SystemUiConnectivityStateSource.State.Unknown,
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun visibleWifiRemainsCenterPriorityDuringAirplaneMode() {
        val result =
            ConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(2),
                        internetValidated = true,
                    ),
                airplaneMode = true,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
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
            CombinedStatusPresentationStateStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )

        val result =
            ConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.NONE,
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
            CombinedStatusPresentationStateStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )

        val result =
            ConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.NONE,
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
    fun vpnDefaultNetworkDoesNotSuppressAuthoritativeMobileTypeAtBootstrap() {
        val result =
            ConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.VPN,
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
    fun vpnOnlyTransportWaitsForAuthoritativeWifiAbsence() {
        val mobileType =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source =
                    NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.VPN,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = false,
            )

        assertNull(
            ConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Unknown,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
            ),
        )
        assertNull(
            ConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
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
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.OTHER,
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
    fun unknownConnectivityDoesNotRenderWifiWhenSystemUiInternetSemanticsAreUnknown() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity = SystemUiConnectivityStateSource.State.Unknown

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
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val segments =
            (0..3).map { level ->
                val result =
                    ConnectivityPolicy.resolve(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
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
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = null,
                signal = SignalStrength.Unavailable,
                internetValidated = true,
            )
        val result =
            ConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
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
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
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
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = false,
            )

        val ready =
            ConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
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
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(1),
                internetValidated = null,
            )
        val wifiDefault =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val cellularDefault =
            wifiDefault.copy(
                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
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
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = null,
                        signal = SignalStrength.Unknown,
                        internetValidated = true,
                    ),
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
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
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 99,
                signal = SignalStrength.Unknown,
                internetValidated = false,
            )
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
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
