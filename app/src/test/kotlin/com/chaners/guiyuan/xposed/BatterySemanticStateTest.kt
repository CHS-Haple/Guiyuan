package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySemanticStateTest {
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
