package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsEntryIconTest {
    @Test
    fun persistedStylesHaveStableDefaults() {
        assertEquals(SettingsEntryIcon.Rounded, SettingsEntryIcon.from(0))
        assertEquals(SettingsEntryIcon.Hollow, SettingsEntryIcon.from(1))
        assertEquals(SettingsEntryIcon.Rounded, SettingsEntryIcon.from(-1))
        assertEquals(SettingsEntryIcon.Rounded, SettingsEntryIcon.from(123))
    }
}
