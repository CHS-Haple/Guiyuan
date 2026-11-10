package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class VisualRepo(context: Context) {
    private val context = context.applicationContext
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateVisualPrefsIfNeeded(preferences)
        retireLegacyIdleActivityPreference(preferences)
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
        visualProfileKey(preferences.readLayout(), baseKey)

    fun setDualSignal(enabled: Boolean) {
        preferences.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, enabled)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .apply()
    }

    fun setDualRowDotArrangement(style: SignalDotArrangement) {
        preferences.edit().putString(activeProfileKey(DUAL_ROW_DOT_ARRANGEMENT_KEY), style.persistedValue).apply()
    }

    fun setBothSidesShowActivity(enabled: Boolean) {
        preferences.edit().putBoolean(activeProfileKey(BOTH_SIDES_ACTIVITY_KEY), enabled).apply()
    }

    fun setBothSidesHideIdleActivity(enabled: Boolean) {
        preferences.edit().putBoolean(activeProfileKey(BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY), enabled).apply()
    }

    fun setBothSidesBottomCharging(enabled: Boolean) {
        preferences.edit().putBoolean(activeProfileKey(BOTH_SIDES_CHARGING_KEY), enabled).apply()
    }

    fun setBothSidesActivityScale(scale: Float) {
        preferences.edit().putFloat(
            activeProfileKey(BOTH_SIDES_ACTIVITY_SCALE_KEY),
            scale.finiteIn(
                BOTH_SIDES_BOTTOM_INFO_SCALE_MIN,
                BOTH_SIDES_BOTTOM_INFO_SCALE_MAX,
                BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT,
            ),
        ).apply()
    }

    fun setBothSidesActivityOffsetY(offset: Float) {
        preferences.edit().putFloat(
            activeProfileKey(BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY),
            offset.finiteIn(NETWORK_TOP_OFFSET_MIN, NETWORK_TOP_OFFSET_MAX, NETWORK_TOP_OFFSET_DEFAULT),
        ).apply()
    }

    fun setBothSidesChargingOffsetY(offset: Float) {
        preferences.edit().putFloat(
            activeProfileKey(BOTH_SIDES_CHARGING_OFFSET_Y_KEY),
            offset.finiteIn(NETWORK_TOP_OFFSET_MIN, NETWORK_TOP_OFFSET_MAX, NETWORK_TOP_OFFSET_DEFAULT),
        ).apply()
    }

    fun setSingleRowDotArrangement(style: SignalDotArrangement) {
        require(style != SignalDotArrangement.BOTH_SIDES) {
            "Two-sided signal dots require dual-row mode"
        }
        preferences.edit().putString(
            activeProfileKey(SINGLE_ROW_DOT_STYLE_KEY),
            style.persistedValue,
        ).apply()
    }

    fun setFixedLowerOpening(enabled: Boolean) {
        preferences.edit().putBoolean(activeProfileKey(LOWER_FIXED_OPENING_KEY), enabled).apply()
    }

    fun setMobileTypeBottomOffsetY(offset: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY),
                offset.finiteIn(
                    NETWORK_TOP_OFFSET_MIN,
                    NETWORK_TOP_OFFSET_MAX,
                    mobileTypeBottomOffsetDefault(preferences.readLayout()),
                ),
            ).apply()
    }

    fun setLayout(layout: ContentLayout) {
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

    fun setTopReadout(enabled: Boolean) {
        val layout = preferences.readLayout()
        val dual = preferences.dualSignalEnabled()
        val readoutKey = visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY)
        val offsetKey = visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY)
        val editor = preferences.edit().putBoolean(readoutKey, enabled)
        if (
            layout == ContentLayout.BATTERY_CENTER &&
            enabled &&
            !preferences.profileBoolean(layout, BATTERY_TOP_READOUT_ENABLED_KEY, false) &&
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

    fun setChargingIcon(enabled: Boolean) {
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
            .putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, enabled)
            .apply()
    }

    fun setCcTintTransition(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, enabled)
            .apply()
    }

    fun setTopTextScale(scale: Float) {
        val uiScale =
            scale.finiteIn(
                BATTERY_TOP_TEXT_UI_SCALE_MIN,
                BATTERY_TOP_TEXT_UI_SCALE_MAX,
                topTextUiScaleDefault(preferences.readLayout()),
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
            offset.finiteIn(
                BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
                0f,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(BATTERY_TOP_VERTICAL_OFFSET_KEY),
                topOffsetYRaw(uiOffset),
            )
            .apply()
    }

    fun setWifiOffsetY(offset: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(WIFI_VERTICAL_OFFSET_KEY),
                offset.finiteIn(
                    NETWORK_TOP_OFFSET_MIN,
                    NETWORK_TOP_OFFSET_MAX,
                    NETWORK_TOP_OFFSET_DEFAULT,
                ),
            ).apply()
    }

    fun setMobileTypeOffsetY(offset: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(MOBILE_TYPE_VERTICAL_OFFSET_KEY),
                offset.finiteIn(
                    NETWORK_TOP_OFFSET_MIN,
                    NETWORK_TOP_OFFSET_MAX,
                    NETWORK_TOP_OFFSET_DEFAULT,
                ),
            ).apply()
    }

    fun setSignalDotOffsetY(offset: Float) {
        preferences.writeSignalDotOffsetY(offset)
    }

    fun setCombinedScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(COMBINED_SCALE_KEY),
                scale.finiteIn(
                    COMBINED_SCALE_MIN,
                    COMBINED_SCALE_MAX,
                    COMBINED_SCALE_DEFAULT,
                ),
            )
            .apply()
    }

    fun setOuterWeightScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(OUTER_WEIGHT_SCALE_KEY),
                scale.finiteIn(
                    OUTER_WEIGHT_SCALE_MIN,
                    OUTER_WEIGHT_SCALE_MAX,
                    OUTER_WEIGHT_SCALE_DEFAULT,
                ),
            )
            .apply()
    }

    fun setWifiWeight(weight: Int) {
        preferences.edit()
            .putInt(activeProfileKey(WIFI_WEIGHT_KEY), weight.coerceIn(WIFI_WEIGHT_MIN, WIFI_WEIGHT_MAX))
            .apply()
    }

    fun setWifiScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(WIFI_SIZE_SCALE_KEY),
                scale.finiteIn(
                    WIFI_SIZE_SCALE_MIN,
                    WIFI_SIZE_SCALE_MAX,
                    WIFI_SIZE_SCALE_DEFAULT,
                ),
            )
            .apply()
    }

    fun setAirplaneWeight(weight: Int) {
        preferences.edit()
            .putInt(activeProfileKey(AIRPLANE_WEIGHT_KEY), weight.coerceIn(AIRPLANE_WEIGHT_MIN, AIRPLANE_WEIGHT_MAX))
            .apply()
    }

    fun setAirplaneScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(AIRPLANE_SIZE_SCALE_KEY),
                scale.finiteIn(
                    AIRPLANE_SIZE_SCALE_MIN,
                    AIRPLANE_SIZE_SCALE_MAX,
                    AIRPLANE_SIZE_SCALE_DEFAULT,
                ),
            )
            .apply()
    }

    fun setNoSimScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(NO_SIM_SIZE_SCALE_KEY),
                scale.finiteIn(
                    NO_SIM_SIZE_SCALE_MIN,
                    NO_SIM_SIZE_SCALE_MAX,
                    NO_SIM_SIZE_SCALE_DEFAULT,
                ),
            )
            .apply()
    }

    fun setMobileTypeScale(scale: Float) {
        preferences.edit()
            .putFloat(
                activeProfileKey(MOBILE_TYPE_SIZE_SCALE_KEY),
                scale.finiteIn(
                    MOBILE_TYPE_SIZE_SCALE_MIN,
                    MOBILE_TYPE_SIZE_SCALE_MAX,
                    mobileTypeScaleDefault(preferences.readLayout()),
                ),
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

    fun backup(): VisualBackup {
        val layout = preferences.readLayout()
        return VisualBackup(
            singleLayout = layout,
            dualLayout = layout,
            dualEnabled = preferences.dualSignalEnabled(),
            dualInitialized = preferences.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false),
            profiles = listOf(false, true).flatMap { dual ->
                ContentLayout.entries.map { layout ->
                    preferences.readVisualCfg(dual = dual, layoutOverride = layout)
                }
            },
        )
    }

    fun backupSnapshot(): Map<String, Any?> = preferences.backupVisualSnapshot()

    fun rollbackBackup(snapshot: Map<String, Any?>): Boolean =
        preferences.rollbackBackupVisuals(snapshot)

    fun restore(backup: VisualBackup, library: BatteryColorSchemeLibrary): Boolean {
        val editor = preferences.editBackupVisuals()
        backup.profiles.filter { it.dualSignal == backup.dualEnabled }
            .forEach { editor.putVisualCfg(it) }
        val active = backup.profiles.first { it.dualSignal == backup.dualEnabled }
        editor.putBatteryColors(active)
        editor.putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, active.fillFollowsRetract)
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, active.ccTintTransition)
        val applied = library.matchingKey(active) ?: library.activeSchemeKey
        writeBatterySchemeLibrary(editor, library.copy(activeSchemeKey = applied))
        val activeLayout = if (backup.dualEnabled) backup.dualLayout else backup.singleLayout
        editor.putString(CONTENT_LAYOUT_KEY, activeLayout.persistedValue)
        // Shared visual profiles require no separate dual-bank initialization.
        // Even a valid legacy backup with dualInitialized=false must be ready
        // for toggling dual mode and exporting a new backup.
        editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        editor.putBoolean(DUAL_SIGNAL_ENABLED_KEY, backup.dualEnabled)
        editor.putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        editor.putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
        // Restored physical charging scales use the current reference.
        return editor.commit()
    }

    fun resetToDefaults() {
        resetVisualPreferences(preferences, BatterySchemeRepo(context).current())
    }

    fun setTopChargingIconScale(scale: Float) {
        val uiScale =
            scale.finiteIn(
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
                topChargingIconUiScaleDefault(preferences.readLayout()),
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

private fun storedProfileKey(
    layout: ContentLayout,
    baseKey: String,
    legacyDualBank: Boolean,
): String {
    val sharedKey = visualProfileKey(layout, baseKey)
    return if (legacyDualBank) visualModeKey(true, sharedKey) else sharedKey
}

private fun SharedPreferences.profileBoolean(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Boolean,
    legacyDualBank: Boolean = false,
): Boolean {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return if (contains(profileKey)) {
        getBoolean(profileKey, defaultValue)
    } else if (legacyDualBank) {
        defaultValue
    } else {
        getBoolean(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileFloat(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Float,
    legacyDualBank: Boolean = false,
): Float {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return if (contains(profileKey)) {
        getFloat(profileKey, defaultValue)
    } else if (legacyDualBank) {
        defaultValue
    } else {
        getFloat(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileInt(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Int,
    legacyDualBank: Boolean = false,
): Int {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else if (legacyDualBank) {
        defaultValue
    } else {
        getInt(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileString(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: String?,
    legacyDualBank: Boolean,
): String? {
    val profileKey = storedProfileKey(layout, baseKey, legacyDualBank)
    return when {
        contains(profileKey) -> getString(profileKey, defaultValue)
        legacyDualBank -> defaultValue
        else -> getString(baseKey, defaultValue)
    }
}

internal fun SharedPreferences.readVisualCfg(
    dual: Boolean = dualSignalEnabled(),
    layoutOverride: ContentLayout? = null,
): VisualCfg {
    val layout = layoutOverride ?: readLayout()
    // SystemUI can bind an older remote snapshot before the app migrates it.
    // Only read the retired dual bank when that bank actually exists.
    val legacyDualBank = dual &&
        getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0) < 1 &&
        // The local v4 style migration may leave a temporary old dual bank.
        getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) < 4 &&
        getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false) &&
        PROFILE_VISUAL_BASE_KEYS.any { key ->
            contains(visualModeKey(true, visualProfileKey(layout, key)))
        }
    fun flag(key: String, default: Boolean) =
        profileBoolean(layout, key, default, legacyDualBank)
    fun size(key: String, default: Float) =
        profileFloat(layout, key, default, legacyDualBank)
    fun weight(key: String, default: Int) =
        profileInt(layout, key, default, legacyDualBank)
    val topOffset = size(BATTERY_TOP_VERTICAL_OFFSET_KEY, topOffsetYDefault(layout))
    // Preserve existing battery-centered network position when split keys are first read.
    val oldNetworkOffset =
        if (layout == ContentLayout.BATTERY_CENTER) topOffsetYUi(topOffset) else 0f

    val dualStyle = (
        profileString(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY, null, legacyDualBank)
            ?: getString(visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY), null)
        )?.let(SignalDotArrangement::fromPersisted)
        ?: SignalDotArrangement.BOTH_SIDES
    val singleStyle = profileString(layout, SINGLE_ROW_DOT_STYLE_KEY, null, legacyDualBank)
        ?.let(SignalDotArrangement::fromPersisted)
        ?: if (flag(LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY, false) &&
            dualStyle == SignalDotArrangement.HORIZONTAL) {
            SignalDotArrangement.HORIZONTAL
        } else {
            SignalDotArrangement.CURVED
        }

    return VisualCfg(
        layout = layout,
        dualSignal = dual,
        dualRowDotArrangement = dualStyle,
        singleRowDotArrangement = singleStyle,
        bothSidesShowActivity = flag(BOTH_SIDES_ACTIVITY_KEY, true),
        bothSidesHideIdleActivity = flag(BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY, false),
        bothSidesBottomCharging = flag(BOTH_SIDES_CHARGING_KEY, false),
        bothSidesActivityScale = size(BOTH_SIDES_ACTIVITY_SCALE_KEY, BOTH_SIDES_BOTTOM_INFO_SCALE_DEFAULT),
        bothSidesActivityOffsetY = size(BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        bothSidesChargingOffsetY = size(BOTH_SIDES_CHARGING_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        fixedLowerOpening = flag(LOWER_FIXED_OPENING_KEY, false),
        mobileTypeBottomOffsetY = size(MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY, mobileTypeBottomOffsetDefault(layout)),
        mobileFollowsBatteryColor = flag(MOBILE_FOLLOWS_BATTERY_COLOR_KEY, false),
        centerFollowsBatteryColor = flag(CENTER_FOLLOWS_BATTERY_COLOR_KEY, false),
        showTopReadout = flag(BATTERY_TOP_READOUT_ENABLED_KEY, topReadoutDefault(layout)),
        topTextFollowsBatteryColor = flag(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY, true),
        showChargingIcon = flag(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY, true),
        topChargingIconFollowsBatteryColor = flag(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY, true),
        fillFollowsRetract = getBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, true),
        ccTintTransition = getBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, true),
        topTextScale = size(BATTERY_TOP_TEXT_SCALE_KEY, topTextScaleDefault(layout)),
        topTextWeight = weight(BATTERY_TOP_TEXT_WEIGHT_KEY, BATTERY_TOP_TEXT_WEIGHT_DEFAULT),
        topOffsetY = topOffset,
        wifiOffsetY = size(WIFI_VERTICAL_OFFSET_KEY, oldNetworkOffset),
        mobileTypeOffsetY = size(MOBILE_TYPE_VERTICAL_OFFSET_KEY, oldNetworkOffset),
        curvedSignalDotOffsetY = size(CURVED_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        horizontalSignalDotOffsetY = size(HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        bothSidesSignalDotOffsetY = size(BOTH_SIDES_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        topChargingIconScale = size(BATTERY_TOP_CHARGING_ICON_SCALE_KEY, topChargingIconScaleDefault(layout)),
        combinedScale = size(COMBINED_SCALE_KEY, COMBINED_SCALE_DEFAULT),
        outerWeightScale = size(OUTER_WEIGHT_SCALE_KEY, OUTER_WEIGHT_SCALE_DEFAULT),
        wifiScale = size(WIFI_SIZE_SCALE_KEY, WIFI_SIZE_SCALE_DEFAULT),
        wifiWeight = weight(WIFI_WEIGHT_KEY, WIFI_WEIGHT_DEFAULT),
        airplaneScale = size(AIRPLANE_SIZE_SCALE_KEY, AIRPLANE_SIZE_SCALE_DEFAULT),
        airplaneWeight = weight(AIRPLANE_WEIGHT_KEY, AIRPLANE_WEIGHT_DEFAULT),
        noSimScale = size(NO_SIM_SIZE_SCALE_KEY, NO_SIM_SIZE_SCALE_DEFAULT),
        mobileTypeScale = size(MOBILE_TYPE_SIZE_SCALE_KEY, mobileTypeScaleDefault(layout)),
        mobileTypeWeight = weight(MOBILE_TYPE_WEIGHT_KEY, MOBILE_TYPE_WEIGHT_DEFAULT),
        batteryColorPreset =
            BatteryColorPreset.fromPersisted(
                getString(
                    BATTERY_COLOR_PRESET_KEY,
                    BatteryColorPreset.HYPEROS.persistedValue,
                ),
            ),
        batteryColorModes =
            BatteryColorModes(
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
            BatteryColorOverrides(
                normal = optionalColor(BATTERY_COLOR_NORMAL_KEY),
                powerSave = optionalColor(BATTERY_COLOR_POWER_SAVE_KEY),
                performance = optionalColor(BATTERY_COLOR_PERFORMANCE_KEY),
                superPowerSave = optionalColor(BATTERY_COLOR_SUPER_POWER_SAVE_KEY),
                charging = optionalColor(BATTERY_COLOR_CHARGING_KEY),
                low = optionalColor(BATTERY_COLOR_LOW_KEY),
            ),
    ).normalized()
}

internal fun SharedPreferences.Editor.putVisualCfg(
    settings: VisualCfg,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val layout = normalized.layout
    val editor = putBoolean(
        DUAL_SIGNAL_ENABLED_KEY,
        normalized.dualSignal,
    ).putString(
        CONTENT_LAYOUT_KEY,
        layout.persistedValue,
    ).putString(
        visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY),
        normalized.dualRowDotArrangement.persistedValue,
    ).putString(
        visualProfileKey(layout, SINGLE_ROW_DOT_STYLE_KEY),
        normalized.singleRowDotArrangement.persistedValue,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_KEY),
        normalized.bothSidesShowActivity,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_HIDE_IDLE_ACTIVITY_KEY),
        normalized.bothSidesHideIdleActivity,
    ).putBoolean(
        visualProfileKey(layout, BOTH_SIDES_CHARGING_KEY),
        normalized.bothSidesBottomCharging,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_SCALE_KEY),
        normalized.bothSidesActivityScale,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_ACTIVITY_OFFSET_Y_KEY),
        normalized.bothSidesActivityOffsetY,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_CHARGING_OFFSET_Y_KEY),
        normalized.bothSidesChargingOffsetY,
    ).putBoolean(
        visualProfileKey(layout, LOWER_FIXED_OPENING_KEY),
        normalized.fixedLowerOpening,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY),
        normalized.mobileTypeBottomOffsetY,
    ).putBoolean(
        visualProfileKey(layout, MOBILE_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.mobileFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, CENTER_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.centerFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY),
        normalized.showTopReadout,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.topTextFollowsBatteryColor,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_ENABLED_KEY),
        normalized.showChargingIcon,
    ).putBoolean(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.topChargingIconFollowsBatteryColor,
    ).putBoolean(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        normalized.fillFollowsRetract,
    ).putBoolean(
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
        normalized.ccTintTransition,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_TEXT_SCALE_KEY),
        normalized.topTextScale,
    ).putInt(
        visualProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY),
        normalized.topTextWeight,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY),
        normalized.topOffsetY,
    ).putFloat(
        visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY),
        normalized.wifiOffsetY,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_VERTICAL_OFFSET_KEY),
        normalized.mobileTypeOffsetY,
    ).putFloat(
        visualProfileKey(layout, CURVED_SIGNAL_DOT_OFFSET_Y_KEY),
        normalized.curvedSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY),
        normalized.horizontalSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, BOTH_SIDES_SIGNAL_DOT_OFFSET_Y_KEY),
        normalized.bothSidesSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
        normalized.topChargingIconScale,
    ).putFloat(
        visualProfileKey(layout, COMBINED_SCALE_KEY),
        normalized.combinedScale,
    ).putFloat(
        visualProfileKey(layout, OUTER_WEIGHT_SCALE_KEY),
        normalized.outerWeightScale,
    ).putFloat(
        visualProfileKey(layout, WIFI_SIZE_SCALE_KEY),
        normalized.wifiScale,
    ).putInt(
        visualProfileKey(layout, WIFI_WEIGHT_KEY),
        normalized.wifiWeight,
    ).putFloat(
        visualProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY),
        normalized.airplaneScale,
    ).putInt(
        visualProfileKey(layout, AIRPLANE_WEIGHT_KEY),
        normalized.airplaneWeight,
    ).putFloat(
        visualProfileKey(layout, NO_SIM_SIZE_SCALE_KEY),
        normalized.noSimScale,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY),
        normalized.mobileTypeScale,
    ).putInt(
        visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY),
        normalized.mobileTypeWeight,
    ).putBatteryColors(normalized)
    // One writer serves app-local settings and the SystemUI remote mirror.
    // Publish the schema with the values so stale dual banks cannot win later.
    return editor
        .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        .putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
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

