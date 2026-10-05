package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusRenderModelTest {
    @Test
    fun wifiValidatedWinsTheCenterIndicator() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                                validated = true,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        val center = model?.centerIndicator as? CenterIndicator.Wifi
        assertEquals(3, center?.segments)
        assertEquals(InternetState.VALIDATED, center?.internet)
    }

    @Test
    fun unknownNativeWifiInternetWithCellularDefaultUsesMobilePresentation() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                                internetValidated = null,
                            ),
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                                validated = true,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        val center = model?.centerIndicator as? CenterIndicator.MobileType
        assertEquals("5G", center?.label)
        assertEquals(InternetState.VALIDATED, center?.internet)
    }

    @Test
    fun defaultDataFallbackSelectsMatchingMobileSignal() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            linkedMapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(1),
                                ),
                                4 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                                validated = true,
                            ),
                    ),
                defaultDataSubscriptionId = 4,
            )

        assertEquals(4, model?.effectiveDataSubscriptionId)
        assertEquals(4, model?.mobileLevel)
    }

    @Test
    fun cellularValidatedUsesSystemMobileType() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                4 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                                validated = true,
                            ),
                        networkType = mobileType("5G-A"),
                    ),
                defaultDataSubscriptionId = 4,
            )

        val center = model?.centerIndicator as? CenterIndicator.MobileType
        assertEquals("5G-A", center?.label)
        assertEquals(InternetState.VALIDATED, center?.internet)
        assertEquals(4, model?.effectiveDataSubscriptionId)
    }

    @Test
    fun wifiConnectedWithoutInternetKeepsWifiWithNoInternetState() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                                validated = false,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        val center = model?.centerIndicator as? CenterIndicator.Wifi
        assertEquals(InternetState.NO_INTERNET, center?.internet)
    }

    @Test
    fun wifiDisconnectedFallsBackToValidatedCellularType() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                                validated = true,
                            ),
                        networkType = mobileType("4G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        val center = model?.centerIndicator as? CenterIndicator.MobileType
        assertEquals("4G", center?.label)
    }

    @Test
    fun otherTransportWithUnknownWifiInternetFallsBackToMobileType() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.OTHER,
                                validated = true,
                                mobileDataEnabled = true,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        val center = model?.centerIndicator as? CenterIndicator.MobileType
        assertEquals("5G", center?.label)
        assertEquals(InternetState.VALIDATED, center?.internet)
    }

    @Test
    fun otherTransportFallsBackToMobileTypeWhenWifiIsHidden() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.OTHER,
                                validated = true,
                                mobileDataEnabled = true,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        val center = model?.centerIndicator as? CenterIndicator.MobileType
        assertEquals("5G", center?.label)
        assertEquals(InternetState.VALIDATED, center?.internet)
    }

    @Test
    fun mobileDataEnabledWithoutActiveTransportLeavesCenterEmpty() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(3),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.NONE,
                                validated = false,
                                mobileDataEnabled = true,
                            ),
                        networkType = mobileType("4G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Empty)
    }

    @Test
    fun mobileDataDisabledWithSignalLeavesCenterEmptyAndKeepsSignalLevel() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(3),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.NONE,
                                validated = false,
                                mobileDataEnabled = false,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Empty)
        assertEquals(3, model?.mobileLevel)
        assertEquals(1, model?.effectiveDataSubscriptionId)
    }

    @Test
    fun completeNoNetworkLeavesCenterEmptyAndUsesSignalAreaForStatus() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Unavailable,
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.NONE,
                                validated = false,
                                mobileDataEnabled = false,
                            ),
                        networkType = null,
                    ),
                defaultDataSubscriptionId = 1,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Empty)
        assertNull(model?.mobileLevel)
    }

    @Test
    fun unknownConnectivityDoesNotInventNoNetwork() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(),
                defaultDataSubscriptionId = 1,
            )

        assertNull(model)
    }

    @Test
    fun nativeNoSimPresentationOverridesStaleMobileSignalAndType() {
        val noSimIcon =
            PresentationStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                4 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                                validated = true,
                            ),
                        mobilePresentation =
                            NativePresentationResolver.Snapshot(
                                mode = NativePresentationResolver.Mode.UNKNOWN,
                                boundRoots = 0,
                                visibleRoots = 0,
                                activeSubscriptionIds = emptyList(),
                                presentationRootSubscriptionId = null,
                                effectiveDataSubscriptionId = 4,
                                networkTypeSubscriptionId = 4,
                                networkType = mobileType("5G"),
                            ),
                        statusIcons =
                            PresentationStore.StatusIconPresentation(
                                noSimVisible = true,
                                noSimIcon = noSimIcon,
                            ),
                    ),
                defaultDataSubscriptionId = 4,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.NoSim)
        assertNull(model?.mobileLevel)
        assertEquals(-1, model?.effectiveDataSubscriptionId)
        assertTrue(model?.mobileUnavailableMark == true)
    }

    @Test
    fun nativeNoSimWithWifiSuppressesStaleMobileButKeepsWifiCenter() {
        val noSimIcon =
            PresentationStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 10,
                                signal = SignalStrength.Level(3),
                                internetValidated = true,
                            ),
                        mobile =
                            mapOf(
                                4 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                                validated = true,
                            ),
                        mobilePresentation =
                            NativePresentationResolver.Snapshot(
                                mode = NativePresentationResolver.Mode.UNKNOWN,
                                boundRoots = 0,
                                visibleRoots = 0,
                                activeSubscriptionIds = emptyList(),
                                presentationRootSubscriptionId = null,
                                effectiveDataSubscriptionId = 4,
                                networkTypeSubscriptionId = 4,
                                networkType = mobileType("5G"),
                            ),
                        statusIcons =
                            PresentationStore.StatusIconPresentation(
                                noSimVisible = true,
                                noSimIcon = noSimIcon,
                            ),
                    ),
                defaultDataSubscriptionId = 4,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Wifi)
        assertNull(model?.mobileLevel)
        assertEquals(-1, model?.effectiveDataSubscriptionId)
        assertTrue(model?.mobileUnavailableMark == true)
    }

    @Test
    fun noSimWithoutWifiKeepsUnavailableMarkAlongsideNativeNoSimCenter() {
        val noSimIcon =
            PresentationStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile = emptyMap(),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.NONE,
                                validated = false,
                                mobileDataEnabled = false,
                            ),
                        statusIcons =
                            PresentationStore.StatusIconPresentation(
                                noSimVisible = true,
                                noSimIcon = noSimIcon,
                            ),
                    ),
                defaultDataSubscriptionId = -1,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.NoSim)
        assertNull(model?.mobileLevel)
        assertTrue(model?.mobileUnavailableMark == true)
    }

    @Test
    fun noSimWithWifiKeepsUnavailableMarkWhileWifiOwnsCenter() {
        val noSimIcon =
            PresentationStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 10,
                                signal = SignalStrength.Level(3),
                                internetValidated = true,
                            ),
                        mobile = emptyMap(),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                                validated = true,
                                mobileDataEnabled = false,
                            ),
                        statusIcons =
                            PresentationStore.StatusIconPresentation(
                                noSimVisible = true,
                                noSimIcon = noSimIcon,
                            ),
                    ),
                defaultDataSubscriptionId = -1,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Wifi)
        assertNull(model?.mobileLevel)
        assertTrue(model?.mobileUnavailableMark == true)
    }

    @Test
    fun airplaneModeUsesAirplaneCenterAndUnavailableMobileSignal() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                        airplaneMode = true,
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.NONE,
                                validated = false,
                                mobileDataEnabled = false,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 1,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Airplane)
        assertNull(model?.mobileLevel)
        assertTrue(model?.mobileUnavailableMark == true)
    }

    @Test
    fun airplaneExitRecoveryShowsEmptyCenterAndDimDotsWithoutCross() {
        val model =
            CombinedStatusRenderModel.from(
                snapshot =
                    snapshot(
                        wifi = CombinedStatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                4 to CombinedStatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                        airplaneMode = false,
                    ).copy(
                        mobileRecoveryPending = true,
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SystemUiConnectivityStateSource.Transport.VPN,
                                validated = true,
                            ),
                        networkType = mobileType("5G"),
                    ),
                defaultDataSubscriptionId = 4,
            )

        assertTrue(model?.centerIndicator is CenterIndicator.Empty)
        assertNull(model?.mobileLevel)
        assertTrue(model?.mobileUnavailableMark == false)
    }

    private fun snapshot(
        wifi: CombinedStatusStateStore.WifiState,
        mobile: Map<Int, CombinedStatusStateStore.MobileState>,
        airplaneMode: Boolean? = false,
    ) =
        CombinedStatusStateStore.Snapshot(
            battery =
                CombinedStatusStateStore.BatteryState(
                    percent = 80,
                    charging = false,
                ),
            wifi = wifi,
            mobile = mobile,
            airplaneMode = airplaneMode,
        )

    private fun connectivity(
        transport: SystemUiConnectivityStateSource.Transport,
        validated: Boolean,
        mobileDataEnabled: Boolean? = true,
    ) =
        SystemUiConnectivityStateSource.State(
            known = true,
            transport = transport,
            validated = validated,
            hasInternetCapability = validated,
            mobileDataEnabled = mobileDataEnabled,
        )

    private fun presentation(
        connectivity: SystemUiConnectivityStateSource.State,
        networkType: NativePresentationResolver.NetworkType?,
    ) =
        PresentationStore.Snapshot(
            connectivity = connectivity,
            mobilePresentation =
                NativePresentationResolver.Snapshot(
                    mode = NativePresentationResolver.Mode.SINGLE,
                    boundRoots = 1,
                    visibleRoots = 1,
                    activeSubscriptionIds = listOf(1),
                    presentationRootSubscriptionId = 1,
                    effectiveDataSubscriptionId = 1,
                    networkTypeSubscriptionId = 1,
                    networkType = networkType,
                ),
        )

    private fun mobileType(label: String) =
        NativePresentationResolver.NetworkType(
            label = label,
            enhanced = false,
            source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
        )
}
