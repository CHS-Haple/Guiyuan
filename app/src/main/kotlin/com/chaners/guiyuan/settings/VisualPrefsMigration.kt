package com.chaners.guiyuan.settings

import android.content.SharedPreferences

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
internal fun migrateSignalDotArrangementsIfNeeded(
    prefs: SharedPreferences,
    sharedEditor: SharedPreferences.Editor? = null,
) {
    if (prefs.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) >= 4) return
    val activeDual = prefs.dualSignalEnabled() &&
        prefs.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false)
    val editor = sharedEditor ?: prefs.edit()
    for (layout in ContentLayout.entries) {
        fun oldKey(key: String): String =
            visualModeKey(activeDual, visualProfileKey(layout, key))
        val dualKey = oldKey(DUAL_ROW_DOT_ARRANGEMENT_KEY)
        val legacyDual = visualModeKey(activeDual, LEGACY_DUAL_DOT_STYLE_KEY)
        val dualStyle = prefs.getString(dualKey, null)
            ?.let(SignalDotArrangement::fromPersisted)
            ?: prefs.getString(legacyDual, null)?.let(SignalDotArrangement::fromPersisted)
            ?: SignalDotArrangement.BOTH_SIDES
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
    if (sharedEditor == null) {
        check(editor.commit()) { "Could not commit dot arrangement migration" }
    }
}

// The retired opt-in-show switch cannot be inverted into a new opt-in-hide
// setting: false previously meant default-off, not an explicit hide request.
internal fun retireLegacyIdleActivityPreference(prefs: SharedPreferences) {
    val retired = buildSet {
        add(LEGACY_BOTH_SIDES_SHOW_IDLE_ACTIVITY_KEY)
        for (layout in ContentLayout.entries) {
            val key = visualProfileKey(layout, LEGACY_BOTH_SIDES_SHOW_IDLE_ACTIVITY_KEY)
            add(key)
            add(visualModeKey(false, key))
            add(visualModeKey(true, key))
        }
    }
    val present = retired.filter(prefs::contains)
    if (present.isEmpty()) return
    val editor = prefs.edit()
    present.forEach(editor::remove)
    editor.apply()
}

private fun copyLegacyVisualValue(
    editor: SharedPreferences.Editor,
    canonical: String,
    key: String,
    value: Any,
) {
    when (value) {
        is Boolean -> editor.putBoolean(canonical, value)
        is Float -> editor.putFloat(canonical, value)
        is Int -> editor.putInt(canonical, value)
        is String -> editor.putString(canonical, value)
        else -> error("Unexpected saved visual preference type: $key")
    }
}

internal const val SHARED_LAYOUT_VISUAL_SCHEMA_KEY = "shared_layout_visual_schema"

internal fun migrateSharedLayoutVisualsIfNeeded(
    prefs: SharedPreferences,
    preserveMigratedSignalStyles: Boolean = false,
    sharedEditor: SharedPreferences.Editor? = null,
) {
    if (prefs.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0) >= 1) return
    val activeDual = prefs.dualSignalEnabled() &&
        prefs.getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false)
    val stored = prefs.all
    val editor = sharedEditor ?: prefs.edit()
    for (layout in ContentLayout.entries) {
        for (key in PROFILE_VISUAL_BASE_KEYS) {
            val canonical = visualProfileKey(layout, key)
            val previousDual = visualModeKey(true, canonical)
            // When the dot-style migration is staged in this same editor,
            // neither a missing dual bank nor a stale unscoped key may
            // overwrite its effective active-mode selection.
            val migratedStyle = preserveMigratedSignalStyles &&
                (key == DUAL_ROW_DOT_ARRANGEMENT_KEY || key == SINGLE_ROW_DOT_STYLE_KEY)
            // The retired dual bank wins when active. Otherwise a saved layout
            // value wins over the old unscoped single-mode value. Staged dot
            // styles must never be overwritten before the shared editor commits.
            val legacyValue = when {
                activeDual -> stored[previousDual]
                migratedStyle || stored.containsKey(canonical) -> null
                else -> stored[key]
            }
            if (legacyValue != null) {
                copyLegacyVisualValue(editor, canonical, key, legacyValue)
            } else if (activeDual && !migratedStyle) {
                // A missing active dual value meant the layout default, not a
                // stale single-mode value. Preserve staged dot styles above.
                editor.remove(canonical)
            }
            editor.remove(previousDual)
        }
    }
    PROFILE_VISUAL_BASE_KEYS.forEach { editor.remove(it) }
    editor.putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
        .putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
    if (sharedEditor == null) {
        check(editor.commit()) { "Could not commit shared layout migration" }
    }
}

// Migrations must finish before the app mirrors its first runtime snapshot.
internal fun migrateVisualPrefsIfNeeded(preferences: SharedPreferences) {
    val migratingDotStyles = preferences.getInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 0) < 4
    val migratingSharedLayout = preferences.getInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 0) < 1
    if (migratingDotStyles && migratingSharedLayout) {
        // Both steps must become durable together: a restart between writes could
        // discard a newly migrated dot style while retiring the old SIM bank.
        val editor = preferences.edit()
        migrateSignalDotArrangementsIfNeeded(preferences, editor)
        migrateSharedLayoutVisualsIfNeeded(preferences, preserveMigratedSignalStyles = true, sharedEditor = editor)
        check(editor.commit()) { "Could not commit visual preference migration" }
    } else {
        migrateSignalDotArrangementsIfNeeded(preferences)
        migrateSharedLayoutVisualsIfNeeded(preferences)
    }
}

// Reset visual choices without deleting user-created battery schemes.
internal fun resetVisualPreferences(
    preferences: SharedPreferences,
    schemes: BatteryColorSchemeLibrary,
) {
    val editor = preferences.edit().clear()
    // A reset creates a current-schema store. Without these markers, the
    // next startup may misinterpret newly saved shared values as legacy
    // single-bank preferences and discard them when dual mode is enabled.
    editor.putInt(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY, 4)
        .putInt(SHARED_LAYOUT_VISUAL_SCHEMA_KEY, 1)
        .putBoolean(DUAL_PROFILES_INITIALIZED_KEY, true)
    writeBatterySchemeLibrary(
        editor,
        schemes.copy(activeSchemeKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY),
    )
    editor.apply()
}
