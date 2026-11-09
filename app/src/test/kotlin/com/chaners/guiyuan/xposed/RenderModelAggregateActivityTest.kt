package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SignalStrength
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderModelAggregateActivityTest {
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

}
