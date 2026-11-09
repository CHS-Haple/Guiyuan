package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.connectivity
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.presentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.mobileType

class RenderModelTest {
    @Test
    fun nativeLowColorAndThresholdReclassificationAreConsistent() {
        val native = StatusStateStore.BatteryState(
            percent = 24,
            charging = false,
            semanticState = BatterySemanticState.NORMAL,
            systemSemanticColor = null,
            lowSystemColor = 0xFFFF5500.toInt(),
        )
        fun model(battery: StatusStateStore.BatteryState, threshold: Int) =
            RenderModel.from(
                // Airplane provides a complete native center; the test only varies battery semantics.
                snapshot = StatusStateStore.Snapshot(battery = battery, airplaneMode = true),
                presentation = PresentationStore.Snapshot(),
                defaultDataSubscriptionId = -1,
                lowBatteryThreshold = threshold,
            )

        val early = model(native, 30)
        assertEquals(BatterySemanticState.LOW, early?.batterySemanticState)
        assertEquals(native.lowSystemColor, early?.batterySystemSemanticColor)
        val normal = model(native, 20)
        assertEquals(BatterySemanticState.NORMAL, normal?.batterySemanticState)
        assertEquals(null, normal?.batterySystemSemanticColor)

        val nativeLow = native.copy(
            percent = 12,
            semanticState = BatterySemanticState.LOW,
            systemSemanticColor = 0xFFFF5500.toInt(),
        )
        val suppressed = model(nativeLow, 5)
        assertEquals(BatterySemanticState.NORMAL, suppressed?.batterySemanticState)
        assertEquals(null, suppressed?.batterySystemSemanticColor)
        val retained = model(nativeLow, 20)
        assertEquals(BatterySemanticState.LOW, retained?.batterySemanticState)
        assertEquals(nativeLow.lowSystemColor, retained?.batterySystemSemanticColor)
    }

    @Test
    fun nativeActivityDrawableFollowsOnlyTheSelectedTransport() {
        val observed = StatusStateStore.Snapshot(
            battery = StatusStateStore.BatteryState(percent = 81, charging = true),
            wifi = StatusStateStore.WifiState.Visible(
                iconResId = 11,
                signal = SignalStrength.Level(3),
            ),
            wifiActivityResId = 120,
            mobile = mapOf(
                1 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
            ),
            mobileActivityResIds = mapOf(1 to 220),
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
            airplaneMode = false,
        )
        val wifiPresentation = presentation(
            connectivity = connectivity(SysUiConnectivitySource.Transport.WIFI, true),
            networkType = mobileType("5G"),
        )
        val wifiModel = RenderModel.from(observed, wifiPresentation, 1)
        assertTrue(wifiModel?.centerIndicator is CenterIndicator.Wifi)
        assertEquals(120, wifiModel?.networkActivityResId)

        val noWifiActivity = RenderModel.from(
            observed.copy(wifiActivityResId = null),
            wifiPresentation,
            1,
        )
        assertNull(noWifiActivity?.networkActivityResId)

        val mobileModel = RenderModel.from(
            observed.copy(
                wifi = StatusStateStore.WifiState.Hidden,
                wifiActivityResId = 120,
            ),
            presentation(
                connectivity = connectivity(SysUiConnectivitySource.Transport.CELLULAR, true),
                networkType = mobileType("5G"),
            ),
            1,
        )
        assertTrue(mobileModel?.centerIndicator is CenterIndicator.MobileType)
        assertEquals(220, mobileModel?.networkActivityResId)
    }

    @Test
    fun defaultNetworkLossDoesNotOverrideNativeActivityVisibility() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()

        val nativeStillVisible = RenderModel.from(observed, lost, 1)
        assertTrue(nativeStillVisible?.centerIndicator is CenterIndicator.Empty)
        assertEquals(220, nativeStillVisible?.networkActivityResId)

