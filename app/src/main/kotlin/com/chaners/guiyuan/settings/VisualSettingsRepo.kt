package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

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
