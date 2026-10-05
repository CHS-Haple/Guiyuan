package com.chaners.guiyuan.settings



internal enum class CombinedStatusContentLayout(
    val persistedValue: String,
) {
    NETWORK_CENTER("network_center"),
    BATTERY_CENTER("battery_center");

    companion object {
        fun fromPersisted(value: String?): CombinedStatusContentLayout =
            entries.firstOrNull { it.persistedValue == value } ?: NETWORK_CENTER
    }
}

internal enum class CombinedStatusBatteryColorPreset(
    val persistedValue: String,
) {
    RECOMMENDED("recommended"),
    HYPEROS("hyperos_native"),
    IOS_STYLE("ios_style");

    companion object {
        fun fromPersisted(value: String?): CombinedStatusBatteryColorPreset =
            entries.firstOrNull { it.persistedValue == value } ?: HYPEROS
    }
}

internal enum class CombinedStatusBatteryColorMode(
    val persistedValue: String,
) {
    PRESET("preset"),
    FOLLOW_SYSTEM("follow_system"),
    CUSTOM("custom");

    companion object {
        fun fromPersisted(value: String?): CombinedStatusBatteryColorMode =
            entries.firstOrNull { it.persistedValue == value } ?: PRESET
    }
}

internal enum class CombinedStatusBatteryColorSlot {
    NORMAL,
    POWER_SAVE,
    PERFORMANCE,
    SUPER_POWER_SAVE,
    CHARGING,
    LOW,
}

internal data class CombinedStatusBatteryColorModes(
    val normal: CombinedStatusBatteryColorMode = CombinedStatusBatteryColorMode.PRESET,
    val powerSave: CombinedStatusBatteryColorMode = CombinedStatusBatteryColorMode.PRESET,
    val performance: CombinedStatusBatteryColorMode = CombinedStatusBatteryColorMode.PRESET,
    val superPowerSave: CombinedStatusBatteryColorMode = CombinedStatusBatteryColorMode.PRESET,
    val charging: CombinedStatusBatteryColorMode = CombinedStatusBatteryColorMode.PRESET,
    val low: CombinedStatusBatteryColorMode = CombinedStatusBatteryColorMode.PRESET,
) {
    fun modeFor(slot: CombinedStatusBatteryColorSlot): CombinedStatusBatteryColorMode =
        when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> normal
            CombinedStatusBatteryColorSlot.POWER_SAVE -> powerSave
            CombinedStatusBatteryColorSlot.PERFORMANCE -> performance
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> superPowerSave
            CombinedStatusBatteryColorSlot.CHARGING -> charging
            CombinedStatusBatteryColorSlot.LOW -> low
        }

    fun withMode(
        slot: CombinedStatusBatteryColorSlot,
        mode: CombinedStatusBatteryColorMode,
    ): CombinedStatusBatteryColorModes =
        when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> copy(normal = mode)
            CombinedStatusBatteryColorSlot.POWER_SAVE -> copy(powerSave = mode)
            CombinedStatusBatteryColorSlot.PERFORMANCE -> copy(performance = mode)
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> copy(superPowerSave = mode)
            CombinedStatusBatteryColorSlot.CHARGING -> copy(charging = mode)
            CombinedStatusBatteryColorSlot.LOW -> copy(low = mode)
        }
}

