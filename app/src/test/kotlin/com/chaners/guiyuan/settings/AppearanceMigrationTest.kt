package com.chaners.guiyuan.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceMigrationTest {
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
    fun existingNavigationLabelPreferencesRemainSeparateAcrossMigration() {
        val regularKey = stringPreferencesKey("floating_navigation_content")
        val liquidKey = stringPreferencesKey("floating_navigation_liquid_content")
        val prefs = mutablePreferencesOf(
            regularKey to NavContent.IconOnly.name,
            liquidKey to NavContent.IconAndText.name,
        )

        migrateOs4Appearance(prefs)

        assertEquals(NavContent.IconOnly.name, prefs[regularKey])
        assertEquals(NavContent.IconAndText.name, prefs[liquidKey])
        assertEquals(NavContent.IconOnly, decodeNavContent(prefs[regularKey]))
        assertEquals(NavContent.IconAndText, decodeNavContent(prefs[liquidKey], NavContent.IconAndText))
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
}
