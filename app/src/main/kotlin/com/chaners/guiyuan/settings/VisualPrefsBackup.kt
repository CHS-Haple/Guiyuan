package com.chaners.guiyuan.settings

import android.content.SharedPreferences

private val visualPreferenceKeys = buildSet {
    add(SIGNAL_DOT_ARRANGEMENT_SCHEMA_KEY)
    add(SHARED_LAYOUT_VISUAL_SCHEMA_KEY)
    add(MOBILE_SIGNAL_DOT_OFFSET_SCHEMA_KEY)
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

// Preserve the released backup shape even though the live layouts are shared.
internal fun SharedPreferences.readVisualBackup(): VisualBackup {
    val layout = readLayout()
    return VisualBackup(
        singleLayout = layout,
        dualLayout = layout,
        dualEnabled = dualSignalEnabled(),
        dualInitialized = getBoolean(DUAL_PROFILES_INITIALIZED_KEY, false),
        profiles = listOf(false, true).flatMap { dual ->
            ContentLayout.entries.map { profileLayout ->
                readVisualCfg(dual = dual, layoutOverride = profileLayout)
            }
        },
    )
}

// The caller validates the archive before restoring either preference store.
internal fun SharedPreferences.restoreVisualBackup(
    backup: VisualBackup,
    library: BatteryColorSchemeLibrary,
): Boolean {
    val editor = editBackupVisuals()
    val activeProfiles = backup.profiles.filter { it.dualSignal == backup.dualEnabled }
    val active = activeProfiles.first()
    activeProfiles.forEach { editor.putNormalizedVisualLayout(it.normalized()) }
    val activeLayout = if (backup.dualEnabled) backup.dualLayout else backup.singleLayout
    // Even a legacy uninitialized single bank must become a current shared store.
    editor.putSharedVisuals(active, activeLayout)
    val applied = library.matchingKey(active) ?: library.activeSchemeKey
    writeBatterySchemeLibrary(editor, library.copy(activeSchemeKey = applied))
    return editor.commit()
}
