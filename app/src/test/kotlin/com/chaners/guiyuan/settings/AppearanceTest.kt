package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private const val TEST_UI_STYLE_KEY = "ui_style"
private const val TEST_NAV_STYLE_KEY = "floating_navigation_style"
private const val TEST_NAV_CONTENT_KEY = "floating_navigation_content"
private const val TEST_UNKNOWN_INPUT = "Unknown"

class AppearanceTest {
    @Test
    fun legacyAppearanceKeepsTheUnmodifiedSystemPalette() {
        val old = Appearance(dynamicColor = true)
        assertFalse(old.customPalette)
        assertEquals(ThemeColorSpec.Spec2021, old.colorSpec)
        assertEquals(ThemePaletteStyle.TonalSpot, old.paletteStyle)
        assertEquals(null, decodeColorSpec(null))
        assertEquals(null, decodeColorSpec("FutureSpec"))
        assertEquals(ThemeColorSpec.Spec2025, decodeColorSpec("Spec2025"))
        assertEquals(ThemePaletteStyle.TonalSpot, decodePalette("UnknownPalette"))
    }

    @Test
    fun presetSourceRetainsLegacySystemAndGeneratedModes() {
        assertEquals(PaletteSource.SYSTEM, Appearance().paletteSource)
        assertEquals(PaletteSource.SYSTEM_SEED, Appearance(customPalette = true).paletteSource)
        assertEquals(
            PaletteSource.PRESET,
            Appearance(customPalette = true, usePresetSeed = true).paletteSource,
        )
        assertEquals(
            PaletteSource.SYSTEM,
            Appearance(customPalette = false, usePresetSeed = true).paletteSource,
        )
        assertEquals(PaletteSource.SYSTEM_SEED, paletteSource(true, false))
        assertEquals(PaletteSource.PRESET, paletteSource(true, true))
        assertEquals(PaletteSource.SYSTEM, paletteSource(false, true))
    }

    @Test
    fun commonSeedChoicesAreStableDistinctAndBounded() {
        val expected = listOf(
            PaletteSeedPreset.BLUE,
            PaletteSeedPreset.CYAN,
            PaletteSeedPreset.GREEN,
            PaletteSeedPreset.PURPLE,
            PaletteSeedPreset.ORANGE,
            PaletteSeedPreset.RED,
            PaletteSeedPreset.PINK,
        )
        assertEquals(expected, PaletteSeedPreset.entries)
        assertEquals(expected.size, expected.map { it.argb }.distinct().size)
        assertEquals(PaletteSeedPreset.BLUE, decodePaletteSeedPreset(null))
        assertEquals(PaletteSeedPreset.BLUE, decodePaletteSeedPreset("unrecognized"))
        assertEquals(0xFFFF3B30.toInt(), PaletteSeedPreset.RED.argb)
        assertEquals(0xFFFF80AB.toInt(), PaletteSeedPreset.PINK.argb)
        for (preset in expected) {
            assertEquals(preset, decodePaletteSeedPreset(preset.name))
            assertEquals(0xFF, preset.argb ushr 24)
        }
    }

    @Test
    fun legacySourceIsInferredOnlyWithoutAnExplicitChoice() {
        for (stored in listOf(null, "FutureSpec", "Spec2021", "Spec2025", "Spec2026")) {
            val spec = decodeColorSpec(stored)
            assertEquals(spec != null, decodeCustomPalette(null, spec))
            assertTrue(decodeCustomPalette(true, spec))
            assertFalse(decodeCustomPalette(false, spec))
        }
    }

    @Test
    fun spec2025SupportMatchesPinnedMiuix() {
        assertEquals(
            listOf(
                ThemePaletteStyle.TonalSpot,
                ThemePaletteStyle.Neutral,
                ThemePaletteStyle.Vibrant,
                ThemePaletteStyle.Expressive,
            ),
            ThemePaletteStyle.entries.filter { it.supportsSpec2025() },
        )
        assertFalse(ThemePaletteStyle.Cmf.supportsSpec2025())
    }

    @Test
    fun legacyCmfStyleAndRequestedSpecRoundTripIndependently() {
        assertEquals(ThemeColorSpec.Spec2025, decodeColorSpec("Spec2026"))
        for (style in ThemePaletteStyle.entries) {
            assertEquals(style, decodePalette(style.name))
        }
        for (spec in ThemeColorSpec.entries) {
            assertEquals(spec, decodeColorSpec(spec.name))
        }
    }

}

