package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
    fun legacyCustomWithoutStoredValueFallsBackToItsPresetSource() {
        assertEquals(
            BatteryColorSchemeSource.IOS,
            batteryColorSchemeSourceFromLegacy(
                mode = BatteryColorMode.CUSTOM,
                hasStoredCustom = false,
                presetSource = BatteryColorSchemeSource.IOS,
            ),
        )
        assertEquals(
            BatteryColorSchemeSource.CUSTOM,
            batteryColorSchemeSourceFromLegacy(
                mode = BatteryColorMode.CUSTOM,
                hasStoredCustom = true,
                presetSource = BatteryColorSchemeSource.IOS,
            ),
        )
        assertEquals(
            BatteryColorSchemeSource.FOLLOW_SYSTEM,
            batteryColorSchemeSourceFromLegacy(
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
        assertNull(customSchemeId(BATTERY_COLOR_SCHEME_HYPEROS_KEY))
    }
    @Test
    fun customSchemeNameLimitIsSharedAndUnicodeCodePointSafe() {
        assertEquals(
            "123456789012345678901234",
            limitBatteryCustomSchemeNameInput("1234567890123456789012345"),
        )
        val emoji = "\uD83D\uDE80"
        val twentyFourEmoji = emoji.repeat(BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS)
        assertEquals(
            twentyFourEmoji,
            limitBatteryCustomSchemeNameInput(twentyFourEmoji + emoji),
        )
        assertEquals(
            "Custom style",
            normalizeBatteryCustomSchemeName("  Custom style  "),
        )
    }

}
