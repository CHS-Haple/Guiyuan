package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.connectivity

class RenderModelDualSimFallbackTest {
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
}
