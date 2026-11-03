package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged


private val visualPreferenceKeys = buildSet {
    add(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY)
    add(SHARED_LAYOUT_VISUAL_SCHEMA_KEY)
    add(CONTENT_LAYOUT_KEY)
    add(DUAL_SIGNAL_ENABLED_KEY)
    add(DUAL_PROFILES_INITIALIZED_KEY)
    addAll(PROFILE_VISUAL_BASE_KEYS)
    addAll(GLOBAL_VISUAL_KEYS)
    ContentLayout.entries.forEach { layout ->
        PROFILE_VISUAL_BASE_KEYS.forEach { key ->
            add(visualProfileKey(layout, key))
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

// Convert the retired lower-row switch using its old effective behavior.
// A single-row choice is shared across SIM modes within each content layout.
internal fun migrateSignalDotArrangementsIfNeeded(prefs: SharedPreferences) {
    if (prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) >= 4) return
    val activeDual = prefs.dualSignalEnabled() &&
        prefs.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false)
    val editor = prefs.edit()
    for (layout in ContentLayout.entries) {
        fun oldKey(key: String): String =
            visualModeKey(activeDual, visualProfileKey(layout, key))
        val dualKey = oldKey(DUAL_ROW_DOT_ARRANGEMENT_KEY)
        val legacyDual = visualModeKey(activeDual, LEGACY_DUAL_DOT_STYLE_KEY)
        val dualStyle = SignalDotArrangement.fromPersisted(
            prefs.getString(dualKey, prefs.getString(legacyDual, null)),
        )
        val singleKey = oldKey(SINGLE_ROW_DOT_STYLE_KEY)
        val legacySwitch = oldKey(LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)
        val singleStyle = if (prefs.contains(singleKey)) {
            SignalDotArrangement.fromPersisted(prefs.getString(singleKey, null))
        } else if (prefs.getBoolean(legacySwitch, false) &&
            dualStyle == SignalDotArrangement.HORIZONTAL) {
            SignalDotArrangement.HORIZONTAL
        } else SignalDotArrangement.CURVED
        editor.putString(visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY), dualStyle.persistedValue)
            .putString(visualProfileKey(layout, SINGLE_ROW_DOT_STYLE_KEY), singleStyle.persistedValue)
        listOf(false, true).forEach { dual ->
            editor.remove(visualModeKey(dual, visualProfileKey(layout, LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)))
        }
    }
    listOf(false, true).forEach { dual ->
        editor.remove(visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY))
    }
    editor.remove(DUAL_ROW_DOT_ARRANGEMENT_KEY)
        .remove(LEGACY_HORIZONTAL_LOWER_SINGLE_ROW_KEY)
        .putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        .apply()
}

internal const val SHARED_LAYOUT_VISUAL_SCHEMA_KEY = "shared_layout_visual_schema"

internal fun migrateSharedLayoutVisualsIfNeeded(
    prefs: SharedPreferences,
    preserveMigratedSignalStyles: Boolean = false,
) {
    if (prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0) >= 1) return
    val activeDual = prefs.dualSignalEnabled() &&
        prefs.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false)
    val stored = prefs.all
    val editor = prefs.edit()
    for (layout in ContentLayout.entries) {
        for (key in PROFILE_VISUAL_BASE_KEYS) {
            val canonical = visualProfileKey(layout, key)
            val previousDual = visualModeKey(true, canonical)
            if (activeDual) {
                when (val value = stored[previousDual]) {
                    is Boolean -> editor.putBoolean(canonical, value)
                    is Float -> editor.putFloat(canonical, value)
                    is Int -> editor.putInt(canonical, value)
                    is String -> editor.putString(canonical, value)
                    null -> {
                        // A missing dual-bank value meant the layout default,
                        // not the single-bank value, in the previous version.
                        // The earlier dot-style migration has already selected
                        // the effective active style and must not be undone.
                        val migratedStyle = preserveMigratedSignalStyles &&
                            key in setOf(DUAL_ROW_DOT_ARRANGEMENT_KEY, SINGLE_ROW_DOT_STYLE_KEY)
                        if (!migratedStyle) editor.remove(canonical)
                    }
                    else -> error("Unexpected saved visual preference type: $key")
                }
            } else if (!stored.containsKey(canonical)) {
                // Earlier single-mode installations may still use unscoped
                // preference names. Materialize those values per layout.
                when (val value = stored[key]) {
                    is Boolean -> editor.putBoolean(canonical, value)
                    is Float -> editor.putFloat(canonical, value)
                    is Int -> editor.putInt(canonical, value)
                    is String -> editor.putString(canonical, value)
                    null -> Unit
                    else -> error("Unexpected saved visual preference type: $key")
                }
            }
            editor.remove(previousDual)
        }
    }
    PROFILE_VISUAL_BASE_KEYS.forEach { editor.remove(it) }
    editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        .putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
        .commit()
}

