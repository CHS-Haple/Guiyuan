package com.chaners.guiyuan.settings

import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    @Test
    fun legacyAppearanceKeepsTheUnmodifiedSystemPalette() {
        val old = Appearance(dynamicColor = true)
        assertEquals(null, old.colorSpec)
        assertEquals(ThemePaletteStyle.TonalSpot, old.paletteStyle)
        assertEquals(null, decodeColorSpec(null))
        assertEquals(null, decodeColorSpec("FutureSpec"))
        assertEquals(ThemeColorSpec.Spec2025, decodeColorSpec("Spec2025"))
        assertEquals(ThemePaletteStyle.TonalSpot, decodePalette("UnknownPalette"))
    }

    @Test
    fun paletteChoicesMatchNewMiuixSpecs() {
        assertEquals(
            listOf(9, 5),
            ThemeColorSpec.entries.map { palettesFor(it).size },
        )
        assertEquals(
            listOf(
                ThemePaletteStyle.TonalSpot,
                ThemePaletteStyle.Neutral,
                ThemePaletteStyle.Vibrant,
                ThemePaletteStyle.Expressive,
                ThemePaletteStyle.Cmf,
            ),
            palettesFor(ThemeColorSpec.Spec2025),
        )
        assertFalse(ThemePaletteStyle.Cmf in palettesFor(ThemeColorSpec.Spec2021))
        assertFalse(ThemePaletteStyle.Rainbow in palettesFor(ThemeColorSpec.Spec2025))
    }

    @Test
    fun legacySpec2026KeepsTheCmfPalette() {
        val spec = decodeColorSpec("Spec2026")
        assertEquals(ThemeColorSpec.Spec2025, spec)
        assertEquals(ThemePaletteStyle.Cmf, compatiblePalette(spec, ThemePaletteStyle.Cmf))
    }

    @Test
    fun incompatiblePalettesNormalizeWithoutChangingTheSystemSetting() {
        assertEquals(
            ThemePaletteStyle.Cmf,
            compatiblePalette(ThemeColorSpec.Spec2025, ThemePaletteStyle.Cmf),
        )
        assertEquals(
            ThemePaletteStyle.TonalSpot,
            compatiblePalette(ThemeColorSpec.Spec2021, ThemePaletteStyle.Cmf),
        )
        assertEquals(
            ThemePaletteStyle.TonalSpot,
            compatiblePalette(ThemeColorSpec.Spec2025, ThemePaletteStyle.Rainbow),
        )
        assertEquals(
            ThemePaletteStyle.Cmf,
            compatiblePalette(null, ThemePaletteStyle.Cmf),
        )
    }

    @Test
    fun appScaleDefaultsTo100PercentAndClampsStoredValues() {
        assertEquals(100, Appearance().uiScalePercent)
        assertEquals(100, uiScalePercent(null))
        assertEquals(80, uiScalePercent(60))
        assertEquals(110, uiScalePercent(125))
        assertEquals(97, uiScalePercent(97))
    }

    @Test
    fun legacyDynamicModeMigratesToSystemWithDynamicColor() {
        val result =
            decodeTheme(
                storedMode = "Dynamic",
                storedDynamicColorEnabled = null,
            )

        assertEquals(ThemeMode.System, result.mode)
        assertTrue(result.dynamicColor)
    }

    @Test
    fun explicitDynamicPreferenceOverridesLegacyFallback() {
        val result =
            decodeTheme(
                storedMode = "Dynamic",
                storedDynamicColorEnabled = false,
            )

        assertEquals(ThemeMode.System, result.mode)
        assertFalse(result.dynamicColor)
    }

    @Test
    fun lightModeRemainsIndependentFromDynamicColor() {
        val result =
            decodeTheme(
                storedMode = "Light",
                storedDynamicColorEnabled = true,
            )

        assertEquals(ThemeMode.Light, result.mode)
        assertTrue(result.dynamicColor)
    }

    @Test
    fun unknownStoredModeFallsBackToSystem() {
        val result =
            decodeTheme(
                storedMode = "Unknown",
                storedDynamicColorEnabled = null,
            )

        assertEquals(ThemeMode.System, result.mode)
        assertFalse(result.dynamicColor)
    }
}
