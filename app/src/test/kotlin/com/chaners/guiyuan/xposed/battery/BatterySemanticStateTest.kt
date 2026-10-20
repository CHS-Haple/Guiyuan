package com.chaners.guiyuan.xposed.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySemanticStateTest {
    @Test
    fun customLowThresholdUsesStrictComparison() {
        val normal = BatterySemanticState.NORMAL
        val nativeLow = BatterySemanticState.LOW
        for (threshold in listOf(5, 20, 30)) {
            assertEquals(BatterySemanticState.LOW, BatterySemanticState.atThreshold(normal, threshold - 1, threshold))
            assertEquals(BatterySemanticState.NORMAL, BatterySemanticState.atThreshold(nativeLow, threshold, threshold))
            assertEquals(BatterySemanticState.NORMAL, BatterySemanticState.atThreshold(nativeLow, threshold + 1, threshold))
        }
        // A corrupt remote preference falls back to the native-compatible 20% setting.
        assertEquals(BatterySemanticState.LOW, BatterySemanticState.atThreshold(normal, 19, -10))
        assertEquals(BatterySemanticState.NORMAL, BatterySemanticState.atThreshold(nativeLow, 20, 31))
    }

    @Test
    fun lowThresholdNeverOverridesNativePriority() {
        for (state in listOf(
            BatterySemanticState.CHARGING,
            BatterySemanticState.POWER_SAVE,
            BatterySemanticState.SUPER_POWER_SAVE,
            BatterySemanticState.PERFORMANCE,
        )) {
            assertEquals(state, BatterySemanticState.atThreshold(state, 0, 30))
            assertEquals(state, BatterySemanticState.atThreshold(state, 30, 5))
        }
        assertEquals(BatterySemanticState.NORMAL, BatterySemanticState.atThreshold(null, 0, 20))
    }

    @Test
    fun mapsNativeProgressStatusesWithoutReconstructingPriority() {
        assertEquals(
            BatterySemanticState.CHARGING,
            BatterySemanticState.fromNativeProgressStatus("QUICK_CHARGING"),
        )
        assertEquals(
            BatterySemanticState.CHARGING,
            BatterySemanticState.fromNativeProgressStatus("PERF_CHARGE_MODE"),
        )
        assertEquals(
            BatterySemanticState.POWER_SAVE,
            BatterySemanticState.fromNativeProgressStatus("POWER_SAVE"),
        )
        assertEquals(
            BatterySemanticState.SUPER_POWER_SAVE,
            BatterySemanticState.fromNativeProgressStatus("SUPER_POWER_SAVE"),
        )
        assertEquals(
            BatterySemanticState.PERFORMANCE,
            BatterySemanticState.fromNativeProgressStatus("PERFORMANCE_MODE"),
        )
        assertEquals(
            BatterySemanticState.LOW,
            BatterySemanticState.fromNativeProgressStatus("LOW"),
        )
        assertEquals(
            BatterySemanticState.NORMAL,
            BatterySemanticState.fromNativeProgressStatus("NORMAL_DARK"),
        )
        assertNull(BatterySemanticState.fromNativeProgressStatus("UNKNOWN"))
    }
}
