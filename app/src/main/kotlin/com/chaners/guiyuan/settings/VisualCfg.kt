package com.chaners.guiyuan.settings



internal enum class ContentLayout(
    val persistedValue: String,
) {
    NETWORK_CENTER("network_center"),
    BATTERY_CENTER("battery_center");

    companion object {
        fun fromPersisted(value: String?): ContentLayout =
            entries.firstOrNull { it.persistedValue == value } ?: NETWORK_CENTER
    }
}

internal enum class BatteryColorPreset(
    val persistedValue: String,
) {
    RECOMMENDED("recommended"),
    HYPEROS("hyperos_native"),
    IOS_STYLE("ios_style");

    companion object {
        fun fromPersisted(value: String?): BatteryColorPreset =
            entries.firstOrNull { it.persistedValue == value } ?: HYPEROS
    }
}

internal enum class BatteryColorMode(
    val persistedValue: String,
) {
    PRESET("preset"),
    FOLLOW_SYSTEM("follow_system"),
    CUSTOM("custom");

    companion object {
        fun fromPersisted(value: String?): BatteryColorMode =
            entries.firstOrNull { it.persistedValue == value } ?: PRESET
    }
}

internal enum class BatteryColorSlot {
    NORMAL,
    POWER_SAVE,
    PERFORMANCE,
    SUPER_POWER_SAVE,
    CHARGING,
    LOW,
}

internal data class BatteryColorModes(
    val normal: BatteryColorMode = BatteryColorMode.PRESET,
    val powerSave: BatteryColorMode = BatteryColorMode.PRESET,
    val performance: BatteryColorMode = BatteryColorMode.PRESET,
    val superPowerSave: BatteryColorMode = BatteryColorMode.PRESET,
    val charging: BatteryColorMode = BatteryColorMode.PRESET,
    val low: BatteryColorMode = BatteryColorMode.PRESET,
) {
    fun modeFor(slot: BatteryColorSlot): BatteryColorMode =
        when (slot) {
            BatteryColorSlot.NORMAL -> normal
            BatteryColorSlot.POWER_SAVE -> powerSave
            BatteryColorSlot.PERFORMANCE -> performance
            BatteryColorSlot.SUPER_POWER_SAVE -> superPowerSave
            BatteryColorSlot.CHARGING -> charging
            BatteryColorSlot.LOW -> low
        }

    fun withMode(
        slot: BatteryColorSlot,
        mode: BatteryColorMode,
    ): BatteryColorModes =
        when (slot) {
            BatteryColorSlot.NORMAL -> copy(normal = mode)
            BatteryColorSlot.POWER_SAVE -> copy(powerSave = mode)
            BatteryColorSlot.PERFORMANCE -> copy(performance = mode)
            BatteryColorSlot.SUPER_POWER_SAVE -> copy(superPowerSave = mode)
            BatteryColorSlot.CHARGING -> copy(charging = mode)
            BatteryColorSlot.LOW -> copy(low = mode)
        }
}

internal data class BatteryColorOverrides(
    val normal: Int? = null,
    val powerSave: Int? = null,
    val performance: Int? = null,
    val superPowerSave: Int? = null,
    val charging: Int? = null,
    val low: Int? = null,
) {
    fun colorFor(slot: BatteryColorSlot): Int? =
        when (slot) {
            BatteryColorSlot.NORMAL -> normal
            BatteryColorSlot.POWER_SAVE -> powerSave
            BatteryColorSlot.PERFORMANCE -> performance
            BatteryColorSlot.SUPER_POWER_SAVE -> superPowerSave
            BatteryColorSlot.CHARGING -> charging
            BatteryColorSlot.LOW -> low
        }

    fun withColor(
        slot: BatteryColorSlot,
        color: Int?,
    ): BatteryColorOverrides {
        val opaque = color?.let { it or 0xFF000000.toInt() }
        return when (slot) {
            BatteryColorSlot.NORMAL -> copy(normal = opaque)
            BatteryColorSlot.POWER_SAVE -> copy(powerSave = opaque)
            BatteryColorSlot.PERFORMANCE -> copy(performance = opaque)
            BatteryColorSlot.SUPER_POWER_SAVE -> copy(superPowerSave = opaque)
            BatteryColorSlot.CHARGING -> copy(charging = opaque)
            BatteryColorSlot.LOW -> copy(low = opaque)
        }
    }
}

