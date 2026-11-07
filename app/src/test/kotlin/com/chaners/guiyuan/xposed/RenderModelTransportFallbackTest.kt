package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.InternetState
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

class RenderModelTransportFallbackTest {
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
}
