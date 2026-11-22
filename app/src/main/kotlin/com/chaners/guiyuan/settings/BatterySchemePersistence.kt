package com.chaners.guiyuan.settings

import android.content.SharedPreferences

internal const val BATTERY_COLOR_SCHEME_PREFIX = "battery_color_scheme_library."

internal fun isBatterySchemeStorageKey(key: String): Boolean =
    key.startsWith(BATTERY_COLOR_SCHEME_PREFIX)
private const val BATTERY_COLOR_SCHEME_SCHEMA_KEY = BATTERY_COLOR_SCHEME_PREFIX + "schema"
private const val BATTERY_COLOR_SCHEME_SCHEMA_CURRENT = 1
internal const val BATTERY_COLOR_SCHEME_ACTIVE_KEY = BATTERY_COLOR_SCHEME_PREFIX + "active"
// Legacy test builds stored a dual applied selection; never write it again.
internal const val BATTERY_COLOR_SCHEME_DUAL_ACTIVE_KEY = BATTERY_COLOR_SCHEME_PREFIX + "dual_active"
private const val BATTERY_COLOR_SCHEME_ORDER_KEY = BATTERY_COLOR_SCHEME_PREFIX + "custom_order"

internal object BatterySchemePersistence {
    fun migrateIfNeeded(preferences: SharedPreferences) {
        if (
            preferences.getInt(BATTERY_COLOR_SCHEME_SCHEMA_KEY, 0) >=
                BATTERY_COLOR_SCHEME_SCHEMA_CURRENT
        ) return

        val visual = preferences.readVisualCfg()
        val needsCustom =
            BatteryColorSlot.entries.any { slot ->
                visual.batteryColorModes.modeFor(slot) != BatteryColorMode.PRESET ||
                    visual.batteryColorOverrides.colorFor(slot) != null
            }
        val base = BatteryBuiltInColorScheme.fromPreset(visual.batteryColorPreset)
        val legacyCustom =
            if (needsCustom) {
                BatteryCustomColorScheme(
                    id = 1,
                    name = "",
                    baseTemplate = base,
                    entries = entriesFromLegacyVisual(visual),
                )
            } else {
                null
            }
        val initial =
            BatteryColorSchemeLibrary(
                activeSchemeKey =
                    if (!needsCustom) {
                        base.key
                    } else {
                        requireNotNull(legacyCustom).key
                    },
                customSchemes = listOfNotNull(legacyCustom),
            )

        val editor = preferences.edit()
        writeBatterySchemeLibrary(editor, initial)
        applyProjection(editor, initial, initial.activeSchemeKey)
        editor.apply()
    }

    fun readLibrary(preferences: SharedPreferences): BatteryColorSchemeLibrary {
        val order =
            preferences.getString(BATTERY_COLOR_SCHEME_ORDER_KEY, null)
                ?.split(',')
                ?.mapNotNull(String::toIntOrNull)
                ?.distinct()
                ?.filter { it in 1..BATTERY_COLOR_SCHEME_CUSTOM_MAX }
                .orEmpty()

        val customs = order.mapNotNull { readCustom(preferences, it) }
        val rawActive =
            preferences.getString(
                BATTERY_COLOR_SCHEME_ACTIVE_KEY,
                BATTERY_COLOR_SCHEME_HYPEROS_KEY,
            ) ?: BATTERY_COLOR_SCHEME_HYPEROS_KEY
        val provisional =
            BatteryColorSchemeLibrary(
                activeSchemeKey = rawActive,
                customSchemes = customs,
            )
        return if (provisional.containsKey(rawActive)) {
            provisional
        } else {
            provisional.copy(activeSchemeKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY)
        }
    }

    private fun readCustom(preferences: SharedPreferences, id: Int): BatteryCustomColorScheme? {
        val prefix = customPrefix(id)
        val baseKey = prefix + "base"
        if (!preferences.contains(baseKey)) return null
        val storedBase = preferences.getString(baseKey, null)
        val base =
            BatteryBuiltInColorScheme.entries.firstOrNull { it.key == storedBase }
                ?: BatteryBuiltInColorScheme.HYPEROS
        var entries = BatteryColorSchemeEntries()
        BatteryColorSlot.entries.forEach { slot ->
            val source =
                BatteryColorSchemeSource.fromPersisted(
                    preferences.getString(customSourceKey(id, slot), null),
                )
            val colorKey = customColorKey(id, slot)
            val color =
                if (preferences.contains(colorKey)) {
                    preferences.getInt(colorKey, 0).opaqueArgb()
                } else {
                    batteryBuiltInColor(base, slot)
                }
            entries =
                entries.withEntry(
                    slot,
                    BatteryColorSchemeEntry(source, color),
                )
        }
        return BatteryCustomColorScheme(
            id = id,
            // Old names may contain control characters that backup files reject.
            name = normalizeCustomSchemeName(preferences.getString(prefix + "name", "") ?: ""),
            baseTemplate = base,
            entries = entries,
        )
    }

