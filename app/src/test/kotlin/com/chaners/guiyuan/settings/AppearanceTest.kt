package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {
    @Test
    fun defaultsPreserveExistingNavStyle() {
        val settings = Appearance()

        assertTrue(settings.navEnabled)
        assertEquals(NavStyle.Glass, settings.navStyle)
        assertEquals(NavContent.IconOnly, settings.navContent)
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
