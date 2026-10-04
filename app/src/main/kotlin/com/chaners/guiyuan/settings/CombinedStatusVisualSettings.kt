package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

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

internal fun isCombinedStatusVisualPreferenceKey(key: String?): Boolean {
    if (key == null) return true
    if (
        key == CONTENT_LAYOUT_KEY ||
        key in PROFILE_VISUAL_BASE_KEYS ||
        key in GLOBAL_VISUAL_KEYS
    ) return true
    return CombinedStatusContentLayout.entries.any { layout ->
        PROFILE_VISUAL_BASE_KEYS.any { baseKey ->
            key == combinedStatusProfileKey(layout, baseKey)
        }
    }
}

internal class CombinedStatusVisualSettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateBatteryColorPresetDefaultIfNeeded(preferences)
        migrateBatteryTopChargingScaleReferenceIfNeeded(preferences)
    }

    val settings: Flow<CombinedStatusVisualSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (isCombinedStatusVisualPreferenceKey(key)) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): CombinedStatusVisualSettings =
        preferences.readCombinedStatusVisualSettings()

    private fun activeProfileKey(baseKey: String): String =
        combinedStatusProfileKey(
            layout = preferences.readCombinedStatusContentLayout(),
            baseKey = baseKey,
        )

    fun setContentLayout(layout: CombinedStatusContentLayout) {
        preferences
            .edit()
            .putString(CONTENT_LAYOUT_KEY, layout.persistedValue)
            .apply()
    }

    fun setMobileFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(MOBILE_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setCenterFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(CENTER_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setBatteryTopReadoutEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_READOUT_ENABLED_KEY), enabled)
            .apply()
    }

    fun setBatteryTopTextFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setBatteryTopChargingIconEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY), enabled)
            .apply()
    }

    fun setBatteryTopChargingIconFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setBatteryFillFollowsRetractEndpoint(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, enabled)
            .apply()
    }

    fun setControlCenterTintTransitionEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, enabled)
            .apply()
    }

    fun setBatteryTopTextScale(scale: Float) {
        val uiScale =
            scale.coerceIn(
                BATTERY_TOP_TEXT_UI_SCALE_MIN,
                BATTERY_TOP_TEXT_UI_SCALE_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(BATTERY_TOP_TEXT_SCALE_KEY),
                (uiScale * BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
                    .coerceIn(BATTERY_TOP_TEXT_SCALE_MIN, BATTERY_TOP_TEXT_SCALE_MAX),
            )
            .apply()
    }

    fun setBatteryTopTextWeight(weight: Int) {
        preferences
            .edit()
            .putInt(
                activeProfileKey(BATTERY_TOP_TEXT_WEIGHT_KEY),
                weight.coerceIn(BATTERY_TOP_TEXT_WEIGHT_MIN, BATTERY_TOP_TEXT_WEIGHT_MAX),
            )
            .apply()
    }

    fun setBatteryTopVerticalOffset(offset: Float) {
        val uiOffset =
            offset.coerceIn(
                BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(BATTERY_TOP_VERTICAL_OFFSET_KEY),
                batteryTopVerticalOffsetRaw(uiOffset),
            )
            .apply()
    }

    fun setCombinedScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(COMBINED_SCALE_KEY),
                scale.coerceIn(COMBINED_SCALE_MIN, COMBINED_SCALE_MAX),
            )
            .apply()
    }

    fun setOuterWeightScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(OUTER_WEIGHT_SCALE_KEY),
                scale.coerceIn(OUTER_WEIGHT_SCALE_MIN, OUTER_WEIGHT_SCALE_MAX),
            )
            .apply()
    }

    fun setWifiSizeScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(WIFI_SIZE_SCALE_KEY),
                scale.coerceIn(WIFI_SIZE_SCALE_MIN, WIFI_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setAirplaneSizeScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(AIRPLANE_SIZE_SCALE_KEY),
                scale.coerceIn(AIRPLANE_SIZE_SCALE_MIN, AIRPLANE_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setNoSimSizeScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(NO_SIM_SIZE_SCALE_KEY),
                scale.coerceIn(NO_SIM_SIZE_SCALE_MIN, NO_SIM_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setMobileTypeSizeScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(MOBILE_TYPE_SIZE_SCALE_KEY),
                scale.coerceIn(MOBILE_TYPE_SIZE_SCALE_MIN, MOBILE_TYPE_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setMobileTypeWeight(weight: Int) {
        preferences.edit()
            .putInt(
                activeProfileKey(MOBILE_TYPE_WEIGHT_KEY),
                weight.coerceIn(MOBILE_TYPE_WEIGHT_MIN, MOBILE_TYPE_WEIGHT_MAX),
            )
            .apply()
    }

    fun setBatteryColorPreset(preset: CombinedStatusBatteryColorPreset) {
        preferences.edit()
            .putString(BATTERY_COLOR_PRESET_KEY, preset.persistedValue)
            .apply()
    }

    fun setBatteryColorMode(
        slot: CombinedStatusBatteryColorSlot,
        mode: CombinedStatusBatteryColorMode,
    ) {
        preferences.edit()
            .putString(batteryColorModeKey(slot), mode.persistedValue)
            .apply()
    }

    fun setBatteryColorOverride(
        slot: CombinedStatusBatteryColorSlot,
        color: Int?,
    ) {
        val key = batteryColorOverrideKey(slot)
        val editor = preferences.edit()
        if (color == null) {
            editor.remove(key)
        } else {
            editor.putInt(key, color or 0xFF000000.toInt())
        }
        editor.apply()
    }

    fun resetBatteryColorSlot(slot: CombinedStatusBatteryColorSlot) {
        preferences.edit()
            .remove(batteryColorModeKey(slot))
            .remove(batteryColorOverrideKey(slot))
            .apply()
    }

    fun resetBatteryColorOverrides() {
        val editor = preferences.edit()
        CombinedStatusBatteryColorSlot.entries.forEach { slot ->
            editor.remove(batteryColorModeKey(slot))
            editor.remove(batteryColorOverrideKey(slot))
        }
        editor.apply()
    }

    fun resetToDefaults() {
        preferences.edit().clear().apply()
    }

    fun setBatteryTopChargingIconScale(scale: Float) {
        val uiScale =
            scale.coerceIn(
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
                (uiScale * BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
                    .coerceIn(
                        BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
                        BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
                    ),
            )
            .apply()
    }
}

internal fun SharedPreferences.readCombinedStatusContentLayout(): CombinedStatusContentLayout =
    CombinedStatusContentLayout.fromPersisted(
        getString(
            CONTENT_LAYOUT_KEY,
            CombinedStatusContentLayout.NETWORK_CENTER.persistedValue,
        ),
    )

internal fun combinedStatusProfileKey(
    layout: CombinedStatusContentLayout,
    baseKey: String,
): String = layout.persistedValue + "." + baseKey

private fun SharedPreferences.profileBoolean(
    layout: CombinedStatusContentLayout,
    baseKey: String,
    defaultValue: Boolean,
): Boolean {
    val profileKey = combinedStatusProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getBoolean(profileKey, defaultValue)
    } else {
        getBoolean(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileFloat(
    layout: CombinedStatusContentLayout,
    baseKey: String,
    defaultValue: Float,
): Float {
    val profileKey = combinedStatusProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getFloat(profileKey, defaultValue)
    } else {
        getFloat(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileInt(
    layout: CombinedStatusContentLayout,
    baseKey: String,
    defaultValue: Int,
): Int {
    val profileKey = combinedStatusProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else {
        getInt(baseKey, defaultValue)
    }
}

internal fun SharedPreferences.readCombinedStatusVisualSettings(): CombinedStatusVisualSettings {
    val layout = readCombinedStatusContentLayout()
    return CombinedStatusVisualSettings(
        contentLayout = layout,
        mobileFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = false,
            ),
        centerFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = false,
            ),
        batteryTopReadoutEnabled =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_READOUT_ENABLED_KEY,
                defaultValue = false,
            ),
        batteryTopTextFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = true,
            ),
        batteryTopChargingIconEnabled =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
                defaultValue = true,
            ),
        batteryTopChargingIconFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = true,
            ),
        batteryFillFollowsRetractEndpoint =
            getBoolean(
                BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
                false,
            ),
        controlCenterTintTransitionEnabled =
            getBoolean(
                CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
                true,
            ),
        batteryTopTextScale =
            profileFloat(
                layout = layout,
                baseKey = BATTERY_TOP_TEXT_SCALE_KEY,
                defaultValue = batteryTopTextScaleDefault(layout),
            ),
        batteryTopTextWeight =
            profileInt(
                layout = layout,
                baseKey = BATTERY_TOP_TEXT_WEIGHT_KEY,
                defaultValue = BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
            ),
        batteryTopVerticalOffset =
            profileFloat(
                layout = layout,
                baseKey = BATTERY_TOP_VERTICAL_OFFSET_KEY,
                defaultValue = BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
            ),
        batteryTopChargingIconScale =
            profileFloat(
                layout = layout,
                baseKey = BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                defaultValue = batteryTopChargingIconScaleDefault(layout),
            ),
        combinedScale =
            profileFloat(layout, COMBINED_SCALE_KEY, COMBINED_SCALE_DEFAULT),
        outerWeightScale =
            profileFloat(layout, OUTER_WEIGHT_SCALE_KEY, OUTER_WEIGHT_SCALE_DEFAULT),
        wifiSizeScale =
            profileFloat(layout, WIFI_SIZE_SCALE_KEY, WIFI_SIZE_SCALE_DEFAULT),
        airplaneSizeScale =
            profileFloat(layout, AIRPLANE_SIZE_SCALE_KEY, AIRPLANE_SIZE_SCALE_DEFAULT),
        noSimSizeScale =
            profileFloat(layout, NO_SIM_SIZE_SCALE_KEY, NO_SIM_SIZE_SCALE_DEFAULT),
        mobileTypeSizeScale =
            profileFloat(
                layout,
                MOBILE_TYPE_SIZE_SCALE_KEY,
                mobileTypeSizeScaleDefault(layout),
            ),
        mobileTypeWeight =
            profileInt(layout, MOBILE_TYPE_WEIGHT_KEY, MOBILE_TYPE_WEIGHT_DEFAULT),
        batteryColorPreset =
            CombinedStatusBatteryColorPreset.fromPersisted(
                getString(
                    BATTERY_COLOR_PRESET_KEY,
                    batteryColorPresetForMissingKey(
                        hadPreviousVisualSchema =
                            contains(BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY),
                    ).persistedValue,
                ),
            ),
        batteryColorModes =
            CombinedStatusBatteryColorModes(
                normal =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_NORMAL_KEY,
                        colorKey = BATTERY_COLOR_NORMAL_KEY,
                    ),
                powerSave =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_POWER_SAVE_KEY,
                        colorKey = BATTERY_COLOR_POWER_SAVE_KEY,
                    ),
                performance =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_PERFORMANCE_KEY,
                        colorKey = BATTERY_COLOR_PERFORMANCE_KEY,
                    ),
                superPowerSave =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY,
                        colorKey = BATTERY_COLOR_SUPER_POWER_SAVE_KEY,
                    ),
                charging =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_CHARGING_KEY,
                        colorKey = BATTERY_COLOR_CHARGING_KEY,
                    ),
                low =
                    batteryColorMode(
                        modeKey = BATTERY_COLOR_MODE_LOW_KEY,
                        colorKey = BATTERY_COLOR_LOW_KEY,
                    ),
            ),
        batteryColorOverrides =
            CombinedStatusBatteryColorOverrides(
                normal = optionalColor(BATTERY_COLOR_NORMAL_KEY),
                powerSave = optionalColor(BATTERY_COLOR_POWER_SAVE_KEY),
                performance = optionalColor(BATTERY_COLOR_PERFORMANCE_KEY),
                superPowerSave = optionalColor(BATTERY_COLOR_SUPER_POWER_SAVE_KEY),
                charging = optionalColor(BATTERY_COLOR_CHARGING_KEY),
                low = optionalColor(BATTERY_COLOR_LOW_KEY),
            ),
    ).normalized()
}

