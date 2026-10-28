package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
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
    fun freshInstallUsesOs4GlassWithCaptions() {
        val settings = Appearance()

        assertTrue(settings.navEnabled)
        assertTrue(settings.navTouchExtension)
        assertEquals(UiStyle.OS4, settings.uiStyle)
        assertEquals(listOf(UiStyle.OS4, UiStyle.OS3), uiStyleMenuOrder)
        assertEquals(NavStyle.Glass, settings.navStyle)
        assertEquals(NavContent.IconAndText, settings.navContent)
        assertEquals(NavContent.IconAndText, settings.liquidContent)
        assertEquals(NavContent.IconAndText, settings.activeNavContent)
        assertEquals(LiquidMode.Balanced, settings.liquidMode)
    }

    @Test
    fun floatingTouchExtensionCanBeDisabledWithoutChangingNavigation() {
        val current = Appearance(navStyle = NavStyle.Liquid, navTouchExtension = false)
        assertFalse(current.navTouchExtension)
        assertEquals(NavStyle.Liquid, current.navStyle)
        assertTrue(current.copy(navTouchExtension = true).navTouchExtension)
    }

    @Test
    fun os4MigrationUpdatesExistingUserWithoutResettingNavigation() {
        val uiStyle = stringPreferencesKey("ui_style")
        val navStyle = stringPreferencesKey("floating_navigation_style")
        val navContent = stringPreferencesKey("floating_navigation_content")
        val navEnabled = booleanPreferencesKey("floating_navigation_bar_enabled")
        val prefs = mutablePreferencesOf(
            uiStyle to UiStyle.OS3.name,
            navStyle to NavStyle.Liquid.name,
            navContent to NavContent.IconOnly.name,
            navEnabled to false,
        )

        migrateOs4Appearance(prefs)

        assertEquals(UiStyle.OS4.name, prefs[uiStyle])
        assertEquals(NavStyle.Liquid.name, prefs[navStyle])
        assertEquals(NavContent.IconOnly.name, prefs[navContent])
        assertEquals(false, prefs[navEnabled])

        prefs[uiStyle] = UiStyle.OS3.name
        migrateOs4Appearance(prefs)
        assertEquals(UiStyle.OS3.name, prefs[uiStyle])
    }

    @Test
    fun cleanInstallGetsOs4AndIconText() {
        val prefs = mutablePreferencesOf()
        migrateOs4Appearance(prefs)
        assertEquals(UiStyle.OS4.name, prefs[stringPreferencesKey("ui_style")])
        assertEquals(
            NavContent.IconAndText.name,
            prefs[stringPreferencesKey("floating_navigation_content")],
        )
    }

    @Test
    fun missingLegacyNavigationContentStillDefaultsToIconOnly() {
        assertEquals(NavContent.IconOnly, decodeNavContent(null))
        assertEquals(NavContent.IconAndText, decodeNavContent(NavContent.IconAndText.name))
    }

    @Test
    fun os4SelectionDoesNotRewriteOs3NavigationPreferences() {
        val old = Appearance(uiStyle = UiStyle.OS3, navStyle = NavStyle.Liquid, liquidMode = LiquidMode.Clear)
        val experimental = old.copy(uiStyle = UiStyle.OS4)
        assertEquals(NavStyle.Liquid, experimental.navStyle)
        assertEquals(NavContent.IconAndText, experimental.activeNavContent)
        assertEquals(LiquidMode.Clear, experimental.liquidMode)
        assertEquals(old, experimental.copy(uiStyle = UiStyle.OS3))
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
    fun balancedLiquidModeReadsLegacyValuesAndRoundTrips() {
        assertEquals(listOf(LiquidMode.Blur, LiquidMode.Balanced, LiquidMode.Clear), LiquidMode.entries)
        assertEquals(LiquidMode.Balanced, Appearance().liquidMode)
        assertEquals(LiquidMode.Balanced, decodeLiquidMode(null))
        assertEquals(LiquidMode.Balanced, decodeLiquidMode("Unknown"))
        assertEquals(LiquidMode.Balanced, decodeLiquidMode("Blur"))
        assertEquals(LiquidMode.Blur, decodeLiquidMode("Blur8"))
        assertEquals(LiquidMode.Balanced, decodeLiquidMode("Default"))
        assertEquals(LiquidMode.Balanced, decodeLiquidMode("Balanced"))
        assertEquals(LiquidMode.Clear, decodeLiquidMode("Clear"))
        assertEquals("Blur8", LiquidMode.Blur.stored)
        assertEquals("Balanced", LiquidMode.Balanced.stored)
        assertEquals("Clear", LiquidMode.Clear.stored)
        LiquidMode.entries.forEach { mode ->
            assertEquals(mode, decodeLiquidMode(mode.stored))
        }
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
