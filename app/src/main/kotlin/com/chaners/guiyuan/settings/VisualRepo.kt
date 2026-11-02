package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged


private val visualPreferenceKeys = buildSet {
    // Retired singleton key can be cleared by backup restore; new values are per profile.
    add(BATTERY_AVOIDANCE_MODE_KEY)
    add(SHARED_CONTENT_STYLE_SCHEMA_KEY)
    add(CONTENT_LAYOUT_KEY)
    add(DUAL_SIGNAL_ENABLED_KEY)
    add(DUAL_PROFILES_INITIALIZED_KEY)
    addAll(PROFILE_VISUAL_BASE_KEYS)
    addAll(GLOBAL_VISUAL_KEYS)
    ContentLayout.entries.forEach { layout ->
        PROFILE_VISUAL_BASE_KEYS.forEach { key ->
            add(visualProfileKey(layout, key))
            add(visualProfileKey(layout, key, dual = true))
        }
    }
}

internal fun isVisualPreferenceKey(key: String?): Boolean =
    key == null || key in visualPreferenceKeys

// A backup owns feature visuals and scheme entries, not the whole preference file.
internal fun isBackupVisualKey(key: String): Boolean =
    key in visualPreferenceKeys || isBatterySchemeStorageKey(key)

internal fun SharedPreferences.editBackupVisuals(): SharedPreferences.Editor =
    edit().also { editor ->
        all.keys.filter(::isBackupVisualKey).forEach(editor::remove)
    }

internal fun SharedPreferences.backupVisualSnapshot(): Map<String, Any?> =
    all.filterKeys(::isBackupVisualKey)
        .mapValues { (_, value) ->
            if (value is Set<*>) value.filterIsInstance<String>().toSet() else value
        }

internal fun SharedPreferences.rollbackBackupVisuals(snapshot: Map<String, Any?>): Boolean {
    val editor = editBackupVisuals()
    snapshot.forEach { (key, value) ->
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> error("Unexpected visual preference value")
        }
    }
    return editor.commit()
}

// v1.0.0 stored dot style per SIM mode. Split that released setting into
// independent layout profiles once. Unreleased Canary settings are not imported.
internal fun migrateLayoutContentStylesIfNeeded(prefs: SharedPreferences) {
    if (prefs.getInt(SHARED_CONTENT_STYLE_SCHEMA_KEY, 0) >= 3) return
    val editor = prefs.edit()
    for (dual in listOf(false, true)) {
        val releasedKey = visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY)
        val releasedStyle = ContentStyle.fromPersisted(prefs.getString(releasedKey, null))
        for (layout in ContentLayout.entries) {
            val target = visualProfileKey(layout, CONTENT_STYLE_KEY, dual)
            if (!prefs.contains(target)) editor.putString(target, releasedStyle.persistedValue)
        }
        editor.remove(releasedKey)
    }
    editor.remove(CONTENT_STYLE_KEY)
        .remove(SINGLE_ROW_FLAT_DOTS_KEY)
        .putInt(SHARED_CONTENT_STYLE_SCHEMA_KEY, 3)
        .apply()
}

// Migrations must finish before the app mirrors its first runtime snapshot.
internal fun migrateVisualPrefsIfNeeded(preferences: SharedPreferences) {
    migrateLayoutContentStylesIfNeeded(preferences)
}

// Reset visual choices without deleting user-created battery schemes.
internal fun resetVisualPreferences(
    preferences: SharedPreferences,
    schemes: BatteryColorSchemeLibrary,
) {
    val editor = preferences.edit().clear()
    writeBatterySchemeLibrary(
        editor,
        schemes.copy(activeSchemeKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY),
    )
    editor.apply()
}

internal class VisualRepo(context: Context) {
    private val context = context.applicationContext
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateVisualPrefsIfNeeded(preferences)
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