// Migrations must finish before the app mirrors its first runtime snapshot.
internal fun migrateVisualPrefsIfNeeded(preferences: SharedPreferences) {
    val migratingDotStyles = preferences.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) < 4
    migrateSignalDotArrangementsIfNeeded(preferences)
    migrateSharedLayoutVisualsIfNeeded(preferences, preserveMigratedSignalStyles = migratingDotStyles)
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
        preferences.edit()
            .putBoolean(DUAL_SIGNAL_ENABLED_KEY, enabled)
            .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
            .apply()
    }

    fun setDualRowDotArrangement(style: SignalDotArrangement) {
        preferences.edit().putString(activeProfileKey(DUAL_ROW_DOT_ARRANGEMENT_KEY), style.persistedValue).apply()
    }

    fun setSingleRowDotArrangement(style: SignalDotArrangement) {
        preferences.edit().putString(
            activeProfileKey(SINGLE_ROW_DOT_STYLE_KEY),
            style.persistedValue,
        ).apply()
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

internal fun visualModeKey(dual: Boolean, key: String): String =
    if (dual) "dual.$key" else key

internal fun SharedPreferences.writeSignalDotOffsetY(offset: Float) {
    val visual = readVisualCfg()
    val key = if ((if (visual.dualSignal) visual.dualRowDotArrangement else visual.singleRowDotArrangement) == SignalDotArrangement.HORIZONTAL) {
        HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY
    } else {
        CURVED_SIGNAL_DOT_OFFSET_Y_KEY
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

internal fun SharedPreferences.dualSignalEnabled(): Boolean =
    getBoolean(DUAL_SIGNAL_ENABLED_KEY, false)

internal fun SharedPreferences.readLayout(): ContentLayout =
    ContentLayout.fromPersisted(
        getString(CONTENT_LAYOUT_KEY, ContentLayout.NETWORK_CENTER.persistedValue),
    )

internal fun visualProfileKey(
    layout: ContentLayout,
    baseKey: String,
    @Suppress("UNUSED_PARAMETER") dual: Boolean = false,
): String = layout.persistedValue + "." + baseKey

private fun SharedPreferences.profileBoolean(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: Boolean,
    @Suppress("UNUSED_PARAMETER") dual: Boolean,
): Boolean {
    val profileKey = visualProfileKey(layout, baseKey, dual)
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
    @Suppress("UNUSED_PARAMETER") dual: Boolean,
): Float {
    val profileKey = visualProfileKey(layout, baseKey, dual)
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
    @Suppress("UNUSED_PARAMETER") dual: Boolean,
): Int {
    val profileKey = visualProfileKey(layout, baseKey, dual)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else {
        getInt(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileString(
    layout: ContentLayout,
    baseKey: String,
    defaultValue: String?,
    @Suppress("UNUSED_PARAMETER") dual: Boolean,
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

    val dualStyle = SignalDotArrangement.fromPersisted(
        profileString(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY, null, dual)
            ?: getString(visualModeKey(dual, LEGACY_DUAL_DOT_STYLE_KEY), null),
    )
    val singleStyle = profileString(layout, SINGLE_ROW_DOT_STYLE_KEY, null, dual)
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
        curvedSignalDotOffsetY = size(CURVED_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
        horizontalSignalDotOffsetY = size(HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY, NETWORK_TOP_OFFSET_DEFAULT),
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
        visualProfileKey(layout, DUAL_ROW_DOT_ARRANGEMENT_KEY, normalized.dualSignal),
        normalized.dualRowDotArrangement.persistedValue,
    ).putString(
        visualProfileKey(layout, SINGLE_ROW_DOT_STYLE_KEY, normalized.dualSignal),
        normalized.singleRowDotArrangement.persistedValue,
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
        visualProfileKey(layout, CURVED_SIGNAL_DOT_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.curvedSignalDotOffsetY,
    ).putFloat(
        visualProfileKey(layout, HORIZONTAL_SIGNAL_DOT_OFFSET_Y_KEY, normalized.dualSignal),
        normalized.horizontalSignalDotOffsetY,
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
