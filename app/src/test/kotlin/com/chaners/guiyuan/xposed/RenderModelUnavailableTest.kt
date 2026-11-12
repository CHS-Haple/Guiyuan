package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.connectivity
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.presentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.mobileType

// Native no-SIM icons in these scenarios belong to the SystemUI resource package.
private const val NATIVE_SYSTEMUI_PACKAGE = "com.android.systemui"

class RenderModelUnavailableTest {
    @Test
    fun nativeNoSimPresentationOverridesStaleMobileSignalAndType() {
        val noSimIcon =
            PresentationStore.NativeIconResource(
                packageName = NATIVE_SYSTEMUI_PACKAGE,
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
                packageName = NATIVE_SYSTEMUI_PACKAGE,
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
                packageName = NATIVE_SYSTEMUI_PACKAGE,
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
                packageName = NATIVE_SYSTEMUI_PACKAGE,
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
    fun nativeActivityIsWithdrawnDuringRecoveryOrNoSimEvenWhenPreviouslyVisible() {
        val observed =
            snapshot(
                wifi = StatusStateStore.WifiState.Hidden,
                mobile = mapOf(
                    1 to StatusStateStore.MobileState(signal = SignalStrength.Level(3)),
                ),
            ).copy(
                mobileActivityResIds = mapOf(1 to 220),
                mobileActivityShownIds = setOf(1),
                mobileActivityVisibilityObservedIds = setOf(1),
            )
        val native =
            presentation(
                connectivity = connectivity(SysUiConnectivitySource.Transport.NONE, false),
                networkType = mobileType("5G"),
            )

        val normal = RenderModel.from(observed, native, 1)
        assertTrue(normal?.centerIndicator is CenterIndicator.Empty)
        assertEquals(220, normal?.networkActivityResId)

        val pending = RenderModel.from(observed.copy(mobileRecoveryPending = true), native, 1)
        assertTrue(pending?.centerIndicator is CenterIndicator.Empty)
        assertNull(pending?.networkActivityResId)

        val noSim =
            RenderModel.from(
                observed,
                native.copy(
                    statusIcons = PresentationStore.StatusIconPresentation(
                        noSimVisible = true,
                        noSimIcon = PresentationStore.NativeIconResource(
                            packageName = NATIVE_SYSTEMUI_PACKAGE,
                            resourceId = 42,
                        ),
                    ),
                ),
                1,
            )
        assertTrue(noSim?.centerIndicator is CenterIndicator.NoSim)
        assertNull(noSim?.networkActivityResId)
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
}