internal fun SharedPreferences.Editor.putCombinedStatusVisualSettings(
    settings: CombinedStatusVisualSettings,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val layout = normalized.contentLayout
    return putString(
        CONTENT_LAYOUT_KEY,
        layout.persistedValue,
    ).putBoolean(
        combinedStatusProfileKey(layout, MOBILE_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.mobileFollowsBatteryColor,
    ).putBoolean(
        combinedStatusProfileKey(layout, CENTER_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.centerFollowsBatteryColor,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY),
        normalized.batteryTopReadoutEnabled,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.batteryTopTextFollowsBatteryColor,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_CHARGING_ICON_ENABLED_KEY),
        normalized.batteryTopChargingIconEnabled,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.batteryTopChargingIconFollowsBatteryColor,
    ).putBoolean(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        normalized.batteryFillFollowsRetractEndpoint,
    ).putBoolean(
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
        normalized.controlCenterTintTransitionEnabled,
    ).putFloat(
        combinedStatusProfileKey(layout, BATTERY_TOP_TEXT_SCALE_KEY),
        normalized.batteryTopTextScale,
    ).putInt(
        combinedStatusProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY),
        normalized.batteryTopTextWeight,
    ).putFloat(
        combinedStatusProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY),
        normalized.batteryTopVerticalOffset,
    ).putFloat(
        combinedStatusProfileKey(layout, BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
        normalized.batteryTopChargingIconScale,
    ).putFloat(
        combinedStatusProfileKey(layout, COMBINED_SCALE_KEY),
        normalized.combinedScale,
    ).putFloat(
        combinedStatusProfileKey(layout, OUTER_WEIGHT_SCALE_KEY),
        normalized.outerWeightScale,
    ).putFloat(
        combinedStatusProfileKey(layout, WIFI_SIZE_SCALE_KEY),
        normalized.wifiSizeScale,
    ).putFloat(
        combinedStatusProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY),
        normalized.airplaneSizeScale,
    ).putFloat(
        combinedStatusProfileKey(layout, NO_SIM_SIZE_SCALE_KEY),
        normalized.noSimSizeScale,
    ).putFloat(
        combinedStatusProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY),
        normalized.mobileTypeSizeScale,
    ).putInt(
        combinedStatusProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY),
        normalized.mobileTypeWeight,
    ).putString(
        BATTERY_COLOR_PRESET_KEY,
        normalized.batteryColorPreset.persistedValue,
    ).applyBatteryColorModes(normalized.batteryColorModes)
        .applyBatteryColorOverrides(normalized.batteryColorOverrides)
}

