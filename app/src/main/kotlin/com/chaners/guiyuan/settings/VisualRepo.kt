package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private val visualPreferenceKeys = buildSet {
    add(CONTENT_LAYOUT_KEY)
    add(DUAL_SIGNAL_ENABLED_KEY)
    add(visualModeKey(true, CONTENT_LAYOUT_KEY))
    addAll(PROFILE_VISUAL_BASE_KEYS)
    addAll(GLOBAL_VISUAL_KEYS)
    GLOBAL_VISUAL_KEYS.forEach { add(visualModeKey(true, it)) }
    ContentLayout.entries.forEach { layout ->
        PROFILE_VISUAL_BASE_KEYS.forEach { key ->
            add(visualProfileKey(layout, key))
            add(visualProfileKey(layout, key, dual = true))
        }
    }
}

internal fun isVisualPreferenceKey(key: String?): Boolean =
    key == null || key in visualPreferenceKeys

internal class VisualRepo(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateBatteryColorPresetDefaultIfNeeded(preferences)
        migrateBatteryTopChargingScaleReferenceIfNeeded(preferences)
    }

    val settings: Flow<VisualCfg> =
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

    fun current(): VisualCfg = preferences.readVisualCfg()

    private fun activeProfileKey(baseKey: String): String =
        visualProfileKey(
            layout = preferences.readLayout(),
            baseKey = baseKey,
            dual = preferences.dualSignalEnabled(),
        )

    private fun activeGlobalKey(baseKey: String): String =
        visualModeKey(preferences.dualSignalEnabled(), baseKey)

    fun setDualSignal(enabled: Boolean) {
        val editor = preferences.edit()
        if (enabled && !preferences.contains(visualModeKey(true, CONTENT_LAYOUT_KEY))) {
            // Clone both existing layout profiles once, then keep them independent.
            val singleLayout = preferences.readLayout(dual = false)
            ContentLayout.entries.forEach { layout ->
                editor.putVisualCfg(
                    preferences.readVisualCfg(dual = false, layoutOverride = layout)
                        .copy(dualSignal = true),
                )
            }
            editor.putString(
                visualModeKey(true, CONTENT_LAYOUT_KEY),
                singleLayout.persistedValue,
            )
        }
        editor.putBoolean(DUAL_SIGNAL_ENABLED_KEY, enabled).apply()
    }

    fun setLayout(layout: ContentLayout) {
        preferences
            .edit()
            .putString(activeGlobalKey(CONTENT_LAYOUT_KEY), layout.persistedValue)
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

    fun setTopReadout(enabled: Boolean) {
        val layout = preferences.readLayout()
        val dual = preferences.dualSignalEnabled()
        val readoutKey = visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY, dual)
        val offsetKey = visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY, dual)
        val editor = preferences.edit().putBoolean(readoutKey, enabled)
        if (
            enabled &&
            !preferences.profileBoolean(layout, BATTERY_TOP_READOUT_ENABLED_KEY, false, dual) &&
            !preferences.contains(offsetKey) &&
            !preferences.contains(visualModeKey(dual, BATTERY_TOP_VERTICAL_OFFSET_KEY))
        ) {
            editor.putFloat(offsetKey, topOffsetYRaw(5f))
        }
        editor.apply()
    }

    fun setTopTextFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setTopChargingIcon(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY), enabled)
            .apply()
    }

    fun setTopChargingIconFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setFillFollowsRetract(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeGlobalKey(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY), enabled)
            .apply()
    }

    fun setCcTintTransition(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeGlobalKey(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY), enabled)
            .apply()
    }

    fun setTopTextScale(scale: Float) {
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

    fun setTopTextWeight(weight: Int) {
        preferences
            .edit()
            .putInt(
                activeProfileKey(BATTERY_TOP_TEXT_WEIGHT_KEY),
                weight.coerceIn(BATTERY_TOP_TEXT_WEIGHT_MIN, BATTERY_TOP_TEXT_WEIGHT_MAX),
            )
            .apply()
    }

    fun setTopOffsetY(offset: Float) {
        val uiOffset =
            offset.coerceIn(
                BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(BATTERY_TOP_VERTICAL_OFFSET_KEY),
                topOffsetYRaw(uiOffset),
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

    fun setWifiScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(WIFI_SIZE_SCALE_KEY),
                scale.coerceIn(WIFI_SIZE_SCALE_MIN, WIFI_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setAirplaneScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(AIRPLANE_SIZE_SCALE_KEY),
                scale.coerceIn(AIRPLANE_SIZE_SCALE_MIN, AIRPLANE_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setNoSimScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(NO_SIM_SIZE_SCALE_KEY),
                scale.coerceIn(NO_SIM_SIZE_SCALE_MIN, NO_SIM_SIZE_SCALE_MAX),
            )
            .apply()
    }

    fun setMobileTypeScale(scale: Float) {
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
            .putString(activeGlobalKey(BATTERY_COLOR_PRESET_KEY), preset.persistedValue)
            .apply()
    }

    fun setBatteryColorMode(
        slot: BatteryColorSlot,
        mode: BatteryColorMode,
    ) {
        preferences.edit()
            .putString(activeGlobalKey(batteryColorModeKey(slot)), mode.persistedValue)
            .apply()
    }

    fun setBatteryColorOverride(
        slot: BatteryColorSlot,
        color: Int?,
    ) {
        val key = activeGlobalKey(batteryColorOverrideKey(slot))
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
            .remove(activeGlobalKey(batteryColorModeKey(slot)))
            .remove(activeGlobalKey(batteryColorOverrideKey(slot)))
            .apply()
    }

    fun resetBatteryColorOverrides() {
        val editor = preferences.edit()
        BatteryColorSlot.entries.forEach { slot ->
            editor.remove(activeGlobalKey(batteryColorModeKey(slot)))
            editor.remove(activeGlobalKey(batteryColorOverrideKey(slot)))
        }
        editor.apply()
    }

    fun resetToDefaults() {
        preferences.edit().clear().apply()
    }

    fun setTopChargingIconScale(scale: Float) {
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

internal fun visualModeKey(dual: Boolean, key: String): String =
    if (dual) "dual.$key" else key

internal fun SharedPreferences.dualSignalEnabled(): Boolean =
    getBoolean(DUAL_SIGNAL_ENABLED_KEY, false)

internal fun SharedPreferences.readLayout(
    dual: Boolean = dualSignalEnabled(),
): ContentLayout =
    ContentLayout.fromPersisted(
        getString(
            visualModeKey(dual, CONTENT_LAYOUT_KEY),
            ContentLayout.NETWORK_CENTER.persistedValue,
        ),
    )

internal fun visualProfileKey(
    layout: ContentLayout,
    baseKey: String,
    dual: Boolean = false,
): String = visualModeKey(dual, layout.persistedValue + "." + baseKey)

private fun SharedPreferences.profileBoolean(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Boolean,
    dual: Boolean,
): Boolean {
    val profileKey = visualProfileKey(layout, baseKey, dual)
    return if (contains(profileKey)) {
        getBoolean(profileKey, defaultValue)
    } else if (dual) {
        defaultValue
    } else {
        getBoolean(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileFloat(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Float,
    dual: Boolean,
): Float {
    val profileKey = visualProfileKey(layout, baseKey, dual)
    return if (contains(profileKey)) {
        getFloat(profileKey, defaultValue)
    } else if (dual) {
        defaultValue
    } else {
        getFloat(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileInt(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Int,
    dual: Boolean,
): Int {
    val profileKey = visualProfileKey(layout, baseKey, dual)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else if (dual) {
        defaultValue
    } else {
        getInt(baseKey, defaultValue)
    }
}

internal fun SharedPreferences.readVisualCfg(
    dual: Boolean = dualSignalEnabled(),
    layoutOverride: ContentLayout? = null,
): VisualCfg {
    val layout = layoutOverride ?: readLayout(dual)
    fun flag(key: String, default: Boolean) = profileBoolean(layout, key, default, dual)
    fun size(key: String, default: Float) = profileFloat(layout, key, default, dual)
    fun weight(key: String, default: Int) = profileInt(layout, key, default, dual)

    return VisualCfg(
        layout = layout,
        dualSignal = dual,
        mobileFollowsBatteryColor = flag(MOBILE_FOLLOWS_BATTERY_COLOR_KEY, false),
        centerFollowsBatteryColor = flag(CENTER_FOLLOWS_BATTERY_COLOR_KEY, false),
        showTopReadout = flag(BATTERY_TOP_READOUT_ENABLED_KEY, false),
        topTextFollowsBatteryColor = flag(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY, true),
        showTopChargingIcon = flag(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY, true),
        topChargingIconFollowsBatteryColor = flag(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY, true),
        fillFollowsRetract = getBoolean(visualModeKey(dual, BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY), true),
        ccTintTransition = getBoolean(visualModeKey(dual, CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY), true),
        topTextScale = size(BATTERY_TOP_TEXT_SCALE_KEY, topTextScaleDefault(layout)),
        topTextWeight = weight(BATTERY_TOP_TEXT_WEIGHT_KEY, BATTERY_TOP_TEXT_WEIGHT_DEFAULT),
        topOffsetY = size(BATTERY_TOP_VERTICAL_OFFSET_KEY, BATTERY_TOP_VERTICAL_OFFSET_DEFAULT),
        topChargingIconScale = size(BATTERY_TOP_CHARGING_ICON_SCALE_KEY, topChargingIconScaleDefault(layout)),
        combinedScale = size(COMBINED_SCALE_KEY, COMBINED_SCALE_DEFAULT),
        outerWeightScale = size(OUTER_WEIGHT_SCALE_KEY, OUTER_WEIGHT_SCALE_DEFAULT),
        wifiScale = size(WIFI_SIZE_SCALE_KEY, WIFI_SIZE_SCALE_DEFAULT),
        airplaneScale = size(AIRPLANE_SIZE_SCALE_KEY, AIRPLANE_SIZE_SCALE_DEFAULT),
        noSimScale = size(NO_SIM_SIZE_SCALE_KEY, NO_SIM_SIZE_SCALE_DEFAULT),
        mobileTypeScale = size(MOBILE_TYPE_SIZE_SCALE_KEY, mobileTypeScaleDefault(layout)),
        mobileTypeWeight = weight(MOBILE_TYPE_WEIGHT_KEY, MOBILE_TYPE_WEIGHT_DEFAULT),
        batteryColorPreset =
            BatteryColorPreset.fromPersisted(
                getString(
                    visualModeKey(dual, BATTERY_COLOR_PRESET_KEY),
                    BatteryColorPreset.HYPEROS.persistedValue,
                ),
            ),
        batteryColorModes =
            BatteryColorModes(
                normal =
                    batteryColorMode(
                        modeKey = visualModeKey(dual, BATTERY_COLOR_MODE_NORMAL_KEY),
                        colorKey = visualModeKey(dual, BATTERY_COLOR_NORMAL_KEY),
                    ),
                powerSave =
                    batteryColorMode(
                        modeKey = visualModeKey(dual, BATTERY_COLOR_MODE_POWER_SAVE_KEY),
                        colorKey = visualModeKey(dual, BATTERY_COLOR_POWER_SAVE_KEY),
                    ),
                performance =
                    batteryColorMode(
                        modeKey = visualModeKey(dual, BATTERY_COLOR_MODE_PERFORMANCE_KEY),
                        colorKey = visualModeKey(dual, BATTERY_COLOR_PERFORMANCE_KEY),
                    ),
                superPowerSave =
                    batteryColorMode(
                        modeKey = visualModeKey(dual, BATTERY_COLOR_MODE_SUPER_POWER_SAVE_KEY),
                        colorKey = visualModeKey(dual, BATTERY_COLOR_SUPER_POWER_SAVE_KEY),
                    ),
                charging =
                    batteryColorMode(
                        modeKey = visualModeKey(dual, BATTERY_COLOR_MODE_CHARGING_KEY),
                        colorKey = visualModeKey(dual, BATTERY_COLOR_CHARGING_KEY),
                    ),
                low =
                    batteryColorMode(
                        modeKey = visualModeKey(dual, BATTERY_COLOR_MODE_LOW_KEY),
                        colorKey = visualModeKey(dual, BATTERY_COLOR_LOW_KEY),
                    ),
            ),
        batteryColorOverrides =
            BatteryColorOverrides(
                normal = optionalColor(visualModeKey(dual, BATTERY_COLOR_NORMAL_KEY)),
                powerSave = optionalColor(visualModeKey(dual, BATTERY_COLOR_POWER_SAVE_KEY)),
                performance = optionalColor(visualModeKey(dual, BATTERY_COLOR_PERFORMANCE_KEY)),
                superPowerSave = optionalColor(visualModeKey(dual, BATTERY_COLOR_SUPER_POWER_SAVE_KEY)),
                charging = optionalColor(visualModeKey(dual, BATTERY_COLOR_CHARGING_KEY)),
                low = optionalColor(visualModeKey(dual, BATTERY_COLOR_LOW_KEY)),
            ),
    ).normalized()
}

internal fun SharedPreferences.Editor.putVisualCfg(
    settings: VisualCfg,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val layout = normalized.layout
    return putString(
        visualModeKey(normalized.dualSignal, CONTENT_LAYOUT_KEY),
        layout.persistedValue,
    ).putBoolean(
        visualProfileKey(layout, MOBILE_FOLLOWS_BATTERY_COLOR_KEY, normalized.dualSignal),
        normalized.mobileFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, CENTER_FOLLOWS_BATTERY_COLOR_KEY, normalized.dualSignal),
        normalized.centerFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY, normalized.dualSignal),
        normalized.showTopReadout,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY, normalized.dualSignal),
        normalized.topTextFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_ENABLED_KEY, normalized.dualSignal),
        normalized.showTopChargingIcon,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY, normalized.dualSignal),
        normalized.topChargingIconFollowsBatteryColor,
    ).putBoolean(
        visualModeKey(normalized.dualSignal, BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY),
        normalized.fillFollowsRetract,
    ).putBoolean(
        visualModeKey(normalized.dualSignal, CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY),
        normalized.ccTintTransition,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_TEXT_SCALE_KEY, normalized.dualSignal),
        normalized.topTextScale,
    ).putInt(
        visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY, normalized.dualSignal),
        normalized.topTextWeight,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY, normalized.dualSignal),
        normalized.topOffsetY,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_SCALE_KEY, normalized.dualSignal),
        normalized.topChargingIconScale,
    ).putFloat(
        visualProfileKey(layout, COMBINED_SCALE_KEY, normalized.dualSignal),
        normalized.combinedScale,
    ).putFloat(
        visualProfileKey(layout, OUTER_WEIGHT_SCALE_KEY, normalized.dualSignal),
        normalized.outerWeightScale,
    ).putFloat(
        visualProfileKey(layout, WIFI_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.wifiScale,
    ).putFloat(
        visualProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.airplaneScale,
    ).putFloat(
        visualProfileKey(layout, NO_SIM_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.noSimScale,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.mobileTypeScale,
    ).putInt(
        visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY, normalized.dualSignal),
        normalized.mobileTypeWeight,
    ).putString(
        visualModeKey(normalized.dualSignal, BATTERY_COLOR_PRESET_KEY),
        normalized.batteryColorPreset.persistedValue,
    ).applyBatteryColorModes(normalized.batteryColorModes, normalized.dualSignal)
        .applyBatteryColorOverrides(normalized.batteryColorOverrides, normalized.dualSignal)
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
    dual: Boolean,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        val key = visualModeKey(dual, batteryColorOverrideKey(slot))
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
    dual: Boolean,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        putString(
            visualModeKey(dual, batteryColorModeKey(slot)),
            modes.modeFor(slot).persistedValue,
        )
    }
    return this
}