class AppearanceCompatibilityTest {
    @Test
    fun appScaleDefaultsTo100PercentAndClampsStoredValues() {
        assertEquals(100, Appearance().uiScalePercent)
        assertEquals(100, uiScalePercent(null))
        assertEquals(80, uiScalePercent(60))
        assertEquals(110, uiScalePercent(125))
        assertEquals(97, uiScalePercent(97))
    }

    @Test
    fun freshInstallUsesGlassWithCaptions() {
        val settings = Appearance()

        assertTrue(settings.navEnabled)
        assertTrue(settings.navTouchExtension)
        assertTrue(settings.topBarProgressiveBlur)
        assertEquals(NavStyle.Glass, settings.navStyle)
        assertEquals(NavContent.IconAndText, settings.navContent)
        assertEquals(LiquidMode.Balanced, settings.liquidMode)
    }

    @Test
    fun progressiveTopBarBlurRemainsConfigurable() {
        val disabled = Appearance(topBarProgressiveBlur = false)
        assertFalse(disabled.topBarProgressiveBlur)
        assertTrue(disabled.copy(topBarProgressiveBlur = true).topBarProgressiveBlur)
    }

    @Test
    fun floatingTouchExtensionCanBeDisabledWithoutChangingNavigation() {
        val current = Appearance(navStyle = NavStyle.Liquid, navTouchExtension = false)
        assertFalse(current.navTouchExtension)
        assertEquals(NavStyle.Liquid, current.navStyle)
        assertTrue(current.copy(navTouchExtension = true).navTouchExtension)
    }

    @Test
    fun retiringStylePreservesExistingNavigation() {
        val uiStyle = stringPreferencesKey(TEST_UI_STYLE_KEY)
        val navStyle = stringPreferencesKey(TEST_NAV_STYLE_KEY)
        val navContent = stringPreferencesKey(TEST_NAV_CONTENT_KEY)
        val navEnabled = booleanPreferencesKey("floating_navigation_bar_enabled")
        val prefs = mutablePreferencesOf(
            uiStyle to "OS3",
            navStyle to NavStyle.Liquid.name,
            navContent to NavContent.IconOnly.name,
            navEnabled to false,
            stringPreferencesKey("theme_mode") to ThemeMode.Dark.name,
            booleanPreferencesKey("dynamic_color_enabled") to true,
            intPreferencesKey("ui_scale_percent") to 97,
            booleanPreferencesKey("floating_navigation_touch_extension") to false,
            booleanPreferencesKey("swipe_back_enabled") to false,
        )
        val expected = prefs.asMap().toMap() - uiStyle

        migrateAppearance(prefs)

        assertEquals(expected, prefs.asMap())
        assertEquals(null, prefs[uiStyle])
        assertEquals(NavStyle.Liquid.name, prefs[navStyle])
        assertEquals(NavContent.IconOnly.name, prefs[navContent])
        assertEquals(false, prefs[navEnabled])

        val migrated = prefs.asMap().toMap()
        migrateAppearance(prefs)
        assertEquals(migrated, prefs.asMap())
    }

    @Test
    fun cleanInstallGetsIconText() {
        val prefs = mutablePreferencesOf()
        migrateAppearance(prefs)
        assertEquals(null, prefs[stringPreferencesKey(TEST_UI_STYLE_KEY)])
        assertEquals(
            NavContent.IconAndText.name,
            prefs[stringPreferencesKey(TEST_NAV_CONTENT_KEY)],
        )
    }

}