internal const val COMBINED_STATUS_VISUAL_PREFS_NAME = "combined_status_visual"
internal const val CONTENT_LAYOUT_KEY = "content_layout"
internal const val MOBILE_FOLLOWS_BATTERY_COLOR_KEY = "mobile_follows_battery_color"
internal const val CENTER_FOLLOWS_BATTERY_COLOR_KEY = "center_follows_battery_color"
internal const val BATTERY_TOP_READOUT_ENABLED_KEY = "battery_top_readout_enabled"
internal const val BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY =
    "battery_top_text_follows_battery_color"
internal const val BATTERY_TOP_CHARGING_ICON_ENABLED_KEY =
    "battery_top_charging_icon_enabled"
internal const val BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY =
    "battery_top_charging_icon_follows_battery_color"
internal const val BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY =
    "battery_fill_follows_retract_endpoint"
internal const val CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY =
    "control_center_tint_transition_enabled"
internal const val BATTERY_TOP_TEXT_SCALE_KEY = "battery_top_text_scale"
internal const val BATTERY_TOP_TEXT_WEIGHT_KEY = "battery_top_text_weight"
internal const val BATTERY_TOP_VERTICAL_OFFSET_KEY = "battery_top_vertical_offset"
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_KEY = "battery_top_charging_icon_scale"
internal const val COMBINED_SCALE_KEY = "combined_scale"
internal const val OUTER_WEIGHT_SCALE_KEY = "outer_weight_scale"
internal const val WIFI_SIZE_SCALE_KEY = "wifi_size_scale"
internal const val AIRPLANE_SIZE_SCALE_KEY = "airplane_size_scale"
internal const val NO_SIM_SIZE_SCALE_KEY = "no_sim_size_scale"
internal const val MOBILE_TYPE_SIZE_SCALE_KEY = "mobile_type_size_scale"
internal const val MOBILE_TYPE_WEIGHT_KEY = "mobile_type_weight"
internal const val BATTERY_COLOR_PRESET_KEY = "battery_color_preset"
internal const val BATTERY_COLOR_MODE_NORMAL_KEY = "battery_color_mode_normal"
internal const val BATTERY_COLOR_MODE_POWER_SAVE_KEY = "battery_color_mode_power_save"
internal const val BATTERY_COLOR_MODE_PERFORMANCE_KEY = "battery_color_mode_performance"
internal const val BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY = "battery_color_mode_super_power_save"
internal const val BATTERY_COLOR_MODE_CHARGING_KEY = "battery_color_mode_charging"
internal const val BATTERY_COLOR_MODE_LOW_KEY = "battery_color_mode_low"
internal const val BATTERY_COLOR_NORMAL_KEY = "battery_color_normal"
internal const val BATTERY_COLOR_POWER_SAVE_KEY = "battery_color_power_save"
internal const val BATTERY_COLOR_PERFORMANCE_KEY = "battery_color_performance"
internal const val BATTERY_COLOR_SUPER_POWER_SAVE_KEY = "battery_color_super_power_save"
internal const val BATTERY_COLOR_CHARGING_KEY = "battery_color_charging"
internal const val BATTERY_COLOR_LOW_KEY = "battery_color_low"
internal const val RUNTIME_REMOTE_PREFS_NAME = "CombinedStatusRuntimeConfig"

