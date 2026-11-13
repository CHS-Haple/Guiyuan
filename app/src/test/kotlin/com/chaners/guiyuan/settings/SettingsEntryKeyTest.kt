package com.chaners.guiyuan.settings

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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

    @Test
    fun clearingPreferencesRestoresObservedEntryDefault() = runBlocking {
        val preferences = VisualTestPreferences()
        preferences.edit().putInt(SETTINGS_ENTRY_KEY, SettingsEntry.Bottom.ordinal).commit()
        val updates = Channel<SettingsEntry>(Channel.UNLIMITED)
        val job = launch {
            observePreferenceChanges(preferences, ::isSettingsEntryKey) {
                SettingsEntry.from(preferences.getInt(SETTINGS_ENTRY_KEY, 0))
            }.collect { updates.send(it) }
        }
        try {
            assertEquals(SettingsEntry.Bottom, withTimeout(2_000) { updates.receive() })
            preferences.edit().putInt("unrelated", 1).commit()
            assertTrue(updates.tryReceive().isFailure)
            preferences.edit().clear().commit()
            assertEquals(SettingsEntry.Disabled, withTimeout(2_000) { updates.receive() })
        } finally {
            job.cancelAndJoin()
            assertEquals(0, preferences.listenerCount)
        }
    }
}