    fun setDualSignal(enabled: Boolean) {
        val editor = preferences.edit()
        if (enabled && !preferences.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false)) {
            val layout = preferences.readLayout()
            ContentLayout.entries.forEach { profile ->
                editor.putVisualCfg(
                    preferences.readVisualCfg(dual = false, layoutOverride = profile)
                        .copy(dualSignal = true),
                )
            }
            editor.putString(CONTENT_LAYOUT_KEY, layout.persistedValue)
            editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        }
        editor.putBoolean(DUAL_SIGNAL_ENABLED_KEY, enabled).apply()
    }

    fun setContentStyle(style: ContentStyle) {
        preferences.edit().putString(activeProfileKey(CONTENT_STYLE_KEY), style.persistedValue).apply()
    }

    fun setBatteryAvoidanceMode(mode: BatteryAvoidanceMode) {
        preferences.writeBatteryAvoidanceMode(mode)
    }

    fun setSingleRowFlatDots(enabled: Boolean) {
        preferences.edit().putBoolean(activeProfileKey(SINGLE_ROW_FLAT_DOTS_KEY), enabled).apply()
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
        val readoutKey = visualProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY, dual)
        val offsetKey = visualProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY, dual)
        val editor = preferences.edit().putBoolean(readoutKey, enabled)
        if (
            layout == ContentLayout.BATTERY_CENTER &&
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

    fun backup(): VisualBackup =
        VisualBackup(
            singleLayout = preferences.readLayout(),
            dualLayout = preferences.readLayout(),
            dualEnabled = preferences.dualSignalEnabled(),
            dualInitialized = preferences.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false),
            profiles = listOf(false, true).flatMap { dual ->
                ContentLayout.entries.map { layout ->
                    preferences.readVisualCfg(dual = dual, layoutOverride = layout)
                }
            },
        )

    fun backupSnapshot(): Map<String, Any?> = preferences.backupVisualSnapshot()

    fun rollbackBackup(snapshot: Map<String, Any?>): Boolean =
        preferences.rollbackBackupVisuals(snapshot)

    fun restore(backup: VisualBackup, library: BatteryColorSchemeLibrary): Boolean {
        val editor = preferences.editBackupVisuals()
        backup.profiles.filter { !it.dualSignal || backup.dualInitialized }
            .forEach { editor.putVisualCfg(it) }
        val active = backup.profiles.first { it.dualSignal == backup.dualEnabled }
        editor.putBatteryColors(active)
        editor.putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, active.fillFollowsRetract)
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, active.ccTintTransition)
        val applied = library.matchingKey(active) ?: library.activeSchemeKey
        writeBatterySchemeLibrary(editor, library.copy(activeSchemeKey = applied))
        val activeLayout = if (backup.dualEnabled) backup.dualLayout else backup.singleLayout
        editor.putString(CONTENT_LAYOUT_KEY, activeLayout.persistedValue)
        if (backup.dualInitialized) {
            editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        }
        editor.putBoolean(DUAL_SIGNAL_ENABLED_KEY, backup.dualEnabled)
        editor.putInt(SHARED_CONTENT_STYLE_SCHEMA_KEY, 3)
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

internal fun visualModeKey(dual: Boolean, key: String): String =
    if (dual) "dual.$key" else key

internal fun SharedPreferences.writeSignalDotOffsetY(offset: Float) {
    val visual = readVisualCfg()
    val key = if (visual.contentStyle == ContentStyle.DUO) {
        DUO_SIGNAL_DOT_OFFSET_Y_KEY
    } else {
        DEFAULT_SIGNAL_DOT_OFFSET_Y_KEY
    }
    edit().putFloat(
        visualProfileKey(visual.layout, key, visual.dualSignal),
        offset.finiteIn(
            NETWORK_TOP_OFFSET_MIN,
            NETWORK_TOP_OFFSET_MAX,
            NETWORK_TOP_OFFSET_DEFAULT,
        ),
    ).apply()
}

internal fun SharedPreferences.writeBatteryAvoidanceMode(mode: BatteryAvoidanceMode) {
    edit().putString(
        visualProfileKey(readLayout(), BATTERY_AVOIDANCE_MODE_KEY, dualSignalEnabled()),
        mode.persistedValue,
    ).apply()
}

internal fun SharedPreferences.dualSignalEnabled(): Boolean =
    getBoolean(DUAL_SIGNAL_ENABLED_KEY, false)

internal fun SharedPreferences.readLayout(): ContentLayout =
    ContentLayout.fromPersisted(
        getString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue),
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

private fun SharedPreferences.profileString(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: String?,
    dual: Boolean,
): String? {
    val profileKey = visualProfileKey(layout, baseKey, dual)
    return if (contains(profileKey)) getString(profileKey, defaultValue)
    else getString(baseKey, defaultValue)
}