    fun applyProjection(
        editor: SharedPreferences.Editor,
        library: BatteryColorSchemeLibrary,
        key: String,
    ) {
        val builtIn = BatteryBuiltInColorScheme.fromKey(key)
        if (builtIn != null) {
            editor.putString(BATTERY_COLOR_PRESET_KEY, builtIn.preset.persistedValue)
            BatteryColorSlot.entries.forEach { slot ->
                editor.putString(
                    batteryColorModeKey(slot),
                    BatteryColorMode.PRESET.persistedValue,
                )
                editor.remove(batteryColorOverrideKey(slot))
            }
            return
        }

        val custom = library.customByKey(key) ?: run {
            applyProjection(editor, library, BATTERY_COLOR_SCHEME_HYPEROS_KEY)
            return
        }
        editor.putString(
            BATTERY_COLOR_PRESET_KEY,
            BatteryColorPreset.HYPEROS.persistedValue,
        )
        BatteryColorSlot.entries.forEach { slot ->
            val entry = custom.entries.entryFor(slot)
            val resolvedColor = batterySchemeEntryColor(entry, slot)
            val mode =
                when {
                    entry.source == BatteryColorSchemeSource.FOLLOW_SYSTEM ->
                        BatteryColorMode.FOLLOW_SYSTEM
                    resolvedColor != null ->
                        BatteryColorMode.CUSTOM
                    else ->
                        BatteryColorMode.FOLLOW_SYSTEM
                }
            editor.putString(batteryColorModeKey(slot), mode.persistedValue)
            val colorKey = batteryColorOverrideKey(slot)
            if (mode == BatteryColorMode.CUSTOM && resolvedColor != null) {
                editor.putInt(colorKey, resolvedColor.opaqueArgb())
            } else {
                editor.remove(colorKey)
            }
        }
    }

    private fun entriesFromLegacyVisual(
        visual: VisualCfg,
    ): BatteryColorSchemeEntries {
        val builtIn = BatteryBuiltInColorScheme.fromPreset(visual.batteryColorPreset)
        val preset = BatteryColorSchemeSource.fromBuiltIn(builtIn)
        var entries = BatteryColorSchemeEntries()
        BatteryColorSlot.entries.forEach { slot ->
            val storedCustom = visual.batteryColorOverrides.colorFor(slot)
            val source =
                schemeSourceFromLegacy(
                    mode = visual.batteryColorModes.modeFor(slot),
                    hasStoredCustom = storedCustom != null,
                    presetSource = preset,
                )
            val color =
                storedCustom ?: batteryBuiltInColor(builtIn, slot)
            entries =
                entries.withEntry(
                    slot,
                    BatteryColorSchemeEntry(source, color),
                )
        }
        return entries
    }
}

internal fun writeBatterySchemeLibrary(
    editor: SharedPreferences.Editor,
    library: BatteryColorSchemeLibrary,
) {
    editor
        .putInt(BATTERY_COLOR_SCHEME_SCHEMA_KEY, BATTERY_COLOR_SCHEME_SCHEMA_CURRENT)
        .putString(BATTERY_COLOR_SCHEME_ACTIVE_KEY, library.activeSchemeKey)
        .putString(
            BATTERY_COLOR_SCHEME_ORDER_KEY,
            library.customSchemes.joinToString(",") { it.id.toString() },
        )
    library.customSchemes.forEach { scheme ->
        val prefix = customPrefix(scheme.id)
        editor
            .putString(prefix + "name", scheme.name)
            .putString(prefix + "base", scheme.baseTemplate.key)
        BatteryColorSlot.entries.forEach { slot ->
            val entry = scheme.entries.entryFor(slot)
            editor.putString(customSourceKey(scheme.id, slot), entry.source.persistedValue)
            val colorKey = customColorKey(scheme.id, slot)
            if (entry.customColor == null) {
                editor.remove(colorKey)
            } else {
                editor.putInt(colorKey, entry.customColor.opaqueArgb())
            }
        }
    }
}

internal fun customPrefix(id: Int): String =
    BATTERY_COLOR_SCHEME_PREFIX + "custom." + id + "."

private fun customSourceKey(
    id: Int,
    slot: BatteryColorSlot,
): String =
    customPrefix(id) + "source." + slot.name.lowercase()

private fun customColorKey(
    id: Int,
    slot: BatteryColorSlot,
): String =
    customPrefix(id) + "color." + slot.name.lowercase()

internal fun schemeSourceFromLegacy(
    mode: BatteryColorMode,
    hasStoredCustom: Boolean,
    presetSource: BatteryColorSchemeSource,
): BatteryColorSchemeSource =
    when (mode) {
        BatteryColorMode.PRESET -> presetSource
        BatteryColorMode.FOLLOW_SYSTEM ->
            BatteryColorSchemeSource.FOLLOW_SYSTEM
        BatteryColorMode.CUSTOM ->
            if (hasStoredCustom) {
                BatteryColorSchemeSource.CUSTOM
            } else {
                presetSource
            }
    }