private val PROFILE_VISUAL_BASE_KEYS =
    setOf(
        MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
        CENTER_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_READOUT_ENABLED_KEY,
        BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
        BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_TEXT_SCALE_KEY,
        BATTERY_TOP_TEXT_WEIGHT_KEY,
        BATTERY_TOP_VERTICAL_OFFSET_KEY,
        BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
        COMBINED_SCALE_KEY,
        OUTER_WEIGHT_SCALE_KEY,
        WIFI_SIZE_SCALE_KEY,
        AIRPLANE_SIZE_SCALE_KEY,
        NO_SIM_SIZE_SCALE_KEY,
        MOBILE_TYPE_SIZE_SCALE_KEY,
        MOBILE_TYPE_WEIGHT_KEY,
    )

private val GLOBAL_VISUAL_KEYS =
    setOf(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
        BATTERY_COLOR_PRESET_KEY,
        BATTERY_COLOR_MODE_NORMAL_KEY,
        BATTERY_COLOR_MODE_POWER_SAVE_KEY,
        BATTERY_COLOR_MODE_PERFORMANCE_KEY,
        BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY,
        BATTERY_COLOR_MODE_CHARGING_KEY,
        BATTERY_COLOR_MODE_LOW_KEY,
        BATTERY_COLOR_NORMAL_KEY,
        BATTERY_COLOR_POWER_SAVE_KEY,
        BATTERY_COLOR_PERFORMANCE_KEY,
        BATTERY_COLOR_SUPER_POWER_SAVE_KEY,
        BATTERY_COLOR_CHARGING_KEY,
        BATTERY_COLOR_LOW_KEY,
    )

