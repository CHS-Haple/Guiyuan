package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySchemeLibraryTest {
    @Test
    fun builtInsMatchPinnedHyperOs() {
        assertEquals(
            listOf(
                BuiltInBatteryScheme.HYPEROS,
                BuiltInBatteryScheme.IOS,
                BuiltInBatteryScheme.LOW_SATURATION,
            ),
            BuiltInBatteryScheme.entries,
        )
        assertEquals(
            0xFF1DCD3A.toInt(),
            batteryBuiltInColor(
                BuiltInBatteryScheme.HYPEROS,
                BatteryColorSlot.CHARGING,
            ),
        )
        assertEquals(
            0xFFFF9F05.toInt(),
            batteryBuiltInColor(
                BuiltInBatteryScheme.HYPEROS,
                BatteryColorSlot.SUPER_POWER_SAVE,
            ),
        )
        assertNull(
            batteryBuiltInColor(
                BuiltInBatteryScheme.HYPEROS,
                BatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun builtInsKeepTemplateRefs() {
        val entries = entriesFromBuiltIn(BuiltInBatteryScheme.IOS)
        assertEquals(
            BatterySchemeSource.IOS,
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
    fun customEntryUsesFixedColor() {
        assertEquals(
            0xFF2468AC.toInt(),
            batterySchemeEntryColor(
                BatterySchemeEntry(
                    source = BatterySchemeSource.CUSTOM,
                    customColor = 0x002468AC,
                ).normalized(),
                BatteryColorSlot.CHARGING,
            ),
        )
        assertNull(
            batterySchemeEntryColor(
                BatterySchemeEntry(
                    source = BatterySchemeSource.FOLLOW_SYSTEM,
                    customColor = 0xFF2468AC.toInt(),
                ),
                BatteryColorSlot.CHARGING,
            ),
        )
    }

    @Test
    fun legacyCustomFallsBackToPreset() {
        assertEquals(
            BatterySchemeSource.IOS,
            batteryColorSchemeSourceFromLegacy(
                mode = BatteryColorMode.CUSTOM,
                hasStoredCustom = false,
                presetSource = BatterySchemeSource.IOS,
            ),
        )
        assertEquals(
            BatterySchemeSource.CUSTOM,
            batteryColorSchemeSourceFromLegacy(
                mode = BatteryColorMode.CUSTOM,
                hasStoredCustom = true,
                presetSource = BatterySchemeSource.IOS,
            ),
        )
        assertEquals(
            BatterySchemeSource.FOLLOW_SYSTEM,
            batteryColorSchemeSourceFromLegacy(
                mode = BatteryColorMode.FOLLOW_SYSTEM,
                hasStoredCustom = true,
                presetSource = BatterySchemeSource.IOS,
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
    fun schemeNameLimitIsUnicodeSafe() {
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