internal data class CombinedStatusBatteryColorOverrides(
    val normal: Int? = null,
    val powerSave: Int? = null,
    val performance: Int? = null,
    val superPowerSave: Int? = null,
    val charging: Int? = null,
    val low: Int? = null,
) {
    fun colorFor(slot: CombinedStatusBatteryColorSlot): Int? =
        when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> normal
            CombinedStatusBatteryColorSlot.POWER_SAVE -> powerSave
            CombinedStatusBatteryColorSlot.PERFORMANCE -> performance
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> superPowerSave
            CombinedStatusBatteryColorSlot.CHARGING -> charging
            CombinedStatusBatteryColorSlot.LOW -> low
        }

    fun withColor(
        slot: CombinedStatusBatteryColorSlot,
        color: Int?,
    ): CombinedStatusBatteryColorOverrides {
        val opaque = color?.let { it or 0xFF000000.toInt() }
        return when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> copy(normal = opaque)
            CombinedStatusBatteryColorSlot.POWER_SAVE -> copy(powerSave = opaque)
            CombinedStatusBatteryColorSlot.PERFORMANCE -> copy(performance = opaque)
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> copy(superPowerSave = opaque)
            CombinedStatusBatteryColorSlot.CHARGING -> copy(charging = opaque)
            CombinedStatusBatteryColorSlot.LOW -> copy(low = opaque)
        }
    }
}

internal object CombinedStatusRecommendedBatteryPalette {
    val POWER_SAVE = 0xFFD5A623.toInt()
    val PERFORMANCE = 0xFF4A7FC1.toInt()
    val SUPER_POWER_SAVE = 0xFFD8752C.toInt()
    val CHARGING = 0xFF3FA760.toInt()
    val LOW = 0xFFD64A4A.toInt()

    fun colorFor(slot: CombinedStatusBatteryColorSlot): Int? =
        when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> null
            CombinedStatusBatteryColorSlot.POWER_SAVE -> POWER_SAVE
            CombinedStatusBatteryColorSlot.PERFORMANCE -> PERFORMANCE
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> SUPER_POWER_SAVE
            CombinedStatusBatteryColorSlot.CHARGING -> CHARGING
            CombinedStatusBatteryColorSlot.LOW -> LOW
        }
}

internal object CombinedStatusHyperOsBatteryPalette {
    // Verified against pinned SystemUI 17.03.260226.r resources/fields.
    val POWER_SAVE = 0xFFFF9F05.toInt()
    val PERFORMANCE = 0xFF3482FF.toInt()
    val SUPER_POWER_SAVE = POWER_SAVE
    val CHARGING = 0xFF1DCD3A.toInt()
    val LOW = 0xFFFA382E.toInt()

    fun colorFor(slot: CombinedStatusBatteryColorSlot): Int? =
        when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> null
            CombinedStatusBatteryColorSlot.POWER_SAVE -> POWER_SAVE
            CombinedStatusBatteryColorSlot.PERFORMANCE -> PERFORMANCE
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> SUPER_POWER_SAVE
            CombinedStatusBatteryColorSlot.CHARGING -> CHARGING
            CombinedStatusBatteryColorSlot.LOW -> LOW
        }
}

internal object CombinedStatusIosStyleBatteryPalette {
    val POWER_SAVE = 0xFFFFCC00.toInt()
    val PERFORMANCE = 0xFF007AFF.toInt()
    val SUPER_POWER_SAVE = 0xFFFF9500.toInt()
    val CHARGING = 0xFF34C759.toInt()
    val LOW = 0xFFFF3B30.toInt()

    fun colorFor(slot: CombinedStatusBatteryColorSlot): Int? =
        when (slot) {
            CombinedStatusBatteryColorSlot.NORMAL -> null
            CombinedStatusBatteryColorSlot.POWER_SAVE -> POWER_SAVE
            CombinedStatusBatteryColorSlot.PERFORMANCE -> PERFORMANCE
            CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> SUPER_POWER_SAVE
            CombinedStatusBatteryColorSlot.CHARGING -> CHARGING
            CombinedStatusBatteryColorSlot.LOW -> LOW
        }
}