// Persisted text scale remains in the pre-521 physical scale.
internal const val BATTERY_TOP_TEXT_UI_SCALE_REFERENCE = 1.3f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MIN = 0.4f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MAX = 1.6f
internal const val BATTERY_TOP_TEXT_SCALE_DEFAULT =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_TEXT_SCALE_MIN =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MIN
internal const val BATTERY_TOP_TEXT_SCALE_MAX =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MAX
internal const val BATTERY_TOP_TEXT_WEIGHT_DEFAULT = 900
internal const val BATTERY_TOP_TEXT_WEIGHT_MIN = 400
internal const val BATTERY_TOP_TEXT_WEIGHT_MAX = 1400
// Runtime/persisted offset is physical canonical displacement. Device review
// established that the previous +3 position is the intended user-facing zero.
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE = 3f
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_MIN = -10f
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_MAX = 10f
internal const val BATTERY_TOP_VERTICAL_OFFSET_DEFAULT =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE
internal const val BATTERY_TOP_VERTICAL_OFFSET_MIN =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE + BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
internal const val BATTERY_TOP_VERTICAL_OFFSET_MAX =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE + BATTERY_TOP_VERTICAL_OFFSET_UI_MAX

// Runtime/persisted charging scale remains a physical multiplier.
// Build 522's user-facing 110% (1.5 × 1.10 = 1.65 physical) becomes
// Build 523's user-facing/default 100% reference.
private const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY = 1.5f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE = 1.65f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN = 0.4f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX = 1.6f
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MIN =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE * BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MAX =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE * BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX
private const val BATTERY_COLOR_PRESET_SCHEMA_KEY =
    "battery_color_preset_schema"
