package com.chaners.guiyuan.settings

import android.content.Context

internal const val SETTINGS_ENTRY_PREFS = "settings_entry"
internal const val SETTINGS_ENTRY_KEY = "settings_entry_position"

internal enum class SettingsEntry {
    Disabled, Top, Middle, Bottom;

    companion object {
        fun from(value: Int): SettingsEntry = entries.getOrNull(value) ?: Disabled
    }
}

internal object SettingsEntryPrefs {
    fun get(ctx: Context): SettingsEntry =
        SettingsEntry.from(
            ctx.getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
                .getInt(SETTINGS_ENTRY_KEY, 0),
        )

    fun set(ctx: Context, entry: SettingsEntry): Boolean =
        ctx.getSharedPreferences(SETTINGS_ENTRY_PREFS, Context.MODE_PRIVATE)
            .edit().putInt(SETTINGS_ENTRY_KEY, entry.ordinal).commit()
}
