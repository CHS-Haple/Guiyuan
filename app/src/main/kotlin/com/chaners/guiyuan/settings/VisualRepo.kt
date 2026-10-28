package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val SHARED_TRANSITIONS_SCHEMA_KEY = "shared_transitions_schema"
internal const val SHARED_BATTERY_COLORS_SCHEMA_KEY = "shared_battery_colors_schema"

private val visualPreferenceKeys = buildSet {
    add(SHARED_TRANSITIONS_SCHEMA_KEY)
    add(SHARED_BATTERY_COLORS_SCHEMA_KEY)
    add(CONTENT_LAYOUT_KEY)
    add(DUAL_SIGNAL_ENABLED_KEY)
    add(DUAL_PROFILES_INITIALIZED_KEY)
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

// A backup owns feature visuals and scheme entries, not the whole preference file.
internal fun isBackupVisualKey(key: String): Boolean =
    key in visualPreferenceKeys ||
        key == BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY ||
        isBatterySchemeStorageKey(key)

// Rewrite only known old visual keys; remote readers also clamp on read.
internal fun migrateTextWeightRanges(preferences: SharedPreferences) {
    val ranges = mapOf(
        MOBILE_TYPE_WEIGHT_KEY to (MOBILE_TYPE_WEIGHT_MIN..MOBILE_TYPE_WEIGHT_MAX),
        BATTERY_TOP_TEXT_WEIGHT_KEY to (BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX),
    )
    val stored = preferences.all
    val changes = ranges.flatMap { (baseKey, range) ->
        val keys = buildList {
            add(baseKey)
            ContentLayout.entries.forEach { layout ->
                add(visualProfileKey(layout, baseKey))
                add(visualProfileKey(layout, baseKey, dual = true))
            }
        }
        keys.mapNotNull { key ->
            val previous = stored[key] as? Int ?: return@mapNotNull null
            val current = previous.coerceIn(range.first, range.last)
            if (previous != current) key to current else null
        }
    }
    if (changes.isEmpty()) return
    preferences.edit().apply {
        changes.forEach { (key, weight) -> putInt(key, weight) }
    }.apply()
}

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

// Retire the old dual-only layout value. The active layout wins at migration.
internal fun migrateSharedLayoutIfNeeded(prefs: SharedPreferences) {
    val legacyKey = visualModeKey(true, CONTENT_LAYOUT_KEY)
    if (!prefs.contains(legacyKey)) return
    val editor = prefs.edit()
    if (prefs.dualSignalEnabled()) {
        editor.putString(
            CONTENT_LAYOUT_KEY,
            ContentLayout.fromPersisted(prefs.getString(legacyKey, null)).persistedValue,
        )
    }
    editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        .remove(legacyKey)
        .apply()
}


// Show/transition settings use one value for every layout and SIM mode.
// Keep the current mode on upgrade, then retire the old dual bank.
internal fun migrateSharedTransitionsIfNeeded(prefs: SharedPreferences) {
    if (prefs.getInt(SHARED_TRANSITIONS_SCHEMA_KEY, 0) >= 1) return
    val dual = prefs.dualSignalEnabled()
    val editor = prefs.edit()
    for (key in listOf(
        BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY,
        CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY,
    )) {
        val current = visualModeKey(dual, key)
        val value = when {
            prefs.contains(current) -> prefs.getBoolean(current, true)
            prefs.contains(key) -> prefs.getBoolean(key, true)
            else -> true
        }
        editor.putBoolean(key, value)
            .remove(visualModeKey(true, key))
    }
    editor.putInt(SHARED_TRANSITIONS_SCHEMA_KEY, 1).apply()
}

// The currently displayed SIM mode wins once; colors no longer have a dual bank.
internal fun migrateSharedBatteryColorsIfNeeded(prefs: SharedPreferences) {
    if (prefs.getInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 0) >= 1) return

    val dual = prefs.dualSignalEnabled()
    val stored = prefs.all
    val editor = prefs.edit()
    BATTERY_COLOR_KEYS.forEach { key ->
        val oldDualKey = visualModeKey(true, key)
        if (dual) {
            when (val value = stored[oldDualKey]) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                null -> editor.remove(key)
                else -> error("Unexpected saved battery color value")
            }
        }
        editor.remove(oldDualKey)
    }

    // A prior test build persisted a second applied selection; retire it.
    val selectedDual = prefs.getString(BATTERY_COLOR_SCHEME_DUAL_ACTIVE_KEY, null)
    if (dual && selectedDual != null) {
        editor.putString(BATTERY_COLOR_SCHEME_ACTIVE_KEY, selectedDual)
    } else if (dual) {
        val visual = prefs.readVisualCfg(dual = true)
        val selected =
            if (prefs.contains(BATTERY_COLOR_SCHEME_ACTIVE_KEY)) {
                BatterySchemeRepo(prefs).current().matchingKey(visual)
            } else {
                null
            }
        val builtIn =
            if (BatteryColorSlot.entries.all { slot ->
                    visual.batteryColorModes.modeFor(slot) == BatteryColorMode.PRESET &&
                        visual.batteryColorOverrides.colorFor(slot) == null
                }
            ) BatteryBuiltInColorScheme.fromPreset(visual.batteryColorPreset).key else null
        (selected ?: builtIn)?.let { editor.putString(BATTERY_COLOR_SCHEME_ACTIVE_KEY, it) }
    }
    editor.remove(BATTERY_COLOR_SCHEME_DUAL_ACTIVE_KEY)
    editor.putInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 1).apply()
}

