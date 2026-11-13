package com.chaners.guiyuan.settings

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
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

@Serializable
internal data class BatteryColorSchemeEntry(
    val source: BatteryColorSchemeSource = BatteryColorSchemeSource.HYPEROS,
    val customColor: Int? = null,
) {
    fun normalized(): BatteryColorSchemeEntry =
        copy(customColor = customColor?.or(0xFF000000.toInt()))
}

@Serializable
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

@Serializable
internal data class BatteryCustomColorScheme(
    val id: Int,
    val name: String,
    val baseTemplate: BatteryBuiltInColorScheme,
    val entries: BatteryColorSchemeEntries,
) {
    val key: String
        get() = customSchemeKey(id)
}

@Serializable
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

// Match persisted pixels only when a known scheme reproduces every color slot.
internal fun BatteryColorSchemeLibrary.matchingKey(visual: VisualCfg): String? {
    val preset = visual.batteryColorPreset
    val slots = BatteryColorSlot.entries
    if (slots.all { slot ->
            visual.batteryColorModes.modeFor(slot) == BatteryColorMode.PRESET &&
                visual.batteryColorOverrides.colorFor(slot) == null
        }
    ) {
        return BatteryBuiltInColorScheme.fromPreset(preset).key
    }
    if (preset != BatteryColorPreset.HYPEROS) return null

    fun matches(scheme: BatteryCustomColorScheme): Boolean = slots.all { slot ->
        val entry = scheme.entries.entryFor(slot)
        val color = batterySchemeEntryColor(entry, slot)
        val mode =
            if (entry.source == BatteryColorSchemeSource.FOLLOW_SYSTEM || color == null) {
                BatteryColorMode.FOLLOW_SYSTEM
            } else {
                BatteryColorMode.CUSTOM
            }
        visual.batteryColorModes.modeFor(slot) == mode &&
            visual.batteryColorOverrides.colorFor(slot) ==
                if (mode == BatteryColorMode.CUSTOM) color else null
    }
    return customByKey(activeSchemeKey)?.takeIf(::matches)?.key
        ?: customSchemes.firstOrNull(::matches)?.key
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
