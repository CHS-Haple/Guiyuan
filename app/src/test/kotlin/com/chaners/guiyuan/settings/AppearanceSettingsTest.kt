package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun defaultsPreserveExistingFloatingNavStyle() {
        val settings = AppearanceSettings()

        assertTrue(settings.floatingNavigationBarEnabled)
        assertEquals(FloatingNavStyle.Glass, settings.floatingNavigationStyle)
        assertEquals(FloatingNavContent.IconOnly, settings.floatingNavigationContent)
    }

    @Test
    fun storedFloatingStyleWinsOverLegacyFlags() {
        val result =
            decodeFloatingNavStyle(
                storedStyle = "Blur",
                storedFloatingBlurEnabled = false,
                legacyBlurEnabled = false,
                legacyGlassEnabled = false,
            )

        assertEquals(FloatingNavStyle.Blur, result)
    }

    @Test
    fun blurFlagMigratesToGlassStyle() {
        assertEquals(
            FloatingNavStyle.Glass,
            decodeFloatingNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = true,
                legacyBlurEnabled = null,
                legacyGlassEnabled = null,
            ),
        )
        assertEquals(
            FloatingNavStyle.Standard,
            decodeFloatingNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = false,
                legacyBlurEnabled = null,
                legacyGlassEnabled = null,
            ),
        )
    }

    @Test
    fun legacyBlurFlagsPreserveStyles() {
        assertEquals(
            FloatingNavStyle.Blur,
            decodeFloatingNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = true,
                legacyGlassEnabled = false,
            ),
        )
        assertEquals(
            FloatingNavStyle.Glass,
            decodeFloatingNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = true,
                legacyGlassEnabled = true,
            ),
        )
        assertEquals(
            FloatingNavStyle.Standard,
            decodeFloatingNavStyle(
                storedStyle = null,
                storedFloatingBlurEnabled = null,
                legacyBlurEnabled = false,
                legacyGlassEnabled = true,
            ),
        )
    }

    @Test
    fun navContentDefaultsToIcons() {
        assertEquals(
            FloatingNavContent.IconOnly,
            decodeFloatingNavContent(null),
        )
        assertEquals(
            FloatingNavContent.IconOnly,
            decodeFloatingNavContent("Unknown"),
        )
        assertEquals(
            FloatingNavContent.IconAndText,
            decodeFloatingNavContent("IconAndText"),
        )
    }

    @Test
    fun dynamicModeMigratesToSystemTheme() {
        val result =
            decodeThemeSelection(
                storedMode = "Dynamic",
                storedDynamicColorEnabled = null,
            )

        assertEquals(AppThemeMode.System, result.mode)
        assertTrue(result.dynamicColorEnabled)
    }

    @Test
    fun explicitDynamicPrefWins() {
        val result =
            decodeThemeSelection(
                storedMode = "Dynamic",
                storedDynamicColorEnabled = false,
            )

        assertEquals(AppThemeMode.System, result.mode)
        assertFalse(result.dynamicColorEnabled)
    }

    @Test
    fun lightModeIgnoresDynamicColor() {
        val result =
            decodeThemeSelection(
                storedMode = "Light",
                storedDynamicColorEnabled = true,
            )

        assertEquals(AppThemeMode.Light, result.mode)
        assertTrue(result.dynamicColorEnabled)
    }

    @Test
    fun unknownStoredModeFallsBackToSystem() {
        val result =
            decodeThemeSelection(
                storedMode = "Unknown",
                storedDynamicColorEnabled = null,
            )

        assertEquals(AppThemeMode.System, result.mode)
        assertFalse(result.dynamicColorEnabled)
    }
}
