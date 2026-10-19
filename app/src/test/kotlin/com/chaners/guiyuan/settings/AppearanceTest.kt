package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun appScaleDefaultsTo100PercentAndClampsStoredValues() {
        assertEquals(100, Appearance().uiScalePercent)
        assertEquals(100, uiScalePercent(null))
        assertEquals(80, uiScalePercent(60))
        assertEquals(110, uiScalePercent(125))
        assertEquals(97, uiScalePercent(97))
    }

    @Test
    fun defaultsPreserveExistingNavStyle() {
        val settings = Appearance()

        assertTrue(settings.navEnabled)
        assertEquals(NavStyle.Glass, settings.navStyle)
        assertEquals(NavContent.IconOnly, settings.navContent)
        assertEquals(NavContent.IconAndText, settings.liquidContent)
        assertEquals(NavContent.IconOnly, settings.activeNavContent)
        assertEquals(LiquidMode.Clear, settings.liquidMode)
    }

    @Test
    fun storedFloatingStyleWinsOverLegacyFlags() {
        val result =
            decodeNavStyle(
                storedStyle = "Blur",
                storedFloatingBlurEnabled = false,
                legacyBlurEnabled = false,
                legacyGlassEnabled = false,
            )

        assertEquals(NavStyle.Blur, result)
    }

    @Test
    fun storedLiquidStyleRoundTrips() {
        val result =
            decodeNavStyle(
                storedStyle = "Liquid",
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = null,
                legacyGlassEnabled = null,
            )

        assertEquals(NavStyle.Liquid, result)
    }

    @Test
    fun currentBlurBooleanMigratesToPreviousGlassAppearance() {
        assertEquals(
            NavStyle.Glass,
            decodeNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = true,
                legacyBlurEnabled = null,
                legacyGlassEnabled = null,
            ),
        )
        assertEquals(
            NavStyle.Standard,
            decodeNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = false,
                legacyBlurEnabled = null,
                legacyGlassEnabled = null,
            ),
        )
    }

    @Test
    fun oldSeparateBlurAndGlassFlagsPreserveThreeStyles() {
        assertEquals(
            NavStyle.Blur,
            decodeNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = true,
                legacyGlassEnabled = false,
            ),
        )
        assertEquals(
            NavStyle.Glass,
            decodeNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = true,
                legacyGlassEnabled = true,
            ),
        )
        assertEquals(
            NavStyle.Standard,
            decodeNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = false,
                legacyGlassEnabled = true,
            ),
        )
    }

    @Test
    fun floatingNavigationContentDefaultsToIconsOnly() {
        assertEquals(
            NavContent.IconOnly,
            decodeNavContent(null),
        )
        assertEquals(
            NavContent.IconOnly,
            decodeNavContent("Unknown"),
        )
        assertEquals(
            NavContent.IconAndText,
            decodeNavContent("IconAndText"),
        )
    }

    @Test
    fun liquidAndOtherStylesKeepSeparateContent() {
        val liquid = Appearance(
            navStyle = NavStyle.Liquid,
            navContent = NavContent.IconOnly,
            liquidContent = NavContent.IconAndText,
        )
        assertEquals(NavContent.IconAndText, liquid.activeNavContent)
        assertEquals(NavContent.IconAndText, Appearance(navStyle = NavStyle.Liquid).activeNavContent)
        assertEquals(NavContent.IconOnly, liquid.copy(navStyle = NavStyle.Glass).activeNavContent)

        val changed = liquid.copy(
            liquidContent = NavContent.IconOnly,
            navContent = NavContent.IconAndText,
        )
        assertEquals(NavContent.IconOnly, changed.activeNavContent)
        assertEquals(NavContent.IconAndText, changed.copy(navStyle = NavStyle.Blur).activeNavContent)
        assertEquals(NavContent.IconAndText, changed.copy(navStyle = NavStyle.Standard).activeNavContent)
    }

    @Test
    fun liquidContentDefaultsToIconsAndText() {
        assertEquals(NavContent.IconAndText, decodeNavContent(null, NavContent.IconAndText))
        assertEquals(NavContent.IconOnly, decodeNavContent("IconOnly", NavContent.IconAndText))
        assertEquals(NavContent.IconAndText, decodeNavContent("Unknown", NavContent.IconAndText))
    }

    @Test
    fun liquidModeDefaultsToClearAndRoundTrips() {
        assertEquals(LiquidMode.Clear, decodeLiquidMode(null))
        assertEquals(LiquidMode.Clear, decodeLiquidMode("Unknown"))
        assertEquals(LiquidMode.Blur, decodeLiquidMode("Blur"))
        assertEquals(LiquidMode.Clear, decodeLiquidMode("Clear"))
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