private const val BATTERY_COLOR_PRESET_SCHEMA_CURRENT = 2
private const val BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY =
    "battery_top_charging_scale_schema"
private const val BATTERY_TOP_CHARGING_SCALE_SCHEMA_CURRENT = 2
private const val BATTERY_TOP_SCALE_EPSILON = 0.0001f

internal fun batteryColorPresetForMissingKey(
    hadPreviousVisualSchema: Boolean,
): CombinedStatusBatteryColorPreset {
    @Suppress("UNUSED_VARIABLE")
    val compatibilityMarker = hadPreviousVisualSchema
    return CombinedStatusBatteryColorPreset.HYPEROS
}

internal fun migrateBatteryColorPresetDefaultIfNeeded(
    preferences: SharedPreferences,
) {
    if (
        preferences.getInt(BATTERY_COLOR_PRESET_SCHEMA_KEY, 0) >=
            BATTERY_COLOR_PRESET_SCHEMA_CURRENT
    ) {
        return
    }

    val editor = preferences.edit()
    if (!preferences.contains(BATTERY_COLOR_PRESET_KEY)) {
        val preset =
            batteryColorPresetForMissingKey(
                hadPreviousVisualSchema =
                    preferences.contains(BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY),
            )
        editor.putString(BATTERY_COLOR_PRESET_KEY, preset.persistedValue)
    }
    editor
        .putInt(
            BATTERY_COLOR_PRESET_SCHEMA_KEY,
            BATTERY_COLOR_PRESET_SCHEMA_CURRENT,
        )
        .apply()
}

