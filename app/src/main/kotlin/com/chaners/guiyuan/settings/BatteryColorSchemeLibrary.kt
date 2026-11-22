package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

internal const val BATTERY_COLOR_SCHEME_CUSTOM_MAX = 5
internal const val BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS = 24
internal const val BATTERY_COLOR_SCHEME_HYPEROS_KEY = "builtin:hyperos"
internal const val BATTERY_COLOR_SCHEME_IOS_KEY = "builtin:ios"
internal const val BATTERY_COLOR_SCHEME_LOW_SATURATION_KEY = "builtin:low_saturation"

private const val CUSTOM_SCHEME_KEY_PREFIX = "custom:"

internal class BatterySchemeRepo(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        ),
    )

    init {
        BatterySchemePersistence.migrateIfNeeded(preferences)
    }

    val library: Flow<BatteryColorSchemeLibrary> =
        observePreferenceChanges(preferences, { key ->
            key == null || key.startsWith(BATTERY_COLOR_SCHEME_PREFIX)
        }, ::current)

    fun current(): BatteryColorSchemeLibrary =
        BatterySchemePersistence.readLibrary(preferences)

    fun nextAvailableCustomId(library: BatteryColorSchemeLibrary = current()): Int? =
        (1..BATTERY_COLOR_SCHEME_CUSTOM_MAX).firstOrNull { id ->
            library.customSchemes.none { it.id == id }
        }

    fun activateScheme(key: String) {
        val library = current()
        val target =
            if (library.containsKey(key)) key else BATTERY_COLOR_SCHEME_HYPEROS_KEY
        val updated = library.copy(activeSchemeKey = target)
        val editor = preferences.edit()
        writeBatterySchemeLibrary(editor, updated)
        BatterySchemePersistence.applyProjection(editor, updated, target)
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
        val prefix = customPrefix(id)
        preferences.all.keys.forEach { key ->
            if (key.startsWith(prefix)) editor.remove(key)
        }
        writeBatterySchemeLibrary(editor, updated)
        if (active != library.activeSchemeKey) {
            BatterySchemePersistence.applyProjection(editor, updated, active)
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
                            customColor = color.opaqueArgb(),
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
            BatterySchemePersistence.applyProjection(editor, updated, changed.key)
        }
        editor.apply()
    }
}

internal fun entriesFromBuiltIn(
    scheme: BatteryBuiltInColorScheme,
): BatteryColorSchemeEntries {
    val source = BatteryColorSchemeSource.fromBuiltIn(scheme)
    fun entry(slot: BatteryColorSlot) =
        BatteryColorSchemeEntry(source, batteryBuiltInColor(scheme, slot)).normalized()

    return BatteryColorSchemeEntries(
        normal = entry(BatteryColorSlot.NORMAL),
        powerSave = entry(BatteryColorSlot.POWER_SAVE),
        performance = entry(BatteryColorSlot.PERFORMANCE),
        superPowerSave = entry(BatteryColorSlot.SUPER_POWER_SAVE),
        charging = entry(BatteryColorSlot.CHARGING),
        low = entry(BatteryColorSlot.LOW),
    )
}

internal fun customSchemeKey(id: Int): String =
    CUSTOM_SCHEME_KEY_PREFIX + id

internal fun customSchemeId(key: String): Int? =
    if (key.startsWith(CUSTOM_SCHEME_KEY_PREFIX)) {
        key.removePrefix(CUSTOM_SCHEME_KEY_PREFIX).toIntOrNull()
    } else {
        null
    }
