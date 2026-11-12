package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot

class RenderModelDualSimTest {
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
            val base = dualPresentation(mapOf(0 to 1, 1 to 4))
            val native = base.copy(
                mobilePresentation = requireNotNull(base.mobilePresentation).copy(
                    activeSubscriptionIds = listOf(1, 4),
                    platformActiveSubscriptionIds = setOf(1, 4),
                    presentationRootSubscriptionId = 1,
                    effectiveDataSubscriptionId = 4,
                    networkTypeSubscriptionId = 4,
                ),
            )
            // The OEM aggregate displays activity on SIM1's root even when SIM2
            // owns mobile data; the resource collector may arrive first.
            StatusStateStore.updateMobileActivityResource(1, 2131237576, true)
            StatusStateStore.updateMobileActivityResource(4, 2131237576, false)
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

            val hidden = StatusStateStore.updateMobileActivityVisibility(1, false)
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
        assertNull(render(verified.copy(platformActiveSubscriptionIds = null))?.dualMobileLevels)
        assertNull(
            render(
                verified.copy(
                    activeSubscriptionIds = listOf(14, 14),
                    platformActiveSubscriptionIds = setOf(14),
                ),
            )?.dualMobileLevels,
        )
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
