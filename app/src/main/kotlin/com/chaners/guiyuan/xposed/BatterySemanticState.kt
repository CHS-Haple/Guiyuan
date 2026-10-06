package com.chaners.guiyuan.xposed

internal enum class BatterySemanticState {
    NORMAL,
    CHARGING,
    POWER_SAVE,
    SUPER_POWER_SAVE,
    PERFORMANCE,
    LOW,
}

internal object BatterySemanticPolicy {
    fun fromNativeProgressStatus(
        statusName: String?,
    ): BatterySemanticState? {
        val normalized = statusName?.removeSuffix("_DARK") ?: return null
        return when (normalized) {
            "CHARGING",
            "QUICK_CHARGING",
            "PERF_CHARGE_MODE",
            "PERF_QC_MODE",
            -> BatterySemanticState.CHARGING
            "POWER_SAVE" -> BatterySemanticState.POWER_SAVE
            "SUPER_POWER_SAVE",
            "SUPER_POWER_SAVE_MODE",
            "SUPER_SAVE",
            "ULTRA_POWER_SAVE",
            -> BatterySemanticState.SUPER_POWER_SAVE
            "PERFORMANCE_MODE" -> BatterySemanticState.PERFORMANCE
            "LOW" -> BatterySemanticState.LOW
            "NORMAL" -> BatterySemanticState.NORMAL
            else -> null
        }
    }
}