        // Changing Android's default-route data flag is not a native GONE.
        assertEquals(
            220,
            RenderModel.from(observed, vpnHandoffPresentation(mobileDataEnabled = false), 1)
                ?.networkActivityResId,
        )
    }

    @Test
    fun nativeMobileActivityVisibilityOutranksTemporaryWifiStateWhenCenterIsEmpty() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()
        for (wifi in listOf(
            StatusStateStore.WifiState.Unknown,
            StatusStateStore.WifiState.Visible(
                iconResId = 12,
                signal = SignalStrength.Level(3),
            ),
        )) {
            val nativeVisible = RenderModel.from(observed.copy(wifi = wifi), lost, 1)
            assertTrue(nativeVisible?.centerIndicator is CenterIndicator.Empty)
            assertEquals(220, nativeVisible?.networkActivityResId)

            val nativeHidden = RenderModel.from(
                observed.copy(wifi = wifi, mobileActivityShownIds = emptySet()),
                lost,
                1,
            )
            assertNull(nativeHidden?.networkActivityResId)
        }
    }

    @Test
    fun nativeDrawableWithoutVisibilityConfirmationRemainsHidden() {
        val observed = vpnHandoffActivity().copy(
            mobileActivityShownIds = emptySet(),
            mobileActivityVisibilityObservedIds = emptySet(),
        )
        val lost = vpnHandoffPresentation()
        // Resource can arrive before the independent native visibility collector.
        // Never treat its presence as an implicit native VISIBLE.
        assertNull(RenderModel.from(observed, lost, 1)?.networkActivityResId)
        val confirmed = observed.copy(
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
        )
        assertEquals(220, RenderModel.from(confirmed, lost, 1)?.networkActivityResId)
    }

    @Test
    fun nativeVisibilityBeforeResourceWaitsForDrawableThenDisplays() {
        val observed = vpnHandoffActivity().copy(mobileActivityResIds = emptyMap())
        val lost = vpnHandoffPresentation()
        // Native VISIBLE alone cannot manufacture a drawable.
        assertNull(RenderModel.from(observed, lost, 1)?.networkActivityResId)
        // The subsequent native drawable must be accepted without waiting for
        // a second visibility emission from the independent collector.
        val drawableArrived = observed.copy(mobileActivityResIds = mapOf(1 to 220))
        assertEquals(220, RenderModel.from(drawableArrived, lost, 1)?.networkActivityResId)
    }

    @Test
    fun nativeVisibilityAndResourceAbsenceIndependentlyHideActivity() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()

        // The visibility collector can hide the View before a resource event.
        val nativeGone = RenderModel.from(
            observed.copy(mobileActivityShownIds = emptySet()),
            lost,
            1,
        )
        assertNull(nativeGone?.networkActivityResId)

        // A missing native drawable is independently sufficient to hide it.
        assertNull(
            RenderModel.from(
                observed.copy(mobileActivityResIds = emptyMap()), lost, 1,
            )?.networkActivityResId,
        )
    }

    @Test
    fun airplaneAndUnavailableSignalSuppressMobileActivity() {
        val observed = vpnHandoffActivity()
        val lost = vpnHandoffPresentation()

        assertNull(
            RenderModel.from(observed.copy(airplaneMode = true), lost, 1)
                ?.networkActivityResId,
        )
        assertNull(
            RenderModel.from(
                observed.copy(mobile = mapOf(
                    1 to StatusStateStore.MobileState(signal = SignalStrength.Unavailable),
                )),
                lost,
                1,
            )?.networkActivityResId,
        )
    }

    private fun vpnHandoffActivity(): StatusStateStore.Snapshot =
        StatusStateStore.Snapshot(
            battery = StatusStateStore.BatteryState(percent = 81, charging = false),
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                1 to StatusStateStore.MobileState(signal = SignalStrength.Level(3)),
            ),
            mobileActivityResIds = mapOf(1 to 220),
            mobileActivityObservedIds = setOf(1),
            mobileActivityShownIds = setOf(1),
            mobileActivityVisibilityObservedIds = setOf(1),
            airplaneMode = false,
        )

    private fun vpnHandoffPresentation(mobileDataEnabled: Boolean = true): PresentationStore.Snapshot =
        presentation(
            connectivity = connectivity(
                transport = SysUiConnectivitySource.Transport.NONE,
                validated = false,
                mobileDataEnabled = mobileDataEnabled,
            ),
            networkType = mobileType("5G"),
        )

    @Test
    fun aggregateActivityReadsSoleVisibleNativeRootEvenWhenDataSimIsSim2() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        ).copy(
            mobileActivityResIds = mapOf(14 to 220),
            mobileActivityShownIds = setOf(14),
            mobileActivityVisibilityObservedIds = setOf(14),
        )

        val aggregated = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        assertTrue(aggregated?.centerIndicator is CenterIndicator.MobileType)
        assertEquals(3, aggregated?.effectiveDataSubscriptionId)
        assertEquals(220, aggregated?.networkActivityResId)

        // A native hide event must clear rather than retain the previous icon.
        val idle = RenderModel.from(
            state.copy(mobileActivityResIds = emptyMap()),
            native,
            3,
            dualSignalEnabled = true,
        )
        assertNull(idle?.networkActivityResId)
    }

    @Test
    fun aggregatedNativeResourceEventShowsOnlyItsVisibleRootWithoutAnotherFlowEmission() {
        StatusStateStore.restoreHotReloadState(null)
        try {
            val native = dualPresentation(mapOf(0 to 1, 1 to 4))
            // The OEM aggregate displays activity on SIM1's root even when SIM2
            // owns mobile data; the resource collector may arrive first.
            StatusStateStore.updateMobileActivityVisibility(1, 2131237576, true)
            StatusStateStore.updateMobileActivityVisibility(4, 2131237576, false)
            val observed = StatusStateStore.snapshot().copy(
                battery = StatusStateStore.BatteryState(percent = 100, charging = false),
                wifi = StatusStateStore.WifiState.Hidden,
                mobile = mapOf(
                    1 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                    4 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                ),
                airplaneMode = false,
            )
            val shown = RenderModel.from(observed, native, 4, dualSignalEnabled = true)
            assertEquals(4, shown?.effectiveDataSubscriptionId)
            assertEquals(2131237576, shown?.networkActivityResId)

            val hidden = StatusStateStore.updateMobileActivityVisibility(1, 2131237576, false)
                ?: error("native GONE must update the aggregate activity")
            assertNull(
                RenderModel.from(
                    hidden.copy(
                        battery = observed.battery,
                        wifi = observed.wifi,
                        mobile = observed.mobile,
                        airplaneMode = observed.airplaneMode,
                    ),
                    native, 4, dualSignalEnabled = true,
                )?.networkActivityResId,
            )
        } finally {
            StatusStateStore.restoreHotReloadState(null)
        }
    }

    @Test
    fun separateAndUnverifiedMobileLayoutsDoNotBorrowAnotherSimsActivity() {
        val aggregated = dualPresentation(mapOf(0 to 14, 1 to 3))
        val native = aggregated.mobilePresentation!!
        val state = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        ).copy(
            mobileActivityResIds = mapOf(14 to 220, 3 to 330),
            mobileActivityShownIds = setOf(14, 3),
            mobileActivityVisibilityObservedIds = setOf(14, 3),
        )

        val aggregate = RenderModel.from(state, aggregated, 3, dualSignalEnabled = true)
        assertEquals(220, aggregate?.networkActivityResId)

        val separate = RenderModel.from(
            state,
            aggregated.copy(mobilePresentation = native.copy(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                visibleRoots = 2,
            )),
            3,
            dualSignalEnabled = true,
        )
        assertEquals(330, separate?.networkActivityResId)

        val notAuthoritative = RenderModel.from(
            state,
            aggregated.copy(mobilePresentation = native.copy(activeSubsAuthoritative = false)),
            3,
            dualSignalEnabled = true,
        )
        assertEquals(330, notAuthoritative?.networkActivityResId)
    }

    @Test
    fun dualRowsFollowPlatformSlotOrderIndependentlyOfDataSim() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = dualState()

        val dataSim1 = RenderModel.from(state, native, 14, dualSignalEnabled = true)
        val dataSim2 = RenderModel.from(state, native, 3, dualSignalEnabled = true)
        val slotLevels = DualMobileLevels(sim1 = 4, sim2 = 2)

        assertEquals(slotLevels, dataSim1?.dualMobileLevels)
        assertEquals(slotLevels, dataSim2?.dualMobileLevels)
        assertEquals(14, dataSim1?.effectiveDataSubscriptionId)
        assertEquals(3, dataSim2?.effectiveDataSubscriptionId)
    }

    @Test
    fun unverifiedPlatformSlotIdentityCannotEnableDualRows() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = dualState()
        fun render(override: NativePresentationResolver.Snapshot) =
            RenderModel.from(
                state,
                native.copy(mobilePresentation = override),
                defaultDataSubscriptionId = 3,
                dualSignalEnabled = true,
            )

        val verified = native.mobilePresentation ?: error("missing native dual presentation")
        assertEquals(DualMobileLevels(4, 2), render(verified)?.dualMobileLevels)
        assertNull(render(verified.copy(activeSubsAuthoritative = false))?.dualMobileLevels)
        assertNull(
            render(
                verified.copy(platformActiveSubscriptionIds = setOf(3, 14, 27)),
            )?.dualMobileLevels,
        )
        assertNull(
            render(verified.copy(slotSubscriptionIds = mapOf(0 to 3, 1 to 3)))?.dualMobileLevels,
        )
    }

    @Test
    fun unknownSignalDimsItsRowButUnavailableSignalInvalidatesDualPair() {
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        val state = dualState(SignalStrength.Unknown)

        assertEquals(
            DualMobileLevels(sim1 = 4, sim2 = null),
            RenderModel.from(state, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
        val unavailable = state.copy(
            mobile = state.mobile + (
                3 to StatusStateStore.MobileState(signal = SignalStrength.Unavailable)
            ),
        )
        assertNull(
            RenderModel.from(unavailable, native, 14, dualSignalEnabled = true)?.dualMobileLevels,
        )
        assertNull(
            RenderModel.from(
                state.copy(airplaneMode = true),
                native,
                14,
                dualSignalEnabled = true,
            )?.dualMobileLevels,
        )
    }

    private fun dualState(
        sim2Signal: SignalStrength = SignalStrength.Level(2),
    ): StatusStateStore.Snapshot =
        snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = sim2Signal),
            ),
        )
}