// Migrations must finish before the app mirrors its first runtime snapshot.
internal fun migrateVisualPrefsIfNeeded(preferences: SharedPreferences) {
    migrateSharedLayoutIfNeeded(preferences)
    migrateSharedTransitionsIfNeeded(preferences)
    migrateSharedBatteryColorsIfNeeded(preferences)
    migrateBatteryColorPresetDefaultIfNeeded(preferences)
    migrateBatteryTopChargingScaleReferenceIfNeeded(preferences)
    migrateTextWeightRanges(preferences)
}

// Reset visual choices without deleting user-created battery schemes.
internal fun resetVisualPreferences(
    preferences: SharedPreferences,
    schemes: BatteryColorSchemeLibrary,
) {
    val editor = preferences.edit().clear()
    editor.putInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 1)
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

    fun setDualDotStyle(style: DualDotStyle) {
        preferences.edit()
            .putString(visualModeKey(true, DUAL_DOT_STYLE_KEY), style.persistedValue)
            .apply()
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

    fun setDualMobileOffsetY(offset: Float) {
        if (!preferences.dualSignalEnabled()) return
        val style = DualDotStyle.fromPersisted(
            preferences.getString(visualModeKey(true, DUAL_DOT_STYLE_KEY), null),
        )
        val key = if (style == DualDotStyle.DUO) DUAL_DUO_OFFSET_Y_KEY else DUAL_DEFAULT_OFFSET_Y_KEY
        preferences.edit()
            .putFloat(
                activeProfileKey(key),
                offset.takeIf(Float::isFinite)
                    ?.coerceIn(NETWORK_TOP_OFFSET_MIN, NETWORK_TOP_OFFSET_MAX)
                    ?: NETWORK_TOP_OFFSET_DEFAULT,
            ).apply()
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
        // Old V1 archives may carry separate colors; the active profile wins globally.
        val active = backup.profiles.first { it.dualSignal == backup.dualEnabled }
        editor.putBatteryColors(active)
        editor.putBoolean(BATTERY_FILL_FOLLOWS_RETRACT_ENDPOINT_KEY, active.fillFollowsRetract)
            .putBoolean(CONTROL_CENTER_TINT_TRANSITION_ENABLED_KEY, active.ccTintTransition)
            .putInt(SHARED_TRANSITIONS_SCHEMA_KEY, 1)
            .putInt(SHARED_BATTERY_COLORS_SCHEMA_KEY, 1)
        val applied = library.matchingKey(active) ?: library.activeSchemeKey
        writeBatterySchemeLibrary(editor, library.copy(activeSchemeKey = applied))
        val activeLayout = if (backup.dualEnabled) backup.dualLayout else backup.singleLayout
        editor.putString(CONTENT_LAYOUT_KEY, activeLayout.persistedValue)
        if (backup.dualInitialized) {
            editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        }
        editor.putBoolean(DUAL_SIGNAL_ENABLED_KEY, backup.dualEnabled)
        // Restored physical charging scales use the current reference.
        editor.putInt(BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY, 2)
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

internal fun SharedPreferences.readVisualCfg(
    dual: Boolean = dualSignalEnabled(),
    layoutOverride: ContentLayout? = null,
): VisualCfg {
    val layout = layoutOverride ?: readLayout()
    fun flag(key: String, default: Boolean) = profileBoolean(layout, key, default, dual)
    fun size(key: String, default: Float) = profileFloat(layout, key, default, dual)
    fun weight(key: String, default: Int) = profileInt(layout, key, default, dual)
    val topOffset = size(BATTERY_TOP_VERTICAL_OFFSET_KEY, BATTERY_TOP_VERTICAL_OFFSET_DEFAULT)
    // Preserve existing battery-centered network position when split keys are first read.
    val oldNetworkOffset =
        if (layout == ContentLayout.BATTERY_CENTER) topOffsetYUi(topOffset) else 0f

    return VisualCfg(
        layout = layout,
        dualSignal = dual,
        dualDotStyle = DualDotStyle.fromPersisted(
            getString(visualModeKey(dual, DUAL_DOT_STYLE_KEY), null),
        ),
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
        dualDefaultOffsetY = size(DUAL_DEFAULT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        dualDuoOffsetY = size(DUAL_DUO_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
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
        visualModeKey(normalized.dualSignal, DUAL_DOT_STYLE_KEY),
        normalized.dualDotStyle.persistedValue,
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
        visualProfileKey(layout, DUAL_DEFAULT_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.dualDefaultOffsetY,
    ).putFloat(
        visualProfileKey(layout, DUAL_DUO_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.dualDuoOffsetY,
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
