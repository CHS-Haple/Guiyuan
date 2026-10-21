package com.chaners.guiyuan.xposed.battery

import com.chaners.guiyuan.settings.validLowBatteryThreshold

internal enum class BatterySemanticState {
    NORMAL,
    CHARGING,
    POWER_SAVE,
    SUPER_POWER_SAVE,
    PERFORMANCE,
    LOW,
    ;

    companion object {
        // Only ordinary and native LOW states may change with the user threshold.
        fun atThreshold(native: BatterySemanticState?, percent: Int, threshold: Int): BatterySemanticState =
            when (native) {
                NORMAL, LOW -> if (percent < validLowBatteryThreshold(threshold)) LOW else NORMAL
                null -> NORMAL
                else -> native
            }

        fun fromNativeProgressStatus(statusName: String?): BatterySemanticState? {
            val normalized = statusName?.removeSuffix("_DARK") ?: return null
            return when (normalized) {
                "CHARGING",
                "QUICK_CHARGING",
                "PERF_CHARGE_MODE",
                "PERF_QC_MODE",
                -> CHARGING
                "POWER_SAVE" -> POWER_SAVE
                "SUPER_POWER_SAVE",
                "SUPER_POWER_SAVE_MODE",
                "SUPER_SAVE",
                "ULTRA_POWER_SAVE",
                -> SUPER_POWER_SAVE
                "PERFORMANCE_MODE" -> PERFORMANCE
                "LOW" -> LOW
                "NORMAL" -> NORMAL
                else -> null
            }
        }
    }
}
