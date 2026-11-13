package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualCfgPaletteTest {
    @Test
    fun hyperosIsTheDefaultPreset() {
        assertEquals(
            BatteryColorPreset.HYPEROS,
            VisualCfg().batteryColorPreset,
        )
        assertEquals(
            BatteryColorPreset.HYPEROS,
            BatteryColorPreset.fromPersisted(null),
        )
    }

    @Test
    fun legacyStoredCustomColorInfersCustomMode() {
        assertEquals(
            BatteryColorMode.CUSTOM,
            batteryColorModeFromPersisted(
                persistedMode = null,
                hasStoredColor = true,
            ),
        )
        assertEquals(
            BatteryColorMode.PRESET,
            batteryColorModeFromPersisted(
                persistedMode = null,
                hasStoredColor = false,
            ),
        )
        assertEquals(
            BatteryColorMode.FOLLOW_SYSTEM,
            batteryColorModeFromPersisted(
                persistedMode = "follow_system",
                hasStoredColor = true,
            ),
        )
    }

    @Test
    fun recommendedPaletteUsesMutedSemanticDefaults() {
        assertEquals(0xFF3FA760.toInt(), RecommendedBatteryPalette.CHARGING)
        assertEquals(0xFFD5A623.toInt(), RecommendedBatteryPalette.POWER_SAVE)
        assertEquals(0xFF4A7FC1.toInt(), RecommendedBatteryPalette.PERFORMANCE)
        assertEquals(0xFFD8752C.toInt(), RecommendedBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFD64A4A.toInt(), RecommendedBatteryPalette.LOW)
        assertEquals(
            null,
            RecommendedBatteryPalette.colorFor(
                BatteryColorSlot.NORMAL,
            ),
        )
    }

    @Test
    fun hyperosPaletteUsesPinnedSystemUiSemanticDefaults() {
        assertEquals(0xFF1DCD3A.toInt(), HyperOsBatteryPalette.CHARGING)
        assertEquals(0xFFFF9F05.toInt(), HyperOsBatteryPalette.POWER_SAVE)
        assertEquals(0xFF3482FF.toInt(), HyperOsBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9F05.toInt(), HyperOsBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFA382E.toInt(), HyperOsBatteryPalette.LOW)
        assertEquals(
            null,
            HyperOsBatteryPalette.colorFor(BatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun iosStylePaletteUsesExpectedSemanticDefaults() {
        assertEquals(0xFF34C759.toInt(), IosStyleBatteryPalette.CHARGING)
        assertEquals(0xFFFFCC00.toInt(), IosStyleBatteryPalette.POWER_SAVE)
        assertEquals(0xFF007AFF.toInt(), IosStyleBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9500.toInt(), IosStyleBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFF3B30.toInt(), IosStyleBatteryPalette.LOW)
        assertEquals(
            null,
            IosStyleBatteryPalette.colorFor(BatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun visualNormalizationMakesEveryStoredOverrideOpaqueWithoutMutatingSource() {
        val source =
            BatteryColorOverrides(
                normal = 0x00112233,
                powerSave = null,
                performance = 0x7F445566,
                superPowerSave = 0xFF778899.toInt(),
                charging = 0x0034C759,
                low = 0,
            )
        val normalized = VisualCfg(batteryColorOverrides = source).normalized()
        val values =
            listOf(
                0xFF112233.toInt(),
                null,
                0xFF445566.toInt(),
                0xFF778899.toInt(),
                0xFF34C759.toInt(),
                0xFF000000.toInt(),
            )

        assertEquals(values, BatteryColorSlot.entries.map(normalized.batteryColorOverrides::colorFor))
        assertEquals(0x00112233, source.normal)
        assertEquals(0x7F445566, source.performance)
        assertEquals(0, source.low)
        assertEquals(normalized, normalized.normalized())
    }

    @Test
    fun customColorOverridesAreForcedOpaque() {
        val overrides =
            BatteryColorOverrides().withColor(
                BatteryColorSlot.CHARGING,
                0x0034C759,
            )
        assertEquals(0xFF34C759.toInt(), overrides.charging)
    }
}