// Persistence migration rules are independent from fresh-install UI defaults.
class AppearanceMigrationTest {
    @Test
    fun migrationUsesActiveStyleLabelChoiceAndRetiresSeparateKey() {
        val regularKey = stringPreferencesKey(TEST_NAV_CONTENT_KEY)
        val liquidKey = stringPreferencesKey("floating_navigation_liquid_content")
        val navStyle = stringPreferencesKey(TEST_NAV_STYLE_KEY)
        val liquidMode = stringPreferencesKey("floating_navigation_liquid_mode")
        for (style in NavStyle.entries) {
            for (regular in NavContent.entries) {
                val liquid = NavContent.entries.first { it != regular }
                val prefs = mutablePreferencesOf(
                    navStyle to style.name,
                    regularKey to regular.name,
                    liquidKey to liquid.name,
                    liquidMode to LiquidMode.Clear.stored,
                )
                migrateAppearance(prefs)
                val expected = if (style == NavStyle.Liquid) liquid else regular
                assertEquals(expected.name, prefs[regularKey])
                assertEquals(null, prefs[liquidKey])
                assertEquals(style.name, prefs[navStyle])
                assertEquals(LiquidMode.Clear.stored, prefs[liquidMode])
                val migrated = prefs.asMap().toMap()
                migrateAppearance(prefs)
                assertEquals(migrated, prefs.asMap())
            }
        }
    }

    @Test
    fun missingLegacyNavigationContentStillDefaultsToIconOnly() {
        assertEquals(NavContent.IconOnly, decodeNavContent(null))
        assertEquals(NavContent.IconAndText, decodeNavContent(NavContent.IconAndText.name))
    }

    @Test
    fun uiStyleMigrationDoesNotResetProgressiveBlurPreference() {
        val blurKey = booleanPreferencesKey("top_bar_progressive_blur")
        val prefs = mutablePreferencesOf(
            stringPreferencesKey(TEST_UI_STYLE_KEY) to "OS3",
            blurKey to false,
        )

        migrateAppearance(prefs)

        assertEquals(false, prefs[blurKey])
        assertEquals(null, prefs[stringPreferencesKey(TEST_UI_STYLE_KEY)])
    }

    @Test
    fun retiringOnlyStyleKeysPreservesImplicitLegacyIconOnly() {
        val style = stringPreferencesKey(TEST_UI_STYLE_KEY)
        val upgraded = booleanPreferencesKey("ui_style_os4_upgrade_done")
        val content = stringPreferencesKey(TEST_NAV_CONTENT_KEY)
        for (oldStyle in listOf("OS3", "OS4", "Unknown")) {
            val prefs = mutablePreferencesOf(style to oldStyle, upgraded to true)
            migrateAppearance(prefs)
            assertEquals(null, prefs[style])
            assertEquals(null, prefs[upgraded])
            assertEquals(NavContent.IconOnly.name, prefs[content])
            val migrated = prefs.asMap().toMap()
            migrateAppearance(prefs)
            assertEquals(migrated, prefs.asMap())
        }
    }

    @Test
    fun migrationPreservesImplicitLiquidLabelDefault() {
        val content = stringPreferencesKey(TEST_NAV_CONTENT_KEY)
        val nav = stringPreferencesKey(TEST_NAV_STYLE_KEY)
        val prefs = mutablePreferencesOf(
            stringPreferencesKey(TEST_UI_STYLE_KEY) to "OS3",
            nav to NavStyle.Liquid.name,
        )
        migrateAppearance(prefs)
        assertEquals(NavContent.IconAndText.name, prefs[content])
        assertEquals(NavStyle.Liquid.name, prefs[nav])
    }

}

class AppearanceNavigationTest {
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
            decodeNavContent(TEST_UNKNOWN_INPUT),
        )
        assertEquals(
            NavContent.IconAndText,
            decodeNavContent("IconAndText"),
        )
    }

    @Test
    fun allNavigationStylesShareTheSameContentChoice() {
        for (content in NavContent.entries) {
            val appearance = Appearance(navContent = content)
            NavStyle.entries.forEach { style ->
                assertEquals(content, appearance.copy(navStyle = style).navContent)
            }
        }
    }

    @Test
    fun balancedLiquidModeReadsLegacyValuesAndRoundTrips() {
        assertEquals(listOf(LiquidMode.Blur, LiquidMode.Balanced, LiquidMode.Clear), LiquidMode.entries)
        assertEquals(LiquidMode.Balanced, Appearance().liquidMode)
        assertEquals(LiquidMode.Balanced, decodeLiquidMode(null))
        assertEquals(LiquidMode.Balanced, decodeLiquidMode(TEST_UNKNOWN_INPUT))
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

}

class AppearanceThemeModeTest {
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
                storedMode = TEST_UNKNOWN_INPUT,
                storedDynamicColorEnabled = null,
            )

        assertEquals(ThemeMode.System, result.mode)
        assertFalse(result.dynamicColor)
    }
}
