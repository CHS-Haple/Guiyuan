package com.chaners.guiyuan.settings

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceFlowTest {
    @Test
    fun eachCollectorReceivesInitialSnapshotAfterRegistrationAndUnregisters() = runBlocking {
        val prefs = VisualTestPreferences()
        val events = Channel<Int>(Channel.UNLIMITED)
        val flow = observePreferenceChanges(prefs, { it == "selected" }) {
            assertTrue(prefs.listenerCount > 0)
            prefs.getInt("selected", 0)
        }
        val first = launch { flow.collect { events.send(it) } }
        val second = launch { flow.collect { events.send(it) } }
        try {
            assertEquals(0, withTimeout(2_000) { events.receive() })
            assertEquals(0, withTimeout(2_000) { events.receive() })
            assertEquals(2, prefs.listenerCount)
        } finally {
            first.cancelAndJoin()
            assertEquals(1, prefs.listenerCount)
            second.cancelAndJoin()
            assertEquals(0, prefs.listenerCount)
        }
    }

    @Test
    fun unrelatedAndNullKeysAreIgnoredWhileRepeatedSnapshotsAreSuppressed() = runBlocking {
        val prefs = VisualTestPreferences()
        val events = Channel<Int>(Channel.UNLIMITED)
        var reads = 0
        val collector = launch {
            observePreferenceChanges(
                preferences = prefs,
                matchesKey = { it == "selected" },
                snapshot = {
                    reads++
                    prefs.getInt("selected", 0)
                },
            ).collect { events.send(it) }
        }
        try {
            assertEquals(0, withTimeout(2_000) { events.receive() })
            assertEquals(1, reads)

            prefs.edit().putInt("unrelated", 7).commit()
            prefs.notifyPreferenceChanged(null)
            assertEquals(1, reads)

            prefs.edit().putInt("selected", 0).commit()
            assertEquals(2, reads)
            prefs.edit().putInt("selected", 1).commit()
            assertEquals(1, withTimeout(2_000) { events.receive() })
            assertEquals(3, reads)

            prefs.notifyPreferenceChanged("selected")
            assertEquals(4, reads)
            prefs.edit().putInt("selected", 2).commit()
            assertEquals(2, withTimeout(2_000) { events.receive() })
            assertEquals(5, reads)
            assertTrue(events.tryReceive().isFailure)
        } finally {
            collector.cancelAndJoin()
            assertEquals(0, prefs.listenerCount)
        }
        prefs.edit().putInt("selected", 3).commit()
        assertEquals(5, reads)
    }
}