internal fun migrateBatteryTopChargingScaleReferenceIfNeeded(
    preferences: SharedPreferences,
) {
    if (
        preferences.getInt(BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY, 1) >=
            BATTERY_TOP_CHARGING_SCALE_SCHEMA_CURRENT
    ) {
        return
    }

    val editor = preferences.edit()
    if (preferences.contains(BATTERY_TOP_CHARGING_ICON_SCALE_KEY)) {
        val raw =
            preferences.getFloat(
                BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY,
            )
        if (
            abs(raw - BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY) <=
                BATTERY_TOP_SCALE_EPSILON
        ) {
            editor.putFloat(
                BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE,
            )
        }
    }
    editor
        .putInt(
            BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY,
            BATTERY_TOP_CHARGING_SCALE_SCHEMA_CURRENT,
        )
        .apply()
}

internal fun batteryTopTextUiScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    when (layout) {
        CombinedStatusContentLayout.NETWORK_CENTER -> 1.2f
        CombinedStatusContentLayout.BATTERY_CENTER -> 1.4f
    }

internal fun mobileTypeSizeScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    when (layout) {
        CombinedStatusContentLayout.NETWORK_CENTER -> MOBILE_TYPE_SIZE_SCALE_DEFAULT
        CombinedStatusContentLayout.BATTERY_CENTER -> 0.8f
    }

internal fun batteryTopChargingIconUiScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    when (layout) {
        CombinedStatusContentLayout.NETWORK_CENTER -> 1f
        CombinedStatusContentLayout.BATTERY_CENTER -> 1.2f
    }

internal fun batteryTopTextScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    (
        BATTERY_TOP_TEXT_UI_SCALE_REFERENCE *
            batteryTopTextUiScaleDefault(layout)
    ).coerceIn(
        BATTERY_TOP_TEXT_SCALE_MIN,
        BATTERY_TOP_TEXT_SCALE_MAX,
    )

internal fun batteryTopChargingIconScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    (
        BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE *
            batteryTopChargingIconUiScaleDefault(layout)
    ).coerceIn(
        BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
        BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
    )

