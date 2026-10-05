package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemUiBatterySemanticPolicyTest {
    @Test
    fun mapsNativeProgressStatusesWithoutReconstructingPriority() {
        assertEquals(
            BatterySemanticState.CHARGING,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("QUICK_CHARGING"),
        )
        assertEquals(
            BatterySemanticState.CHARGING,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("PERF_CHARGE_MODE"),
        )
        assertEquals(
            BatterySemanticState.POWER_SAVE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("POWER_SAVE"),
        )
        assertEquals(
            BatterySemanticState.SUPER_POWER_SAVE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("SUPER_POWER_SAVE"),
        )
        assertEquals(
            BatterySemanticState.PERFORMANCE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("PERFORMANCE_MODE"),
        )
        assertEquals(
            BatterySemanticState.LOW,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("LOW"),
        )
        assertEquals(
            BatterySemanticState.NORMAL,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("NORMAL_DARK"),
        )
        assertNull(SystemUiBatterySemanticPolicy.fromNativeProgressStatus("UNKNOWN"))
    }
}
