package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.InternetState
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.connectivity
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.presentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.mobileType

class RenderModelConnectivityTest {
    @Test
    fun unboundNativeMobileTypeRendersWithoutClaimingSubscriptionOrSignal() {
        val native = NativePresentationResolver.Snapshot(
            mode = NativePresentationResolver.Mode.UNKNOWN,
            boundRoots = 0,
            visibleRoots = 0,
            activeSubscriptionIds = emptyList(),
            presentationRootSubscriptionId = null,
            effectiveDataSubscriptionId = 4,
            networkTypeSubscriptionId = null,
            networkType = mobileType("5G"),
        )
        val model = RenderModel.from(
            snapshot = snapshot(StatusStateStore.WifiState.Hidden, emptyMap()),
            presentation = PresentationStore.Snapshot(
                connectivity = connectivity(SysUiConnectivitySource.Transport.CELLULAR, true),
                mobilePresentation = native,
            ),
            defaultDataSubscriptionId = 4,
        )

        assertEquals("5G", (model?.centerIndicator as? CenterIndicator.MobileType)?.label)
        assertNull(model?.mobileLevel)
        assertFalse(native.nativeMobileReplacementReady)
        assertNull(native.networkTypeSubscriptionId)
    }

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
}