internal object RecommendedBatteryPalette {
    val POWER_SAVE = 0xFFD5A623.toInt()
    val PERFORMANCE = 0xFF4A7FC1.toInt()
    val SUPER_POWER_SAVE = 0xFFD8752C.toInt()
    val CHARGING = 0xFF3FA760.toInt()
    val LOW = 0xFFD64A4A.toInt()

    fun colorFor(slot: BatteryColorSlot): Int? =
        when (slot) {
            BatteryColorSlot.NORMAL -> null
            BatteryColorSlot.POWER_SAVE -> POWER_SAVE
            BatteryColorSlot.PERFORMANCE -> PERFORMANCE
            BatteryColorSlot.SUPER_POWER_SAVE -> SUPER_POWER_SAVE
            BatteryColorSlot.CHARGING -> CHARGING
            BatteryColorSlot.LOW -> LOW
        }
}

internal object HyperOsBatteryPalette {
    // Verified against pinned SystemUI 17.03.260226.r resources/fields.
    val POWER_SAVE = 0xFFFF9F05.toInt()
    val PERFORMANCE = 0xFF3482FF.toInt()
    val SUPER_POWER_SAVE = POWER_SAVE
    val CHARGING = 0xFF1DCD3A.toInt()
    val LOW = 0xFFFA382E.toInt()

    fun colorFor(slot: BatteryColorSlot): Int? =
        when (slot) {
            BatteryColorSlot.NORMAL -> null
            BatteryColorSlot.POWER_SAVE -> POWER_SAVE
            BatteryColorSlot.PERFORMANCE -> PERFORMANCE
            BatteryColorSlot.SUPER_POWER_SAVE -> SUPER_POWER_SAVE
            BatteryColorSlot.CHARGING -> CHARGING
            BatteryColorSlot.LOW -> LOW
        }
}

internal object IosStyleBatteryPalette {
    val POWER_SAVE = 0xFFFFCC00.toInt()
    val PERFORMANCE = 0xFF007AFF.toInt()
    val SUPER_POWER_SAVE = 0xFFFF9500.toInt()
    val CHARGING = 0xFF34C759.toInt()
    val LOW = 0xFFFF3B30.toInt()

    fun colorFor(slot: BatteryColorSlot): Int? =
        when (slot) {
            BatteryColorSlot.NORMAL -> null
            BatteryColorSlot.POWER_SAVE -> POWER_SAVE
            BatteryColorSlot.PERFORMANCE -> PERFORMANCE
            BatteryColorSlot.SUPER_POWER_SAVE -> SUPER_POWER_SAVE
            BatteryColorSlot.CHARGING -> CHARGING
            BatteryColorSlot.LOW -> LOW
        }
}

internal data class VisualCfg(
    val contentLayout: ContentLayout = ContentLayout.NETWORK_CENTER,
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
    val batteryColorPreset: BatteryColorPreset =
        BatteryColorPreset.HYPEROS,
    val batteryColorModes: BatteryColorModes =
        BatteryColorModes(),
    val batteryColorOverrides: BatteryColorOverrides =
        BatteryColorOverrides(),
)

internal fun VisualCfg.normalized(): VisualCfg =
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
            BatteryColorSlot.entries.fold(
                BatteryColorOverrides(),
            ) { overrides, slot ->
                overrides.withColor(slot, batteryColorOverrides.colorFor(slot))
            },
    )
