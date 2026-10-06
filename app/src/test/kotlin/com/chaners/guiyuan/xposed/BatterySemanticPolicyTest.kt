package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySemanticPolicyTest {
    @Test
    fun mapsNativeProgressStatusesWithoutReconstructingPriority() {
        assertEquals(
            BatterySemanticState.CHARGING,
            BatterySemanticPolicy.fromNativeProgressStatus("QUICK_CHARGING"),
        )
        assertEquals(
            BatterySemanticState.CHARGING,
            BatterySemanticPolicy.fromNativeProgressStatus("PERF_CHARGE_MODE"),
        )
        assertEquals(
            BatterySemanticState.POWER_SAVE,
            BatterySemanticPolicy.fromNativeProgressStatus("POWER_SAVE"),
        )
        assertEquals(
            BatterySemanticState.SUPER_POWER_SAVE,
            BatterySemanticPolicy.fromNativeProgressStatus("SUPER_POWER_SAVE"),
        )
        assertEquals(
            BatterySemanticState.PERFORMANCE,
            BatterySemanticPolicy.fromNativeProgressStatus("PERFORMANCE_MODE"),
        )
        assertEquals(
            BatterySemanticState.LOW,
            BatterySemanticPolicy.fromNativeProgressStatus("LOW"),
        )
        assertEquals(
            BatterySemanticState.NORMAL,
            BatterySemanticPolicy.fromNativeProgressStatus("NORMAL_DARK"),
        )
        assertNull(BatterySemanticPolicy.fromNativeProgressStatus("UNKNOWN"))
    }
}
