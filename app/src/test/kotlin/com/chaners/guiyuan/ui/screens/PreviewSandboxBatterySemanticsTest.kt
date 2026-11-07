package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSandboxBatterySemanticsTest {
    @Test
    fun chargingTakesPriorityOverEveryBatteryModeAndLowThreshold() {
        for (mode in PreviewBatteryMode.entries) {
            for (charging in listOf(
                PreviewChargingState.CHARGING,
                PreviewChargingState.SUPER_FAST_CHARGING,
            )) {
                val preview = PreviewSandboxUiState(
                    chargingState = charging,
                    batteryMode = mode,
                    batteryPercent = 0,
                )
                assertEquals(
                    BatterySemanticState.CHARGING,
                    preview.previewBatterySemanticState(lowBatteryThreshold = 30),
                )
            }
        }
    }

    @Test
    fun powerModesRemainAuthoritativeEvenBelowLowThreshold() {
        val modes = listOf(
            PreviewBatteryMode.POWER_SAVE to BatterySemanticState.POWER_SAVE,
            PreviewBatteryMode.PERFORMANCE to BatterySemanticState.PERFORMANCE,
            PreviewBatteryMode.SUPER_POWER_SAVE to BatterySemanticState.SUPER_POWER_SAVE,
        )
        for ((mode, expected) in modes) {
            val preview = PreviewSandboxUiState(batteryMode = mode, batteryPercent = 1)
            assertEquals(expected, preview.previewBatterySemanticState(lowBatteryThreshold = 30))
        }
    }

    @Test
    fun balancedModeUsesStrictLowThreshold() {
        val preview = PreviewSandboxUiState(
            chargingState = PreviewChargingState.NOT_CHARGING,
            batteryMode = PreviewBatteryMode.BALANCED,
            batteryPercent = 19,
        )
        assertEquals(BatterySemanticState.LOW, preview.previewBatterySemanticState(20))
        assertEquals(
            BatterySemanticState.NORMAL,
            preview.copy(batteryPercent = 20).previewBatterySemanticState(20),
        )
        assertEquals(
            BatterySemanticState.NORMAL,
            preview.previewBatterySemanticState(5),
        )
    }
}
