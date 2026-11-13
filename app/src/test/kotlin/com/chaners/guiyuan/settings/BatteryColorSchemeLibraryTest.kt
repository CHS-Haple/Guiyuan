package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val TEST_SCHEME_NAME = "Alpha Beta"

class BatteryColorSchemeLibraryTest {
    @Test
    fun builtInOrderAndHyperosValuesMatchPinnedTarget() {
        assertEquals(
            listOf(
                BatteryBuiltInColorScheme.HYPEROS,
                BatteryBuiltInColorScheme.IOS,
                BatteryBuiltInColorScheme.LOW_SATURATION,
            ),
            BatteryBuiltInColorScheme.entries,
        )
        assertEquals(
            0xFF1DCD3A.toInt(),
            batteryBuiltInColor(
                BatteryBuiltInColorScheme.HYPEROS,
                BatteryColorSlot.CHARGING,
            ),
        )
        assertEquals(
            0xFFFF9F05.toInt(),
            batteryBuiltInColor(
                BatteryBuiltInColorScheme.HYPEROS,
                BatteryColorSlot.SUPER_POWER_SAVE,
            ),
        )
        assertNull(
            batteryBuiltInColor(
                BatteryBuiltInColorScheme.HYPEROS,
                BatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun builtInEntriesMatchIndependentSlotUpdatesForEveryTemplate() {
        for (scheme in BatteryBuiltInColorScheme.entries) {
            val source = BatteryColorSchemeSource.fromBuiltIn(scheme)
            val oldResult = BatteryColorSlot.entries.fold(BatteryColorSchemeEntries()) { entries, slot ->
                entries.withEntry(slot, BatteryColorSchemeEntry(source, batteryBuiltInColor(scheme, slot)))
            }
            val current = entriesFromBuiltIn(scheme)
            assertEquals(oldResult, current)
            for (slot in BatteryColorSlot.entries) {
                assertEquals(source, current.entryFor(slot).source)
                assertEquals(batteryBuiltInColor(scheme, slot), current.entryFor(slot).customColor)
            }
        }
    }

    @Test
    fun builtInEntriesKeepTemplateReferencesInsteadOfFlatteningColors() {
        val entries = entriesFromBuiltIn(BatteryBuiltInColorScheme.IOS)
        assertEquals(
            BatteryColorSchemeSource.IOS,
            entries.charging.source,
        )
        assertEquals(
            0xFF34C759.toInt(),
            batterySchemeEntryColor(
                entries.charging,
                BatteryColorSlot.CHARGING,
            ),
        )
    }

    @Test
    fun customEntryUsesFixedColorWhileFollowSystemHasNoFixedColor() {
        assertEquals(
            0xFF2468AC.toInt(),
            batterySchemeEntryColor(
                BatteryColorSchemeEntry(
                    source = BatteryColorSchemeSource.CUSTOM,
                    customColor = 0x002468AC,
                ).normalized(),
                BatteryColorSlot.CHARGING,
            ),
        )
        assertNull(
            batterySchemeEntryColor(
                BatteryColorSchemeEntry(
                    source = BatteryColorSchemeSource.FOLLOW_SYSTEM,
                    customColor = 0xFF2468AC.toInt(),
                ),
                BatteryColorSlot.CHARGING,
            ),
        )
    }

    @Test
    fun customSchemeReadUsesTemplateColorOnlyWhenStoredColorIsAbsent() {
        val preferences = VisualTestPreferences()
        val repo = BatterySchemeRepo(preferences)
        val id = repo.createCustom("My scheme", BATTERY_COLOR_SCHEME_IOS_KEY)
            ?: error("expected a free custom scheme ID")
        repo.setCustomColor(id, BatteryColorSlot.CHARGING, 0x002468AC)

        // Missing per-slot color must fall back to the selected built-in base
        // without overriding the independently stored source selection.
        preferences.edit()
            .remove("battery_color_scheme_library.custom.$id.color.power_save")
            .apply()
        val loaded = requireNotNull(BatterySchemeRepo(preferences).current().customById(id))

        assertEquals(BatteryBuiltInColorScheme.IOS, loaded.baseTemplate)
        assertEquals(
            batteryBuiltInColor(BatteryBuiltInColorScheme.IOS, BatteryColorSlot.POWER_SAVE),
            loaded.entries.powerSave.customColor,
        )
        assertEquals(BatteryColorSchemeSource.IOS, loaded.entries.powerSave.source)
        assertEquals(0xFF2468AC.toInt(), loaded.entries.charging.customColor)
        assertEquals(BatteryColorSchemeSource.CUSTOM, loaded.entries.charging.source)
        assertNull(loaded.entries.normal.customColor)
    }

    @Test
    fun legacyMigrationMatchesIndependentSixSlotAlgorithmForAllPresets() {
        for (preset in BatteryColorPreset.entries) {
            val preferences = VisualTestPreferences()
            val legacy = VisualCfg(
                batteryColorPreset = preset,
                batteryColorModes = BatteryColorModes(
                    normal = BatteryColorMode.FOLLOW_SYSTEM,
                    performance = BatteryColorMode.CUSTOM,
                    superPowerSave = BatteryColorMode.FOLLOW_SYSTEM,
                    charging = BatteryColorMode.CUSTOM,
                ),
                batteryColorOverrides = BatteryColorOverrides(
                    performance = 0x00556677,
                    charging = 0x7F123456,
                ),
            )
            preferences.edit().putBatteryColors(legacy).apply()
            val stored = preferences.readVisualCfg()
            val template = BatteryBuiltInColorScheme.fromPreset(stored.batteryColorPreset)
            val source = BatteryColorSchemeSource.fromBuiltIn(template)
            val historical = BatteryColorSlot.entries.fold(BatteryColorSchemeEntries()) { entries, slot ->
                val custom = stored.batteryColorOverrides.colorFor(slot)
                entries.withEntry(
                    slot,
                    BatteryColorSchemeEntry(
                        schemeSourceFromLegacy(stored.batteryColorModes.modeFor(slot), custom != null, source),
                        custom ?: batteryBuiltInColor(template, slot),
                    ),
                )
            }

            val migrated = BatterySchemeRepo(preferences).current()
            assertEquals(customSchemeKey(1), migrated.activeSchemeKey)
            assertEquals(historical, requireNotNull(migrated.customById(1)).entries)
            assertEquals(migrated, BatterySchemeRepo(preferences).current())
        }
    }

    @Test
    fun pureLegacyPresetsDoNotCreateSpuriousCustomSchemes() {
        for (base in BatteryBuiltInColorScheme.entries) {
            val preferences = VisualTestPreferences()
            preferences.edit().putBatteryColors(VisualCfg(batteryColorPreset = base.preset)).apply()

            val migrated = BatterySchemeRepo(preferences).current()
            assertEquals(base.key, migrated.activeSchemeKey)
            assertEquals(0, migrated.customSchemes.size)
        }
    }

    @Test
    fun legacyCustomWithoutStoredValueFallsBackToItsPresetSource() {
        assertEquals(
            BatteryColorSchemeSource.IOS,
            schemeSourceFromLegacy(
                mode = BatteryColorMode.CUSTOM,
                hasStoredCustom = false,
                presetSource = BatteryColorSchemeSource.IOS,
            ),
        )
        assertEquals(
            BatteryColorSchemeSource.CUSTOM,
            schemeSourceFromLegacy(
                mode = BatteryColorMode.CUSTOM,
                hasStoredCustom = true,
                presetSource = BatteryColorSchemeSource.IOS,
            ),
        )
        assertEquals(
            BatteryColorSchemeSource.FOLLOW_SYSTEM,
            schemeSourceFromLegacy(
                mode = BatteryColorMode.FOLLOW_SYSTEM,
                hasStoredCustom = true,
                presetSource = BatteryColorSchemeSource.IOS,
            ),
        )
    }

    @Test
    fun customKeysRoundTripAndRejectBuiltIns() {
        assertEquals("custom:3", customSchemeKey(3))
        assertEquals(3, customSchemeId("custom:3"))
        assertEquals(3, customSchemeId("custom:+3"))
        assertNull(customSchemeId("custom:"))
        assertNull(customSchemeId("custom:3-extra"))
        assertNull(customSchemeId("Custom:3"))
        assertNull(customSchemeId(BATTERY_COLOR_SCHEME_HYPEROS_KEY))
    }
    @Test
    fun customSchemeNamesNormalizePastedControlAndBidiCharacters() {
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\nBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u2028Beta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u202EBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u200FBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u061CBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u2066Beta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u206FBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\uD800Beta"))
        assertEquals("Rocket \uD83D\uDE80", normalizeCustomSchemeName("Rocket \uD83D\uDE80"))
    }

    @Test
    fun customSchemeNameLimitIsSharedAndUnicodeCodePointSafe() {
        assertEquals(
            "123456789012345678901234",
            limitCustomSchemeName("1234567890123456789012345"),
        )
        val emoji = "\uD83D\uDE80"
        val twentyFourEmoji = emoji.repeat(BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS)
        assertEquals(
            twentyFourEmoji,
            limitCustomSchemeName(twentyFourEmoji + emoji),
        )
        assertEquals(
            "Custom style",
            normalizeCustomSchemeName("  Custom style  "),
        )
    }

}
