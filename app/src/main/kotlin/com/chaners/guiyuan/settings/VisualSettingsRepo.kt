package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal fun isVisualPreferenceKey(key: String?): Boolean {
    if (key == null) return true
    if (
        key == CONTENT_LAYOUT_KEY ||
        key in PROFILE_VISUAL_BASE_KEYS ||
        key in GLOBAL_VISUAL_KEYS
    ) return true
    return ContentLayout.entries.any { layout ->
        PROFILE_VISUAL_BASE_KEYS.any { baseKey ->
            key == visualProfileKey(layout, baseKey)
        }
    }
}

internal class VisualSettingsRepo(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateBatteryPreset(preferences)
        migrateChargingScaleRef(preferences)
    }

    val settings: Flow<VisualSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (isVisualPreferenceKey(key)) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): VisualSettings =
        preferences.readVisualSettings()

    private fun activeProfileKey(baseKey: String): String =
        visualProfileKey(
            layout = preferences.readContentLayout(),
            baseKey = baseKey,
        )

    fun setContentLayout(layout: ContentLayout) {
        preferences
            .edit()
            .putString(CONTENT_LAYOUT_KEY, layout.persistedValue)
            .apply()
    }

    fun setMobileFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(MOBILE_FOLLOWS_BATTERY_KEY), enabled)
            .apply()
    }

    fun setCenterFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(CENTER_FOLLOWS_BATTERY_KEY), enabled)
            .apply()
    }

    fun setTopReadoutEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(TOP_READOUT_KEY), enabled)
            .apply()
    }

    fun setTopTextFollowsBattery(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(TOP_TEXT_FOLLOWS_BATTERY_KEY), enabled)
            .apply()
    }

    fun setChargingIconEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(CHARGING_ICON_ENABLED_KEY), enabled)
            .apply()
    }

    fun setChargingIconFollowsBattery(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(CHARGE_ICON_FOLLOWS_KEY), enabled)
            .apply()
    }

    fun setFillFollowsRetract(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(FILL_FOLLOWS_RETRACT_KEY, enabled)
            .apply()
    }

    fun setCcTintTransition(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(CC_TINT_TRANSITION_KEY, enabled)
            .apply()
    }

    fun setTopTextScale(scale: Float) {
        val uiScale =
            scale.coerceIn(
                TOP_TEXT_UI_MIN,
                TOP_TEXT_UI_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(TOP_TEXT_SCALE_KEY),
                (uiScale * TOP_TEXT_UI_REF)
                    .coerceIn(BATTERY_TOP_TEXT_SCALE_MIN, BATTERY_TOP_TEXT_SCALE_MAX),
            )
            .apply()
    }

    fun setTopTextWeight(weight: Int) {
        preferences
            .edit()
            .putInt(
                activeProfileKey(TOP_TEXT_WEIGHT_KEY),
                weight.coerceIn(BATTERY_TOP_TEXT_WEIGHT_MIN, BATTERY_TOP_TEXT_WEIGHT_MAX),
            )
            .apply()
    }

    fun setTopOffset(offset: Float) {
        val uiOffset =
            offset.coerceIn(
                TOP_OFFSET_UI_MIN,
                TOP_OFFSET_UI_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(TOP_OFFSET_KEY),
                topOffsetRaw(uiOffset),
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

    fun setBatteryColorPreset(preset: BatteryColorPreset) {
        preferences.edit()
            .putString(BATTERY_COLOR_PRESET_KEY, preset.persistedValue)
            .apply()
    }

    fun setBatteryColorMode(
        slot: BatteryColorSlot,
        mode: BatteryColorMode,
    ) {
        preferences.edit()
            .putString(batteryColorModeKey(slot), mode.persistedValue)
            .apply()
    }

    fun setBatteryColorOverride(
        slot: BatteryColorSlot,
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

    fun resetBatteryColorSlot(slot: BatteryColorSlot) {
        preferences.edit()
            .remove(batteryColorModeKey(slot))
            .remove(batteryColorOverrideKey(slot))
            .apply()
    }

    fun resetBatteryColorOverrides() {
        val editor = preferences.edit()
        BatteryColorSlot.entries.forEach { slot ->
            editor.remove(batteryColorModeKey(slot))
            editor.remove(batteryColorOverrideKey(slot))
        }
        editor.apply()
    }

    fun resetToDefaults() {
        preferences.edit().clear().apply()
    }

    fun setChargingIconScale(scale: Float) {
        val uiScale =
            scale.coerceIn(
                CHARGING_ICON_UI_MIN,
                CHARGING_ICON_UI_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(CHARGING_ICON_SCALE_KEY),
                (uiScale * CHARGING_ICON_UI_REF)
                    .coerceIn(
                        CHARGING_ICON_SCALE_MIN,
                        CHARGING_ICON_SCALE_MAX,
                    ),
            )
            .apply()
    }
}

internal fun SharedPreferences.readContentLayout(): ContentLayout =
    ContentLayout.fromPersisted(
        getString(
            CONTENT_LAYOUT_KEY,
            ContentLayout.NETWORK_CENTER.persistedValue,
        ),
    )

internal fun visualProfileKey(
    layout: ContentLayout,
    baseKey: String,
): String = layout.persistedValue + "." + baseKey

private fun SharedPreferences.profileBoolean(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Boolean,
): Boolean {
    val profileKey = visualProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getBoolean(profileKey, defaultValue)
    } else {
        getBoolean(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileFloat(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Float,
): Float {
    val profileKey = visualProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getFloat(profileKey, defaultValue)
    } else {
        getFloat(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileInt(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Int,
): Int {
    val profileKey = visualProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else {
        getInt(baseKey, defaultValue)
    }
}

internal fun SharedPreferences.readVisualSettings(): VisualSettings {
    val layout = readContentLayout()
    return VisualSettings(
        contentLayout = layout,
        mobileFollowsBattery =
            profileBoolean(
                layout = layout,
                baseKey = MOBILE_FOLLOWS_BATTERY_KEY,
                defaultValue = false,
            ),
        centerFollowsBattery =
            profileBoolean(
                layout = layout,
                baseKey = CENTER_FOLLOWS_BATTERY_KEY,
                defaultValue = false,
            ),
        topReadoutEnabled =
            profileBoolean(
                layout = layout,
                baseKey = TOP_READOUT_KEY,
                defaultValue = false,
            ),
        topTextFollowsBattery =
            profileBoolean(
                layout = layout,
                baseKey = TOP_TEXT_FOLLOWS_BATTERY_KEY,
                defaultValue = true,
            ),
        chargingIconEnabled =
            profileBoolean(
                layout = layout,
                baseKey = CHARGING_ICON_ENABLED_KEY,
                defaultValue = true,
            ),
        chargingIconFollowsBattery =
            profileBoolean(
                layout = layout,
                baseKey = CHARGE_ICON_FOLLOWS_KEY,
                defaultValue = true,
            ),
        fillFollowsRetract =
            getBoolean(
                FILL_FOLLOWS_RETRACT_KEY,
                false,
            ),
        ccTintTransitionEnabled =
            getBoolean(
                CC_TINT_TRANSITION_KEY,
                true,
            ),
        batteryTopTextScale =
            profileFloat(
                layout = layout,
                baseKey = TOP_TEXT_SCALE_KEY,
                defaultValue = topTextScaleDefault(layout),
            ),
        batteryTopTextWeight =
            profileInt(
                layout = layout,
                baseKey = TOP_TEXT_WEIGHT_KEY,
                defaultValue = TOP_TEXT_WEIGHT_DEFAULT,
            ),
        topOffset =
            profileFloat(
                layout = layout,
                baseKey = TOP_OFFSET_KEY,
                defaultValue = TOP_OFFSET_DEFAULT,
            ),
        chargingIconScale =
            profileFloat(
                layout = layout,
                baseKey = CHARGING_ICON_SCALE_KEY,
                defaultValue = chargingIconScaleDefault(layout),
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
            BatteryColorPreset.fromPersisted(
                getString(
                    BATTERY_COLOR_PRESET_KEY,
                    presetForMissingColorKey(
                        hadPreviousVisualSchema =
                            contains(CHARGING_SCALE_SCHEMA_KEY),
                    ).persistedValue,
                ),
            ),
        batteryColorModes =
            BatteryColorModes(
                normal =
                    batteryColorMode(
                        modeKey = BATTERY_MODE_NORMAL_KEY,
                        colorKey = BATTERY_NORMAL_COLOR_KEY,
                    ),
                powerSave =
                    batteryColorMode(
                        modeKey = BATTERY_MODE_POWER_SAVE_KEY,
                        colorKey = BATTERY_POWER_SAVE_COLOR_KEY,
                    ),
                performance =
                    batteryColorMode(
                        modeKey = BATTERY_MODE_PERF_KEY,
                        colorKey = BATTERY_PERF_COLOR_KEY,
                    ),
                superPowerSave =
                    batteryColorMode(
                        modeKey = BATTERY_MODE_SUPER_SAVE_KEY,
                        colorKey = BATTERY_SUPER_SAVE_COLOR_KEY,
                    ),
                charging =
                    batteryColorMode(
                        modeKey = BATTERY_MODE_CHARGING_KEY,
                        colorKey = BATTERY_CHARGING_COLOR_KEY,
                    ),
                low =
                    batteryColorMode(
                        modeKey = BATTERY_MODE_LOW_KEY,
                        colorKey = BATTERY_LOW_COLOR_KEY,
                    ),
            ),
        batteryColorOverrides =
            BatteryColorOverrides(
                normal = optionalColor(BATTERY_NORMAL_COLOR_KEY),
                powerSave = optionalColor(BATTERY_POWER_SAVE_COLOR_KEY),
                performance = optionalColor(BATTERY_PERF_COLOR_KEY),
                superPowerSave = optionalColor(BATTERY_SUPER_SAVE_COLOR_KEY),
                charging = optionalColor(BATTERY_CHARGING_COLOR_KEY),
                low = optionalColor(BATTERY_LOW_COLOR_KEY),
            ),
    ).normalized()
}

internal fun SharedPreferences.Editor.putVisualSettings(
    settings: VisualSettings,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val layout = normalized.contentLayout
    return putString(
        CONTENT_LAYOUT_KEY,
        layout.persistedValue,
    ).putBoolean(
        visualProfileKey(layout, MOBILE_FOLLOWS_BATTERY_KEY),
        normalized.mobileFollowsBattery,
    ).putBoolean(
        visualProfileKey(layout, CENTER_FOLLOWS_BATTERY_KEY),
        normalized.centerFollowsBattery,
    ).putBoolean(
        visualProfileKey(layout, TOP_READOUT_KEY),
        normalized.topReadoutEnabled,
    ).putBoolean(
        visualProfileKey(layout, TOP_TEXT_FOLLOWS_BATTERY_KEY),
        normalized.topTextFollowsBattery,
    ).putBoolean(
        visualProfileKey(layout, CHARGING_ICON_ENABLED_KEY),
        normalized.chargingIconEnabled,
    ).putBoolean(
        visualProfileKey(layout, CHARGE_ICON_FOLLOWS_KEY),
        normalized.chargingIconFollowsBattery,
    ).putBoolean(
        FILL_FOLLOWS_RETRACT_KEY,
        normalized.fillFollowsRetract,
    ).putBoolean(
        CC_TINT_TRANSITION_KEY,
        normalized.ccTintTransitionEnabled,
    ).putFloat(
        visualProfileKey(layout, TOP_TEXT_SCALE_KEY),
        normalized.batteryTopTextScale,
    ).putInt(
        visualProfileKey(layout, TOP_TEXT_WEIGHT_KEY),
        normalized.batteryTopTextWeight,
    ).putFloat(
        visualProfileKey(layout, TOP_OFFSET_KEY),
        normalized.topOffset,
    ).putFloat(
        visualProfileKey(layout, CHARGING_ICON_SCALE_KEY),
        normalized.chargingIconScale,
    ).putFloat(
        visualProfileKey(layout, COMBINED_SCALE_KEY),
        normalized.combinedScale,
    ).putFloat(
        visualProfileKey(layout, OUTER_WEIGHT_SCALE_KEY),
        normalized.outerWeightScale,
    ).putFloat(
        visualProfileKey(layout, WIFI_SIZE_SCALE_KEY),
        normalized.wifiSizeScale,
    ).putFloat(
        visualProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY),
        normalized.airplaneSizeScale,
    ).putFloat(
        visualProfileKey(layout, NO_SIM_SIZE_SCALE_KEY),
        normalized.noSimSizeScale,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY),
        normalized.mobileTypeSizeScale,
    ).putInt(
        visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY),
        normalized.mobileTypeWeight,
    ).putString(
        BATTERY_COLOR_PRESET_KEY,
        normalized.batteryColorPreset.persistedValue,
    ).applyBatteryColorModes(normalized.batteryColorModes)
        .applyBatteryColorOverrides(normalized.batteryColorOverrides)
}

private fun SharedPreferences.batteryColorMode(
    modeKey: String,
    colorKey: String,
): BatteryColorMode =
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
    overrides: BatteryColorOverrides,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
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
    modes: BatteryColorModes,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        putString(
            batteryColorModeKey(slot),
            modes.modeFor(slot).persistedValue,
        )
    }
    return this
}
