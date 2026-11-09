package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.SignalStrength
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.dualPresentation
import com.chaners.guiyuan.xposed.RenderModelTestFixtures.snapshot

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
