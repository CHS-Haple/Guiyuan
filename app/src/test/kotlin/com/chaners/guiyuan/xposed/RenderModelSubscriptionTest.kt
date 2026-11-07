package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.SignalStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.mobileType

class RenderModelSubscriptionTest {
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
    fun dualSwitchNeedsFullPlatformIdentityAndKeepsSingleDefault() {
        val source = snapshot(
            wifi = StatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                14 to StatusStateStore.MobileState(signal = SignalStrength.Level(4)),
                3 to StatusStateStore.MobileState(signal = SignalStrength.Level(2)),
            ),
        )
        val native = dualPresentation(mapOf(0 to 14, 1 to 3))
        assertNull(RenderModel.from(source, native, 3)?.dualMobileLevels)
        assertEquals(
            DualMobileLevels(4, 2),
            RenderModel.from(source, native, 3, dualSignalEnabled = true)?.dualMobileLevels,
        )
        val extraPlatform = native.copy(
            mobilePresentation = native.mobilePresentation?.copy(
                platformActiveSubscriptionIds = setOf(14, 3, 27),
            ),
        )
        assertNull(
            RenderModel.from(source, extraPlatform, 3, dualSignalEnabled = true)
                ?.dualMobileLevels,
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
}
