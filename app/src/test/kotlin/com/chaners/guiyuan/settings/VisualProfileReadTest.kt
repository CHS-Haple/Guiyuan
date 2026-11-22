package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualProfileReadTest {
    private val layout = ContentLayout.BATTERY_CENTER

    @Test
    fun scopedValuesWinOverBaseValuesForAllTypes() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean("flag", false)
            .putFloat("scale", 1.25f)
            .putInt("weight", 15)
            .putString("style", "base")
            .putBoolean(visualProfileKey(layout, "flag"), true)
            .putFloat(visualProfileKey(layout, "scale"), 0.75f)
            .putInt(visualProfileKey(layout, "weight"), 90)
            .putString(visualProfileKey(layout, "style"), "scoped")
            .apply()
        assertEquals(true, prefs.profileBoolean(layout, "flag", false))
        assertEquals(0.75f, prefs.profileFloat(layout, "scale", 0f), 0f)
        assertEquals(90, prefs.profileInt(layout, "weight", 0))
        assertEquals("scoped", prefs.profileString(layout, "style", null, legacyDualBank = false))
    }

    @Test
    fun missingScopedValuesReadBaseKeysForAllTypes() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean("flag", true)
            .putFloat("scale", 0.7f)
            .putInt("weight", 66)
            .putString("style", "base")
            .apply()
        assertEquals(true, prefs.profileBoolean(layout, "flag", false))
        assertEquals(0.7f, prefs.profileFloat(layout, "scale", 0f), 0f)
        assertEquals(66, prefs.profileInt(layout, "weight", 0))
        assertEquals("base", prefs.profileString(layout, "style", null, legacyDualBank = false))
    }

    @Test
    fun missingLegacyDualBankIgnoresBothBaseAndSharedValues() {
        val prefs = VisualTestPreferences()
        prefs.edit()
            .putBoolean("flag", true)
            .putFloat("scale", 0.7f)
            .putInt("weight", 66)
            .putString("style", "base")
            .putBoolean(visualProfileKey(layout, "flag"), true)
            .putFloat(visualProfileKey(layout, "scale"), 0.7f)
            .putInt(visualProfileKey(layout, "weight"), 66)
            .putString(visualProfileKey(layout, "style"), "shared")
            .apply()
        assertEquals(false, prefs.profileBoolean(layout, "flag", false, legacyDualBank = true))
        assertEquals(1f, prefs.profileFloat(layout, "scale", 1f, legacyDualBank = true), 0f)
        assertEquals(25, prefs.profileInt(layout, "weight", 25, legacyDualBank = true))
        assertEquals("default", prefs.profileString(layout, "style", "default", legacyDualBank = true))
    }

    @Test
    fun existingLegacyDualBankHasItsOwnPriorityForAllTypes() {
        val prefs = VisualTestPreferences()
        fun dualKey(base: String) = visualModeKey(true, visualProfileKey(layout, base))
        prefs.edit()
            .putBoolean(dualKey("flag"), true)
            .putFloat(dualKey("scale"), 0.85f)
            .putInt(dualKey("weight"), 75)
            .putString(dualKey("style"), "dual")
            .apply()
        assertEquals(true, prefs.profileBoolean(layout, "flag", false, legacyDualBank = true))
        assertEquals(0.85f, prefs.profileFloat(layout, "scale", 1f, legacyDualBank = true), 0f)
        assertEquals(75, prefs.profileInt(layout, "weight", 25, legacyDualBank = true))
        assertEquals("dual", prefs.profileString(layout, "style", null, legacyDualBank = true))
    }

    @Test
    fun missingKeysReturnDefaultsIncludingNullableString() {
        val prefs = VisualTestPreferences()
        assertEquals(true, prefs.profileBoolean(layout, "flag", true))
        assertEquals(1.5f, prefs.profileFloat(layout, "scale", 1.5f), 0f)
        assertEquals(42, prefs.profileInt(layout, "weight", 42))
        assertEquals(null, prefs.profileString(layout, "style", null, legacyDualBank = false))
        assertEquals("fallback", prefs.profileString(layout, "style", "fallback", legacyDualBank = false))
        assertEquals(null, prefs.profileString(layout, "style", null, legacyDualBank = true))
    }
}
