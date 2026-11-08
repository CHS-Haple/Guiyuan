package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val BATTERY_COLOR_SCHEME_SCHEMA_KEY = BATTERY_COLOR_SCHEME_PREFIX + "schema"
private const val BATTERY_COLOR_SCHEME_SCHEMA_CURRENT = 1
private const val BATTERY_COLOR_SCHEME_ORDER_KEY = BATTERY_COLOR_SCHEME_PREFIX + "custom_order"

internal class BatterySchemeRepo(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        ),
    )

    init {
        migrateIfNeeded()
    }

    val library: Flow<BatteryColorSchemeLibrary> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == null || key.startsWith(BATTERY_COLOR_SCHEME_PREFIX)) {
                        emitCurrent()
                    }
                }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): BatteryColorSchemeLibrary =
        readLibrary()

    fun nextAvailableCustomId(library: BatteryColorSchemeLibrary = current()): Int? {
        val used = library.customSchemes.mapTo(mutableSetOf()) { it.id }
        return (1..BATTERY_COLOR_SCHEME_CUSTOM_MAX).firstOrNull { it !in used }
    }

    fun activateScheme(key: String) {
        val library = current()
        val target =
            if (library.containsKey(key)) key else BATTERY_COLOR_SCHEME_HYPEROS_KEY
        val updated = library.copy(activeSchemeKey = target)
        val editor = preferences.edit()
        writeBatterySchemeLibrary(editor, updated)
        applyProjection(editor, updated, target)
        editor.apply()
    }

    fun createCustom(
        name: String,
        fromSchemeKey: String,
    ): Int? {
        val library = current()
        if (library.customSchemes.size >= BATTERY_COLOR_SCHEME_CUSTOM_MAX) return null
        val id = nextAvailableCustomId(library) ?: return null
        val sourceBuiltIn =
            BatteryBuiltInColorScheme.fromKey(fromSchemeKey)
        val sourceCustom = library.customByKey(fromSchemeKey)
        val baseTemplate =
            sourceBuiltIn ?: sourceCustom?.baseTemplate ?: BatteryBuiltInColorScheme.HYPEROS
        val entries =
            sourceCustom?.entries ?: entriesFromBuiltIn(baseTemplate)
        val custom =
            BatteryCustomColorScheme(
                id = id,
                name = normalizeCustomSchemeName(name),
                baseTemplate = baseTemplate,
                entries = entries,
            )
        val updated =
            library.copy(
                customSchemes = library.customSchemes + custom,
            )
        val editor = preferences.edit()
        writeBatterySchemeLibrary(editor, updated)
        editor.apply()
        return id
    }

    fun renameCustom(
        id: Int,
        name: String,
    ) {
        updateCustom(id) { scheme ->
            scheme.copy(name = normalizeCustomSchemeName(name))
        }
    }

    fun deleteCustom(id: Int) {
        val library = current()
        val removed = library.customById(id) ?: return
        val remaining = library.customSchemes.filterNot { it.id == id }
        val active =
            if (library.activeSchemeKey == removed.key) {
                removed.baseTemplate.key
            } else {
                library.activeSchemeKey
            }
        val updated =
            library.copy(
                activeSchemeKey = active,
                customSchemes = remaining,
            )
        val editor = preferences.edit()
        preferences.all.keys
            .filter { it.startsWith(customPrefix(id)) }
            .forEach(editor::remove)
        writeBatterySchemeLibrary(editor, updated)
        if (active != library.activeSchemeKey) {
            applyProjection(editor, updated, active)
        }
        editor.apply()
    }

    fun setCustomSource(
        id: Int,
        slot: BatteryColorSlot,
        source: BatteryColorSchemeSource,
    ) {
        updateCustom(id) { scheme ->
            val current = scheme.entries.entryFor(slot)
            scheme.copy(
                entries =
                    scheme.entries.withEntry(
                        slot,
                        current.copy(source = source),
                    ),
            )
        }
    }

    fun setCustomColor(
        id: Int,
        slot: BatteryColorSlot,
        color: Int,
    ) {
        updateCustom(id) { scheme ->
            scheme.copy(
                entries =
                    scheme.entries.withEntry(
                        slot,
                        BatteryColorSchemeEntry(
                            source = BatteryColorSchemeSource.CUSTOM,
                            customColor = color or 0xFF000000.toInt(),
                        ),
                    ),
            )
        }
    }

    fun restoreCustomSlot(
        id: Int,
        slot: BatteryColorSlot,
    ) {
        updateCustom(id) { scheme ->
            val source = BatteryColorSchemeSource.fromBuiltIn(scheme.baseTemplate)
            scheme.copy(
                entries =
                    scheme.entries.withEntry(
                        slot,
                        BatteryColorSchemeEntry(
                            source = source,
                            customColor = batteryBuiltInColor(scheme.baseTemplate, slot),
                        ),
                    ),
            )
        }
    }

    private fun updateCustom(
        id: Int,
        transform: (BatteryCustomColorScheme) -> BatteryCustomColorScheme,
    ) {
        val library = current()
        val current = library.customById(id) ?: return
        val changed = transform(current)
        if (changed == current) return
        val updated =
            library.copy(
                customSchemes =
                    library.customSchemes.map { scheme ->
                        if (scheme.id == id) changed else scheme
                    },
            )
        val editor = preferences.edit()
        writeBatterySchemeLibrary(editor, updated)
        if (changed.entries != current.entries && updated.activeSchemeKey == changed.key) {
            applyProjection(editor, updated, changed.key)
        }
        editor.apply()
    }

    private fun migrateIfNeeded() {
        if (
            preferences.getInt(BATTERY_COLOR_SCHEME_SCHEMA_KEY, 0) >=
                BATTERY_COLOR_SCHEME_SCHEMA_CURRENT
        ) return

        val visual = preferences.readVisualCfg()
        val allPreset =
            BatteryColorSlot.entries.all { slot ->
                visual.batteryColorModes.modeFor(slot) == BatteryColorMode.PRESET
            }
        val hasStoredCustomMemory =
            BatteryColorSlot.entries.any { slot ->
                visual.batteryColorOverrides.colorFor(slot) != null
            }
        val base = BatteryBuiltInColorScheme.fromPreset(visual.batteryColorPreset)
        val legacyCustom =
            if (!allPreset || hasStoredCustomMemory) {
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
                    if (allPreset && !hasStoredCustomMemory) {
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

    private fun readLibrary(): BatteryColorSchemeLibrary {
        val order =
            preferences.getString(BATTERY_COLOR_SCHEME_ORDER_KEY, null)
                ?.split(',')
                ?.mapNotNull(String::toIntOrNull)
                ?.distinct()
                ?.filter { it in 1..BATTERY_COLOR_SCHEME_CUSTOM_MAX }
                .orEmpty()

        val customs = order.mapNotNull(::readCustom)
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

    private fun readCustom(id: Int): BatteryCustomColorScheme? {
        val prefix = customPrefix(id)
        if (!preferences.contains(prefix + "base")) return null
        val base =
            BatteryBuiltInColorScheme.entries.firstOrNull {
                it.key == preferences.getString(prefix + "base", null)
            } ?: BatteryBuiltInColorScheme.HYPEROS
        var entries = entriesFromBuiltIn(base)
        BatteryColorSlot.entries.forEach { slot ->
            val source =
                BatteryColorSchemeSource.fromPersisted(
                    preferences.getString(customSourceKey(id, slot), null),
                )
            val color =
                if (preferences.contains(customColorKey(id, slot))) {
                    preferences.getInt(customColorKey(id, slot), 0)
                        .or(0xFF000000.toInt())
                } else {
                    entries.entryFor(slot).customColor
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

    private fun applyProjection(
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
                editor.putInt(colorKey, resolvedColor.or(0xFF000000.toInt()))
            } else {
                editor.remove(colorKey)
            }
        }
    }

    private fun entriesFromLegacyVisual(
        visual: VisualCfg,
    ): BatteryColorSchemeEntries {
        val preset =
            BatteryColorSchemeSource.fromBuiltIn(
                BatteryBuiltInColorScheme.fromPreset(visual.batteryColorPreset),
            )
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
                storedCustom
                    ?: batteryBuiltInColor(
                        BatteryBuiltInColorScheme.fromPreset(visual.batteryColorPreset),
                        slot,
                    )
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
                editor.putInt(colorKey, entry.customColor.or(0xFF000000.toInt()))
            }
        }
    }
}

private fun customPrefix(id: Int): String =
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
