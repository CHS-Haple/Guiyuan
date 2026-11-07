package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

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
}