internal fun batteryTopTextUiScale(rawScale: Float): Float =
    (rawScale / BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
        .coerceIn(
            BATTERY_TOP_TEXT_UI_SCALE_MIN,
            BATTERY_TOP_TEXT_UI_SCALE_MAX,
        )

internal fun batteryTopChargingIconUiScale(rawScale: Float): Float =
    (rawScale / BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
        .coerceIn(
            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
        )


internal fun batteryTopVerticalOffsetUi(rawOffset: Float): Float =
    (rawOffset - BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE)
        .coerceIn(
            BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
        )

internal fun batteryTopVerticalOffsetRaw(uiOffset: Float): Float =
    (
        uiOffset.coerceIn(
            BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
        ) + BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE
    ).coerceIn(
        BATTERY_TOP_VERTICAL_OFFSET_MIN,
        BATTERY_TOP_VERTICAL_OFFSET_MAX,
    )


internal const val COMBINED_SCALE_DEFAULT = 1f
internal const val COMBINED_SCALE_MIN = 0.60f
internal const val COMBINED_SCALE_MAX = 1.00f
internal const val OUTER_WEIGHT_SCALE_DEFAULT = 1f
internal const val OUTER_WEIGHT_SCALE_MIN = 0.70f
internal const val OUTER_WEIGHT_SCALE_MAX = 1.30f
internal const val WIFI_SIZE_SCALE_DEFAULT = 1f
internal const val WIFI_SIZE_SCALE_MIN = 0.40f
internal const val WIFI_SIZE_SCALE_MAX = 1.25f
internal const val AIRPLANE_SIZE_SCALE_DEFAULT = 1f
internal const val AIRPLANE_SIZE_SCALE_MIN = WIFI_SIZE_SCALE_MIN
internal const val AIRPLANE_SIZE_SCALE_MAX = WIFI_SIZE_SCALE_MAX
internal const val NO_SIM_SIZE_SCALE_DEFAULT = 1f
internal const val NO_SIM_SIZE_SCALE_MIN = WIFI_SIZE_SCALE_MIN
internal const val NO_SIM_SIZE_SCALE_MAX = WIFI_SIZE_SCALE_MAX
internal const val MOBILE_TYPE_SIZE_SCALE_DEFAULT = 1f
internal const val MOBILE_TYPE_SIZE_SCALE_MIN = 0.40f
internal const val MOBILE_TYPE_SIZE_SCALE_MAX = 1.25f
internal const val MOBILE_TYPE_WEIGHT_DEFAULT = 900
internal const val MOBILE_TYPE_WEIGHT_MIN = 400
internal const val MOBILE_TYPE_WEIGHT_MAX = 1400

internal fun batteryColorModeKey(slot: CombinedStatusBatteryColorSlot): String =
    when (slot) {
        CombinedStatusBatteryColorSlot.NORMAL -> BATTERY_COLOR_MODE_NORMAL_KEY
        CombinedStatusBatteryColorSlot.POWER_SAVE -> BATTERY_COLOR_MODE_POWER_SAVE_KEY
        CombinedStatusBatteryColorSlot.PERFORMANCE -> BATTERY_COLOR_MODE_PERFORMANCE_KEY
        CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY
        CombinedStatusBatteryColorSlot.CHARGING -> BATTERY_COLOR_MODE_CHARGING_KEY
        CombinedStatusBatteryColorSlot.LOW -> BATTERY_COLOR_MODE_LOW_KEY
    }

internal fun batteryColorOverrideKey(slot: CombinedStatusBatteryColorSlot): String =
    when (slot) {
        CombinedStatusBatteryColorSlot.NORMAL -> BATTERY_COLOR_NORMAL_KEY
        CombinedStatusBatteryColorSlot.POWER_SAVE -> BATTERY_COLOR_POWER_SAVE_KEY
        CombinedStatusBatteryColorSlot.PERFORMANCE -> BATTERY_COLOR_PERFORMANCE_KEY
        CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> BATTERY_COLOR_SUPER_POWER_SAVE_KEY
        CombinedStatusBatteryColorSlot.CHARGING -> BATTERY_COLOR_CHARGING_KEY
        CombinedStatusBatteryColorSlot.LOW -> BATTERY_COLOR_LOW_KEY
    }

internal fun batteryColorModeFromPersisted(
    persistedMode: String?,
    hasStoredColor: Boolean,
): CombinedStatusBatteryColorMode =
    if (persistedMode != null) {
        CombinedStatusBatteryColorMode.fromPersisted(persistedMode)
    } else if (hasStoredColor) {
        CombinedStatusBatteryColorMode.CUSTOM
    } else {
        CombinedStatusBatteryColorMode.PRESET
    }

private fun SharedPreferences.batteryColorMode(
    modeKey: String,
    colorKey: String,
): CombinedStatusBatteryColorMode =
    batteryColorModeFromPersisted(
        persistedMode = getString(modeKey, null),
        hasStoredColor = contains(colorKey),
    )

private fun SharedPreferences.optionalColor(key: String): Int? =
    if (contains(key)) {
        getInt(key, 0) or 0xFF000000.toInt()
    } else {
        null
    }

private fun SharedPreferences.Editor.applyBatteryColorOverrides(
    overrides: CombinedStatusBatteryColorOverrides,
): SharedPreferences.Editor {
    CombinedStatusBatteryColorSlot.entries.forEach { slot ->
        val key = batteryColorOverrideKey(slot)
        val color = overrides.colorFor(slot)
        if (color == null) {
            remove(key)
        } else {
            putInt(key, color or 0xFF000000.toInt())
        }
    }
    return this
}


private fun SharedPreferences.Editor.applyBatteryColorModes(
    modes: CombinedStatusBatteryColorModes,
): SharedPreferences.Editor {
    CombinedStatusBatteryColorSlot.entries.forEach { slot ->
        putString(
            batteryColorModeKey(slot),
            modes.modeFor(slot).persistedValue,
        )
    }
    return this
}
