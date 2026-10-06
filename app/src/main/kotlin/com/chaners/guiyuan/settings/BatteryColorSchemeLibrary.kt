package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val BATTERY_COLOR_SCHEME_CUSTOM_MAX = 5
internal const val BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS = 24
internal const val BATTERY_COLOR_SCHEME_HYPEROS_KEY = "builtin:hyperos"
internal const val BATTERY_COLOR_SCHEME_IOS_KEY = "builtin:ios"
internal const val BATTERY_COLOR_SCHEME_LOW_SATURATION_KEY = "builtin:low_saturation"

private const val BATTERY_COLOR_SCHEME_PREFIX = "battery_color_scheme_library."
private const val BATTERY_COLOR_SCHEME_SCHEMA_KEY = BATTERY_COLOR_SCHEME_PREFIX + "schema"
private const val BATTERY_COLOR_SCHEME_SCHEMA_CURRENT = 1
private const val BATTERY_COLOR_SCHEME_ACTIVE_KEY = BATTERY_COLOR_SCHEME_PREFIX + "active"
private const val BATTERY_COLOR_SCHEME_ORDER_KEY = BATTERY_COLOR_SCHEME_PREFIX + "custom_order"

internal enum class BatteryBuiltInColorScheme(
    val key: String,
    val preset: BatteryColorPreset,
) {
    HYPEROS(
        BATTERY_COLOR_SCHEME_HYPEROS_KEY,
        BatteryColorPreset.HYPEROS,
    ),
    IOS(
        BATTERY_COLOR_SCHEME_IOS_KEY,
        BatteryColorPreset.IOS_STYLE,
    ),
    LOW_SATURATION(
        BATTERY_COLOR_SCHEME_LOW_SATURATION_KEY,
        BatteryColorPreset.RECOMMENDED,
    );

    companion object {
        fun fromKey(key: String?): BatteryBuiltInColorScheme? =
            entries.firstOrNull { it.key == key }

        fun fromPreset(preset: BatteryColorPreset): BatteryBuiltInColorScheme =
            when (preset) {
                BatteryColorPreset.HYPEROS -> HYPEROS
                BatteryColorPreset.IOS_STYLE -> IOS
                BatteryColorPreset.RECOMMENDED -> LOW_SATURATION
            }
    }
}

internal enum class BatteryColorSchemeSource(
    val persistedValue: String,
) {
    HYPEROS("hyperos"),
    IOS("ios"),
    LOW_SATURATION("low_saturation"),
    FOLLOW_SYSTEM("follow_system"),
    CUSTOM("custom");

    companion object {
        fun fromPersisted(value: String?): BatteryColorSchemeSource =
            entries.firstOrNull { it.persistedValue == value } ?: HYPEROS

        fun fromBuiltIn(scheme: BatteryBuiltInColorScheme): BatteryColorSchemeSource =
            when (scheme) {
                BatteryBuiltInColorScheme.HYPEROS -> HYPEROS
                BatteryBuiltInColorScheme.IOS -> IOS
                BatteryBuiltInColorScheme.LOW_SATURATION -> LOW_SATURATION
            }
    }
}

internal data class BatteryColorSchemeEntry(
    val source: BatteryColorSchemeSource = BatteryColorSchemeSource.HYPEROS,
    val customColor: Int? = null,
) {
    fun normalized(): BatteryColorSchemeEntry =
        copy(customColor = customColor?.or(0xFF000000.toInt()))
}

internal data class BatteryColorSchemeEntries(
    val normal: BatteryColorSchemeEntry = BatteryColorSchemeEntry(),
    val powerSave: BatteryColorSchemeEntry = BatteryColorSchemeEntry(),
    val performance: BatteryColorSchemeEntry = BatteryColorSchemeEntry(),
    val superPowerSave: BatteryColorSchemeEntry = BatteryColorSchemeEntry(),
    val charging: BatteryColorSchemeEntry = BatteryColorSchemeEntry(),
    val low: BatteryColorSchemeEntry = BatteryColorSchemeEntry(),
) {
    fun entryFor(slot: BatteryColorSlot): BatteryColorSchemeEntry =
        when (slot) {
            BatteryColorSlot.NORMAL -> normal
            BatteryColorSlot.POWER_SAVE -> powerSave
            BatteryColorSlot.PERFORMANCE -> performance
            BatteryColorSlot.SUPER_POWER_SAVE -> superPowerSave
            BatteryColorSlot.CHARGING -> charging
            BatteryColorSlot.LOW -> low
        }

    fun withEntry(
        slot: BatteryColorSlot,
        entry: BatteryColorSchemeEntry,
    ): BatteryColorSchemeEntries =
        when (slot) {
            BatteryColorSlot.NORMAL -> copy(normal = entry.normalized())
            BatteryColorSlot.POWER_SAVE -> copy(powerSave = entry.normalized())
            BatteryColorSlot.PERFORMANCE -> copy(performance = entry.normalized())
            BatteryColorSlot.SUPER_POWER_SAVE ->
                copy(superPowerSave = entry.normalized())
            BatteryColorSlot.CHARGING -> copy(charging = entry.normalized())
            BatteryColorSlot.LOW -> copy(low = entry.normalized())
        }
}

internal data class BatteryCustomColorScheme(
    val id: Int,
    val name: String,
    val baseTemplate: BatteryBuiltInColorScheme,
    val entries: BatteryColorSchemeEntries,
) {
    val key: String
        get() = customSchemeKey(id)
}

