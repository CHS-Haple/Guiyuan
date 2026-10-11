package com.chaners.guiyuan.settings

import android.content.Context

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

internal object SettingsEntryPrefs {
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
