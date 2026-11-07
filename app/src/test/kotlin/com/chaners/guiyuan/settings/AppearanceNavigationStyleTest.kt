package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceNavigationStyleTest {
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
}
