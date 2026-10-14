package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.InternetState
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderModelTest {
    @Test
    fun wifiValidatedWinsTheCenterIndicator() {
        val model =
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.WIFI,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                                internetValidated = null,
                            ),
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.CELLULAR,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            linkedMapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(1),
                                ),
                                4 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.WIFI,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                4 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.CELLULAR,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.WIFI,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.CELLULAR,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = 1,
                                signal = SignalStrength.Level(3),
                            ),
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.OTHER,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.OTHER,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(3),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.NONE,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(3),
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.NONE,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Unavailable,
                                ),
                            ),
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.NONE,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                4 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.CELLULAR,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
                                iconResId = 10,
                                signal = SignalStrength.Level(3),
                                internetValidated = true,
                            ),
                        mobile =
                            mapOf(
                                4 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.WIFI,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile = emptyMap(),
                    ),
                presentation =
                    PresentationStore.Snapshot(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.NONE,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi =
                            StatusStateStore.WifiState.Visible(
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
                                transport = SysUiConnectivitySource.Transport.WIFI,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                1 to StatusStateStore.MobileState(
                                    signal = SignalStrength.Level(4),
                                ),
                            ),
                        airplaneMode = true,
                    ),
                presentation =
                    presentation(
                        connectivity =
                            connectivity(
                                transport = SysUiConnectivitySource.Transport.NONE,
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
            RenderModel.from(
                snapshot =
                    snapshot(
                        wifi = StatusStateStore.WifiState.Hidden,
                        mobile =
                            mapOf(
                                4 to StatusStateStore.MobileState(
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
                                transport = SysUiConnectivitySource.Transport.VPN,
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


    @Test
    fun dataSimSwitchKeepsRowsAndUpdatesCenterType() {
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
            ),
        )
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val dataSim2 = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        val sim1Type = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                effectiveDataSubscriptionId = 14,
                networkTypeSubscriptionId = 14,
                networkType = mobileType("4G"),
            ),
        )
        val dataSim1 = RenderModel.from(state, sim1Type, 14, dualSignalEnabled = true)

        val fixedRows = DualMobileLevels(sim1 = 2, sim2 = 4)
        assertEquals(fixedRows, dataSim2?.dualMobileLevels)
        assertEquals(fixedRows, dataSim1?.dualMobileLevels)
        assertEquals(3, dataSim2?.effectiveDataSubscriptionId)
        assertEquals(14, dataSim1?.effectiveDataSubscriptionId)
        assertEquals(4, dataSim2?.mobileLevel)
        assertEquals(2, dataSim1?.mobileLevel)
        assertEquals("5G", (dataSim2?.centerIndicator as? CenterIndicator.MobileType)?.label)
        assertEquals("4G", (dataSim1?.centerIndicator as? CenterIndicator.MobileType)?.label)

        // Switching data SIM without a matching native type does not borrow a label.
        val switchedWithoutType = RenderModel.from(state, native, 14, dualSignalEnabled = true)
        assertEquals(fixedRows, switchedWithoutType?.dualMobileLevels)
        assertTrue(switchedWithoutType?.centerIndicator is CenterIndicator.Empty)
    }

    @Test
    fun optInDualSignalsFollowPhysicalSlotsNotSubscriptionIdOrDataSim() {
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(1)),
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
            ),
        )
        val presentation = dualPresentation(
            slotSubscriptionIds = mapOf(0 to 14, 1 to 3),
        )

        val single = RenderModel.from(state, presentation, 3)
        assertNull(single?.dualMobileLevels)
        assertEquals(1, single?.mobileLevel)

        val dual = RenderModel.from(state, presentation, 3, dualSignalEnabled = true)
        assertEquals(DualMobileLevels(sim1 = 4, sim2 = 1), dual?.dualMobileLevels)
        assertEquals(3, dual?.effectiveDataSubscriptionId)
        assertEquals(1, dual?.mobileLevel)
    }

    @Test
    fun unknownDualSignalKeepsOtherRowButUnavailableOrMissingFallsBack() {
        val source = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(1)),
            ),
        )
        val native = dualPresentation(slotSubscriptionIds = mapOf(0 to 14, 1 to 3))
        val unknownSim2 =
            source.copy(
                mobile =
                    source.mobile +
                        (3 to StatusStateStore.MobileState(signal = SignalStrength.Unknown)),
            )
        val unknownSim1 =
            source.copy(
                mobile =
                    source.mobile +
                        (14 to StatusStateStore.MobileState(signal = SignalStrength.Unknown)),
            )
        val bothUnknown =
            source.copy(
                mobile = mapOf(
                    14 to StatusStateStore.MobileState(signal = SignalStrength.Unknown),
                    3 to StatusStateStore.MobileState(signal = SignalStrength.Unknown),
                ),
            )
        assertEquals(
            DualMobileLevels(4, null),
            RenderModel.from(unknownSim2, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
        assertEquals(
            DualMobileLevels(null, 1),
            RenderModel.from(unknownSim1, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
        assertEquals(
            DualMobileLevels(null, null),
            RenderModel.from(bothUnknown, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
        assertNull(RenderModel.from(bothUnknown, native, 14, dualSignalEnabled = true)?.mobileLevel)

        val zeroSim2 =
            source.copy(
                mobile =
                    source.mobile +
                        (3 to StatusStateStore.MobileState(signal = SignalStrength.Level(0))),
            )
        assertEquals(
            DualMobileLevels(4, 0),
            RenderModel.from(zeroSim2, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
        assertNull(
            RenderModel.from(
                source.copy(
                    mobile =
                        source.mobile +
                            (3 to StatusStateStore.MobileState(signal = SignalStrength.Unavailable)),
                ),
                native,
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
        assertNull(
            RenderModel.from(
                source.copy(mobile = source.mobile - 3),
                native,
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
    }

    @Test
    fun dualSignalOptInRequiresAuthoritativeUnambiguousSlotsAndTwoNativeSubscriptions() {
        val source = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )
        val valid = dualPresentation(slotSubscriptionIds = mapOf(0 to 14, 1 to 3))
        val unresolved = listOf(
            valid.copy(mobilePresentation =
                valid.mobilePresentation?.copy(slotSubscriptionIds = emptyMap())),
            valid.copy(mobilePresentation =
                valid.mobilePresentation?.copy(slotSubscriptionIds = mapOf(0 to 3, 1 to 3))),
            valid.copy(mobilePresentation =
                valid.mobilePresentation?.copy(mode = NativePresentationResolver.Mode.SINGLE)),
            valid.copy(mobilePresentation =
                valid.mobilePresentation?.copy(activeSubsAuthoritative = false)),
            valid.copy(mobilePresentation =
                valid.mobilePresentation?.copy(activeSubscriptionIds = listOf(3, 14, 27))),
            valid.copy(mobilePresentation = null),
        )
        for (presentation in unresolved) {
            assertNull(
                RenderModel.from(source, presentation, 14, dualSignalEnabled = true)
                    ?.dualMobileLevels,
            )
        }
        assertEquals(
            DualMobileLevels(4, 2),
            RenderModel.from(
                source,
                valid.copy(mobilePresentation =
                    valid.mobilePresentation?.copy(
                        mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                        visibleRoots = 2,
                    )),
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
    }

    @Test
    fun airplaneRecoveryAndNoSimSuppressDualRowsWithoutChangingSingleFallback() {
        val source = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )
        val native = dualPresentation(slotSubscriptionIds = mapOf(0 to 14, 1 to 3))
        assertNull(
            RenderModel.from(
                source.copy(airplaneMode = true),
                native,
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
        assertNull(
            RenderModel.from(
                source.copy(mobileRecoveryPending = true),
                native,
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
        assertNull(
            RenderModel.from(
                source,
                native.copy(statusIcons = PresentationStore.StatusIconPresentation(
                    noSimVisible = true,
                )),
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
        assertEquals(
            DualMobileLevels(4, 2),
            RenderModel.from(source, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
    }


    @Test
    fun optInDualFallsBackToCurrentSingleSimAfterOtherSimIsRemoved() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val single = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                mode = NativePresentationResolver.Mode.SINGLE,
                activeSubscriptionIds = listOf(3),
                platformActiveSubscriptionIds = setOf(3),
                slotSubscriptionIds = emptyMap(),
                visibleRoots = 1,
                activeBoundRoots = 1,
                effectiveDataSubscriptionId = 14,
            ),
        )
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(1)),
            ),
        )
        val optedIn = RenderModel.from(state, single, 14, dualSignalEnabled = true)
        assertNull(optedIn?.dualMobileLevels)
        assertEquals(1, optedIn?.mobileLevel)
        assertEquals(3, optedIn?.effectiveDataSubscriptionId)

        // The original single-card selection stays unchanged without opt-in.
        val legacy = RenderModel.from(state, single, 14)
        assertNull(legacy?.dualMobileLevels)
        assertEquals(4, legacy?.mobileLevel)
    }

    @Test
    fun optInSingleNeverShowsStaleRemovedSubscriptionWhenOnlyActiveIsUnknown() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val single = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                mode = NativePresentationResolver.Mode.SINGLE,
                activeSubscriptionIds = listOf(3),
                platformActiveSubscriptionIds = setOf(3),
                slotSubscriptionIds = emptyMap(),
                visibleRoots = 1,
                activeBoundRoots = 1,
            ),
        )
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Unknown),
            ),
        )
        val model = RenderModel.from(state, single, 14, dualSignalEnabled = true)
        assertNull(model?.dualMobileLevels)
        assertNull(model?.mobileLevel)
        assertEquals(3, model?.effectiveDataSubscriptionId)

        val recovered = RenderModel.from(
            state.copy(mobile = state.mobile + (
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2))
            )),
            single,
            14,
            dualSignalEnabled = true,
        )
        assertEquals(2, recovered?.mobileLevel)
        assertNull(recovered?.dualMobileLevels)
    }

    @Test
    fun optInSingleRespectsPhysicalSecondSlotWhenItIsOnlyRemainingSim() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val single = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                mode = NativePresentationResolver.Mode.SINGLE,
                activeSubscriptionIds = listOf(3),
                platformActiveSubscriptionIds = setOf(3),
                slotSubscriptionIds = emptyMap(),
                visibleRoots = 1,
                activeBoundRoots = 1,
            ),
        )
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )
        val model = RenderModel.from(state, single, 14, dualSignalEnabled = true)
        assertNull(model?.dualMobileLevels)
        assertEquals(2, model?.mobileLevel)
        assertEquals(3, model?.effectiveDataSubscriptionId)
    }

    @Test
    fun optInPartialNativeBindingDoesNotClaimOnlyPhysicalSim() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        // Native SystemUI exposes one root while SubscriptionManager still reports two cards.
        val partial = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                mode = NativePresentationResolver.Mode.SINGLE,
                activeSubscriptionIds = listOf(3),
                slotSubscriptionIds = emptyMap(),
                visibleRoots = 1,
                activeBoundRoots = 1,
                networkTypeSubscriptionId = 3,
            ),
        )
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(1)),
            ),
        )

        val optIn = RenderModel.from(state, partial, 14, dualSignalEnabled = true)
        assertNull(optIn?.dualMobileLevels)
        assertNull(optIn?.mobileLevel)
        assertEquals(-1, optIn?.effectiveDataSubscriptionId)
        assertTrue(optIn?.centerIndicator is CenterIndicator.Empty)

        // The shipped single-SIM path retains native's selected data sub (3).
        val legacy = RenderModel.from(state, partial, 14)
        assertEquals(1, legacy?.mobileLevel)
        assertEquals(3, legacy?.effectiveDataSubscriptionId)

        // Only after the platform reports a truly removed SIM may opt-in use
        // the one surviving bound subscription as single-card fallback.
        val removed = partial.copy(
            mobilePresentation = partial.mobilePresentation?.copy(
                platformActiveSubscriptionIds = setOf(3),
            ),
        )
        val restored = RenderModel.from(state, removed, 14, dualSignalEnabled = true)
        assertNull(restored?.dualMobileLevels)
        assertEquals(1, restored?.mobileLevel)
        assertEquals(3, restored?.effectiveDataSubscriptionId)
    }

    @Test
    fun optInDualRejectsStaleDataSimOutsideAuthoritativeActiveSet() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                99 to StatusStateStore.MobileState(signal = SignalStrength.Level(3)),
            ),
        )

        val stale = RenderModel.from(state, native, 99, dualSignalEnabled = true)
        assertEquals(DualMobileLevels(sim1 = 2, sim2 = 4), stale?.dualMobileLevels)
        assertEquals(-1, stale?.effectiveDataSubscriptionId)
        assertNull(stale?.mobileLevel)
        assertTrue(stale?.centerIndicator is CenterIndicator.Empty)

        val switched = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        assertEquals(DualMobileLevels(sim1 = 2, sim2 = 4), switched?.dualMobileLevels)
        assertEquals(3, switched?.effectiveDataSubscriptionId)
        assertEquals(4, switched?.mobileLevel)
        assertEquals("5G", (switched?.centerIndicator as? CenterIndicator.MobileType)?.label)

        // The shipped single-row path still follows the native presentation policy.
        val legacy = RenderModel.from(state, native, 99)
        assertEquals(3, legacy?.effectiveDataSubscriptionId)
        assertEquals(4, legacy?.mobileLevel)
    }

    @Test
    fun optInDualNeverBorrowsOtherCardWhenDataSignalIsUnknown() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Unknown),
            ),
        )

        val dual = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        assertEquals(DualMobileLevels(4, null), dual?.dualMobileLevels)
        assertNull(dual?.mobileLevel)
        assertEquals(3, dual?.effectiveDataSubscriptionId)

        val missing = RenderModel.from(
            state.copy(mobile = state.mobile - 3),
            native,
            3,
            dualSignalEnabled = true,
        )
        assertNull(missing?.dualMobileLevels)
        assertNull(missing?.mobileLevel)
        assertEquals(3, missing?.effectiveDataSubscriptionId)

        // The shipped single-row path retains its existing fallback.
        val legacy = RenderModel.from(state, native, 3)
        assertEquals(4, legacy?.mobileLevel)
        assertEquals(14, legacy?.effectiveDataSubscriptionId)
    }

    @Test
    fun optInDualDoesNotTreatNativeFallbackAsDefaultDataSim() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )

        val dual = RenderModel.from(state, native, -1, dualSignalEnabled = true)
        assertEquals(DualMobileLevels(sim1 = 4, sim2 = 2), dual?.dualMobileLevels)
        assertNull(dual?.mobileLevel)
        assertEquals(-1, dual?.effectiveDataSubscriptionId)

        val legacy = RenderModel.from(state, native, -1)
        assertEquals(2, legacy?.mobileLevel)
        assertEquals(3, legacy?.effectiveDataSubscriptionId)
    }

    @Test
    fun optInDualNetworkTypeNeverBorrowsAggregateRootsOtherSim() {
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val wrong = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                networkTypeSubscriptionId = 14,
            ),
        )
        val dual = RenderModel.from(state, wrong, 3, dualSignalEnabled = true)

        assertTrue(dual?.centerIndicator is CenterIndicator.Empty)
        assertEquals(DualMobileLevels(4, 2), dual?.dualMobileLevels)
        assertEquals(3, dual?.effectiveDataSubscriptionId)

        val unknownTransport = RenderModel.from(
            state,
            wrong.copy(connectivity = SysUiConnectivitySource.State.Unknown),
            3,
            dualSignalEnabled = true,
        )
        assertTrue(unknownTransport?.centerIndicator is CenterIndicator.Empty)

        // Wi-Fi handoff remains unresolved rather than being cleared by a SIM mismatch.
        val wifiHandoff = RenderModel.from(
            state,
            wrong.copy(
                connectivity = connectivity(
                    transport = SysUiConnectivitySource.Transport.WIFI,
                    validated = true,
                ),
            ),
            3,
            dualSignalEnabled = true,
        )
        assertNull(wifiHandoff)

        val correct = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        assertEquals("5G", (correct?.centerIndicator as? CenterIndicator.MobileType)?.label)

        // The shipped single-row path continues to show the observed native label.
        val legacy = RenderModel.from(state, wrong, 3)
        assertEquals("5G", (legacy?.centerIndicator as? CenterIndicator.MobileType)?.label)
    }

    @Test
    fun optInUnknownDataIdentityCannotShowArbitraryMobileType() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )
        val model = RenderModel.from(state, native, -1, dualSignalEnabled = true)
        assertTrue(model?.centerIndicator is CenterIndicator.Empty)
        assertEquals(DualMobileLevels(4, 2), model?.dualMobileLevels)
        assertEquals(-1, model?.effectiveDataSubscriptionId)

        // Unknown data-SIM identity must not suppress an unrelated Wi-Fi indicator.
        val wifi = state.copy(
            wifi = StatusStateStore.WifiState.Visible(
                iconResId = 12,
                signal = SignalStrength.Level(3),
                internetValidated = true,
            ),
        )
        val withWifi = RenderModel.from(wifi, native, -1, dualSignalEnabled = true)
        assertTrue(withWifi?.centerIndicator is CenterIndicator.Wifi)
    }

    private fun dualPresentation(
        slotSubscriptionIds: Map<Int, Int>,
    ) = PresentationStore.Snapshot(
        connectivity = connectivity(
            transport = SysUiConnectivitySource.Transport.CELLULAR,
            validated = true,
        ),
        mobilePresentation = NativePresentationResolver.Snapshot(
            mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
            boundRoots = 2,
            activeBoundRoots = 2,
            visibleRoots = 1,
            activeSubscriptionIds = listOf(3, 14),
            slotSubscriptionIds = slotSubscriptionIds,
            activeSubsAuthoritative = true,
            platformActiveSubscriptionIds = setOf(3, 14),
            presentationRootSubscriptionId = 14,
            effectiveDataSubscriptionId = 3,
            networkTypeSubscriptionId = 3,
            networkType = mobileType("5G"),
        ),
    )

    private fun snapshot(
        wifi: StatusStateStore.WifiState,
        mobile: Map<Int, StatusStateStore.MobileState>,
        airplaneMode: Boolean? = false,
    ) =
        StatusStateStore.Snapshot(
            battery =
                StatusStateStore.BatteryState(
                    percent = 80,
                    charging = false,
                ),
            wifi = wifi,
            mobile = mobile,
            airplaneMode = airplaneMode,
        )

    private fun connectivity(
        transport: SysUiConnectivitySource.Transport,
        validated: Boolean,
        mobileDataEnabled: Boolean? = true,
    ) =
        SysUiConnectivitySource.State(
            known = true,
            transport = transport,
            validated = validated,
            hasInternetCapability = validated,
            mobileDataEnabled = mobileDataEnabled,
        )

    private fun presentation(
        connectivity: SysUiConnectivitySource.State,
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
