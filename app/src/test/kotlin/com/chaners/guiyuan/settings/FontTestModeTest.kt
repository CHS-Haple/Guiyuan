package com.chaners.guiyuan.settings

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontTestModeTest {
    @Test
    fun labSettingIsB1977OnlyAndMirrorsWithoutBackupField() {
        assertTrue(FontTestMode.available)
        val local = VisualTestPreferences()
        val remote = VisualTestPreferences()
        local.edit().putString(FONT_METHOD_LAB_KEY, FontTestMode.SETTINGS.persistedValue).apply()
        assertTrue(isVisualPreferenceKey(FONT_METHOD_LAB_KEY))
        assertFalse(isBackupVisualKey(FONT_METHOD_LAB_KEY))
        val visual = local.readVisualCfg()
        assertEquals(FontTestMode.SETTINGS, visual.fontTestMode)
        remote.edit().putVisualCfg(visual)
            .putString(FONT_METHOD_LAB_KEY, visual.fontTestMode.persistedValue).apply()
        assertEquals(FontTestMode.SETTINGS, remote.readVisualCfg().fontTestMode)
        assertFalse(Json.encodeToString(VisualCfg.serializer(), visual).contains("fontTestMode"))
    }

    @Test
    fun invalidMethodFallsBackToAutomatic() {
        assertEquals(FontTestMode.AUTO, FontTestMode.fromStored(null))
        assertEquals(FontTestMode.AUTO, FontTestMode.fromStored("not-supported"))
        assertEquals(FontTestMode.NATIVE, FontTestMode.fromStored("native"))
    }
}
