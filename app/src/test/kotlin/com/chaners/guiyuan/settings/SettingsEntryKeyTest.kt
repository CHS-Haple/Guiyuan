package com.chaners.guiyuan.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsEntryKeyTest {
    @Test
    fun entryObserversIncludeClearButIgnoreUnrelatedChanges() {
        assertTrue(isSettingsEntryKey(SETTINGS_ENTRY_KEY))
        assertTrue(isSettingsEntryKey(SETTINGS_ENTRY_ICON_KEY))
        assertTrue(isSettingsEntryKey(null))
        assertFalse(isSettingsEntryKey("unrelated"))
        assertFalse(isSettingsEntryKey(""))
    }
}
