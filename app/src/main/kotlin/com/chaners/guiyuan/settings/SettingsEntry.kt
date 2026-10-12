package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val SETTINGS_ENTRY_PREFS = "settings_entry"
internal const val SETTINGS_ENTRY_KEY = "settings_entry_position"
internal const val SETTINGS_ENTRY_ICON_KEY = "settings_entry_icon"

internal enum class SettingsEntry {
    Disabled, Top, Middle, Bottom;

    companion object {
        fun from(value: Int): SettingsEntry = entries.getOrNull(value) ?: Disabled
    }
}

internal enum class SettingsEntryIcon {
    Rounded, Hollow;

    companion object {
        fun from(value: Int): SettingsEntryIcon = entries.getOrNull(value) ?: Rounded
    }
}

internal data class SettingsEntryCfg(
    val entry: SettingsEntry = SettingsEntry.Disabled,
    val icon: SettingsEntryIcon = SettingsEntryIcon.Rounded,
)

internal object SettingsEntryPrefs {
    fun observe(ctx: Context): Flow<SettingsEntryCfg> = callbackFlow {
        val prefs = ctx.applicationContext.getSharedPreferences(
            SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE,
        )
        fun emitCurrent() {
            trySend(
                SettingsEntryCfg(
                    entry = SettingsEntry.from(prefs.getInt(SETTINGS_ENTRY_KEY, 0)),
                    icon = SettingsEntryIcon.from(prefs.getInt(SETTINGS_ENTRY_ICON_KEY, 0)),
                ),
            )
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == SETTINGS_ENTRY_KEY || key == SETTINGS_ENTRY_ICON_KEY) {
                emitCurrent()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        emitCurrent()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun get(ctx: Context): SettingsEntry =
        SettingsEntry.from(
            ctx.getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
                .getInt(SETTINGS_ENTRY_KEY, 0),
        )

    fun icon(ctx: Context): SettingsEntryIcon =
        SettingsEntryIcon.from(
            ctx.getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
                .getInt(SETTINGS_ENTRY_ICON_KEY, 0),
        )

    fun setIcon(ctx: Context, style: SettingsEntryIcon): Boolean =
        ctx.getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(SETTINGS_ENTRY_ICON_KEY, style.ordinal).commit()

    fun set(ctx: Context, entry: SettingsEntry): Boolean =
        ctx.getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(SETTINGS_ENTRY_KEY, entry.ordinal).commit()
}