internal data class BatteryColorSchemeLibrary(
    val activeSchemeKey: String = BATTERY_COLOR_SCHEME_HYPEROS_KEY,
    val customSchemes: List<BatteryCustomColorScheme> = emptyList(),
) {
    fun customById(id: Int): BatteryCustomColorScheme? =
        customSchemes.firstOrNull { it.id == id }

    fun customByKey(key: String): BatteryCustomColorScheme? =
        customSchemeId(key)?.let(::customById)

    fun containsKey(key: String): Boolean =
        BatteryBuiltInColorScheme.fromKey(key) != null || customByKey(key) != null
}

internal fun batteryBuiltInColor(
    scheme: BatteryBuiltInColorScheme,
    slot: BatteryColorSlot,
): Int? =
    when (scheme) {
        BatteryBuiltInColorScheme.HYPEROS ->
            HyperOsBatteryPalette.colorFor(slot)
        BatteryBuiltInColorScheme.IOS ->
            IosStyleBatteryPalette.colorFor(slot)
        BatteryBuiltInColorScheme.LOW_SATURATION ->
            RecommendedBatteryPalette.colorFor(slot)
    }

internal fun batterySchemeEntryColor(
    entry: BatteryColorSchemeEntry,
    slot: BatteryColorSlot,
): Int? =
    when (entry.source) {
        BatteryColorSchemeSource.HYPEROS ->
            batteryBuiltInColor(BatteryBuiltInColorScheme.HYPEROS, slot)
        BatteryColorSchemeSource.IOS ->
            batteryBuiltInColor(BatteryBuiltInColorScheme.IOS, slot)
        BatteryColorSchemeSource.LOW_SATURATION ->
            batteryBuiltInColor(BatteryBuiltInColorScheme.LOW_SATURATION, slot)
        BatteryColorSchemeSource.FOLLOW_SYSTEM -> null
        BatteryColorSchemeSource.CUSTOM -> entry.customColor
    }

internal class BatteryColorSchemeLibraryRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
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

    fun nextAvailableCustomId(): Int? {
        val used = current().customSchemes.mapTo(mutableSetOf()) { it.id }
        return (1..BATTERY_COLOR_SCHEME_CUSTOM_MAX).firstOrNull { it !in used }
    }

    fun activateScheme(key: String) {
        val library = current()
        val target =
            if (library.containsKey(key)) key else BATTERY_COLOR_SCHEME_HYPEROS_KEY
        val updated = library.copy(activeSchemeKey = target)
        val editor = preferences.edit()
        writeLibrary(editor, updated)
        applyProjection(editor, updated, target)
        editor.apply()
    }

    fun createCustom(
        name: String,
        fromSchemeKey: String,
    ): Int? {
        val library = current()
        if (library.customSchemes.size >= BATTERY_COLOR_SCHEME_CUSTOM_MAX) return null
        val id = nextAvailableCustomId() ?: return null
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
                name = normalizeBatteryCustomSchemeName(name),
                baseTemplate = baseTemplate,
                entries = entries,
            )
        val updated =
            library.copy(
                customSchemes = library.customSchemes + custom,
            )
        val editor = preferences.edit()
        writeLibrary(editor, updated)
        editor.apply()
        return id
    }

    fun renameCustom(
        id: Int,
        name: String,
    ) {
        updateCustom(id) { scheme ->
            scheme.copy(name = normalizeBatteryCustomSchemeName(name))
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
        writeLibrary(editor, updated)
        applyProjection(editor, updated, active)
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
        val updated =
            library.copy(
                customSchemes =
                    library.customSchemes.map { scheme ->
                        if (scheme.id == id) changed else scheme
                    },
            )
        val editor = preferences.edit()
        writeLibrary(editor, updated)
        if (updated.activeSchemeKey == changed.key) {
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
                    if (allPreset) {
                        base.key
                    } else {
                        requireNotNull(legacyCustom).key
                    },
                customSchemes = listOfNotNull(legacyCustom),
            )

        val editor = preferences.edit()
        writeLibrary(editor, initial)
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
            name = preferences.getString(prefix + "name", "") ?: "",
            baseTemplate = base,
            entries = entries,
        )
    }

    private fun writeLibrary(
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
                batteryColorSchemeSourceFromLegacy(
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

internal fun entriesFromBuiltIn(
    scheme: BatteryBuiltInColorScheme,
): BatteryColorSchemeEntries {
    val source = BatteryColorSchemeSource.fromBuiltIn(scheme)
    var entries = BatteryColorSchemeEntries()
    BatteryColorSlot.entries.forEach { slot ->
        entries =
            entries.withEntry(
                slot,
                BatteryColorSchemeEntry(
                    source = source,
                    customColor = batteryBuiltInColor(scheme, slot),
                ),
            )
    }
    return entries
}

internal fun customSchemeKey(id: Int): String =
    "custom:" + id

internal fun customSchemeId(key: String): Int? =
    key.removePrefix("custom:")
        .takeIf { key.startsWith("custom:") }
        ?.toIntOrNull()

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


internal fun limitBatteryCustomSchemeNameInput(value: String): String {
    val codePointCount = value.codePointCount(0, value.length)
    if (codePointCount <= BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS) return value
    val endIndex =
        value.offsetByCodePoints(
            0,
            BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS,
        )
    return value.substring(0, endIndex)
}

internal fun normalizeBatteryCustomSchemeName(value: String): String =
    limitBatteryCustomSchemeNameInput(value.trim())


internal fun batteryColorSchemeSourceFromLegacy(
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