internal fun SharedPreferences.readVisualCfg(
    dual: Boolean = dualSignalEnabled(),
    layoutOverride: ContentLayout? = null,
): VisualCfg {
    val layout = layoutOverride ?: readLayout()
    fun flag(key: String, default: Boolean) = profileBoolean(layout, key, default, dual)
    fun size(key: String, default: Float) = profileFloat(layout, key, default, dual)
    fun weight(key: String, default: Int) = profileInt(layout, key, default, dual)
    val topOffset = size(BATTERY_TOP_VERTICAL_OFFSET_KEY, topOffsetYDefault(layout))
    // Preserve existing battery-centered network position when split keys are first read.
    val oldNetworkOffset =
        if (layout == ContentLayout.BATTERY_CENTER) topOffsetYUi(topOffset) else 0f

    return VisualCfg(
        layout = layout,
        dualSignal = dual,
        contentStyle = ContentStyle.fromPersisted(
            profileString(layout, CONTENT_STYLE_KEY, null, dual)
                ?: getString(visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY), null),
        ),
        singleRowFlatDots = flag(SINGLE_ROW_FLAT_DOTS_KEY, false),
        batteryAvoidanceMode = BatteryAvoidanceMode.fromPersisted(
            getString(
                visualProfileKey(layout, BATTERY_AVOIDANCE_MODE_KEY, dual),
                // B1727 wrote one global choice; keep that choice only for
                // signal-centered layout rather than overriding other layouts.
                if (layout == ContentLayout.MOBILE_CENTER) {
                    getString(BATTERY_AVOIDANCE_MODE_KEY, null)
                } else null,
            ),
            batteryAvoidanceModeDefault(layout),
        ),
        mobileTypeBottomOffsetY = size(MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY, mobileTypeBottomOffsetDefault(layout)),
        mobileFollowsBatteryColor = flag(MOBILE_FOLLOWS_BATTERY_COLOR_KEY, false),
        centerFollowsBatteryColor = flag(CENTER_FOLLOWS_BATTERY_COLOR_KEY, false),
        showTopReadout = flag(BATTERY_TOP_READOUT_ENABLED_KEY, topReadoutDefault(layout)),
        topTextFollowsBatteryColor = flag(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY, true),
        showTopChargingIcon = flag(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY, true),
        topChargingIconFollowsBatteryColor = flag(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY, true),
        fillFollowsRetract = getBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, true),
        ccTintTransition = getBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, true),
        topTextScale = size(BATTERY_TOP_TEXT_SCALE_KEY, topTextScaleDefault(layout)),
        topTextWeight = weight(BATTERY_TOP_TEXT_WEIGHT_KEY, BATTERY_TOP_TEXT_WEIGHT_DEFAULT),
        topOffsetY = topOffset,
        wifiOffsetY = size(WIFI_VERTICAL_OFFSET_KEY, oldNetworkOffset),
        mobileTypeOffsetY = size(MOBILE_TYPE_VERTICAL_OFFSET_KEY, oldNetworkOffset),
        defaultSignalDotOffsetY = size(DEFAULT_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        duoSignalDotOffsetY = size(DUO_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
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
        visualProfileKey(layout, CONTENT_STYLE_KEY, normalized.dualSignal),
        normalized.contentStyle.persistedValue,
    ).putString(
        visualProfileKey(layout, BATTERY_AVOIDANCE_MODE_KEY, normalized.dualSignal),
        normalized.batteryAvoidanceMode.persistedValue,
    ).putBoolean(
        visualProfileKey(layout, SINGLE_ROW_FLAT_DOTS_KEY, normalized.dualSignal),
        normalized.singleRowFlatDots,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_BOTTOM_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.mobileTypeBottomOffsetY,
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
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        normalized.fillFollowsRetract,
    ).putBoolean(
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
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
        visualProfileKey(layout, WIFI_VERTICAL_OFFSET_KEY, normalized.dualSignal),
        normalized.wifiOffsetY,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_VERTICAL_OFFSET_KEY, normalized.dualSignal),
        normalized.mobileTypeOffsetY,
    ).putFloat(
        visualProfileKey(layout, DEFAULT_SIGNAL_DOT_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.defaultSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, DUO_SIGNAL_DOT_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.duoSignalDotOffsetY,
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
    ).putInt(
        visualProfileKey(layout, WIFI_WEIGHT_KEY, normalized.dualSignal),
        normalized.wifiWeight,
    ).putFloat(
        visualProfileKey(layout, AIRPLANE_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.airplaneScale,
    ).putInt(
        visualProfileKey(layout, AIRPLANE_WEIGHT_KEY, normalized.dualSignal),
        normalized.airplaneWeight,
    ).putFloat(
        visualProfileKey(layout, NO_SIM_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.noSimScale,
    ).putFloat(
        visualProfileKey(layout, MOBILE_TYPE_SIZE_SCALE_KEY, normalized.dualSignal),
        normalized.mobileTypeScale,
    ).putInt(
        visualProfileKey(layout, MOBILE_TYPE_WEIGHT_KEY, normalized.dualSignal),
        normalized.mobileTypeWeight,
    ).putBatteryColors(normalized)
    return if (normalized.dualSignal) {
        editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
    } else {
        editor
    }
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

internal fun SharedPreferences.Editor.putBatteryColors(settings: VisualCfg): SharedPreferences.Editor =
    putString(BATTERY_COLOR_PRESET_KEY, settings.batteryColorPreset.persistedValue)
        .applyBatteryColorModes(settings.batteryColorModes)
        .applyBatteryColorOverrides(settings.batteryColorOverrides)

private fun SharedPreferences.Editor.applyBatteryColorOverrides(
    overrides: BatteryColorOverrides,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        val key = batteryColorOverrideKey(slot)
        val color = overrides.colorFor(slot)
        if (color == null) remove(key)
        else putInt(key, color or 0xFF000000.toInt())
    }
    return this
}

private fun SharedPreferences.Editor.applyBatteryColorModes(
    modes: BatteryColorModes,
): SharedPreferences.Editor {
    BatteryColorSlot.entries.forEach { slot ->
        putString(batteryColorModeKey(slot), modes.modeFor(slot).persistedValue)
    }
    return this
}