internal data class CombinedStatusVisualSettings(
    val contentLayout: CombinedStatusContentLayout = CombinedStatusContentLayout.NETWORK_CENTER,
    val mobileFollowsBatteryColor: Boolean = false,
    val centerFollowsBatteryColor: Boolean = false,
    val batteryTopReadoutEnabled: Boolean = false,
    val batteryTopTextFollowsBatteryColor: Boolean = true,
    val batteryTopChargingIconEnabled: Boolean = true,
    val batteryTopChargingIconFollowsBatteryColor: Boolean = true,
    val batteryFillFollowsRetractEndpoint: Boolean = false,
    val controlCenterTintTransitionEnabled: Boolean = true,
    val batteryTopTextScale: Float = batteryTopTextScaleDefault(contentLayout),
    val batteryTopTextWeight: Int = BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
    val batteryTopVerticalOffset: Float = BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
    val batteryTopChargingIconScale: Float =
        batteryTopChargingIconScaleDefault(contentLayout),
    val combinedScale: Float = COMBINED_SCALE_DEFAULT,
    val outerWeightScale: Float = OUTER_WEIGHT_SCALE_DEFAULT,
    val wifiSizeScale: Float = WIFI_SIZE_SCALE_DEFAULT,
    val airplaneSizeScale: Float = AIRPLANE_SIZE_SCALE_DEFAULT,
    val noSimSizeScale: Float = NO_SIM_SIZE_SCALE_DEFAULT,
    val mobileTypeSizeScale: Float = mobileTypeSizeScaleDefault(contentLayout),
    val mobileTypeWeight: Int = MOBILE_TYPE_WEIGHT_DEFAULT,
    val batteryColorPreset: CombinedStatusBatteryColorPreset =
        CombinedStatusBatteryColorPreset.HYPEROS,
    val batteryColorModes: CombinedStatusBatteryColorModes =
        CombinedStatusBatteryColorModes(),
    val batteryColorOverrides: CombinedStatusBatteryColorOverrides =
        CombinedStatusBatteryColorOverrides(),
)

internal fun CombinedStatusVisualSettings.normalized(): CombinedStatusVisualSettings =
    copy(
        batteryTopTextScale =
            batteryTopTextScale.coerceIn(
                BATTERY_TOP_TEXT_SCALE_MIN,
                BATTERY_TOP_TEXT_SCALE_MAX,
            ),
        batteryTopTextWeight =
            batteryTopTextWeight.coerceIn(
                BATTERY_TOP_TEXT_WEIGHT_MIN,
                BATTERY_TOP_TEXT_WEIGHT_MAX,
            ),
        batteryTopVerticalOffset =
            batteryTopVerticalOffset.coerceIn(
                BATTERY_TOP_VERTICAL_OFFSET_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_MAX,
            ),
        batteryTopChargingIconScale =
            batteryTopChargingIconScale.coerceIn(
                BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
                BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
            ),
        combinedScale = combinedScale.coerceIn(COMBINED_SCALE_MIN, COMBINED_SCALE_MAX),
        outerWeightScale =
            outerWeightScale.coerceIn(OUTER_WEIGHT_SCALE_MIN, OUTER_WEIGHT_SCALE_MAX),
        wifiSizeScale = wifiSizeScale.coerceIn(WIFI_SIZE_SCALE_MIN, WIFI_SIZE_SCALE_MAX),
        airplaneSizeScale =
            airplaneSizeScale.coerceIn(AIRPLANE_SIZE_SCALE_MIN, AIRPLANE_SIZE_SCALE_MAX),
        noSimSizeScale =
            noSimSizeScale.coerceIn(NO_SIM_SIZE_SCALE_MIN, NO_SIM_SIZE_SCALE_MAX),
        mobileTypeSizeScale =
            mobileTypeSizeScale.coerceIn(MOBILE_TYPE_SIZE_SCALE_MIN, MOBILE_TYPE_SIZE_SCALE_MAX),
        mobileTypeWeight =
            mobileTypeWeight.coerceIn(MOBILE_TYPE_WEIGHT_MIN, MOBILE_TYPE_WEIGHT_MAX),
        batteryColorOverrides =
            CombinedStatusBatteryColorSlot.entries.fold(
                CombinedStatusBatteryColorOverrides(),
            ) { overrides, slot ->
                overrides.withColor(slot, batteryColorOverrides.colorFor(slot))
            },
    )
