package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

private const val FLAG_KEY = "flag"
private const val SCALE_KEY = "scale"
private const val WEIGHT_KEY = "weight"
private const val STYLE_KEY = "style"

class VisualProfileReadTest {
    private val layout = ContentLayout.BATTERY_CENTER

    @Test
    fun scopedValuesWinOverBaseValuesForAllTypes() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean(FLAG_KEY, false)
            .putFloat(SCALE_KEY, 1.25f)
            .putInt(WEIGHT_KEY, 15)
            .putString(STYLE_KEY, "base")
            .putBoolean(visualProfileKey(layout, FLAG_KEY), true)
            .putFloat(visualProfileKey(layout, SCALE_KEY), 0.75f)
            .putInt(visualProfileKey(layout, WEIGHT_KEY), 90)
            .putString(visualProfileKey(layout, STYLE_KEY), "scoped")
            .apply()
        assertEquals(true, prefs.profileBoolean(layout, FLAG_KEY, false))
        assertEquals(0.75f, prefs.profileFloat(layout, SCALE_KEY, 0f), 0f)
        assertEquals(90, prefs.profileInt(layout, WEIGHT_KEY, 0))
        assertEquals("scoped", prefs.profileString(layout, STYLE_KEY, null, legacyDualBank = false))
    }

    @Test
    fun missingScopedValuesReadBaseKeysForAllTypes() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean(FLAG_KEY, true)
            .putFloat(SCALE_KEY, 0.7f)
            .putInt(WEIGHT_KEY, 66)
            .putString(STYLE_KEY, "base")
            .apply()
        assertEquals(true, prefs.profileBoolean(layout, FLAG_KEY, false))
        assertEquals(0.7f, prefs.profileFloat(layout, SCALE_KEY, 0f), 0f)
        assertEquals(66, prefs.profileInt(layout, WEIGHT_KEY, 0))
        assertEquals("base", prefs.profileString(layout, STYLE_KEY, null, legacyDualBank = false))
    }

    @Test
    fun missingLegacyDualBankIgnoresBothBaseAndSharedValues() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean(FLAG_KEY, true)
            .putFloat(SCALE_KEY, 0.7f)
            .putInt(WEIGHT_KEY, 66)
            .putString(STYLE_KEY, "base")
            .putBoolean(visualProfileKey(layout, FLAG_KEY), true)
            .putFloat(visualProfileKey(layout, SCALE_KEY), 0.7f)
            .putInt(visualProfileKey(layout, WEIGHT_KEY), 66)
            .putString(visualProfileKey(layout, STYLE_KEY), "shared")
            .apply()
        assertEquals(false, prefs.profileBoolean(layout, FLAG_KEY, false, legacyDualBank = true))
        assertEquals(1f, prefs.profileFloat(layout, SCALE_KEY, 1f, legacyDualBank = true), 0f)
        assertEquals(25, prefs.profileInt(layout, WEIGHT_KEY, 25, legacyDualBank = true))
        assertEquals("default", prefs.profileString(layout, STYLE_KEY, "default", legacyDualBank = true))
    }

    @Test
    fun existingLegacyDualBankHasItsOwnPriorityForAllTypes() {
        val prefs = VisualTestPreferences()
        fun dualKey(base: String) = visualModeKey(true, visualProfileKey(layout, base))
        prefs.edit()
            .putBoolean(dualKey(FLAG_KEY), true)
            .putFloat(dualKey(SCALE_KEY), 0.85f)
            .putInt(dualKey(WEIGHT_KEY), 75)
            .putString(dualKey(STYLE_KEY), "dual")
            .apply()
        assertEquals(true, prefs.profileBoolean(layout, FLAG_KEY, false, legacyDualBank = true))
        assertEquals(0.85f, prefs.profileFloat(layout, SCALE_KEY, 1f, legacyDualBank = true), 0f)
        assertEquals(75, prefs.profileInt(layout, WEIGHT_KEY, 25, legacyDualBank = true))
        assertEquals("dual", prefs.profileString(layout, STYLE_KEY, null, legacyDualBank = true))
    }

    @Test
    fun missingKeysReturnDefaultsIncludingNullableString() {
        val prefs = VisualTestPreferences()
        assertEquals(true, prefs.profileBoolean(layout, FLAG_KEY, true))
        assertEquals(1.5f, prefs.profileFloat(layout, SCALE_KEY, 1.5f), 0f)
        assertEquals(42, prefs.profileInt(layout, WEIGHT_KEY, 42))
        assertEquals(null, prefs.profileString(layout, STYLE_KEY, null, legacyDualBank = false))
        assertEquals("fallback", prefs.profileString(layout, STYLE_KEY, "fallback", legacyDualBank = false))
        assertEquals(null, prefs.profileString(layout, STYLE_KEY, null, legacyDualBank = true))
    }
}
