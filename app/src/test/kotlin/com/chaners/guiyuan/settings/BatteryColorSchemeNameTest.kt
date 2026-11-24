package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val TEST_SCHEME_NAME = "Alpha Beta"

class BatteryColorSchemeNameTest {
    @Test
    fun customKeysRoundTripAndRejectBuiltIns() {
        assertEquals("custom:3", customSchemeKey(3))
        assertEquals(3, customSchemeId("custom:3"))
        assertEquals(3, customSchemeId("custom:+3"))
        assertNull(customSchemeId("custom:"))
        assertNull(customSchemeId("custom:3-extra"))
        assertNull(customSchemeId("Custom:3"))
        assertNull(customSchemeId(BATTERY_COLOR_SCHEME_HYPEROS_KEY))
    }
    @Test
    fun customSchemeNamesNormalizePastedControlAndBidiCharacters() {
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\nBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u2028Beta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u202EBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u200FBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u061CBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u2066Beta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\u206FBeta"))
        assertEquals(TEST_SCHEME_NAME, normalizeCustomSchemeName("Alpha\uD800Beta"))
        assertEquals("Rocket \uD83D\uDE80", normalizeCustomSchemeName("Rocket \uD83D\uDE80"))
    }

    @Test
    fun customSchemeNameLimitIsSharedAndUnicodeCodePointSafe() {
        assertEquals(
            "123456789012345678901234",
            limitCustomSchemeName("1234567890123456789012345"),
        )
        val emoji = "\uD83D\uDE80"
        val twentyFourEmoji = emoji.repeat(BATTERY_COLOR_SCHEME_NAME_MAX_CODE_POINTS)
        assertEquals(
            twentyFourEmoji,
            limitCustomSchemeName(twentyFourEmoji + emoji),
        )
        assertEquals(
            "Custom style",
            normalizeCustomSchemeName("  Custom style  "),
        )
    }
}
